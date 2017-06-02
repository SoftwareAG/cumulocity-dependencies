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

package io.moquette.spi.persistence;

import static io.moquette.BrokerConstants.AUTOSAVE_INTERVAL_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.moquette.proto.MQTTException;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore implements IStore {

    private MapDBMessagesStore msgStore;

    private MapDBSessionsStore sessionsStore;

    /**
     * This is a DTO used to persist minimal status (clean session and activation status) of
     * a session.
     */
    public static class PersistentSession implements Serializable {
        public final boolean cleanSession;

        public PersistentSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private DB m_db;

    private final String m_storePath;

    private final int m_autosaveInterval; // in seconds

    private volatile boolean initialized = false;

    protected final ScheduledExecutorService m_scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("moquette-mapdb-%d").build());

    public MapDBPersistentStore(IConfig props) {
        this.m_storePath = props.getProperty(PERSISTENT_STORE_PROPERTY_NAME, "");
        this.m_autosaveInterval = Integer.parseInt(props.getProperty(AUTOSAVE_INTERVAL_PROPERTY_NAME, "30"));
    }

    /**
     * Factory method to create message store backed by MapDB
     */
    public IMessagesStore messagesStore() {
        //TODO check m_db is valid and
        return msgStore;
    }


    public ISessionsStore sessionsStore(IMessagesStore msgStore) {
        return sessionsStore;
    }

    public void initStore() {
        if (initialized)
            return;
        if (m_storePath == null || m_storePath.isEmpty()) {
            m_db = DBMaker
                    .newMemoryDB()
                    .make();
        } else {
            File tmpFile;
            try {
                tmpFile = new File(m_storePath);
                boolean fileNewlyCreated = tmpFile.createNewFile();
                LOG.info("Starting with {} [{}] db file", fileNewlyCreated ? "fresh" : "existing", m_storePath);
            } catch (IOException ex) {
                LOG.error(null, ex);
                throw new MQTTException("Can't create temp file for subscriptions storage [" + m_storePath + "]", ex);
            }
            m_db = DBMaker
                    .newFileDB(tmpFile)
                    .make();
        }
        m_scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    sessionsStore.cleanup();
                } catch (Exception ex) {
                    LOG.warn("Garbage collection on persistent storage failed ", ex);
                }
                try {
                    m_db.commit();
                } catch (Exception ex) {
                    LOG.warn("Save to persistent storage failed ", ex);
                }
            }
        }, this.m_autosaveInterval, this.m_autosaveInterval, TimeUnit.SECONDS);

        msgStore = new MapDBMessagesStore(m_db);
        msgStore.initStore();
        sessionsStore = new MapDBSessionsStore(m_db, msgStore);
        sessionsStore.initStore();
        initialized = true;
    }

    public void close() {
        if (this.m_db.isClosed()) {
            LOG.debug("already closed");
            return;
        }
        this.m_db.commit();
        //LOG.debug("persisted subscriptions {}", m_persistentSubscriptions);
        this.m_db.close();
        LOG.debug("closed disk storage");
        this.m_scheduler.shutdown();
        LOG.debug("Persistence commit scheduler is shutdown");
    }
}
