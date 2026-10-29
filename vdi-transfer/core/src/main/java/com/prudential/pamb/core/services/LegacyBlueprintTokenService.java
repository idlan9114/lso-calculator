package com.prudential.pamb.core.services;

import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;

/**
 * Generates and caches the RS256 JWT used to authenticate the Legacy Blueprint client API.
 */
public interface LegacyBlueprintTokenService {

    /**
     * Returns a valid signed JWT, reusing the cached token until shortly before it expires.
     *
     * @throws LegacyBlueprintApiException if the key or issuer is missing or the key is invalid
     */
    String getToken() throws LegacyBlueprintApiException;

    /** Drops the cached token so the next call signs a fresh one (used after an HTTP 401). */
    void invalidate();
}
