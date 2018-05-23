package org.cometd.server;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.jayway.jsonpath.JsonPath;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.common.JSONContext;
import org.cometd.server.transport.LongPollingTransport;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.eclipse.jetty.continuation.ContinuationThrowable;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static org.cometd.server.SessionState.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;

public class ServerSessionImplTest {

    BayeuxServerImpl server = new BayeuxServerImpl();

    @Before
    public void setup() throws Exception {
        server.setOption("long-polling.maxInterval", 1000000);
        server.start();
    }


    @Test
    public void shouldHandshakeClientAndAssignClientIdAndSessionShouldBeInitialized() throws IOException, ServletException {
        //Given
        LongPollingTransport transport = transport();
        Response response = new Response();

        //When
        transport.handle(request().handshake().build(), response.build());

        String clientId = response.resolve("$[0].clientId");

        //Then
        assertThat(clientId).isNotNull().isNotEmpty();
        assertThat(((ServerSessionImpl) server.getSession(clientId)).getState()).isEqualTo(INITIALIZED);
    }

    private LongPollingTransport transport() {
        LongPollingTransport transport = (LongPollingTransport) server.getTransport("long-polling");
        server.setCurrentTransport(transport);
        return transport;
    }

    @Test
    public void shouldSwitchSessionToActiveWhenClientConnects() throws IOException, ServletException {
        //Given
        LongPollingTransport transport = transport();

        Response handshake = new Response();

        //When
        transport.handle(request().handshake().build(), handshake.build());

        String clientId = handshake.resolve("$[0].clientId");

        Response connect = new Response();

        transport.handle(request().clientId(clientId).connect().build(), connect.build());

        //Then
        assertThat(((ServerSessionImpl) server.getSession(clientId)).getState()).isEqualTo(ACTIVE);
    }


    @Test
    public void shouldSwitchSessionToInactiveWhenConnectionWasClosed() throws IOException, ServletException {
        //Given
        LongPollingTransport transport = transport();
        Response handshake = new Response();

        //When
        transport.handle(request().handshake().build(), handshake.build());

        String clientId = handshake.resolve("$[0].clientId");

        Response connect = new Response();

        transport.handle(request().clientId(clientId).connect().response(connect).build(), connect.invalidConnection().build());
        setCurrentMillisFixed(currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));
        server.sweep();
        setCurrentMillisFixed(currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));
        server.sweep();

        //Then
        assertThat(((ServerSessionImpl) server.getSession(clientId)).getState()).isEqualTo(INACTIVE);
    }


    @Test
    public void shouldNotSendMessageToInvalidateSession() throws IOException, ServletException {
        //Given
        LongPollingTransport transport = transport();
        Response handshake = new Response();

        //When
        transport.handle(request().handshake().build(), handshake.build());

        String clientId = handshake.resolve("$[0].clientId");

        transport.handle(request().clientId(clientId).subscribe("/some/channel").build(), new Response().build());

        Response connect = new Response();
        transport.handle(request().clientId(clientId).connect().response(connect).build(), connect.invalidConnection().build());

        setCurrentMillisFixed(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30));
        server.sweep();

        LocalSession test = server.newLocalSession("test");
        test.handshake();
        test.getChannel("/some/channel").publish(new HashMap<>());
        test.disconnect();
        //Then
        assertThat(((ServerSessionImpl) server.getSession(clientId)).getState()).isEqualTo(INACTIVE);
        assertThat(((ServerSessionImpl) server.getSession(clientId)).getQueue()).isNotEmpty();
    }

    private RequestBuilder request() {
        return new RequestBuilder();
    }


    class RequestBuilder {

        private String content;
        private String clientId;
        private Response response;

        public HttpServletRequest build() {
            final Continuation continuation = new MockContinuation();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setAttribute("org.eclipse.jetty.continuation", continuation);
            if (content != null) {
                request.setContent(content.getBytes());
            }

            return request;

        }

        public RequestBuilder response(Response response) {
            this.response = response;
            return this;
        }

        public RequestBuilder handshake() {
            this.content = loadFrom("classpath:templates/handshake.json");
            return this;
        }

        public RequestBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public RequestBuilder connect() {
            this.content = template().inject(loadFrom("classpath:templates/connect.json"));
            return this;
        }

        public RequestBuilder connect(int timeout) {
            this.content = template().with("timeout", timeout).inject(loadFrom("classpath:templates/connect.json"));
            return this;
        }

        private Template template() {
            return new Template().with("clientId", clientId);
        }


        public RequestBuilder subscribe(String subscribe) {
            this.content = template().with("subscribe", subscribe).inject(loadFrom("classpath:templates/subscribe.json"));
            return this;
        }

        private class MockContinuation implements Continuation {
            private boolean completed = false;

            @Override
            public void setTimeout(long l) {

            }

            @Override
            public void suspend() {

            }

            @Override
            public void suspend(ServletResponse servletResponse) {

            }

            @Override
            public void resume() {
                checkState(!completed, "Completed continuation can't be resumed");
            }

            @Override
            public void complete() {
                completed = true;
            }

            @Override
            public boolean isSuspended() {
                return !completed;
            }

            @Override
            public boolean isResumed() {
                return false;
            }

            @Override
            public boolean isExpired() {
                return false;
            }

            @Override
            public boolean isInitial() {
                return false;
            }

            @Override
            public boolean isResponseWrapped() {
                return false;
            }

            @Override
            public ServletResponse getServletResponse() {
                if (response != null) {
                    return response.build();
                }
                throw new IllegalStateException("Response not defined");
            }

            @Override
            public void addContinuationListener(ContinuationListener continuationListener) {

            }

            @Override
            public void setAttribute(String s, Object o) {

            }

            @Override
            public Object getAttribute(String s) {
                return null;
            }

            @Override
            public void removeAttribute(String s) {

            }

            @Override
            public void undispatch() throws ContinuationThrowable {

            }
        }
    }

    class Template {
        private final ImmutableMap.Builder<String, Object> variables = ImmutableMap.<String, Object>builder();

        public Template with(String property, Object value) {
            variables.put(property, value);
            return this;
        }

        public String inject(String template) {
            return environment().resolvePlaceholders(template);
        }

        private StandardEnvironment environment() {
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("variables", variables.build()));
            return environment;
        }
    }


    private String loadFrom(final String file) {
        try {
            return new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return new DefaultResourceLoader().getResource(file).getInputStream();
                }
            }.asCharSource(Charsets.UTF_8).read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class Response {
        MockHttpServletResponse response = new MockHttpServletResponse();
        private boolean invalidConnection = false;

        public <T> T resolve(String path) {
            try {
                return JsonPath.parse(response.getContentAsString()).read(JsonPath.compile(path));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public void reset() {
            response.reset();
        }

        public int getStatus() {
            return response.getStatus();
        }

        public String getErrorMessage() {
            return response.getErrorMessage();
        }

        public HttpServletResponse build() {
            if (invalidConnection)
                return new HttpServletResponseWrapper(response) {
                    ServletOutputStream outputStream = new ServletOutputStream() {
                        @Override
                        public void write(int i) throws IOException {
                            throw new IOException("Connection invalid");
                        }
                    };

                    private PrintWriter writer = new PrintWriter(outputStream);

                    @Override
                    public ServletOutputStream getOutputStream() throws IOException {

                        return outputStream;
                    }

                    @Override
                    public PrintWriter getWriter() throws IOException {

                        return writer;
                    }
                };
            else {
                return response;
            }

        }

        public Response invalidConnection() {
            invalidConnection = true;
            return this;
        }
    }

}
