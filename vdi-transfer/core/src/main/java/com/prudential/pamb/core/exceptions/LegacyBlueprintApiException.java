package com.prudential.pamb.core.exceptions;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Thrown when the Legacy Blueprint client API call or its JWT generation fails. Messages never
 * contain request or response bodies, so they are safe to log.
 */
public class LegacyBlueprintApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final transient JsonNode responseBody;

    public LegacyBlueprintApiException(String message) {
        this(message, 0, null, null);
    }

    public LegacyBlueprintApiException(String message, Throwable cause) {
        this(message, 0, null, cause);
    }

    public LegacyBlueprintApiException(String message, int status, Throwable cause) {
        this(message, status, null, cause);
    }

    public LegacyBlueprintApiException(String message, int status, JsonNode responseBody, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    /** HTTP status returned by the API, or 0 when no response was received. */
    public int getStatus() {
        return status;
    }

    /** JSON error body returned by the API, or {@code null}. Never logged. */
    public JsonNode getResponseBody() {
        return responseBody;
    }
}
