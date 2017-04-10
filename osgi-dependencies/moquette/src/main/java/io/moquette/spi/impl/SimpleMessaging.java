/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.spi.impl;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import io.moquette.BrokerConstants;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import io.moquette.spi.ServiceLocator.ServiceLookup;
import io.moquette.spi.impl.security.*;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.IMessagingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Singleton class that orchestrate the execution of the protocol.
 *
 * It's main responsibility is instantiate the ProtocolProcessor.
 *
 * @author andrea
 */
public class SimpleMessaging {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessaging.class);

    private SubscriptionsStore subscriptions;

    private IStore m_mapStorage;

    private BrokerInterceptor m_interceptor;

    private static SimpleMessaging INSTANCE;

    private ProtocolProcessor m_processor;

    private SimpleMessaging() {
    }

    public static SimpleMessaging getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleMessaging();
        }
        return INSTANCE;
    }

    /**
     * Initialize the processing part of the broker.
     * @param props the properties carrier where some props like port end host could be loaded.
     *              For the full list check of configurable properties check moquette.conf file.
     * @param embeddedObservers a list of callbacks to be notified of certain events inside the broker.
     *                          Could be empty list of null.
     * */
    public ProtocolProcessor init(IConfig props, List<? extends InterceptHandler> embeddedObservers, ServiceLookup serviceLocator) {
        if (m_processor == null) {
            m_processor = serviceLocator.lookup(ProtocolProcessor.class);
        }
        subscriptions = serviceLocator.lookup(SubscriptionsStore.class);

        m_mapStorage = serviceLocator.lookup(IStore.class);
        m_mapStorage.initStore();
        IMessagesStore messagesStore = m_mapStorage.messagesStore();
        ISessionsStore sessionsStore = m_mapStorage.sessionsStore(messagesStore);

        List<InterceptHandler> observers = new ArrayList<>(embeddedObservers);
        String interceptorClassName = props.getProperty("intercept.handler");
        if (interceptorClassName != null && !interceptorClassName.isEmpty()) {
            try {
                InterceptHandler handler = serviceLocator.lookup(InterceptHandler.class, this.<InterceptHandler>load(interceptorClassName));
                observers.add(handler);
            } catch (Throwable ex) {
                LOG.error("Can't load the intercept handler {}", ex);
            }
        }
        m_interceptor = new BrokerInterceptor(observers);

        subscriptions.init(sessionsStore);

        String configPath = System.getProperty("moquette.path", null);
        String authenticatorClassName = props.getProperty(BrokerConstants.AUTHENTICATOR_CLASS_NAME, "");
        IAuthenticator authenticator = null;
        if (!authenticatorClassName.isEmpty()) {
            authenticator = serviceLocator.lookup(IAuthenticator.class, this.<IAuthenticator>load(authenticatorClassName));
            LOG.info("Loaded custom authenticator {}", authenticatorClassName);
        }

        if (authenticator == null) {
            String passwdPath = props.getProperty(BrokerConstants.PASSWORD_FILE_PROPERTY_NAME, "");
            if (passwdPath.isEmpty()) {
                try {
                    authenticator = serviceLocator.lookup(IAuthenticator.class);
                } catch (Exception ex) {
                    authenticator = serviceLocator.lookup(IAuthenticator.class, AcceptAllAuthenticator.class);
                }
            } else {
                authenticator = new FileAuthenticator(configPath, passwdPath);
            }
        }
        IAuthorizator authorizator = null;
        String authorizatorClassName = props.getProperty(BrokerConstants.AUTHORIZATOR_CLASS_NAME, "");
        if (!authorizatorClassName.isEmpty()) {
            authorizator = serviceLocator.lookup(IAuthorizator.class, this.<IAuthorizator>load(authorizatorClassName));
            LOG.info("Loaded custom authorizator {}", authorizatorClassName);
        }

        if (authorizator == null) {
            String aclFilePath = props.getProperty(BrokerConstants.ACL_FILE_PROPERTY_NAME, "");
            if (aclFilePath != null && !aclFilePath.isEmpty()) {
                authorizator = new DenyAllAuthorizator();
                File aclFile = new File(configPath, aclFilePath);
                try {
                    authorizator = ACLFileParser.parse(aclFile);
                } catch (ParseException pex) {
                    LOG.error(String.format("Format error in parsing acl file %s", aclFile), pex);
                }
                LOG.info("Using acl file defined at path {}", aclFilePath);
            } else {
                try {
                    authorizator = serviceLocator.lookup(IAuthorizator.class);
                } catch (Exception ex) {
                    authorizator = serviceLocator.lookup(IAuthorizator.class, PermitAllAuthorizator.class);
                }
                LOG.info("Starting without ACL definition");
            }

        }

        boolean allowAnonymous = Boolean.parseBoolean(props.getProperty(BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME, "true"));
        m_processor.init(subscriptions,
            messagesStore,
            sessionsStore,
            authenticator,
            serviceLocator.lookup(IMessagingPolicy.class),
            allowAnonymous,
            authorizator,
            m_interceptor);
        return m_processor;
    }

    private <T> Class<T> load(String className) {
        try {
            return (Class<T>) this
                       .getClass()
                       .getClassLoader()
                       .loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        this.m_mapStorage.close();
    }
}
