package com.prudential.pamb.core.configurations.impl;

import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * OSGi-backed {@link LegacyBlueprintConfig}. Values come from the {@code $[env:...]} config in
 * {@code ui.config}, which resolves the LEGACY_BP_* environment variables on Adobe Cloud.
 */
@Component(service = LegacyBlueprintConfig.class, immediate = true)
@Designate(ocd = LegacyBlueprintConfigImpl.Configuration.class)
public class LegacyBlueprintConfigImpl implements LegacyBlueprintConfig {

    private String apiBaseUrl;
    private String signingKey;
    private String issuer;
    private String subject;
    private String audience;
    private String mailFrom;
    private int connectTimeoutSeconds;
    private int requestTimeoutSeconds;
    private boolean trackingAllowed;

    @Override
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    @Override
    public String getSigningKey() {
        return signingKey;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getAudience() {
        return audience;
    }

    @Override
    public String getMailFrom() {
        return mailFrom;
    }

    @Override
    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    @Override
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    @Override
    public boolean isTrackingAllowed() {
        return trackingAllowed;
    }

    @Activate
    @Modified
    protected void activate(Configuration config) {
        this.apiBaseUrl = stripTrailingSlash(config.apiBaseUrl());
        this.signingKey = config.signingKey();
        this.issuer = config.issuer();
        this.subject = config.subject();
        this.audience = config.audience();
        this.mailFrom = config.mailFrom();
        this.connectTimeoutSeconds = Math.max(1, config.connectTimeoutSeconds());
        this.requestTimeoutSeconds = Math.max(1, config.requestTimeoutSeconds());
        this.trackingAllowed = config.trackingAllowed();
    }

    // API_PATH starts with '/', so a trailing slash here would double up.
    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    @ObjectClassDefinition(name = "Legacy Blueprint (LSO Calculator) Configuration",
            description = "Client API endpoint, JWT signing and sender settings for the Legacy Blueprint Planner.")
    public @interface Configuration {

        @AttributeDefinition(name = "API base URL",
                description = "Resolved from environment variable LEGACY_BP_API_URL. The path "
                        + LegacyBlueprintConstants.API_PATH + " is appended in code.")
        String apiBaseUrl() default EMPTY;

        @AttributeDefinition(name = "JWT signing key (RSA private key, PEM)",
                description = "Resolved from secret environment variable LEGACY_BP_JWT_SECRET.",
                type = AttributeType.PASSWORD)
        String signingKey() default EMPTY;

        @AttributeDefinition(name = "JWT issuer (iss)",
                description = "Resolved from environment variable LEGACY_BP_JWT_ISSUER.")
        String issuer() default EMPTY;

        @AttributeDefinition(name = "JWT subject (sub)")
        String subject() default LegacyBlueprintConstants.DEFAULT_SUBJECT;

        @AttributeDefinition(name = "JWT audience (aud)",
                description = "Optional. Resolved from environment variable LEGACY_BP_JWT_AUDIENCE; omitted when blank.")
        String audience() default EMPTY;

        @AttributeDefinition(name = "Sender email address",
                description = "Resolved from environment variable LEGACY_BP_MAIL_FROM.")
        String mailFrom() default EMPTY;

        @AttributeDefinition(name = "Connect timeout (seconds)")
        int connectTimeoutSeconds() default 10;

        @AttributeDefinition(name = "Request timeout (seconds)")
        int requestTimeoutSeconds() default 30;

        @AttributeDefinition(name = "Allow GA tracking",
                description = "Set to false on author so component analytics events are never pushed there.")
        boolean trackingAllowed() default true;
    }
}
