package io.moquette.spi.security;

import io.moquette.proto.messages.PublishMessage;
import io.moquette.proto.messages.SubscribeMessage;
import io.moquette.spi.IMessagesStore;
import io.netty.channel.Channel;

public interface IMessagingPolicy {

    boolean canSubscribe(Channel channel, SubscribeMessage.Couple msg);
    
    boolean handleMessageInService(Channel channel, PublishMessage msg);
    
    boolean handleMessageInService(Channel channel, IMessagesStore.StoredMessage msg);
}
