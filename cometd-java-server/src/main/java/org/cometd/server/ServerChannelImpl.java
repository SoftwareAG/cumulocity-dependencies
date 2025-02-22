/*
 * Copyright (c) 2008-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cometd.server;

import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.*;
import org.eclipse.jetty.util.AttributesMap;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.component.Dumpable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerChannelImpl implements ServerChannel, Dumpable {
    private static final Logger _logger = LoggerFactory.getLogger(ServerChannel.class);
    private final BayeuxServerImpl _bayeux;
    private final ChannelId _id;
    private final AttributesMap _attributes = new AttributesMap();
    private final Set<ServerSession> _subscribers = new CopyOnWriteArraySet<>();
    private final List<ServerChannelListener> _listeners = new CopyOnWriteArrayList<>();
    private final List<Authorizer> _authorizers = new CopyOnWriteArrayList<>();
    private final CountDownLatch _initialized = new CountDownLatch(1);
    private final AtomicInteger _sweeperPasses = new AtomicInteger();
    private boolean _lazy;
    private long _lazyTimeout = -1;
    private boolean _persistent;

    protected ServerChannelImpl(BayeuxServerImpl bayeux, ChannelId id) {
        _bayeux = bayeux;
        _id = id;
        setPersistent(!isBroadcast());
    }

    /**
     * Waits for the channel to be {@link #initialized() initialized}, to avoid
     * that channels are returned to applications in a half-initialized state,
     * in particular before {@link Initializer}s have run.
     *
     * @see BayeuxServerImpl#createChannelIfAbsent(String, Initializer...)
     */
    void waitForInitialized() {
        try {
            if (!_initialized.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Not Initialized: " + this);
            }
        } catch (InterruptedException x) {
            throw new IllegalStateException("Initialization interrupted: " + this, x);
        }
    }

    /**
     * Marks this channel as initialized, notifying other threads that may
     * {@link #waitForInitialized() wait for initialization}.
     */
    void initialized() {
        resetSweeperPasses();
        _initialized.countDown();
    }

    void resetSweeperPasses() {
        _sweeperPasses.set(0);
    }

    public boolean subscribe(ServerSession session) {
        return subscribe((ServerSessionImpl)session, null);
    }

    protected boolean subscribe(ServerSessionImpl session, ServerMessage message) {
        if (!session.isHandshook()) {
            return false;
        }

        // Maintain backward compatibility by allowing subscriptions
        // to service channels to be a no-operation, but succeed
        if (isService()) {
            return true;
        }
        if (isMeta()) {
            return false;
        }

        resetSweeperPasses();

        if (_subscribers.add(session)) {
            session.subscribedTo(this);
            for (ServerChannelListener listener : _listeners) {
                if (listener instanceof SubscriptionListener) {
                    notifySubscribed((SubscriptionListener)listener, session, this, message);
                }
            }
            for (BayeuxServer.BayeuxServerListener listener : _bayeux.getListeners()) {
                if (listener instanceof BayeuxServer.SubscriptionListener) {
                    notifySubscribed((BayeuxServer.SubscriptionListener)listener, session, this, message);
                }
            }
        }

        return true;
    }

    private void notifySubscribed(SubscriptionListener listener, ServerSession session, ServerChannel channel, ServerMessage message) {
        try {
            listener.subscribed(session, channel, message);
        } catch (Throwable x) {
            _logger.info("Exception while invoking listener " + listener, x);
        }
    }

    private void notifySubscribed(BayeuxServer.SubscriptionListener listener, ServerSession session, ServerChannel channel, ServerMessage message) {
        try {
            listener.subscribed(session, channel, message);
        } catch (Throwable x) {
            _logger.info("Exception while invoking listener " + listener, x);
        }
    }

    public boolean unsubscribe(ServerSession session) {
        return unsubscribe((ServerSessionImpl)session, null);
    }

    protected boolean unsubscribe(ServerSessionImpl session, ServerMessage message) {
        // The unsubscription may arrive when the session
        // is already disconnected; unsubscribe in any case

        // Subscriptions to service channels are allowed but
        // are a no-operation, so be symmetric here
        if (isService()) {
            return true;
        }
        if (isMeta()) {
            return false;
        }

        if (_subscribers.remove(session)) {
            session.unsubscribedFrom(this);
            for (ServerChannelListener listener : _listeners) {
                if (listener instanceof SubscriptionListener) {
                    notifyUnsubscribed((SubscriptionListener)listener, session, this, message);
                }
            }
            for (BayeuxServer.BayeuxServerListener listener : _bayeux.getListeners()) {
                if (listener instanceof BayeuxServer.SubscriptionListener) {
                    notifyUnsubscribed((BayeuxServer.SubscriptionListener)listener, session, this, message);
                }
            }
        }

        return true;
    }

    private void notifyUnsubscribed(BayeuxServer.SubscriptionListener listener, ServerSession session, ServerChannel channel, ServerMessage message) {
        try {
            listener.unsubscribed(session, channel, message);
        } catch (Throwable x) {
            _logger.info("Exception while invoking listener " + listener, x);
        }
    }

    private void notifyUnsubscribed(SubscriptionListener listener, ServerSession session, ServerChannel channel, ServerMessage message) {
        try {
            listener.unsubscribed(session, channel, message);
        } catch (Throwable x) {
            _logger.info("Exception while invoking listener " + listener, x);
        }
    }

    public Set<ServerSession> getSubscribers() {
        return Collections.unmodifiableSet(subscribers());
    }

    public Set<ServerSession> subscribers() {
        return _subscribers;
    }

    public boolean isBroadcast() {
        return !isMeta() && !isService();
    }

    public boolean isDeepWild() {
        return _id.isDeepWild();
    }

    public boolean isLazy() {
        return _lazy;
    }

    public boolean isPersistent() {
        return _persistent;
    }

    public boolean isWild() {
        return _id.isWild();
    }

    public void setLazy(boolean lazy) {
        _lazy = lazy;
        if (!lazy) {
            _lazyTimeout = -1;
        }
    }

    public long getLazyTimeout() {
        return _lazyTimeout;
    }

    public void setLazyTimeout(long lazyTimeout) {
        _lazyTimeout = lazyTimeout;
        setLazy(lazyTimeout > 0);
    }

    public void setPersistent(boolean persistent) {
        resetSweeperPasses();
        _persistent = persistent;
    }

    public void addListener(ServerChannelListener listener) {
        resetSweeperPasses();
        _listeners.add(listener);
    }

    public void removeListener(ServerChannelListener listener) {
        _listeners.remove(listener);
    }

    public List<ServerChannelListener> getListeners() {
        return Collections.unmodifiableList(listeners());
    }

    protected List<ServerChannelListener> listeners() {
        return _listeners;
    }

    public ChannelId getChannelId() {
        return _id;
    }

    public String getId() {
        return _id.toString();
    }

    public boolean isMeta() {
        return _id.isMeta();
    }

    public boolean isService() {
        return _id.isService();
    }

    public void publish(Session from, ServerMessage.Mutable mutable) {
        if (isWild()) {
            throw new IllegalStateException("Wild publish");
        }

        ServerSessionImpl session = (from instanceof ServerSessionImpl)
                ? (ServerSessionImpl)from
                : ((from instanceof LocalSession) ? (ServerSessionImpl)((LocalSession)from).getServerSession() : null);

        if (ChannelId.isBroadcast(mutable.getChannel())) {
            // Do not leak the clientId to other subscribers
            // as we are now "sending" this message.
            mutable.setClientId(null);

            // Reset the messageId to avoid clashes with message-based transports such
            // as websocket whose clients may rely on the messageId to match request/responses.
            /** CUMULOCITY PATCH START **/
            //mutable.setId(null);
            /** CUMULOCITY PATCH END **/
        }

        if (mutable instanceof ServerMessageImpl) {
            ((ServerMessageImpl)mutable).setLocal(true);
        }

        if (_bayeux.extendSend(session, null, mutable)) {
            _bayeux.doPublish(session, this, mutable);
        }
    }

    public void publish(Session from, Object data) {
        ServerMessage.Mutable mutable = _bayeux.newMessage();
        mutable.setChannel(getId());
        if (from != null) {
            mutable.setClientId(from.getId());
        }
        mutable.setData(data);
        publish(from, mutable);
    }

    protected void sweep() {
        waitForInitialized();

        if (!_subscribers.isEmpty()) {
            for (ServerSession session : _subscribers) {
                if (!session.isHandshook()) {
                    unsubscribe(session);
                }
            }
        }

        if (isPersistent()) {
            return;
        }

        if (!_subscribers.isEmpty()) {
            return;
        }

        if (!_authorizers.isEmpty()) {
            return;
        }

        if (!_listeners.isEmpty()) {
            for (ServerChannelListener listener : _listeners) {
                if (!(listener instanceof ServerChannelListener.Weak)) {
                    return;
                }
            }
        }

        if (_sweeperPasses.incrementAndGet() < 3) {
            return;
        }

        remove();
    }

    public void remove() {
        if (_bayeux.removeServerChannel(this)) {
            for (ServerSession subscriber : _subscribers) {
                ((ServerSessionImpl)subscriber).unsubscribedFrom(this);
            }
            _subscribers.clear();
        }

        _listeners.clear();
    }

    public void setAttribute(String name, Object value) {
        _attributes.setAttribute(name, value);
    }

    public Object getAttribute(String name) {
        return _attributes.getAttribute(name);
    }

    public Set<String> getAttributeNames() {
        return _attributes.getAttributeNameSet();
    }

    public Object removeAttribute(String name) {
        Object old = getAttribute(name);
        _attributes.removeAttribute(name);
        return old;
    }

    public void addAuthorizer(Authorizer authorizer) {
        _authorizers.add(authorizer);
    }

    public void removeAuthorizer(Authorizer authorizer) {
        _authorizers.remove(authorizer);
    }

    public List<Authorizer> getAuthorizers() {
        return Collections.unmodifiableList(authorizers());
    }

    protected List<Authorizer> authorizers() {
        return _authorizers;
    }

    @Override
    public String dump() {
        return ContainerLifeCycle.dump(this);
    }

    @Override
    public void dump(Appendable out, String indent) throws IOException {
        ContainerLifeCycle.dumpObject(out, this);

        List<Dumpable> children = new ArrayList<>();

        children.add(new Dumpable() {
            @Override
            public String dump() {
                return null;
            }

            @Override
            public void dump(Appendable out, String indent) throws IOException {
                List<Authorizer> authorizers = getAuthorizers();
                ContainerLifeCycle.dumpObject(out, "authorizers: " + authorizers.size());
                if (_bayeux.isDetailedDump()) {
                    ContainerLifeCycle.dump(out, indent, authorizers);
                }
            }
        });

        children.add(new Dumpable() {
            @Override
            public String dump() {
                return null;
            }

            @Override
            public void dump(Appendable out, String indent) throws IOException {
                List<ServerChannelListener> listeners = getListeners();
                ContainerLifeCycle.dumpObject(out, "listeners: " + listeners.size());
                if (_bayeux.isDetailedDump()) {
                    ContainerLifeCycle.dump(out, indent, listeners);
                }
            }
        });

        children.add(new Dumpable() {
            @Override
            public String dump() {
                return null;
            }

            @Override
            public void dump(Appendable out, String indent) throws IOException {
                Set<ServerSession> subscribers = getSubscribers();
                ContainerLifeCycle.dumpObject(out, "subscribers: " + subscribers.size());
                if (_bayeux.isDetailedDump()) {
                    ContainerLifeCycle.dump(out, indent, subscribers);
                }
            }
        });

        ContainerLifeCycle.dump(out, indent, children);
    }

    @Override
    public String toString() {
        return _id.toString();
    }
}
