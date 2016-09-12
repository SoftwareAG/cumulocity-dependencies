package org.cometd.server.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerMessage.Mutable;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerSession.RemoveListener;
import org.cometd.server.AbstractServerTransport;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.ServerSessionImpl;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LongPollingTransport extends HttpTransport {

    public final static String PREFIX = "long-polling";

    public final static String AUTOBATCH_OPTION = "autoBatch";

    private final Logger log = LoggerFactory.getLogger(LongPollingTransport.class);

    private final Collection<LongPollScheduler> schedulers = new CopyOnWriteArrayList<LongPollScheduler>();

    private boolean _autoBatch;

    private Integer heartbeatMinutes;

    protected LongPollingTransport(BayeuxServerImpl bayeux, String name, Integer heartbeatMinutes) {
        super(bayeux, name);
        setOptionPrefix(PREFIX);
        this.heartbeatMinutes = heartbeatMinutes;
    }

    @Override
    protected void init() {
        super.init();
        _autoBatch = getOption(AUTOBATCH_OPTION, true);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!isResumed(request)) {
            log.debug("new request");
            boolean batch = false;
            ServerSessionImpl session = null;

            try {
                ServerMessage.Mutable[] messages = parseMessages(request);
                if (messages == null)
                    return;

                PrintWriter writer = null;

                for (ServerMessage.Mutable message : messages) {
                    boolean connect = isConnectChannel(message);

                    // Get the session from the message
                    String clientId = message.getClientId();
                    log.debug("recived message from {} >> {}", clientId, message.getJSON());
                    if (session == null || clientId != null && !clientId.equals(session.getId())) {
                        session = (ServerSessionImpl) getBayeux().getSession(clientId);
                        if (_autoBatch && !batch && session != null && !message.isMeta()) {
                            // start a batch to group all resulting messages into a single response.
                            batch = true;
                            session.startBatch();
                        }
                    } else if (!session.isHandshook()) {
                        batch = false;
                        session = null;
                    }

                    if (connect && session != null) {
                        session.setScheduler(null);
                    }

                    boolean wasConnected = session != null && session.isConnected();

                    ServerMessage.Mutable reply = bayeuxServerHandle(session, message);

                    if (reply != null) {
                        if (session == null) {
                            session = (ServerSessionImpl) getBayeux().getSession(reply.getClientId());
                        } else {
                            if (connect) {
                                try {
                                    if (!session.hasNonLazyMessages() && reply.isSuccessful()) {
                                        long timeout = session.calculateTimeout(getTimeout());
                                        if (timeout > 0 && wasConnected && session.isConnected()) {
                                            suspendRequest(request, response, session, timeout, reply);
                                            reply = null;
                                        }
                                    }
                                } finally {
                                    if (reply != null)
                                        writer = writeQueueForMetaConnect(request, response, session, writer);
                                }
                            } else {
                                if (!isMetaConnectDeliveryOnly() && !session.isMetaConnectDeliveryOnly()) {
                                    writer = writeQueue(request, response, session, writer);
                                }
                            }
                        }

                    }
                    if (reply != null) {
                        if (connect && session != null && session.isDisconnected()) {
                            reconnect(reply);
                        }

                        writer = sendReply(request, response, session, writer, reply);
                    }
                    message.setAssociated(null);
                }
                if (writer != null)
                    finishWrite(writer, session);
            } catch (ParseException x) {
                handleJSONParseException(request, response, x.getMessage(), x.getCause());
            } finally {
                flushSession(batch, session);
            }
        } else {
            log.debug("request resumed");
            sendMessages(request, response);
        }
    }

    private void flushSession(boolean batch, ServerSessionImpl session) {
        if (batch) {
            if (!session.endBatch() && isAlwaysFlushingAfterHandle())
                session.flush();
        } else if (session != null && isAlwaysFlushingAfterHandle()) {
            session.flush();
        }
    }

    private void sendMessages(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LongPollScheduler scheduler = (LongPollScheduler) request.getAttribute(LongPollScheduler.ATTRIBUTE);
        ServerSessionImpl session = scheduler.getSession();
        PrintWriter writer = writeQueueForMetaConnect(request, response, session, null);
        ServerMessage.Mutable reply = scheduler.getReply();
        if (session.isDisconnected())
            reconnect(reply);
        writer = sendReply(request, response, session, writer, reply);
        finishWrite(writer, session);
    }

    private void reconnect(ServerMessage.Mutable reply) {
        reply.getAdvice(true).put(Message.RECONNECT_FIELD, Message.RECONNECT_NONE_VALUE);
    }

    private PrintWriter sendReply(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session, PrintWriter writer,
            ServerMessage.Mutable reply) throws IOException {
        reply = getBayeux().extendReply(session, session, reply);

        if (reply != null) {
            getBayeux().freeze(reply);
            writer = writeMessage(request, response, writer, session, reply);
        }
        return writer;
    }

    private boolean isConnectChannel(ServerMessage.Mutable message) {
        return Channel.META_CONNECT.equals(message.getChannel());
    }

    private boolean isResumed(HttpServletRequest request) {
        return ContinuationSupport.getContinuation(request).isResumed() && request.getAttribute(LongPollScheduler.ATTRIBUTE) != null;
    }

    private void suspendRequest(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session, long timeout,
            Mutable reply) {
        log.debug("suspending session {}", session.getId());
        LongPollScheduler scheduler;
        Continuation continuation = ContinuationSupport.getContinuation(request);
        continuation.setTimeout(timeout);
        continuation.suspend(response);
        scheduler = newLongPollScheduler(session, continuation, reply);
        request.setAttribute(LongPollScheduler.ATTRIBUTE, scheduler);
        session.setScheduler(scheduler);
    }

    protected LongPollScheduler newLongPollScheduler(ServerSessionImpl session, Continuation continuation, Mutable reply) {
        final LongPollScheduler longPollScheduler = new LongPollScheduler(session, continuation, reply);
        schedulers.add(longPollScheduler);
        return longPollScheduler;
    }

    private PrintWriter writeQueueForMetaConnect(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session,
            PrintWriter writer) throws IOException {
        return writeQueue(request, response, session, writer);
    }

    protected ServerMessage.Mutable bayeuxServerHandle(ServerSessionImpl session, ServerMessage.Mutable message) {
        return getBayeux().handle(session, message);
    }

    protected void handleJSONParseException(HttpServletRequest request, HttpServletResponse response, String json, Throwable exception)
            throws ServletException, IOException {
        log.warn("Error parsing JSON: " + json, exception);
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Sweep the transport for old Browser IDs
     *
     * @see org.cometd.server.AbstractServerTransport#sweep()
     */
    protected void sweep() {
        for (LongPollScheduler scheduler : schedulers) {
            scheduler.validate();
        }
    }

    private PrintWriter writeQueue(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session, PrintWriter writer)
            throws IOException {
        List<ServerMessage> queue = session.takeQueue();
        for (ServerMessage m : queue) {
            if (m.getData() != null) {
                log.debug("sending message {} >>> {}", session.getId(), m.getJSON());
                writer = writeMessage(request, response, writer, session, m);
            }
        }
        log.debug("messages sended {} >>> {}", session.getId(), queue);
        return writer;
    }

    protected ServerMessage.Mutable[] parseMessages(String[] requestParameters) throws IOException, ParseException {
        if (requestParameters == null || requestParameters.length == 0) {
            throw new IOException("Missing '" + MESSAGE_PARAM + "' request parameter");
        }

        if (requestParameters.length == 1)
            return parseMessages(requestParameters[0]);

        List<ServerMessage.Mutable> messages = new ArrayList<ServerMessage.Mutable>();
        for (String batch : requestParameters) {
            if (batch == null)
                continue;
            messages.addAll(Arrays.asList(parseMessages(batch)));
        }
        return messages.toArray(new ServerMessage.Mutable[messages.size()]);
    }

    private boolean isValid(final ServletResponse response) {
        try {
            response.getWriter().write(' ');
            response.getWriter().flush();
            return !response.getWriter().checkError();
        } catch (IOException e) {
            return false;
        }
    }

    protected abstract ServerMessage.Mutable[] parseMessages(HttpServletRequest request) throws IOException, ParseException;

    /**
     * @return true if the transport always flushes at the end of a call to {@link #handle(HttpServletRequest, HttpServletResponse)}.
     */
    protected abstract boolean isAlwaysFlushingAfterHandle();

    protected abstract PrintWriter writeMessage(HttpServletRequest request, HttpServletResponse response, PrintWriter writer,
            ServerSessionImpl session, ServerMessage message) throws IOException;

    protected abstract void finishWrite(PrintWriter writer, ServerSessionImpl session) throws IOException;

    protected class LongPollScheduler implements AbstractServerTransport.OneTimeScheduler, ContinuationListener, RemoveListener {

        private final Logger log = LoggerFactory.getLogger(LongPollScheduler.class);

        private static final String ATTRIBUTE = "org.cometd.scheduler";

        private final ServerSessionImpl session;

        private final Continuation continuation;

        private final ServerMessage.Mutable reply;

        private Duration validTime;

        private Interval lastValidation;

        public LongPollScheduler(ServerSessionImpl session, Continuation continuation, ServerMessage.Mutable reply) {
            this.session = session;
            this.session.addListener(this);
            this.continuation = continuation;
            this.continuation.addContinuationListener(this);
            this.reply = reply;

            validTime = Duration.standardMinutes(heartbeatMinutes);
            this.lastValidation = new Interval(new DateTime(), validTime);
        }

        public void validate() {
            if (!lastValidation.containsNow()) {
                log.debug("validating session {}", session.getId());
                try {
                    if (!isValid(continuation.getServletResponse())) {
                        log.debug("long poll interupted session {}", session.getId());
                        cancel();
                    }
                    lastValidation = new Interval(new DateTime(), validTime);
                } catch (Exception e) {
                    log.debug("validation error", e);
                }
            }
        }

        private void cleanup() {
            schedulers.remove(this);
            session.removeListener(this);
            session.deactivate();
        }

        public void cancel() {
            log.debug("canceling {} - {}", session.getId(), this);
            cleanup();
            if (continuation != null && continuation.isSuspended() && !continuation.isExpired()) {
                try {
                    ((HttpServletResponse) continuation.getServletResponse()).sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                } catch (Exception x) {
                    log.trace("", x);
                }
                try {
                    continuation.complete();

                } catch (Exception x) {
                    log.trace("", x);
                }
            }
        }

        public void schedule() {
            continuation.resume();
            cleanup();
        }

        public ServerSessionImpl getSession() {
            return session;
        }

        public ServerMessage.Mutable getReply() {
            Map<String, Object> advice = session.takeAdvice();
            if (advice != null)
                reply.put(Message.ADVICE_FIELD, advice);
            return reply;
        }

        public void onComplete(Continuation continuation) {
            cleanup();
        }

        public void onTimeout(Continuation continuation) {
            session.setScheduler(null);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((continuation == null) ? 0 : continuation.hashCode());
            result = prime * result + ((reply == null) ? 0 : reply.hashCode());
            result = prime * result + ((session == null) ? 0 : session.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LongPollScheduler other = (LongPollScheduler) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (continuation == null) {
                if (other.continuation != null)
                    return false;
            } else if (!continuation.equals(other.continuation))
                return false;
            if (reply == null) {
                if (other.reply != null)
                    return false;
            } else if (!reply.equals(other.reply))
                return false;
            if (session == null) {
                if (other.session != null)
                    return false;
            } else if (!session.equals(other.session))
                return false;
            return true;
        }

        private LongPollingTransport getOuterType() {
            return LongPollingTransport.this;
        }

        @Override
        public void removed(ServerSession session, boolean timeout) {
            cleanup();
        }

    }
}
