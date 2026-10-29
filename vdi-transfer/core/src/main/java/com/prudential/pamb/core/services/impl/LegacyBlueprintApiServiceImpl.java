package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintApiResult;
import com.prudential.pamb.core.services.LegacyBlueprintApiService;
import com.prudential.pamb.core.services.LegacyBlueprintTokenService;
import com.prudential.pamb.core.utils.RsaJwtSigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Locale;

/**
 * Default {@link LegacyBlueprintApiService}: authenticated JSON POST to
 * {@code LEGACY_BP_API_URL + /TE/api/v1/te/legacyBlueprints} with one retry after a 401. Request
 * and response bodies are never logged because they contain personal data.
 */
@Component(service = LegacyBlueprintApiService.class, immediate = true)
@Slf4j
public class LegacyBlueprintApiServiceImpl implements LegacyBlueprintApiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int MAX_RESPONSE_CHARS = 16 * 1024;
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String APPLICATION_JSON = "application/json";

    @Reference
    private LegacyBlueprintConfig config;

    @Reference
    private LegacyBlueprintTokenService tokenService;

    // Reusable and thread-safe; created on first use so it picks up the configured connect timeout.
    private volatile HttpClient httpClient;

    @Override
    public LegacyBlueprintApiResult submit(ObjectNode payload) throws LegacyBlueprintApiException {
        String url = buildEndpoint();
        String body;
        try {
            body = MAPPER.writeValueAsString(payload);
        } catch (IOException e) {
            throw new LegacyBlueprintApiException("Unable to serialise the Legacy Blueprint payload.", e);
        }

        HttpResponse<String> response = executePost(url, body, tokenService.getToken());
        if (response.statusCode() == HTTP_UNAUTHORIZED) {
            log.warn("LegacyBlueprint API: received 401. Invalidating token and retrying once.");
            tokenService.invalidate();
            response = executePost(url, body, tokenService.getToken());
        }

        int status = response.statusCode();
        JsonNode responseBody = parseBody(response.body());
        if (status < 200 || status >= 300) {
            throw new LegacyBlueprintApiException("Legacy Blueprint API returned status " + status + ".", status, responseBody, null);
        }
        return new LegacyBlueprintApiResult(status, responseBody);
    }

    private String buildEndpoint() throws LegacyBlueprintApiException {
        String base = config.getApiBaseUrl();
        if (RsaJwtSigner.isUnresolved(base)) {
            throw new LegacyBlueprintApiException("Legacy Blueprint API base URL (LEGACY_BP_API_URL) is not configured.");
        }
        String scheme = StringUtils.substringBefore(base, "://").toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            throw new LegacyBlueprintApiException("LEGACY_BP_API_URL must be an http(s) URL.");
        }
        return base + LegacyBlueprintConstants.API_PATH;
    }

    // The response contract is only known from a sample ({"code","id","status"}), so any 2xx is
    // treated as accepted. Only a JSON object of reasonable size is passed on to the frontend.
    private static JsonNode parseBody(String body) {
        if (StringUtils.isBlank(body) || body.length() > MAX_RESPONSE_CHARS) {
            return null;
        }
        try {
            JsonNode json = MAPPER.readTree(body);
            return json != null && json.isObject() ? json : null;
        } catch (IOException e) {
            log.warn("LegacyBlueprint API: response body is not JSON; it is not passed to the frontend.");
            return null;
        }
    }

    // Protected so unit tests can stub the transport without a live endpoint.
    protected HttpResponse<String> executePost(String url, String body, String token) throws LegacyBlueprintApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
                    .header(HEADER_AUTHORIZATION, BEARER_PREFIX + token)
                    .header(HEADER_CONTENT_TYPE, APPLICATION_JSON)
                    .header(HEADER_ACCEPT, APPLICATION_JSON)
                    .build();
            return client().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new LegacyBlueprintApiException("Legacy Blueprint API timed out.", e);
        } catch (IOException | IllegalArgumentException e) {
            throw new LegacyBlueprintApiException("Transport error calling the Legacy Blueprint API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LegacyBlueprintApiException("Interrupted while calling the Legacy Blueprint API.", e);
        }
    }

    private HttpClient client() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(config.getConnectTimeoutSeconds()))
                            .followRedirects(HttpClient.Redirect.NEVER)
                            .build();
                    httpClient = client;
                }
            }
        }
        return client;
    }
}
