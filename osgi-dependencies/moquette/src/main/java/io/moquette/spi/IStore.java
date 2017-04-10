package io.moquette.spi;

import java.io.Closeable;

public interface IStore extends Closeable{
    void initStore();

    IMessagesStore messagesStore();

    ISessionsStore sessionsStore(IMessagesStore messagesStore);

    void close();
}
