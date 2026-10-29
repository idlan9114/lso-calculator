package com.prudential.pamb.core.models.dto.legacyblueprint;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * HTTP status and JSON body returned by the Legacy Blueprint client API. The body is passed back to
 * the frontend as-is; it is {@code null} when the API sent no body or a body that isn't JSON.
 */
public class LegacyBlueprintApiResult {

    private final int status;
    private final JsonNode body;

    public LegacyBlueprintApiResult(int status, JsonNode body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public JsonNode getBody() {
        return body;
    }

    /** The {@code id} field of the body (e.g. {@code 6a2fc52ff68b9017691b10c8}), or {@code null}. */
    public String getReferenceId() {
        JsonNode id = body == null ? null : body.get("id");
        return id != null && id.isValueNode() ? id.asText() : null;
    }
}
