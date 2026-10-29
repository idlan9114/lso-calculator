package com.prudential.pamb.core.constants;

/**
 * Constants for the Fund Prices component. API paths are built from the issuer/base URL plus
 * the path suffixes below - only the issuer comes from the environment, never the full URL.
 */
public final class FundPricesConstants {

    private FundPricesConstants() {
    }

    public static final String RESOURCE_TYPE = "prudential-aem-malaysia/components/content/fundPrices";

    // Table Type dialog values.
    public static final String TABLE_TYPE_FUND_PRICE = "fundPrice";
    public static final String TABLE_TYPE_FUND_PRICE_HISTORY = "fundPriceHistory";

    // Appended to the issuer/base URL to form the endpoints.
    public static final String PATH_FUND_PRICE = "/TE/api/v1/te/fundprice";
    public static final String PATH_FUND_PRICE_HISTORY = "/TE/api/v1/te/fundpricehistory";

    // JWT claims and cache (RS256 signing lives in utils.RsaJwtSigner).
    public static final String DEFAULT_SUBJECT = "website";
    public static final long IAT_SKEW_SECONDS = 30L;
    public static final long TOKEN_TTL_SECONDS = 3600L;
    public static final long CACHE_REFRESH_BUFFER_SECONDS = 60L;

    // Request headers.
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
}
