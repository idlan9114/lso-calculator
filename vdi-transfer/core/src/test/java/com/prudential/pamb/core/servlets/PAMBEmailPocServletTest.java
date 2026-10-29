package com.prudential.pamb.core.servlets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.services.EmailAttachment;
import com.prudential.pamb.core.services.PAMBEmailService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class PAMBEmailPocServletTest {

    private static final String COMPONENT_PATH = "/content/pamb/en/poc/jcr:content/root/emailpoc";

    private final AemContext context = new AemContext();

    private PAMBEmailService emailService;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setUp() {
        emailService = mock(PAMBEmailService.class);
        context.registerService(PAMBEmailService.class, emailService);
        context.create().resource(COMPONENT_PATH,
                "sling:resourceType", PAMBEmailPocServlet.RESOURCE_TYPE,
                "subject", "Authored subject");
        context.currentResource(COMPONENT_PATH);
        request = context.request();
        response = context.response();
    }

    private PAMBEmailPocServlet servlet(Object... config) {
        return context.registerInjectActivateService(new PAMBEmailPocServlet(), config);
    }

    private JsonObject body() {
        return JsonParser.parseString(response.getOutputAsString()).getAsJsonObject();
    }

    @Test
    void disabledByDefault_returns404() throws Exception {
        servlet().doPost(request, response);

        assertEquals(SC_NOT_FOUND, response.getStatus());
        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void validEmail_sendsBundledPdfThroughEmailService() throws Exception {
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(true);
        request.setParameterMap(Collections.singletonMap(PAMBConstants.EMAIL, " user@example.com "));

        servlet("enabled", true, "fromAddress", "sender@example.com").doPost(request, response);

        assertEquals(SC_OK, response.getStatus());
        assertTrue(body().get("sent").getAsBoolean());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmailAttachment>> attachments = ArgumentCaptor.forClass(List.class);
        verify(emailService).sendEmail(params.capture(), attachments.capture());

        assertEquals("user@example.com", params.getValue().get(PAMBConstants.TO_EMAIL_IDS));
        assertEquals("sender@example.com", params.getValue().get(PAMBConstants.FROM_EMAIL_ID));
        assertEquals("Authored subject", params.getValue().get(PAMBConstants.SUBJECT));

        EmailAttachment pdf = attachments.getValue().get(0);
        assertEquals(PAMBEmailPocServlet.ATTACHMENT_FILE_NAME, pdf.getFileName());
        assertEquals("application/pdf", pdf.getContentType());
        assertTrue(pdf.getContent().length > 0);
    }

    @Test
    void multipleRecipients_areRejected() throws Exception {
        request.setParameterMap(Collections.singletonMap(PAMBConstants.EMAIL, "a@example.com,b@example.com"));

        servlet("enabled", true).doPost(request, response);

        assertEquals(SC_BAD_REQUEST, response.getStatus());
        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void recipientOutsideAllowedDomains_isRejected() throws Exception {
        request.setParameterMap(Collections.singletonMap(PAMBConstants.EMAIL, "user@gmail.com"));

        servlet("enabled", true, "allowedRecipientDomains", new String[]{"Example.com"}).doPost(request, response);

        assertEquals(SC_BAD_REQUEST, response.getStatus());
        verify(emailService, never()).sendEmail(anyMap(), anyList());
    }

    @Test
    void recipientInAllowedDomain_isSent() throws Exception {
        when(emailService.sendEmail(anyMap(), anyList())).thenReturn(true);
        request.setParameterMap(Collections.singletonMap(PAMBConstants.EMAIL, "user@EXAMPLE.com"));

        servlet("enabled", true, "allowedRecipientDomains", new String[]{"example.com"}).doPost(request, response);

        assertEquals(SC_OK, response.getStatus());
    }

    @Test
    void serviceFailure_returns500() throws Exception {
        when(emailService.sendEmail(anyMap(), any(List.class))).thenReturn(false);
        request.setParameterMap(Collections.singletonMap(PAMBConstants.EMAIL, "user@example.com"));

        servlet("enabled", true).doPost(request, response);

        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertFalse(body().get("sent").getAsBoolean());
    }
}
