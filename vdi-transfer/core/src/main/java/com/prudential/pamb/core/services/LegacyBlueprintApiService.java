package com.prudential.pamb.core.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintApiResult;

/**
 * Posts a completed Legacy Blueprint to the client-owned API.
 */
public interface LegacyBlueprintApiService {

    /**
     * Sends the payload with a Bearer JWT. A 401 invalidates the token and is retried once.
     *
     * @param payload request body built by {@code LegacyBlueprintPayloadMapper}
     * @return the 2xx status and JSON body returned by the API
     * @throws LegacyBlueprintApiException on configuration, transport, timeout or non-2xx errors
     *                                     (carrying the status and JSON error body when there was a response)
     */
    LegacyBlueprintApiResult submit(ObjectNode payload) throws LegacyBlueprintApiException;
}
