package org.cometd.server.transport;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
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

    private final Logger _logger = LoggerFactory.getLogger(getClass());

    private final Collection<LongPollScheduler> schedulers = new CopyOnWriteArrayList<LongPollScheduler>();

    private boolean _autoBatch;

    protected LongPollingTransport(BayeuxServerImpl bayeux, String name) {
        super(bayeux, name);
        setOptionPrefix(PREFIX);
    }

    @Override
    protected void init() {
        super.init();
        _autoBatch = getOption(AUTOBATCH_OPTION, true);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!isResumed(request)) {
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
                    String client_id = message.getClientId();
                    if (session == null || client_id != null && !client_id.equals(session.getId())) {
                        session = (ServerSessionImpl) getBayeux().getSession(client_id);
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
                        session.cancelSchedule();
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
                                            reply = suspendRequest(request, response, session, reply, timeout);
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
        // Get the resumed session
        ServerSessionImpl session = scheduler.getSession();
        metaConnectResumed(request, session);

        PrintWriter writer = writeQueueForMetaConnect(request, response, session, null);

        // Send the connect reply
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

    private ServerMessage.Mutable suspendRequest(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session,
            ServerMessage.Mutable reply, long timeout) {
        LongPollScheduler scheduler;
        Continuation continuation = ContinuationSupport.getContinuation(request);
        continuation.setTimeout(timeout);
        continuation.suspend(response);
        scheduler = newLongPollScheduler(session, continuation, reply);
        request.setAttribute(LongPollScheduler.ATTRIBUTE, scheduler);
        session.setScheduler(scheduler);
        reply = null;
        metaConnectSuspended(request, session, timeout);
        return reply;
    }

    protected LongPollScheduler newLongPollScheduler(ServerSessionImpl session, Continuation continuation,
            ServerMessage.Mutable metaConnectReply) {
        final LongPollScheduler longPollScheduler = new LongPollScheduler(session, continuation, metaConnectReply);
        schedulers.add(longPollScheduler);
        return longPollScheduler;
    }

    private PrintWriter writeQueueForMetaConnect(HttpServletRequest request, HttpServletResponse response, ServerSessionImpl session,
            PrintWriter writer) throws IOException {
        try {
            return writeQueue(request, response, session, writer);
        } finally {
            if (session.isConnected())
                session.deactivate();
        }
    }

    protected ServerMessage.Mutable bayeuxServerHandle(ServerSessionImpl session, ServerMessage.Mutable message) {
        return getBayeux().handle(session, message);
    }

    protected void metaConnectSuspended(HttpServletRequest request, ServerSession session, long timeout) {
    }

    protected void metaConnectResumed(HttpServletRequest request, ServerSession session) {
    }

    protected void handleJSONParseException(HttpServletRequest request, HttpServletResponse response, String json, Throwable exception)
            throws ServletException, IOException {
        _logger.warn("Error parsing JSON: " + json, exception);
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
        for (ServerMessage m : queue)
            writer = writeMessage(request, response, writer, session, m);
        return writer;
    }

    protected ServerMessage.Mutable[] parseMessages(String[] requestParameters) throws IOException, ParseException {
        if (requestParameters == null || requestParameters.length == 0)
            throw new IOException("Missing '" + MESSAGE_PARAM + "' request parameter");

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

        private final Duration validTime = Duration.standardMinutes(10);

        private Interval lastValidation = new Interval(new DateTime(), validTime);

        public LongPollScheduler(ServerSessionImpl session, Continuation continuation, ServerMessage.Mutable reply) {
            this.session = session;
            this.session.addListener(this);
            this.continuation = continuation;
            this.continuation.addContinuationListener(this);
            this.reply = reply;
        }

        public void validate() {
            if (!lastValidation.containsNow()) {
                log.trace("validating session {}", session);
                try {
                    continuation.getServletResponse().getWriter().write(" ");
                    if (continuation.getServletResponse().getWriter().checkError()) {
                        log.debug("long poll interupted session {}", session);
                        cleanup();
                        session.cancelSchedule();
                    }
                    lastValidation = new Interval(new DateTime(), validTime);
                } catch (IOException e) {
                }
            }
        }

        private void cleanup() {
            schedulers.remove(this);
            session.removeListener(this);
        }

        public void cancel() {
            if (continuation != null && continuation.isSuspended() && !continuation.isExpired()) {
                try {
                    ((HttpServletResponse) continuation.getServletResponse()).sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                } catch (Exception x) {
                    _logger.trace("", x);
                }

                try {
                    continuation.complete();
                    cleanup();
                } catch (Exception x) {
                    _logger.trace("", x);
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
            session.cancelSchedule();
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
