package com.prudential.pamb.core.configurations;

/**
 * Legacy Blueprint (lsoCalculator) integration settings, sourced from Cloud Manager environment
 * variables via OSGi {@code $[env:...]} / {@code $[secret:...]} substitution. Never authored.
 */
public interface LegacyBlueprintConfig {

    /** Client API base URL from {@code LEGACY_BP_API_URL}; the fixed path is appended in code. */
    String getApiBaseUrl();

    /** RSA private key (PEM) from the secret {@code LEGACY_BP_JWT_SECRET}. */
    String getSigningKey();

    /** JWT {@code iss} claim from {@code LEGACY_BP_JWT_ISSUER}. */
    String getIssuer();

    /** JWT {@code sub} claim (plain OSGi value, defaults to {@code website}). */
    String getSubject();

    /** Optional JWT {@code aud} claim from {@code LEGACY_BP_JWT_AUDIENCE}; omitted when blank. */
    String getAudience();

    /** Sender address for the blueprint email from {@code LEGACY_BP_MAIL_FROM}. */
    String getMailFrom();

    int getConnectTimeoutSeconds();

    int getRequestTimeoutSeconds();

    /** Whether the component may push GA events on this instance (switched off on author). */
    boolean isTrackingAllowed();
}
