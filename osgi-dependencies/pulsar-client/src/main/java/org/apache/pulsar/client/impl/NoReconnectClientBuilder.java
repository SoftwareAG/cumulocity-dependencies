package org.apache.pulsar.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;

public class NoReconnectClientBuilder extends ClientBuilderImpl {

    public NoReconnectClientBuilder() {
        this(new ClientConfigurationData());
    }

    public NoReconnectClientBuilder(ClientConfigurationData conf) {
        this.conf = conf;
    }

    public static NoReconnectClientBuilder noReconnectClientBuilder() {
        return new NoReconnectClientBuilder();
    }

    @Override
    public PulsarClient build() throws PulsarClientException {
        if (StringUtils.isBlank(conf.getServiceUrl()) && conf.getServiceUrlProvider() == null) {
            throw new IllegalArgumentException("service URL or service URL provider needs to be specified on the ClientBuilder object.");
        }
        if (StringUtils.isNotBlank(conf.getServiceUrl()) && conf.getServiceUrlProvider() != null) {
            throw new IllegalArgumentException("Can only chose one way service URL or service URL provider.");
        }
        if (conf.getServiceUrlProvider() != null) {
            if (StringUtils.isBlank(conf.getServiceUrlProvider().getServiceUrl())) {
                throw new IllegalArgumentException("Cannot get service url from service url provider.");
            } else {
                conf.setServiceUrl(conf.getServiceUrlProvider().getServiceUrl());
            }
        }
        PulsarClient client = new NoReconnectClientImpl(conf);
        if (conf.getServiceUrlProvider() != null) {
            conf.getServiceUrlProvider().initialize(client);
        }
        return client;
    }

}

