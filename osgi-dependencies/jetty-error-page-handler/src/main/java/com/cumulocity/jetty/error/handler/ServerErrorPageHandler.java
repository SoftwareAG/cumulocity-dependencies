package com.cumulocity.jetty.error.handler;

import lombok.*;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import static java.util.Collections.unmodifiableMap;

public class ServerErrorPageHandler extends ErrorPageErrorHandler {

    private static final Logger LOG = Log.getLogger(ServerErrorPageHandler.class);

    private static final String COMPONENT_NAME = "System";

    public ServerErrorPageHandler() {
        LOG.info("Initializing custom jetty error page handler.");
    }

    @Override
    protected void generateAcceptableResponse(Request baseRequest, HttpServletRequest request, HttpServletResponse response, int code, String message, String mimeType)
            throws IOException {
        baseRequest.setHandled(true);
        Writer writer = getAcceptableWriter(baseRequest, request, response);
        if (null != writer) {
            response.setContentType(MimeTypes.Type.APPLICATION_JSON.asString());
            response.setStatus(code);
            handleErrorPage(request, writer, code, message);
        }
    }

    @Override
    protected Writer getAcceptableWriter(Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        return response.getWriter();
    }

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks) {
        ErrorMessage errorMessage = ErrorMessage.get(code);
        try {
            writer.write(
                    "{\"error\": \"" + COMPONENT_NAME + "/" + errorMessage.getError() + "\", " +
                         "\"message\": \"" + errorMessage.getMessage() + "\"}"
            );
        } catch (Exception e) {
            LOG.warn("Could not write an error! " + e.getMessage(), e);
        }
    }

    @AllArgsConstructor
    static class ErrorMessage {
        /*
        Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content
        https://tools.ietf.org/html/rfc7231
        */
        static Map<Integer, ErrorMessage> SERVER_ERRORS = unmodifiableMap(new HashMap<Integer, ErrorMessage>() {{
            put(404, new ErrorMessage("Not Found", "Target resource not found"));
            put(501, new ErrorMessage("Not Implemented", "Server functionality to process request is not implemented"));
            put(502, new ErrorMessage("Bad Gateway", "Server cannot proxy request"));
            put(503, new ErrorMessage("Service Unavailable", "Server is currently unable to handle the request"));
            put(504, new ErrorMessage("Gateway Timeout", "Server did not receive a timely response from an upstream server"));
        }});
        static ErrorMessage DEFAULT = new ErrorMessage("Internal Server Error", "Unexpected error occurred");

        @Getter @Setter
        String error;
        @Getter @Setter
        String message;

        protected static ErrorMessage get(int code) {
            return SERVER_ERRORS.getOrDefault(code, DEFAULT);
        }
    }
}
