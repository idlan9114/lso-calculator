package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The authored values the server needs, read from the lsoCalculator component resource. Dialog
 * fields are stored in one child node per tab (e.g. {@code settings/emailTemplate}).
 */
public final class LegacyBlueprintAuthoredContent {

    public static final String NODE_SETTINGS = "settings";
    public static final String NODE_PRIORITIES = "priorities";
    public static final String NODE_ABOUT_PEOPLE = "aboutPeople";
    public static final String NODE_DISTRIBUTION = "distribution";
    public static final String NODE_RELATIONSHIP_OPTIONS = "relationshipOptions";

    public static final String PROP_EMAIL_TEMPLATE = "emailTemplate";
    public static final String PROP_LEAD_SOURCE = "leadSourceCode";
    public static final String PROP_PRODUCT_CODE = "productCode";
    public static final String PROP_PRIVACY_PAGE = "privacyPage";
    public static final String PROP_ERROR_PAGE = "errorPage";

    /** Default relationship options, used until an author saves the dialog. */
    private static final Map<String, String> DEFAULT_RELATIONSHIPS = defaults();

    private final ValueMap settings;
    private final Map<String, String> relationshipLabels;
    private final Map<String, String> needLabels;
    private final Map<String, String> partLabels;

    private LegacyBlueprintAuthoredContent(Resource component) {
        this.settings = valueMap(component, NODE_SETTINGS);
        this.relationshipLabels = readRelationships(component);
        this.needLabels = readNeedLabels(valueMap(component, NODE_PRIORITIES));
        this.partLabels = readPartLabels(valueMap(component, NODE_DISTRIBUTION));
    }

    public static LegacyBlueprintAuthoredContent from(Resource component) {
        return new LegacyBlueprintAuthoredContent(component);
    }

    public String getEmailTemplatePath() {
        return settings.get(PROP_EMAIL_TEMPLATE, String.class);
    }

    public String getLeadSourceCode() {
        return settings.get(PROP_LEAD_SOURCE, String.class);
    }

    public String getProductCode() {
        return settings.get(PROP_PRODUCT_CODE, String.class);
    }

    public String getPrivacyPagePath() {
        return settings.get(PROP_PRIVACY_PAGE, String.class);
    }

    /** Authored relationship option values (what the API and GA receive). */
    public List<String> getRelationshipValues() {
        return new ArrayList<>(relationshipLabels.keySet());
    }

    /** Visitor-facing label for a relationship value, falling back to the value itself. */
    public String relationshipLabel(String value) {
        return StringUtils.defaultIfBlank(relationshipLabels.get(value), value);
    }

    /** Authored label for a need ID (priority options 1-5 map to the v8 need order). */
    public String needLabel(String needId) {
        return StringUtils.defaultIfBlank(needLabels.get(needId), LegacyBlueprintConstants.NEEDS.get(needId));
    }

    /** Authored title of a payout method, e.g. "All at once". */
    public String partLabel(String part) {
        return StringUtils.defaultIfBlank(partLabels.get(part), LegacyBlueprintConstants.PART_API_VALUES.get(part));
    }

    private static Map<String, String> readRelationships(Resource component) {
        Resource about = component == null ? null : component.getChild(NODE_ABOUT_PEOPLE);
        Resource options = about == null ? null : about.getChild(NODE_RELATIONSHIP_OPTIONS);
        if (options == null) {
            return DEFAULT_RELATIONSHIPS;
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (Resource item : options.getChildren()) {
            ValueMap properties = item.getValueMap();
            String value = StringUtils.trimToNull(properties.get("value", String.class));
            if (value != null) {
                labels.put(value, StringUtils.defaultIfBlank(properties.get("label", String.class), value));
            }
        }
        return labels.isEmpty() ? DEFAULT_RELATIONSHIPS : labels;
    }

    private static Map<String, String> readNeedLabels(ValueMap priorities) {
        Map<String, String> labels = new LinkedHashMap<>();
        int index = 1;
        for (String needId : LegacyBlueprintConstants.NEEDS.keySet()) {
            labels.put(needId, priorities.get("option" + index + "Label", String.class));
            index++;
        }
        return labels;
    }

    private static Map<String, String> readPartLabels(ValueMap distribution) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(LegacyBlueprintConstants.PART_LUMP, distribution.get("lumpTitle", String.class));
        labels.put(LegacyBlueprintConstants.PART_STRUCTURED, distribution.get("structuredTitle", String.class));
        labels.put(LegacyBlueprintConstants.PART_MILESTONES, distribution.get("milestonesTitle", String.class));
        return labels;
    }

    private static ValueMap valueMap(Resource component, String child) {
        Resource resource = component == null ? null : component.getChild(child);
        return resource == null ? ValueMap.EMPTY : resource.getValueMap();
    }

    private static Map<String, String> defaults() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (String value : new String[]{"Spouse", "Child", "Parent", "Relative", "Other"}) {
            labels.put(value, value);
        }
        return Collections.unmodifiableMap(labels);
    }
}
