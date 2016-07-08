package io.moquette.spi.security;

import io.moquette.proto.messages.PublishMessage;
import io.moquette.proto.messages.SubscribeMessage;
import io.moquette.spi.IMessagesStore;
import io.moquette.server.ServerChannel;

public interface IMessagingPolicy {

    boolean canSubscribe(ServerChannel channel, SubscribeMessage.Couple msg);
    
    boolean handleMessageInService(ServerChannel channel, PublishMessage msg);
    
    boolean handleMessageInService(ServerChannel channel, IMessagesStore.StoredMessage msg);
}
