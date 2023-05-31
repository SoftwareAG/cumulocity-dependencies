package org.apache.pulsar.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.conf.ClientConfigurationData;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;

import java.util.concurrent.CompletableFuture;

public class CustomClientBuilder extends ClientBuilderImpl {

    public CustomClientBuilder() {
        this(new ClientConfigurationData());
    }

    public CustomClientBuilder(ClientConfigurationData conf) {
        this.conf = conf;
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
        PulsarClient client = new CustomPulsarClientImpl(conf);
        if (conf.getServiceUrlProvider() != null) {
            conf.getServiceUrlProvider().initialize(client);
        }
        return client;
    }

    public static class CustomPulsarClientImpl extends PulsarClientImpl {

        public CustomPulsarClientImpl(ClientConfigurationData conf) throws PulsarClientException {
            super(conf);
        }

        public ProducerBuilder<byte[]> newProducer() {
            return new CustomProducerBuilder<>(this, Schema.BYTES);
        }

        public <T> ProducerBuilder<T> newProducer(Schema<T> schema) {
            return new CustomProducerBuilder<>(this, schema);
        }

        protected <T> ProducerImpl<T> newProducerImpl(String topic, int partitionIndex,
                                                      ProducerConfigurationData conf,
                                                      Schema<T> schema,
                                                      ProducerInterceptors interceptors,
                                                      CompletableFuture<Producer<T>> producerCreatedFuture) {
            return new CustomProducerBuilder.CustomProducerImpl<>(CustomPulsarClientImpl.this,
                    topic,
                    conf,
                    producerCreatedFuture,
                    partitionIndex,
                    schema,
                    interceptors);
        }
    }
}

