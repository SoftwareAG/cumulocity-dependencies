package io.moquette.spi.security;

import io.moquette.proto.messages.PublishMessage;
import io.moquette.proto.messages.SubscribeMessage;
import io.moquette.server.netty.ServerChannel;
import io.moquette.spi.IMessagesStore;
import io.netty.channel.Channel;

public interface IMessagingPolicy {

    boolean canSubscribe(ServerChannel channel, SubscribeMessage.Couple msg);
    
    boolean handleMessageInService(ServerChannel channel, PublishMessage msg);
    
    boolean handleMessageInService(ServerChannel channel, IMessagesStore.StoredMessage msg);
}
