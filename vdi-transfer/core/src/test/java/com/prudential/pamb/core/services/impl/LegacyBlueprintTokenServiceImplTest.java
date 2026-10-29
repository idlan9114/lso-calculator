package com.prudential.pamb.core.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyBlueprintTokenServiceImplTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ISSUER = "https://pru-api-nprd.example.com";

    @Mock
    private LegacyBlueprintConfig config;

    @InjectMocks
    private LegacyBlueprintTokenServiceImpl service;

    private PublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        // Test-only key pair generated per run; no real key material is used.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        publicKey = keyPair.getPublic();
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----";

        lenient().when(config.getSigningKey()).thenReturn(pem);
        lenient().when(config.getIssuer()).thenReturn(ISSUER);
        lenient().when(config.getSubject()).thenReturn("website");
        lenient().when(config.getAudience()).thenReturn("$[env:LEGACY_BP_JWT_AUDIENCE]");
    }

    @Test
    void signsRs256WithIssSubIatExp() throws Exception {
        String[] parts = service.getToken().split("\\.");

        JsonNode header = decode(parts[0]);
        assertEquals("RS256", header.get("alg").asText());
        JsonNode claims = decode(parts[1]);
        assertEquals(ISSUER, claims.get("iss").asText());
        assertEquals("website", claims.get("sub").asText());
        assertEquals(claims.get("iat").asLong() + 3600, claims.get("exp").asLong());
        assertFalse(claims.has("aud"), "unresolved audience placeholder must be omitted");
        assertTrue(verify(parts));
    }

    @Test
    void addsAudienceWhenConfigured() throws Exception {
        when(config.getAudience()).thenReturn("legacy-blueprints");
        JsonNode claims = decode(service.getToken().split("\\.")[1]);
        assertEquals("legacy-blueprints", claims.get("aud").asText());
    }

    @Test
    void cachesUntilInvalidated() throws Exception {
        String first = service.getToken();
        assertSame(first, service.getToken());
        service.invalidate();
        assertNotSame(first, service.getToken());
    }

    @Test
    void missingIssuerOrKey_fails() {
        when(config.getIssuer()).thenReturn("$[env:LEGACY_BP_JWT_ISSUER]");
        assertThrows(LegacyBlueprintApiException.class, service::getToken);

        when(config.getIssuer()).thenReturn(ISSUER);
        when(config.getSigningKey()).thenReturn("$[secret:LEGACY_BP_JWT_SECRET]");
        LegacyBlueprintApiException e = assertThrows(LegacyBlueprintApiException.class, service::getToken);
        assertTrue(e.getMessage().contains("LEGACY_BP_JWT_SECRET"));
    }

    private static JsonNode decode(String part) throws Exception {
        return MAPPER.readTree(Base64.getUrlDecoder().decode(part));
    }

    private boolean verify(String[] parts) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(parts[2]));
    }
}
