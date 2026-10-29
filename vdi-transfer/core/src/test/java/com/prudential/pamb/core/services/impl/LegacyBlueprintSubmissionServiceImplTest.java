package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintApiResult;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmissionResult;
import com.prudential.pamb.core.services.LegacyBlueprintApiService;
import com.prudential.pamb.core.services.PAMBEmailService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.known;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.lump;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.person;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class LegacyBlueprintSubmissionServiceImplTest {

    private static final String PAGE = "/content/pamb/en/legacy";
    private static final String COMPONENT = PAGE + "/jcr:content/root/lso";
    private static final String TEMPLATE = "/content/dam/pamb/email-templates/legacy-blueprint/legacy-blueprint-en.html";

    private final AemContext context = new AemContext();

    private LegacyBlueprintConfig config;
    private LegacyBlueprintApiService apiService;
    private PAMBEmailService emailService;
    private LegacyBlueprintSubmissionServiceImpl service;
    private Resource component;

    @BeforeEach
    void setUp() {
        config = mock(LegacyBlueprintConfig.class);
        apiService = mock(LegacyBlueprintApiService.class);
        emailService = mock(PAMBEmailService.class);
        when(config.getMailFrom()).thenReturn("no-reply@prudential.com.my");
        context.registerService(LegacyBlueprintConfig.class, config);
        context.registerService(LegacyBlueprintApiService.class, apiService);
        context.registerService(PAMBEmailService.class, emailService);
        service = context.registerInjectActivateService(new LegacyBlueprintSubmissionServiceImpl());

        context.create().page(PAGE);
        context.create().page("/content/pamb/en/privacy-notice");
        context.create().asset(TEMPLATE, new ByteArrayInputStream(
                "<title>Your plan, ${name}</title><p>${legacyAmount}</p><table>${peopleRows}</table>".getBytes(StandardCharsets.UTF_8)),
                "text/html");
        component = context.create().resource(COMPONENT, "sling:resourceType", "prudential-aem-malaysia/components/content/lsoCalculator");
        settings(TEMPLATE);
    }

    private void settings(String template) {
        Resource existing = component.getChild("settings");
        if (existing != null) {
            existing.adaptTo(ModifiableValueMap.class).put("emailTemplate", template);
            return;
        }
        context.create().resource(component, "settings",
                "emailTemplate", template,
                "privacyPage", "/content/pamb/en/privacy-notice",
                "productCode", "PRULady",
                "leadSourceCode", "LEGACY_BP_WEB");
    }

    private static LegacyBlueprintSubmission valid() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 100, lump());
        return submission;
    }

    @Test
    void invalidSubmission_callsNothing() throws Exception {
        LegacyBlueprintSubmission submission = valid();
        submission.setLegacyAmount(10L);

        LegacyBlueprintSubmissionResult result = service.submit(submission, component);

        assertEquals("invalid", result.getStatus());
        assertTrue(result.getErrors().contains("legacyAmount.belowMinimum"));
        verify(apiService, never()).submit(any());
        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void success_postsPayloadAndSendsTemplateEmail() throws Exception {
        when(apiService.submit(any())).thenReturn(apiResult(200, "{\"code\":\"200\",\"id\":\"ref-1\",\"status\":\"accepted\"}"));
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(true);

        LegacyBlueprintSubmissionResult result = service.submit(valid(), component);

        assertEquals("ok", result.getStatus());
        assertTrue(result.isApiSubmitted());
        assertTrue(result.isEmailSent());
        assertEquals("ref-1", result.getApiReference());
        assertEquals(200, result.getApiStatus());
        assertEquals("accepted", result.getApiResponse().get("status").asText());
        assertNotNull(result.getPlan());

        ArgumentCaptor<ObjectNode> payload = ArgumentCaptor.forClass(ObjectNode.class);
        verify(apiService).submit(payload.capture());
        assertEquals("PRULady", payload.getValue().get("productCode").asText());
        assertEquals("English", payload.getValue().get("language").asText());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> email = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendEmail(email.capture(), anyList());
        assertEquals("aisyah@example.com", email.getValue().get(PAMBConstants.TO_EMAIL_IDS));
        assertEquals("no-reply@prudential.com.my", email.getValue().get(PAMBConstants.FROM_EMAIL_ID));
        assertEquals("Your plan, Aisyah Rahman", email.getValue().get(PAMBConstants.SUBJECT));
        assertTrue(email.getValue().get(PAMBConstants.MSG_BODY).contains("<p>RM 1,000,000</p>"));
        assertTrue(email.getValue().get(PAMBConstants.MSG_BODY).contains(">Jane<"));
    }

    @Test
    void apiFailure_stillSendsEmail() throws Exception {
        JsonNode error = new ObjectMapper().readTree("{\"code\":\"503\",\"message\":\"Service unavailable\"}");
        when(apiService.submit(any())).thenThrow(new LegacyBlueprintApiException("down", 503, error, null));
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(true);

        LegacyBlueprintSubmissionResult result = service.submit(valid(), component);

        assertEquals("partial", result.getStatus());
        assertFalse(result.isApiSubmitted());
        assertTrue(result.isEmailSent());
        assertEquals(503, result.getApiStatus());
        assertEquals("Service unavailable", result.getApiResponse().get("message").asText());
    }

    @Test
    void apiUnreachable_reportsStatusZero() throws Exception {
        when(apiService.submit(any())).thenThrow(new LegacyBlueprintApiException("timed out"));
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(true);

        LegacyBlueprintSubmissionResult result = service.submit(valid(), component);

        assertEquals(0, result.getApiStatus());
        assertNull(result.getApiResponse());
    }

    private static LegacyBlueprintApiResult apiResult(int status, String json) throws Exception {
        return new LegacyBlueprintApiResult(status, new ObjectMapper().readTree(json));
    }

    @Test
    void bothFailing_reportsFailed() throws Exception {
        when(apiService.submit(any())).thenThrow(new LegacyBlueprintApiException("down"));
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(false);

        assertEquals("failed", service.submit(valid(), component).getStatus());
    }

    @Test
    void templateOutsideFolderOrMissing_isNotSent() throws Exception {
        when(apiService.submit(any())).thenReturn(apiResult(200, "{\"id\":\"ref\"}"));

        settings("/content/dam/pamb/other/template.html");
        assertFalse(service.submit(valid(), component).isEmailSent());

        settings("/content/dam/pamb/email-templates/legacy-blueprint/../secret.html");
        assertFalse(service.submit(valid(), component).isEmailSent());

        settings("/content/dam/pamb/email-templates/legacy-blueprint/missing.html");
        assertFalse(service.submit(valid(), component).isEmailSent());

        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void missingSender_isNotSent() throws Exception {
        when(config.getMailFrom()).thenReturn("$[env:LEGACY_BP_MAIL_FROM]");
        when(apiService.submit(any())).thenReturn(apiResult(200, "{\"id\":\"ref\"}"));

        LegacyBlueprintSubmissionResult result = service.submit(valid(), component);

        assertFalse(result.isEmailSent());
        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void authoredRelationshipValuesAreEnforced() {
        context.create().resource(COMPONENT + "/aboutPeople/relationshipOptions/item0", "label", "Wife", "value", "wife");
        List<String> errors = service.submit(valid(), component).getErrors();
        assertTrue(errors.contains("people[0].relationship"));
    }
}
