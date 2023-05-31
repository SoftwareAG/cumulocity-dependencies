package org.apache.pulsar.client.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;
import org.apache.pulsar.common.util.FutureUtil;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkArgument;

public class CustomProducerBuilder<T> extends ProducerBuilderImpl<T> {

    public CustomProducerBuilder(PulsarClientImpl client, Schema<T> schema) {
        super(client, schema);
    }

    @Override
    public CompletableFuture<Producer<T>> createAsync() {
        // config validation
        checkArgument(!(getConf().isBatchingEnabled() && getConf().isChunkingEnabled()),
                "Batching and chunking of messages can't be enabled together");
        if (getConf().getTopicName() == null) {
            return FutureUtil
                    .failedFuture(new IllegalArgumentException("Topic name must be set on the producer builder"));
        }

        try {
            setMessageRoutingMode();
        } catch (PulsarClientException pce) {
            return FutureUtil.failedFuture(pce);
        }

        return getInterceptorList() == null || getInterceptorList().size() == 0 ?
                getClient().createProducerAsync(getConf(), getSchema(), null) :
                getClient().createProducerAsync(getConf(), getSchema(), new ProducerInterceptors(getInterceptorList()));
    }

    // copied the method because it is "private"
    private void setMessageRoutingMode() throws PulsarClientException {
        if(getConf().getMessageRoutingMode() == null && getConf().getCustomMessageRouter() == null) {
            messageRoutingMode(MessageRoutingMode.RoundRobinPartition);
        } else if(getConf().getMessageRoutingMode() == null && getConf().getCustomMessageRouter() != null) {
            messageRoutingMode(MessageRoutingMode.CustomPartition);
        } else if((getConf().getMessageRoutingMode() == MessageRoutingMode.CustomPartition
                && getConf().getCustomMessageRouter() == null)
                || (getConf().getMessageRoutingMode() != MessageRoutingMode.CustomPartition
                && getConf().getCustomMessageRouter() != null)) {
            throw new PulsarClientException("When 'messageRouter' is set, 'messageRoutingMode' " +
                    "should be set as " + MessageRoutingMode.CustomPartition);
        }
    }

    @Slf4j
    public static class CustomProducerImpl<T> extends ProducerImpl<T> {
        public CustomProducerImpl(PulsarClientImpl client, String topic, ProducerConfigurationData conf, CompletableFuture<Producer<T>> producerCreatedFuture, int partitionIndex, Schema<T> schema, ProducerInterceptors interceptors) {
            super(client, topic, conf, producerCreatedFuture, partitionIndex, schema, interceptors);
        }

        @SneakyThrows
        @Override
        void reconnectLater(Throwable exception) {
            log.info("\n\t########## {} reconnectLater", getClass().getSimpleName(), exception);
            if (exception instanceof PulsarClientException.ProducerBlockedQuotaExceededError) {
                producerCreatedFuture.completeExceptionally(exception);
//                closeProducerTasks(); don't need this, it is effectively also called when producerCreatedFuture is completed exceptionally
                setState(State.Failed);
                getClient().cleanupProducer(this);
                return;
            } else if (exception != null) {
                log.warn("Encountered pulsar exception");
            }
            super.reconnectLater(exception);
        }

        @Override
        public CompletableFuture<Void> closeAsync() {
            log.info("\n\t########## {} closeAsync() called", getClass().getSimpleName());
            return super.closeAsync();
        }

        @Override
        public void close() throws PulsarClientException {
            StopWatch stopWatch = StopWatch.createStarted();
            super.close();
            stopWatch.stop();
            log.info("\n\t########## {} close() called, took {}", getClass().getSimpleName(), stopWatch.formatTime());
        }
    }
}
