package com.prudential.pamb.core.constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed rules and option sets for the Legacy Blueprint Planner (lsoCalculator) component, taken
 * from the v8 prototype. Authors configure copy only; the IDs, limits and the English values sent
 * to the client API are fixed here so they stay stable across languages.
 */
public final class LegacyBlueprintConstants {

    private LegacyBlueprintConstants() {
    }

    public static final String RESOURCE_TYPE = "prudential-aem-malaysia/components/content/lsoCalculator";
    public static final String SELECTOR_SUBMIT = "submit";

    /** Appended to LEGACY_BP_API_URL to form the client endpoint. */
    public static final String API_PATH = "/TE/api/v1/te/legacyBlueprints";
    public static final String EMAIL_TEMPLATE_ROOT = "/content/dam/pamb/email-templates/legacy-blueprint/";
    public static final String DEFAULT_EMAIL_SUBJECT = "Your Legacy Blueprint";

    // JWT claims and cache (same contract as the Fund Prices API).
    public static final String SIGNING_KEY_VAR = "LEGACY_BP_JWT_SECRET";
    public static final String DEFAULT_SUBJECT = "website";
    public static final long IAT_SKEW_SECONDS = 30L;
    public static final long TOKEN_TTL_SECONDS = 3600L;
    public static final long CACHE_REFRESH_BUFFER_SECONDS = 60L;

    // Business rules.
    public static final long LEGACY_MINIMUM = 1_000_000L;
    public static final long LEGACY_MAXIMUM = 1_000_000_000L;
    public static final long PLAN_STEP = 1_000_000L;
    public static final int MAX_PEOPLE = 5;
    public static final int MAX_MILESTONES = 5;
    public static final int INTRO_SCALE_MAX = 5;
    public static final long MONTHLY_PAYMENT_MINIMUM = 2_000L;
    public static final long YEARLY_PAYMENT_MINIMUM = 24_000L;
    public static final int MAX_PAYMENT_YEARS = 100;
    public static final int MAX_STEP_UP_RATE = 100;
    public static final int MAX_START_AGE = 120;
    public static final int DEFAULT_MILESTONE_AGE = 21;
    public static final int DEFAULT_WITHIN_YEARS = 10;
    public static final int MAX_WITHIN_YEARS = 50;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_REQUEST_BYTES = 64 * 1024;

    public static final String MODE_KNOWN = "known";
    public static final String MODE_UNSURE = "unsure";

    public static final String PART_LUMP = "lump";
    public static final String PART_STRUCTURED = "structured";
    public static final String PART_MILESTONES = "milestones";
    public static final List<String> PARTS = list(PART_LUMP, PART_STRUCTURED, PART_MILESTONES);

    public static final String FREQUENCY_MONTHLY = "monthly";
    public static final String FREQUENCY_YEARLY = "yearly";
    public static final String START_NOW = "now";
    public static final String START_AGE = "age";
    public static final String MILESTONE_SPECIAL_AGE = "age";

    /** Relationship values (case-insensitive) with a maximum count; values come from the authored options. */
    public static final Map<String, Integer> RELATIONSHIP_LIMITS = map("spouse", 1, "parent", 2);

    // Stable option IDs -> fixed English values sent to the client API (matches Legacy-Blueprints-Request.json).
    public static final Map<String, String> NEEDS = map(
            "everyday", "Everyday financial security",
            "education", "Education or a first home",
            "care", "Healthcare or caregiving",
            "family", "Marriage or starting a family",
            "retirement", "Retirement and later years");

    public static final Map<String, String> PEACE_OF_MIND = map(
            "enough", "Knowing they'll have enough",
            "timing", "Support at the right time",
            "flex", "Giving them flexibility",
            "wishes", "Knowing my wishes are followed",
            "unsure", "I'm not sure yet");

    public static final Map<String, String> AGE_RANGES = map(
            "under35", "Under 35",
            "35-44", "35-44",
            "45-54", "45-54",
            "55-64", "55-64",
            "65plus", "65 and above",
            "na", "Prefer not to say");

    public static final Map<String, String> RELATIONSHIP_STATUSES = map(
            "single", "Single",
            "married", "Married",
            "divorced", "Divorced / separated",
            "widowed", "Widowed",
            "na", "Prefer not to say");

    public static final Map<String, String> PART_API_VALUES = map(
            PART_LUMP, "All at once",
            PART_STRUCTURED, "Little by little",
            PART_MILESTONES, "At big life moments");

    /** Milestone IDs in v8 order -> the API field infix (bigLifeMoment{Infix}Percent / Amount). */
    public static final Map<String, String> MILESTONES = map(
            "college", "University",
            "marriage", "Family",
            "business", "Business",
            "property", "HomeCare",
            "care", "Medical",
            "retirement", "Retirement",
            MILESTONE_SPECIAL_AGE, "SpecialAge");

    /** Suggested amounts shown as guide chips in calculator ("not sure") mode, per milestone. */
    public static final Map<String, List<Long>> MILESTONE_GUIDE_PICKS = map(
            "college", list(50_000L, 100_000L, 250_000L),
            "marriage", list(30_000L, 50_000L, 100_000L),
            "business", list(50_000L, 100_000L, 200_000L),
            "property", list(50_000L, 100_000L, 200_000L),
            "care", list(100_000L, 250_000L, 500_000L),
            "retirement", list(100_000L, 250_000L, 500_000L),
            MILESTONE_SPECIAL_AGE, list(20_000L, 50_000L, 100_000L));

    public static final String FREQUENCY_MONTHLY_API = "Every month";
    public static final String FREQUENCY_YEARLY_API = "Every year";
    public static final String GROW_YES_API = "Yes, increase it";
    public static final String GROW_NO_API = "No, keep it steady";
    public static final String START_NOW_API = "Right away";
    public static final String START_AGE_API = "At a certain age";

    /** Country dialling codes accepted by the contact form (v8), without the leading '+'. */
    public static final List<String> COUNTRY_CODES = list(
            "60", "65", "62", "673", "66", "63", "84", "86", "852", "91", "61", "44", "1");
    public static final String MALAYSIA_CODE = "60";

    public static final List<String> STATES = list(
            "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang", "Perak", "Perlis",
            "Pulau Pinang", "Sabah", "Sarawak", "Selangor", "Terengganu", "W.P. Kuala Lumpur",
            "W.P. Labuan", "W.P. Putrajaya");

    // Preset chips shown by the frontend (values fixed in code, labels authored).
    public static final List<Long> LEGACY_AMOUNT_CHIPS = list(1_000_000L, 2_000_000L, 3_000_000L, 5_000_000L, 10_000_000L);
    public static final List<Long> PUT_ASIDE_CHIPS = list(0L, 100_000L, 250_000L, 500_000L, 1_000_000L);
    public static final List<Long> LUMP_AMOUNT_CHIPS = list(100_000L, 250_000L, 500_000L, 1_000_000L);
    public static final List<Integer> YEAR_CHIPS = list(5, 10, 15, 20, 25);
    public static final List<Integer> WITHIN_YEAR_CHIPS = list(5, 10, 15, 20);
    public static final List<Integer> STEP_UP_RATE_CHIPS = list(1, 2, 3, 5);
    public static final List<Integer> STEP_UP_INTERVAL_CHIPS = list(1, 2, 3, 5);
    public static final List<Integer> START_AGE_CHIPS = list(18, 21, 25, 55, 60);

    public static final String LANGUAGE_DEFAULT = "English";
    public static final Map<String, String> LANGUAGES = map("en", "English", "ms", "Bahasa Malaysia", "zh", "Chinese");

    @SafeVarargs
    private static <T> List<T> list(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    private static <V> Map<String, V> map(Object... keyValues) {
        Map<String, V> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put((String) keyValues[i], (V) keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(result);
    }
}
