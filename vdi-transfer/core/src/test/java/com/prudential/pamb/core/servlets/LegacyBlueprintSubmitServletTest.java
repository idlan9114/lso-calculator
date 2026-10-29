package com.prudential.pamb.core.servlets;

import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmissionResult;
import com.prudential.pamb.core.services.LegacyBlueprintSubmissionService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class LegacyBlueprintSubmitServletTest {

    private static final String COMPONENT = "/content/pamb/en/legacy/jcr:content/root/lso";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AemContext context = new AemContext();

    private LegacyBlueprintSubmissionService submissionService;
    private LegacyBlueprintSubmitServlet servlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setUp() {
        submissionService = mock(LegacyBlueprintSubmissionService.class);
        context.registerService(LegacyBlueprintSubmissionService.class, submissionService);
        servlet = context.registerInjectActivateService(new LegacyBlueprintSubmitServlet());
        context.create().resource(COMPONENT, "sling:resourceType", "prudential-aem-malaysia/components/content/lsoCalculator");
        context.currentResource(COMPONENT);
        request = context.request();
        response = context.response();
        request.setMethod("POST");
    }

    private void body(String json) {
        request.setContentType("application/json");
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode output() throws Exception {
        return MAPPER.readTree(response.getOutputAsString());
    }

    @Test
    void editorModes_areBlocked() throws Exception {
        body("{}");
        for (WCMMode mode : Arrays.asList(WCMMode.EDIT, WCMMode.PREVIEW)) {
            response = new MockSlingHttpServletResponse();
            mode.toRequest(request);
            servlet.doPost(request, response);
            assertEquals(403, response.getStatus());
            assertEquals("blocked", output().get("status").asText());
        }
        verify(submissionService, never()).submit(any(), any());
    }

    @Test
    void nonJson_isRejected() throws Exception {
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent("a=b".getBytes(StandardCharsets.UTF_8));

        servlet.doPost(request, response);

        assertEquals(415, response.getStatus());
    }

    @Test
    void malformedJson_isRejected() throws Exception {
        body("{not json");

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("malformed", output().get("status").asText());
    }

    @Test
    void oversizedBody_isRejected() throws Exception {
        char[] padding = new char[70 * 1024];
        Arrays.fill(padding, 'x');
        body("{\"contact\":{\"fullName\":\"" + new String(padding) + "\"}}");

        servlet.doPost(request, response);

        assertEquals(413, response.getStatus());
        verify(submissionService, never()).submit(any(), any());
    }

    @Test
    void validSubmission_returnsResult() throws Exception {
        LegacyBlueprintSubmissionResult result = new LegacyBlueprintSubmissionResult();
        result.setStatus("partial");
        result.setEmailSent(true);
        result.setPlan(new LegacyBlueprintPlan());
        when(submissionService.submit(any(), any())).thenReturn(result);
        body("{\"importance\":5,\"needs\":[\"everyday\"],\"unknownField\":true}");

        servlet.doPost(request, response);

        assertEquals(200, response.getStatus());
        JsonNode json = output();
        assertEquals("partial", json.get("status").asText());
        assertEquals(true, json.get("emailSent").asBoolean());
        assertFalse(json.get("apiSubmitted").asBoolean());
        assertFalse(json.has("valid"));
        ArgumentCaptor<LegacyBlueprintSubmission> captor = ArgumentCaptor.forClass(LegacyBlueprintSubmission.class);
        verify(submissionService).submit(captor.capture(), any());
        assertEquals(5, captor.getValue().getImportance());
    }

    @Test
    void invalidSubmission_returns400WithCodes() throws Exception {
        when(submissionService.submit(any(), any()))
                .thenReturn(LegacyBlueprintSubmissionResult.invalid(Collections.singletonList("contact.email")));
        body("{}");

        servlet.doPost(request, response);

        assertEquals(400, response.getStatus());
        assertEquals("contact.email", output().get("errors").get(0).asText());
    }

    @Test
    void unexpectedError_returns500WithoutDetails() throws Exception {
        when(submissionService.submit(any(), any())).thenThrow(new IllegalStateException("secret detail"));
        body("{}");

        servlet.doPost(request, response);

        assertEquals(500, response.getStatus());
        assertEquals("{\"status\":\"error\"}", response.getOutputAsString());
    }
}
