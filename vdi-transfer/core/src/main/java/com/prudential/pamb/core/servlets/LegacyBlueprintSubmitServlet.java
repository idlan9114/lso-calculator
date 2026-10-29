package com.prudential.pamb.core.servlets;

import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmissionResult;
import com.prudential.pamb.core.services.LegacyBlueprintSubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

/**
 * Accepts the completed Legacy Blueprint at {@code <componentPath>.submit.json} (JSON POST, CSRF
 * token required by the platform filter). Submissions from the author editor (edit / preview mode)
 * are blocked so no API call or email is triggered while authoring.
 */
@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = LegacyBlueprintConstants.RESOURCE_TYPE,
        selectors = LegacyBlueprintConstants.SELECTOR_SUBMIT,
        extensions = "json",
        methods = METHOD_POST)
@ServiceDescription("Legacy Blueprint (LSO Calculator) submission servlet")
@Slf4j
public class LegacyBlueprintSubmitServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    static final int SC_FORBIDDEN = 403;
    static final int SC_BAD_REQUEST = 400;
    static final int SC_PAYLOAD_TOO_LARGE = 413;
    static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    static final int SC_INTERNAL_SERVER_ERROR = 500;

    private static final String APPLICATION_JSON = "application/json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

    @Reference
    private transient LegacyBlueprintSubmissionService submissionService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType(APPLICATION_JSON);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");

        WCMMode mode = WCMMode.fromRequest(request);
        if (mode == WCMMode.EDIT || mode == WCMMode.PREVIEW) {
            writeStatus(response, SC_FORBIDDEN, "blocked");
            return;
        }
        String contentType = StringUtils.defaultString(request.getContentType()).toLowerCase(Locale.ROOT);
        if (!contentType.startsWith(APPLICATION_JSON)) {
            writeStatus(response, SC_UNSUPPORTED_MEDIA_TYPE, "unsupportedMediaType");
            return;
        }
        if (request.getContentLength() > LegacyBlueprintConstants.MAX_REQUEST_BYTES) {
            writeStatus(response, SC_PAYLOAD_TOO_LARGE, "tooLarge");
            return;
        }

        LegacyBlueprintSubmission submission;
        try {
            byte[] body = readBounded(request.getInputStream());
            if (body == null) {
                writeStatus(response, SC_PAYLOAD_TOO_LARGE, "tooLarge");
                return;
            }
            submission = MAPPER.readValue(body, LegacyBlueprintSubmission.class);
        } catch (IOException e) {
            log.info("LegacyBlueprint | malformed submission at {}: {}", request.getResource().getPath(), e.getClass().getSimpleName());
            writeStatus(response, SC_BAD_REQUEST, "malformed");
            return;
        }

        try {
            LegacyBlueprintSubmissionResult result = submissionService.submit(submission, request.getResource());
            response.setStatus(result.isValid() ? SlingHttpServletResponse.SC_OK : SC_BAD_REQUEST);
            MAPPER.writeValue(response.getWriter(), result);
        } catch (RuntimeException e) {
            log.error("LegacyBlueprint | unexpected error processing submission at {}", request.getResource().getPath(), e);
            writeStatus(response, SC_INTERNAL_SERVER_ERROR, "error");
        }
    }

    // Returns null when the body is larger than the limit (Content-Length may be absent or wrong).
    private static byte[] readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            if (out.size() + read > LegacyBlueprintConstants.MAX_REQUEST_BYTES) {
                return null;
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void writeStatus(SlingHttpServletResponse response, int status, String value) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("status", value);
        response.setStatus(status);
        response.getWriter().write(body.toString());
    }
}
