package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.exceptions.JwtSigningException;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.services.LegacyBlueprintTokenService;
import com.prudential.pamb.core.utils.JwtTokenCache;
import com.prudential.pamb.core.utils.RsaJwtSigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.time.Instant;

/**
 * RS256 JWT for the Legacy Blueprint API: claims {@code iss}, {@code sub}, {@code iat}
 * (now - 30s), {@code exp} (iat + 1h) and, only when configured, {@code aud}. Cached in memory
 * until 60 seconds before expiry.
 */
@Component(service = LegacyBlueprintTokenService.class, immediate = true)
@Slf4j
public class LegacyBlueprintTokenServiceImpl implements LegacyBlueprintTokenService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Reference
    private LegacyBlueprintConfig config;

    private final JwtTokenCache tokenCache = new JwtTokenCache(LegacyBlueprintConstants.IAT_SKEW_SECONDS,
            LegacyBlueprintConstants.TOKEN_TTL_SECONDS, LegacyBlueprintConstants.CACHE_REFRESH_BUFFER_SECONDS);

    @Override
    public String getToken() throws LegacyBlueprintApiException {
        return tokenCache.get(Instant.now().getEpochSecond(), this::sign);
    }

    @Override
    public void invalidate() {
        log.debug("LegacyBlueprint JWT: invalidating cached token.");
        tokenCache.invalidate();
    }

    private String sign(long iat, long exp) throws LegacyBlueprintApiException {
        String issuer = config.getIssuer();
        if (RsaJwtSigner.isUnresolved(issuer)) {
            throw new LegacyBlueprintApiException("Cannot generate JWT: issuer (LEGACY_BP_JWT_ISSUER) is not configured.");
        }

        ObjectNode claims = MAPPER.createObjectNode();
        claims.put("iss", issuer);
        claims.put("sub", StringUtils.defaultIfBlank(config.getSubject(), LegacyBlueprintConstants.DEFAULT_SUBJECT));
        if (!RsaJwtSigner.isUnresolved(config.getAudience())) {
            claims.put("aud", config.getAudience());
        }
        claims.put("iat", iat);
        claims.put("exp", exp);

        try {
            String token = RsaJwtSigner.sign(claims, config.getSigningKey(), LegacyBlueprintConstants.SIGNING_KEY_VAR);
            log.info("LegacyBlueprint JWT: generated new token (iat={}, exp={}).", iat, exp);
            return token;
        } catch (JwtSigningException e) {
            throw new LegacyBlueprintApiException(e.getMessage(), e);
        }
    }
}
