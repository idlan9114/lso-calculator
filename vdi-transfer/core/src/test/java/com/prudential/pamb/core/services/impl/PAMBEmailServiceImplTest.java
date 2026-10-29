package com.prudential.pamb.core.services.impl;

import com.adobe.granite.crypto.CryptoSupport;
import com.day.cq.mailer.MailingException;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.prudential.pamb.core.services.EmailAttachment;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prudential.core.constants.PrudentialConstants.*;
import static com.prudential.core.constants.PrudentialConstants.BCC_EMAIL_IDS;
import static com.prudential.core.constants.PrudentialConstants.CC_EMAIL_IDS;
import static com.prudential.core.constants.PrudentialConstants.FROM_EMAIL_ID;
import static com.prudential.core.constants.PrudentialConstants.TO_EMAIL_IDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PAMBEmailServiceImplTest {

    @Mock
    private CryptoSupport cryptoSupport;

    @Mock
    private MessageGatewayService messageGatewayService;

    @Mock
    private MessageGateway messageGateway;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private RequestParameter requestParameter;

    @InjectMocks
    private PAMBEmailServiceImpl emailService;

    private Map<String, String> emailParameterMap;

    @BeforeEach
    void setUp() {
        emailParameterMap = new HashMap<>();
        emailParameterMap.put(SUBJECT, "Test Subject");
        emailParameterMap.put(MSG_BODY, "<h1>Test Message</h1>");
        emailParameterMap.put(TO_EMAIL_IDS, "test@example.com");
        emailParameterMap.put(FROM_EMAIL_ID, "sender@example.com");
    }

    @Test
    void testSendEmail_Success() throws Exception {
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithCCAndBCC() throws Exception {
        emailParameterMap.put(CC_EMAIL_IDS, "cc@example.com,cc2@example.com");
        emailParameterMap.put(BCC_EMAIL_IDS, "bcc@example.com");
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithMultipleToEmails() throws Exception {
        emailParameterMap.put(TO_EMAIL_IDS, "test1@example.com,test2@example.com,test3@example.com");
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }


    void testSendEmail(String type) {
        if ("SingleID".equalsIgnoreCase(type)) {
            emailParameterMap.put(TO_EMAIL_IDS, " test1@example.com , test2@example.com ");
            emailParameterMap.put(CC_EMAIL_IDS, " cc1@example.com , cc2@example.com ");
            emailParameterMap.put(BCC_EMAIL_IDS, " bcc1@example.com , bcc2@example.com ");
        } else if ("MultipleID".equalsIgnoreCase(type)) {
            emailParameterMap.put(TO_EMAIL_IDS, "valid@example.com");
            emailParameterMap.put(CC_EMAIL_IDS, "valid-cc@example.com");
            emailParameterMap.put(BCC_EMAIL_IDS, "valid-bcc@example.com");
        } else {
            emailParameterMap.put(TO_EMAIL_IDS, "");
            emailParameterMap.put(CC_EMAIL_IDS, "   ");
            emailParameterMap.put(BCC_EMAIL_IDS, null);
        }
    }

    @Test
    void testSendEmail_WithEmptyEmailStrings() throws Exception {
        testSendEmail("");
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
        testSendEmail("SingleID");
        result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway, times(2)).send(any(HtmlEmail.class));
        testSendEmail("MultipleID");
        result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway, times(3)).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithAttachments() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        byte[] fileContent = "test file content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);
        when(requestParameter.getFileName()).thenReturn("test.txt");
        when(requestParameter.getContentType()).thenReturn("text/plain");
        when(requestParameter.getInputStream()).thenReturn(inputStream);
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithAttachmentPrefix() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        byte[] fileContent = "test file content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(fileContent);
        when(requestParameter.getFileName()).thenReturn("attachment/test.txt");
        when(requestParameter.getContentType()).thenReturn("text/plain");
        when(requestParameter.getInputStream()).thenReturn(inputStream);
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithInvalidAttachment() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        when(requestParameter.getFileName()).thenReturn("");
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_AttachmentIOException() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        when(requestParameter.getFileName()).thenReturn("test.txt");
        when(requestParameter.getContentType()).thenReturn("text/plain");
        when(requestParameter.getInputStream()).thenThrow(new IOException("IO Error"));
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_NoMessageGateway() {
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(null);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertFalse(result);
        verify(messageGateway, never()).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_MailingException() throws Exception {
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        doThrow(new MailingException("Mailing error")).when(messageGateway).send(any(HtmlEmail.class));
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertFalse(result);
    }

    @Test
    void testSendEmail_WithEmptyParameterMap() {
        Map<String, String> emptyMap = new HashMap<>();
        boolean result = emailService.sendEmail(emptyMap, request);
        assertFalse(result);
    }

    @Test
    void testSendEmail_AttachmentWithNullFileName() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        when(requestParameter.getFileName()).thenReturn(null);
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_AttachmentWithNullContentType() throws Exception {
        RequestParameter[] attachments = {requestParameter};
        when(requestParameter.getFileName()).thenReturn("test.txt");
        when(requestParameter.getContentType()).thenReturn(null);
        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_MultipleAttachments() throws Exception {
        RequestParameter attachment1 = mock(RequestParameter.class);
        RequestParameter attachment2 = mock(RequestParameter.class);
        RequestParameter[] attachments = {attachment1, attachment2};
        when(attachment1.getFileName()).thenReturn("file1.txt");
        when(attachment1.getContentType()).thenReturn("text/plain");
        when(attachment1.getInputStream()).thenReturn(new ByteArrayInputStream("content1".getBytes()));

        when(attachment2.getFileName()).thenReturn("file2.pdf");
        when(attachment2.getContentType()).thenReturn("application/pdf");
        when(attachment2.getInputStream()).thenReturn(new ByteArrayInputStream("content2".getBytes()));

        when(request.getRequestParameters("attachments")).thenReturn(attachments);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_WithCallerSuppliedAttachments() throws Exception {
        List<EmailAttachment> attachments = Arrays.asList(
                new EmailAttachment("flyer.pdf", "application/pdf", "pdf content".getBytes()),
                new EmailAttachment("", "application/pdf", "skipped: no name".getBytes()),
                null);
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, attachments);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
        verify(request, never()).getRequestParameters(any());
    }

    @Test
    void testSendEmail_WithNullAttachmentList() throws Exception {
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        boolean result = emailService.sendEmail(emailParameterMap, (List<EmailAttachment>) null);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_SingleEmailInString() throws Exception {
        emailParameterMap.put(TO_EMAIL_IDS, "single@example.com");
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }

    @Test
    void testSendEmail_EmailStringWithOnlyCommas() throws Exception {
        emailParameterMap.put(TO_EMAIL_IDS, ",,,");
        emailParameterMap.put(CC_EMAIL_IDS, ", ,");
        when(messageGatewayService.getGateway(SimpleEmail.class)).thenReturn(messageGateway);
        when(request.getRequestParameters("attachments")).thenReturn(null);
        boolean result = emailService.sendEmail(emailParameterMap, request);
        assertTrue(result);
        verify(messageGateway).send(any(HtmlEmail.class));
    }
}