package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.FundPricesConfig;
import com.prudential.pamb.core.constants.FundPricesConstants;
import com.prudential.pamb.core.exceptions.FundPricesApiException;
import com.prudential.pamb.core.exceptions.JwtSigningException;
import com.prudential.pamb.core.services.JwtTokenService;
import com.prudential.pamb.core.utils.JwtTokenCache;
import com.prudential.pamb.core.utils.RsaJwtSigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.time.Instant;

/**
 * Generates and caches the RS256 JWT for the Fund Prices APIs. Signing and caching are shared
 * with the Legacy Blueprint integration via {@link RsaJwtSigner} and {@link JwtTokenCache}.
 */
@Component(service = JwtTokenService.class, immediate = true)
@Slf4j
public class JwtTokenServiceImpl implements JwtTokenService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SIGNING_KEY_VAR = "FUND_PRICES_CMP_JWT_SIGNING_KEY";

    @Reference
    private FundPricesConfig config;

    private final JwtTokenCache tokenCache = new JwtTokenCache(FundPricesConstants.IAT_SKEW_SECONDS,
            FundPricesConstants.TOKEN_TTL_SECONDS, FundPricesConstants.CACHE_REFRESH_BUFFER_SECONDS);

    @Override
    public String getToken() throws FundPricesApiException {
        return tokenCache.get(Instant.now().getEpochSecond(), this::sign);
    }

    @Override
    public void invalidate() {
        log.debug("FundPrices JWT: invalidating cached token.");
        tokenCache.invalidate();
    }

    private String sign(long iat, long exp) throws FundPricesApiException {
        String issuer = config.getIssuer();
        if (StringUtils.isBlank(issuer)) {
            throw new FundPricesApiException("Cannot generate JWT: issuer (FUND_PRICES_CMP_JWT_ISS) is not configured.");
        }

        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("iss", issuer);
        payload.put("sub", StringUtils.defaultIfBlank(config.getSubject(), FundPricesConstants.DEFAULT_SUBJECT));
        payload.put("iat", iat);
        payload.put("exp", exp);

        try {
            String token = RsaJwtSigner.sign(payload, config.getSigningKey(), SIGNING_KEY_VAR);
            log.info("FundPrices JWT: generated new token (iat={}, exp={}).", iat, exp);
            return token;
        } catch (JwtSigningException e) {
            throw new FundPricesApiException(e.getMessage(), e);
        }
    }
}
