package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintApiResult;
import com.prudential.pamb.core.services.LegacyBlueprintTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyBlueprintApiServiceImplTest {

    private static final String BASE = "https://pru-api-nprd.example.com";
    private static final String ENDPOINT = BASE + "/TE/api/v1/te/legacyBlueprints";

    @Mock
    private LegacyBlueprintConfig config;

    @Mock
    private LegacyBlueprintTokenService tokenService;

    @Spy
    @InjectMocks
    private LegacyBlueprintApiServiceImpl service;

    private ObjectNode payload;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(config.getApiBaseUrl()).thenReturn(BASE);
        lenient().when(tokenService.getToken()).thenReturn("token-1", "token-2");
        payload = new ObjectMapper().createObjectNode().put("fullName", "Test");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        lenient().when(response.statusCode()).thenReturn(status);
        lenient().when(response.body()).thenReturn(body);
        return response;
    }

    @Test
    void success_returnsReferenceId() throws Exception {
        doReturn(response(200, "{\"code\":\"200\",\"id\":\"6a2fc52f\",\"status\":\"accepted\"}"))
                .when(service).executePost(eq(ENDPOINT), anyString(), eq("token-1"));

        LegacyBlueprintApiResult result = service.submit(payload);
        assertEquals(200, result.getStatus());
        assertEquals("6a2fc52f", result.getReferenceId());
        assertEquals("accepted", result.getBody().get("status").asText());
        verify(tokenService, never()).invalidate();
    }

    @Test
    void success_withoutJsonBody_hasNoBodyOrReference() throws Exception {
        doReturn(response(202, "accepted")).when(service).executePost(eq(ENDPOINT), anyString(), anyString());
        LegacyBlueprintApiResult result = service.submit(payload);
        assertEquals(202, result.getStatus());
        assertNull(result.getBody());
        assertNull(result.getReferenceId());
    }

    @Test
    void unauthorized_refreshesTokenAndRetriesOnce() throws Exception {
        doReturn(response(401, "")).when(service).executePost(ENDPOINT, "{\"fullName\":\"Test\"}", "token-1");
        doReturn(response(201, "{\"id\":\"x\"}")).when(service).executePost(ENDPOINT, "{\"fullName\":\"Test\"}", "token-2");

        assertEquals("x", service.submit(payload).getReferenceId());
        verify(tokenService).invalidate();
        verify(service, times(2)).executePost(eq(ENDPOINT), anyString(), anyString());
    }

    @Test
    void serverError_throwsWithStatus() throws Exception {
        doReturn(response(503, "busy")).when(service).executePost(eq(ENDPOINT), anyString(), anyString());

        LegacyBlueprintApiException e = assertThrows(LegacyBlueprintApiException.class, () -> service.submit(payload));
        assertEquals(503, e.getStatus());
        assertNull(e.getResponseBody());
    }

    @Test
    void clientError_carriesJsonErrorBody() throws Exception {
        doReturn(response(400, "{\"code\":\"400\",\"message\":\"countryNationalId is invalid\"}"))
                .when(service).executePost(eq(ENDPOINT), anyString(), anyString());

        LegacyBlueprintApiException e = assertThrows(LegacyBlueprintApiException.class, () -> service.submit(payload));
        assertEquals(400, e.getStatus());
        assertEquals("countryNationalId is invalid", e.getResponseBody().get("message").asText());
    }

    @Test
    void timeout_isReportedAsApiFailure() throws Exception {
        doThrow(new LegacyBlueprintApiException("Legacy Blueprint API timed out.", new HttpTimeoutException("t")))
                .when(service).executePost(eq(ENDPOINT), anyString(), anyString());

        assertThrows(LegacyBlueprintApiException.class, () -> service.submit(payload));
    }

    @Test
    void missingOrInvalidBaseUrl_failsBeforeCalling() throws Exception {
        when(config.getApiBaseUrl()).thenReturn("$[env:LEGACY_BP_API_URL]");
        assertThrows(LegacyBlueprintApiException.class, () -> service.submit(payload));

        when(config.getApiBaseUrl()).thenReturn("file:///etc/passwd");
        assertThrows(LegacyBlueprintApiException.class, () -> service.submit(payload));
        verify(service, never()).executePost(anyString(), anyString(), anyString());
    }
}
