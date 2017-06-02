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

import java.util.*;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ClientSessionListener;
import io.moquette.spi.ClientSessionListener.FlightAcknowledged;
import io.moquette.spi.ClientSessionListener.SecondPhaseAcknowledged;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.persistence.MapDBPersistentStore.PersistentSession;
import lombok.extern.slf4j.Slf4j;
import org.mapdb.DB;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;

/**
 * ISessionsStore implementation backed by MapDB.
 *
 * @author andrea
 */
@Slf4j
class MapDBSessionsStore implements ISessionsStore {

    //maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, Map<Integer, String>> m_inflightStore;

    //map clientID <-> set of currently in flight packet identifiers
    private ConcurrentMap<String, Set<Integer>> m_inFlightIds;

    private ConcurrentMap<String, PersistentSession> m_persistentSessions;

    //maps clientID->[guid*], insertion order cares, it's queue
    private ConcurrentMap<String, List<String>> m_enqueuedStore;

    //maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, Map<Integer, String>> m_secondPhaseStore;

    private final DB m_db;

    private final MapDBMessagesStore m_messagesStore;

    private final ConcurrentMap<String, Collection<ClientSessionListener>> listeners = Maps.newConcurrentMap();

    MapDBSessionsStore(DB db, MapDBMessagesStore messagesStore) {
        m_db = db;
        m_messagesStore = messagesStore;
    }


    @Override
    public void initStore() {
        m_inflightStore = m_db.getHashMap("inflight");
        m_inFlightIds = m_db.getHashMap("inflightPacketIDs");
        m_persistentSessions = m_db.getHashMap("sessions");
        m_enqueuedStore = m_db.getHashMap("sessionQueue");
        m_secondPhaseStore = m_db.getHashMap("secondPhase");
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        log.debug("addNewSubscription invoked with subscription {}", newSubscription);
        final String clientID = newSubscription.getClientId();
        m_db
                .getHashMap("subscriptions_" + clientID)
                .put(newSubscription.getTopicFilter(), newSubscription);

        if (log.isTraceEnabled()) {
            log.trace("subscriptions_{}: {}", clientID, m_db.getHashMap("subscriptions_" + clientID));
        }
    }

    @Override
    public void removeSubscription(String topicFilter, String clientID) {
        log.debug("removeSubscription topic filter: {} for clientID: {}", topicFilter, clientID);
        if (!m_db.exists("subscriptions_" + clientID)) {
            return;
        }
        m_db.getHashMap("subscriptions_" + clientID)
                .remove(topicFilter);
    }

    @Override
    public void wipeSubscriptions(String clientID) {
        log.debug("wipeSubscriptions");
        if (log.isTraceEnabled()) {
            log.trace("Subscription pre wipe: subscriptions_{}: {}", clientID, m_db.getHashMap("subscriptions_" + clientID));
        }
        m_db.delete("subscriptions_" + clientID);
        if (log.isTraceEnabled()) {
            log.trace("Subscription post wipe: subscriptions_{}: {}", clientID, m_db.getHashMap("subscriptions_" + clientID));
        }
    }

    @Override
    public List<ClientTopicCouple> listAllSubscriptions() {
        final List<ClientTopicCouple> allSubscriptions = new ArrayList<>();
        for (String clientID : m_persistentSessions.keySet()) {
            ConcurrentMap<String, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + clientID);
            for (String topicFilter : clientSubscriptions.keySet()) {
                allSubscriptions.add(new ClientTopicCouple(clientID, topicFilter));
            }
        }
        log.debug("retrieveAllSubscriptions returning subs {}", allSubscriptions);
        return allSubscriptions;
    }

    @Override
    public Subscription getSubscription(ClientTopicCouple couple) {
        ConcurrentMap<String, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + couple.clientID);
        log.debug("subscriptions_{}: {}", couple.clientID, clientSubscriptions);
        return clientSubscriptions.get(couple.topicFilter);
    }

    @Override
    public boolean contains(String clientID) {
        return m_db.exists("subscriptions_" + clientID);
    }

    @Override
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        log.debug("createNewSession for client <{}> with clean flag <{}>", clientID, cleanSession);
        if (m_persistentSessions.containsKey(clientID)) {
            log.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        log.debug("clientID {} is a newcome, creating it's empty subscriptions set", clientID);
        m_persistentSessions.putIfAbsent(clientID, new PersistentSession(cleanSession));
        return new ClientSession(clientID, m_messagesStore, this, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        if (!m_persistentSessions.containsKey(clientID)) {
            return null;
        }

        PersistentSession storedSession = m_persistentSessions.get(clientID);
        return new ClientSession(clientID, m_messagesStore, this, storedSession.cleanSession);
    }

    @Override
    public void updateCleanStatus(String clientID, boolean cleanSession) {
        m_persistentSessions.put(clientID, new MapDBPersistentStore.PersistentSession(cleanSession));
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     */
    @Override
    public int nextPacketID(String clientID) {
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient == null) {
            int nextPacketId = 1;
            inFlightForClient = new HashSet<>();
            inFlightForClient.add(nextPacketId);
            this.m_inFlightIds.put(clientID, inFlightForClient);
            return nextPacketId;

        }

        int maxId = inFlightForClient.isEmpty() ? 0 : Collections.max(inFlightForClient);
        int nextPacketId = (maxId + 1) % 0xFFFF;
        inFlightForClient.add(nextPacketId);
        return nextPacketId;
    }

    @Override
    public void inFlightAck(String clientID, int messageID) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            log.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        String guid;
        synchronized (m) {
            guid = m.remove(messageID);
            m_inflightStore.put(clientID, m);

        }
        final FlightAcknowledged event = new FlightAcknowledged(sessionForClient(clientID), guid);
        notifyListeners(clientID, new EventNotifier() {
            @Override
            public void notify(ClientSessionListener listener) {
                listener.onAcknowledged(event);
            }
        });

        //remove from the ids store
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            synchronized (inFlightForClient) {
                inFlightForClient.remove(messageID);
                m_inFlightIds.put(clientID, inFlightForClient);
            }
        }
    }

    @Override
    public Collection<String> pendingAck(String clientID) {
        ConcurrentMap<Integer, String> messageGUIDMap = m_db.getHashMap(messageId2GuidsMapName(clientID));
        if (messageGUIDMap == null || messageGUIDMap.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(messageGUIDMap.values());
    }

    @Override
    public void inFlight(String clientID, int messageID, String guid) {
        Map<Integer, String> m = this.m_inflightStore.putIfAbsent(clientID, new HashMap<Integer, String>());
        synchronized (m) {
            m.put(messageID, guid);
            this.m_inflightStore.put(clientID, m);
        }
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID) {
        log.debug("acknowledging inflight clientID <{}> messageID {}", clientID, messageID);
        Map<Integer, String> m = this.m_inflightStore.putIfAbsent(clientID, new HashMap<Integer, String>());
        String guid;
        synchronized (m) {
            guid = m.remove(messageID);
            if (guid == null) return;
            m_inflightStore.put(clientID, m);
        }

        log.debug("Moving to second phase store");
        Map<Integer, String> messageIDs = m_secondPhaseStore.putIfAbsent(clientID, new HashMap<Integer, String>());
        synchronized (messageIDs) {
            messageIDs.put(messageID, guid);
            m_secondPhaseStore.put(clientID, messageIDs);
        }
    }

    @Override
    public void bindToDeliver(String guid, String clientID) {
        List<String> guids = m_enqueuedStore.putIfAbsent(clientID, new ArrayList<String>());
        synchronized (guids) {
            guids.add(guid);
            m_enqueuedStore.put(clientID, guids);
        }
    }

    @Override
    public Collection<String> enqueued(String clientID) {
        return m_enqueuedStore.putIfAbsent(clientID, new ArrayList<String>());
    }

    @Override
    public void removeEnqueued(String clientID, String guid) {
        List<String> guids = m_enqueuedStore.putIfAbsent(clientID, new ArrayList<String>());
        synchronized (guids) {
            guids.remove(guid);
            m_enqueuedStore.put(clientID, guids);
        }
    }

    @Override
    public void secondPhaseAcknowledged(String clientID, int messageID) {
        Map<Integer, String> messageIDs = m_secondPhaseStore.putIfAbsent(clientID, new HashMap<Integer, String>());
        String message;
        synchronized (messageIDs) {
            message = messageIDs.remove(messageID);
            m_secondPhaseStore.put(clientID, messageIDs);
        }
        final SecondPhaseAcknowledged event = new SecondPhaseAcknowledged(sessionForClient(clientID), message);
        notifyListeners(clientID, new EventNotifier() {
            @Override
            public void notify(ClientSessionListener listener) {
                listener.onAcknowledged(event);
            }
        });

    }

    @Override
    public String mapToGuid(String clientID, int messageID) {
        ConcurrentMap<Integer, String> messageIdToGuid = m_db.getHashMap(messageId2GuidsMapName(clientID));
        final String normal = messageIdToGuid.get(messageID);
        if (normal == null) {
            return m_secondPhaseStore.get(clientID).get(messageID);
        }
        return normal;
    }

    @Override
    public void dropQueue(String clientID) {
        m_enqueuedStore.remove(clientID);
    }

    static String messageId2GuidsMapName(String clientID) {
        return "guidsMapping_" + clientID;
    }

    @Override
    public void register(String clientID, ClientSessionListener listener) {
        sessionListenersFor(clientID).add(listener);
    }

    @Override
    public void unregister(String clientID, ClientSessionListener listener) {
        sessionListenersFor(clientID).remove(listener);
    }


    @Override
    public Collection<ClientSessionListener> sessionListenersFor(String clientID) {
        final Collection<ClientSessionListener> clientListeners = listeners.get(clientID);
        if (clientListeners == null) {
            listeners.putIfAbsent(clientID, Lists.<ClientSessionListener>newCopyOnWriteArrayList());
            return listeners.get(clientID);
        }
        return clientListeners;
    }

    private void notifyListeners(String clientId, EventNotifier eventNotifier) {
        for (ClientSessionListener listener : sessionListenersFor(clientId)) {
            try {
                eventNotifier.notify(listener);
            } catch (Exception ex) {
                log.debug("notify listener failed {} ", listener, ex);
            }
        }
    }

    public void cleanup() {
        m_messagesStore.dropMessagesNotIn(FluentIterable.from(m_enqueuedStore.values())
                .transformAndConcat(Functions.<Iterable<String>>identity())
                .append(FluentIterable.from(m_secondPhaseStore.values()).transformAndConcat(asValues()))
                .toSet());
    }

    private Function<Map<Integer, String>, Iterable<String>> asValues() {
        return new Function<Map<Integer, String>, Iterable<String>>() {
            @Override
            public Iterable<String> apply(Map<Integer, String> input) {
                return input.values();
            }
        };
    }

    interface EventNotifier {
        void notify(ClientSessionListener listener);
    }

}
