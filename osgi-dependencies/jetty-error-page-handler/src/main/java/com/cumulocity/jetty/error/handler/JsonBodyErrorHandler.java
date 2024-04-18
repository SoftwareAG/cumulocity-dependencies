package com.cumulocity.jetty.error.handler;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class JsonBodyErrorHandler extends ErrorPageErrorHandler {

    private static final Logger LOG = Log.getLogger(JsonBodyErrorHandler.class);

    private static final String COMPONENT_NAME = "System";

    public JsonBodyErrorHandler() {
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
        try {
            writer.write("{\n" +
                    "  \"error\": \"" + COMPONENT_NAME + "/" + HttpStatus.getMessage(code) + "\",\n" +
                    "  \"message\": \"" + message + "\"\n" +
                    "}");
        } catch (Exception e) {
            LOG.warn("Could not write an error! " + message, e);
        }
    }
}
