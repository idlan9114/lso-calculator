package com.prudential.pamb.core.servlets;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.services.PAMBEmailService;
import com.prudential.pamb.core.services.PAMBHttpService;
import com.prudential.pamb.core.services.PAMBRecaptchaValidationService;
import com.prudential.pamb.core.services.PAMBTokenService;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.security.auth.login.LoginException;
import javax.servlet.Servlet;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(AemContextExtension.class)
class PAMBContactUsServletTest {

    private final AemContext context = new AemContext();

    private PAMBContactUsServlet servlet;

    @Mock
    private PAMBHttpService httpService;

    @Mock
    private PAMBTokenService tokenService;

    @Mock
    private PAMBEmailService emailService;

    @Mock
    private PAMBRecaptchaValidationService pambRecaptchaValidationService;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @BeforeEach
    void setup() throws Exception {

        MockitoAnnotations.openMocks(this);

        servlet = new PAMBContactUsServlet();

        setPrivateField(servlet, "httpService", httpService);
        setPrivateField(servlet, "tokenService", tokenService);
        setPrivateField(servlet, "emailService", emailService);
        setPrivateField(servlet, "pambRecaptchaValidationService",pambRecaptchaValidationService);

        context.registerService(Servlet.class, servlet);

        context.create().resource(
                "/content/site/en/page/jcr:content/form",
                ResourceResolver.PROPERTY_RESOURCE_TYPE,
                PAMBConstants.FORM_SUBMIT_RESOURCE_TYPE);

        context.create().resource(
                "/content/site/en/page/jcr:content/form/button",
                ResourceResolver.PROPERTY_RESOURCE_TYPE,
                PAMBConstants.FORM_BUTTON_RESOURCE_TYPE,
                "enableRecaptcha",
                false);


        when(pambRecaptchaValidationService.validateToken(anyString(), any())).thenReturn(true);
    }

    private void setPrivateField(Object target, String field, Object value) throws Exception {

        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private MockSlingHttpServletRequest request(Map<String, Object> params) {

        context.currentResource("/content/site/en/page/jcr:content/form");

        MockSlingHttpServletRequest req = context.request();
        req.setMethod("POST");
        req.setParameterMap(params);

        return req;
    }

    private MockSlingHttpServletResponse response() {
        return context.response();
    }


    private String getPostFormDetails() {
        return "{"
                + "\"formType\":\"post\","
                + "\"postDetails\":{"
                + "\"endpoint\":\"https://api.com\","
                + "\"formComponentPath\":\"/content/site/en/page/jcr:content/form\""
                + "}"
                + "}";
    }

    private String getMailFormDetails() {

        return "{"
                + "\"formType\":\"mail\","
                + "\"mailDetails\":{"
                + "\"subject\":\"Test Subject\","
                + "\"formComponentPath\":\"/content/site/form/button\""
                + "}"
                + "}";
    }

    private String getPostAndMailDetails() {

        return "{"
                + "\"formType\":\"post_and_mail\","
                + "\"mailDetails\":{"
                + "\"subject\":\"Mail Subject\","
                + "\"formComponentPath\":\"/content/site/form/button\""
                + "},"
                + "\"postDetails\":{"
                + "\"endpoint\":\"https://api.com\","
                + "\"formComponentPath\":\"/content/site/form\""
                + "}"
                + "}";
    }


    @Test
    void testPostSuccess() throws LoginException {

        Map<String,Object> params = new HashMap<>();

        params.put("formDetails", getPostFormDetails());

        when(tokenService.generateAccessToken())
                .thenReturn("TOKEN");

        when(httpService.doPost(
                anyString(),
                anyString(),
                anyMap()))
                .thenReturn("{\"id\":\"12345\"}");

        MockSlingHttpServletResponse response =
                context.response();

        servlet.doPost(request(params), response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testMailSuccess() {

        Map<String,Object> params = new HashMap<>();

        params.put("formDetails", getMailFormDetails());

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class)))
                .thenReturn(true);

        MockSlingHttpServletResponse response =
                context.response();

        servlet.doPost(request(params), response);

        assertEquals(200, response.getStatus());
    }

    /*@Test
    void testPostAndMailSuccess() throws LoginException {

        Map<String,Object> params = new HashMap<>();

        params.put("formDetails", getPostAndMailDetails());

        when(tokenService.generateAccessToken())
                .thenReturn("TOKEN");

        when(httpService.doPost(
                anyString(),
                anyString(),
                anyMap()))
                .thenReturn("{\"id\":\"111\"}");

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class)))
                .thenReturn(true);

        MockSlingHttpServletResponse response =
                context.response();

        servlet.doPost(request(params), response);

        assertEquals(200, response.getStatus());
    }*/


    @Test
    void testInvalidJson() {

        Map<String,Object> params = new HashMap<>();

        params.put("formDetails", "INVALID_JSON");

        MockSlingHttpServletResponse response =
                context.response();

        assertThrows(
                Exception.class,
                () -> servlet.doPost(request(params), response));
    }

    @Test
    void testMissingFormDetails() {

        Map<String, Object> params = new HashMap<>();

        MockSlingHttpServletResponse res = response();

        servlet.doPost(request(params), res);

        assertTrue(res.getOutputAsString().contains("Bad Request"));
    }




    @Test
    void testEmptyApiResponse() throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put("formDetails", getPostFormDetails());

        // when(tokenService.getToken()).thenReturn("TOKEN");

        when(httpService.doPost(anyString(), anyString(), anyMap())).thenReturn("");

        MockSlingHttpServletResponse res = response();

        servlet.doPost(request(params), res);

        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void testEmailSuccess() throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put("formDetails", getPostFormDetails());

        // when(tokenService.getToken()).thenReturn("TOKEN");

        when(httpService.doPost(anyString(), anyString(), anyMap())).thenReturn("{\"success\":\"true\"}");

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class))).thenReturn(true);

        MockSlingHttpServletResponse res = response();

        servlet.doPost(request(params), res);

        assertEquals(SC_OK, res.getStatus());
    }

    @Test
    void testEmailFailure() throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put("formDetails", getPostFormDetails());

        // when(tokenService.getToken()).thenReturn("TOKEN");

        when(httpService.doPost(anyString(), anyString(), anyMap())).thenReturn("{\"success\":\"true\"}");

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class))).thenReturn(false);

        MockSlingHttpServletResponse res = response();

        servlet.doPost(request(params), res);

        assertNotNull(res.getOutputAsString());
    }



    @Test
    void testCheckCaptchaEnabledResourceNull()
            throws Exception {

        MockSlingHttpServletRequest request = context.request();

        request.setParameterMap(
                Collections.singletonMap(PAMBConstants.FORM_DETAILS_PARAM,
                        "{"
                                + "\"formType\":\"post\","
                                + "\"postDetails\":{"
                                + "\"formComponentPath\":\"/invalid\""
                                + "}"
                                + "}"));

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("checkCaptchaEnabled", SlingHttpServletRequest.class);

        method.setAccessible(true);

        boolean result = (boolean) method.invoke(servlet, request);

        assertFalse(result);
    }

    @Test
    void testCheckCaptchaEnabledMissingComponentPath()
            throws Exception {

        MockSlingHttpServletRequest request = context.request();

        request.setParameterMap(
                Collections.singletonMap(PAMBConstants.FORM_DETAILS_PARAM,
                        "{"
                                + "\"formType\":\"post\","
                                + "\"postDetails\":{}"
                                + "}"));

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("checkCaptchaEnabled", SlingHttpServletRequest.class);method.setAccessible(true);

        boolean result = (boolean) method.invoke(servlet, request);

        assertFalse(result);
    }

    @Test
    void testFindFormButton()
            throws Exception {

        Resource parent = context.create().resource("/content/test");

        Resource button = context.create().resource("/content/test/button", "sling:resourceType", PAMBConstants.FORM_BUTTON_RESOURCE_TYPE);

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("findFormButton", Resource.class);
        method.setAccessible(true);

        Resource result = (Resource) method.invoke(servlet, parent);

        assertNotNull(result);
        assertEquals(button.getPath(), result.getPath());
    }

    @Test
    void testFindFormButtonNull()
            throws Exception {

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("findFormButton",Resource.class);

        method.setAccessible(true);

        Resource result = (Resource) method.invoke(servlet, (Object) null);

        assertNull(result);
    }



    @Test
    void testDoPostBadRequestBranch()
            throws Exception {

        Map<String, Object> params = new HashMap<>();

        params.put(PAMBConstants.FORM_DETAILS_PARAM,
                "{"
                        + "\"formType\":\"post\","
                        + "\"postDetails\":{"
                        + "\"formComponentPath\":\"/invalid\""
                        + "}"
                        + "}");

        MockSlingHttpServletResponse response = context.response();

        servlet.doPost(request(params), response);

        assertTrue(response.getOutputAsString().contains("Bad Request"));
    }

    @Test
    void testProcessApiResponseFailure()
            throws Exception {

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("processApiResponse", String.class);

        method.setAccessible(true);

        JsonObject result = (JsonObject) method.invoke(servlet, "{\"error\":\"failed\"}");

        assertEquals(PAMBConstants.FAILED, result.get(PAMBConstants.STATUS).getAsString());
    }



    @Test
    void testGetMailDetaisSuccess() {

        context.create().resource("/content/testMail", "toEmailIds", new String[]{"test1@test.com", "test2@test.com"}, "ccEmailIds", new String[]{"cc@test.com"});

        JsonObject mailDetails = new JsonObject();
        mailDetails.addProperty("formComponentPath", "/content/testMail");

        JsonObject result = servlet.getMailDetais(context.request(), mailDetails);

        assertEquals("test1@test.com,test2@test.com", result.get(PAMBConstants.TO).getAsString());

        assertEquals("cc@test.com", result.get(PAMBConstants.CC).getAsString());
    }

    @Test
    void testGetMailDetaisNull() {

        JsonObject result = servlet.getMailDetais(context.request(), null);

        assertNull(result);
    }

    @Test
    void testGetMailDetaisResourceNull() {

        JsonObject mailDetails = new JsonObject();

        mailDetails.addProperty("formComponentPath", "/invalid/path");

        JsonObject result = servlet.getMailDetais(context.request(), mailDetails);

        assertNotNull(result);
        assertFalse(result.has(PAMBConstants.TO));
    }

    @Test
    void testExtractPlaceholders() throws Exception {

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("extractPlaceholders", String.class);

        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> result = (Set<String>) method.invoke(servlet, "Hello ${name} ${email}");

        assertEquals(2, result.size());

        assertTrue(result.contains("name"));
        assertTrue(result.contains("email"));
    }

    @Test
    void testGetMailTemplateValues() throws Exception {

        Set<String> placeholders = new HashSet<>();

        placeholders.add("name");
        placeholders.add("email");

        when(request.getParameter("name")).thenReturn("John");

        when(request.getParameter("email")).thenReturn(null);

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("getMailTemplateValues", SlingHttpServletRequest.class, Set.class);

        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(servlet, request, placeholders);

        assertEquals("John", result.get("name"));

        assertEquals("", result.get("email"));
    }

    @Test
    void testGenerateMailBodyResourceNotFound()
            throws Exception {

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("generateMailBody",String.class, SlingHttpServletRequest.class);

        method.setAccessible(true);

        String result = (String) method.invoke(servlet, "/invalid/template", context.request());

        assertEquals("", result);
    }

    @Test
    void testSendEmailSuccess() throws Exception {

        StringWriter stringWriter = new StringWriter();

        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        when(request.getResourceResolver()).thenReturn(context.resourceResolver());

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class))).thenReturn(true);

        JsonObject responseObject = new JsonObject();

        JsonObject mailDetails = new JsonObject();
        mailDetails.addProperty(PAMBConstants.SUBJECT, "Test");

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("sendEmail", SlingHttpServletRequest.class, SlingHttpServletResponse.class, JsonObject.class, JsonObject.class);

        method.setAccessible(true);

        method.invoke(servlet, request, response, mailDetails, responseObject);

        verify(response).setStatus(SC_OK);

        assertTrue(responseObject.has(PAMBConstants.STATUS));

        assertFalse(stringWriter.toString().isEmpty());
    }

    @Test
    void testSendEmailFailureBranch()
            throws Exception {

        StringWriter stringWriter = new StringWriter();

        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        when(request.getResourceResolver()).thenReturn(context.resourceResolver());

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class))).thenReturn(false);

        JsonObject responseObject = new JsonObject();

        JsonObject mailDetails = new JsonObject();
        mailDetails.addProperty(PAMBConstants.SUBJECT, "Test");

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("sendEmail", SlingHttpServletRequest.class, SlingHttpServletResponse.class, JsonObject.class, JsonObject.class);

        method.setAccessible(true);

        method.invoke(servlet, request, response, mailDetails, responseObject);

        verify(response).setStatus(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    void testGetEmailParameters()
            throws Exception {

        when(request.getResourceResolver()).thenReturn(context.resourceResolver());

        JsonObject mailDetails = new JsonObject();

        mailDetails.addProperty(PAMBConstants.SUBJECT, "Test Subject");

        mailDetails.addProperty(PAMBConstants.TO, "to@test.com");

        mailDetails.addProperty(PAMBConstants.CC, "cc@test.com");

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("getEmailParameters", SlingHttpServletRequest.class, JsonObject.class);

        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(servlet, request, mailDetails);

        assertEquals("to@test.com", result.get(PAMBConstants.TO_EMAIL_IDS));

        assertEquals("cc@test.com", result.get(PAMBConstants.CC_EMAIL_IDS));

        assertEquals("Test Subject", result.get(PAMBConstants.SUBJECT));
    }

    @Test
    void testGetEmailParametersMergeEmails()
            throws Exception {

        when(request.getResourceResolver()).thenReturn(context.resourceResolver());

        when(request.getParameter(PAMBConstants.EMAIL)).thenReturn("user@test.com");

        JsonObject mailDetails = new JsonObject();

        mailDetails.addProperty(PAMBConstants.SUBJECT, "Subject");

        mailDetails.addProperty(PAMBConstants.TO, "admin@test.com");

        Method method = PAMBContactUsServlet.class.getDeclaredMethod("getEmailParameters", SlingHttpServletRequest.class, JsonObject.class);

        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(servlet, request, mailDetails);

        assertTrue(result.get(PAMBConstants.TO_EMAIL_IDS).contains("admin@test.com"));

        assertTrue(result.get(PAMBConstants.TO_EMAIL_IDS).contains("user@test.com"));
    }

    @Test
    void testCreateFormDetailsMailSeparateTemplates() throws Exception {

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class))).thenReturn(true);

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.setParameterMap(Collections.singletonMap(
                PAMBConstants.FORM_DETAILS_PARAM,
                "{"
                        + "\"formType\":\"mail_separate_templates\","
                        + "\"mailDetails\":{"
                        + "\"subject\":\"Test Subject\","
                        + "\"formComponentPath\":\"/content/testMail\""
                        + "}"
                        + "}"));

        context.create().resource(
                "/content/testMail",
                "toEmailIds", new String[]{"test@test.com"});

        Method method = PAMBContactUsServlet.class
                .getDeclaredMethod("createFormDetails",
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class);

        method.setAccessible(true);
        method.invoke(servlet, request, response);

        assertEquals(200, response.getStatus());
    }

    @Test
    void testCreateFormDetailsPostAndMailSuccess() throws Exception {

        when(tokenService.generateAccessToken()).thenReturn("TOKEN");

        when(httpService.doPost(anyString(), anyString(), anyMap()))
                .thenReturn("{\"id\":\"123\"}");

        when(emailService.sendEmail(anyMap(), any(SlingHttpServletRequest.class)))
                .thenReturn(true);

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.setParameterMap(Collections.singletonMap(
                PAMBConstants.FORM_DETAILS_PARAM,
                "{"
                        + "\"formType\":\"post_and_mail\","
                        + "\"mailDetails\":{"
                        + "\"subject\":\"Subject\","
                        + "\"formComponentPath\":\"/content/testMail\""
                        + "},"
                        + "\"postDetails\":{"
                        + "\"endpoint\":\"https://api.com\""
                        + "}"
                        + "}"));

        context.create().resource(
                "/content/testMail",
                "toEmailIds", new String[]{"admin@test.com"});

        Method method = PAMBContactUsServlet.class
                .getDeclaredMethod("createFormDetails",
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class);

        method.setAccessible(true);
        method.invoke(servlet, request, response);

        assertEquals(200, response.getStatus());
    }

    /*@Test
    void testCreateFormDetailsPostAndMailFailure() throws Exception {

        when(tokenService.generateAccessToken()).thenReturn("TOKEN");

        when(httpService.doPost(anyString(), anyString(), anyMap()))
                .thenReturn("{\"error\":\"failed\"}");

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.setParameterMap(Collections.singletonMap(
                PAMBConstants.FORM_DETAILS_PARAM,
                "{"
                        + "\"formType\":\"" + PAMBConstants.POST_AND_MAIL + "\","
                        + "\"mailDetails\":{"
                        + "\"subject\":\"Subject\","
                        + "\"formComponentPath\":\"/content/testMail\""
                        + "},"
                        + "\"postDetails\":{"
                        + "\"endpoint\":\"https://api.com\""
                        + "}"
                        + "}"));

        Method method = PAMBContactUsServlet.class.getDeclaredMethod(
                "createFormDetails",
                SlingHttpServletRequest.class,
                SlingHttpServletResponse.class);

        method.setAccessible(true);
        method.invoke(servlet, request, response);

        JsonObject json =
                JsonParser.parseString(response.getOutputAsString())
                        .getAsJsonObject();

        assertEquals("failed",
                json.get("status").getAsString());
    }*/

    @Test
    void testCreateFormDetailsExceptionBranch() throws Exception {

        when(tokenService.generateAccessToken())
                .thenThrow(new RuntimeException("Token Error"));

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.setParameterMap(Collections.singletonMap(
                PAMBConstants.FORM_DETAILS_PARAM,
                getPostFormDetails()));

        Method method = PAMBContactUsServlet.class
                .getDeclaredMethod("createFormDetails",
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class);

        method.setAccessible(true);
        method.invoke(servlet, request, response);

        assertEquals(500, response.getStatus());
        assertTrue(
                response.getOutputAsString()
                        .contains("Internal server error"));
    }


    @Test
    void testCreateFormDetailsPostBlankEndpoint() throws Exception {

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();

        request.setParameterMap(Collections.singletonMap(
                PAMBConstants.FORM_DETAILS_PARAM,
                "{"
                        + "\"formType\":\"post\","
                        + "\"postDetails\":{"
                        + "\"endpoint\":\"\""
                        + "}"
                        + "}"));

        Method method = PAMBContactUsServlet.class
                .getDeclaredMethod("createFormDetails",
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class);

        method.setAccessible(true);
        method.invoke(servlet, request, response);

        verifyNoInteractions(httpService);
    }



}