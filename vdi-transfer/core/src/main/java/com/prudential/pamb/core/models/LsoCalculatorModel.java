package com.prudential.pamb.core.models;

import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.utils.LegacyBlueprintAuthoredContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Exports the authored copy to the frontend as one JSON object, keyed by dialog tab. Properties
 * ending in {@code Rte} are XSS-filtered; server-only settings (email template, lead source,
 * product code) are left out. Business rules come from {@link LegacyBlueprintConstants} under
 * {@code rules} so the frontend never hard-codes them.
 */
@Slf4j
@Model(adaptables = SlingHttpServletRequest.class,
        resourceType = LegacyBlueprintConstants.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class LsoCalculatorModel {

    /** Dialog tabs in journey order; each tab is stored as a child node of the component. */
    static final List<String> CONTENT_NODES = Collections.unmodifiableList(Arrays.asList(
            "labels", "intro", "panels", "priorities", "aboutPeople", "legacy", "distribution", "review",
            "contact", "blueprint"));

    private static final Set<String> EXPORTED_SETTINGS = new HashSet<>(Arrays.asList(
            "gaComponentName", LegacyBlueprintAuthoredContent.PROP_PRIVACY_PAGE, LegacyBlueprintAuthoredContent.PROP_ERROR_PAGE));
    private static final String RTE_SUFFIX = "Rte";
    private static final String PRIVACY_TOKEN = "{privacyLink}";
    private static final Pattern COMPONENT_ID = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0,63}$");
    private static final Pattern MULTIFIELD_ITEM = Pattern.compile("^item\\d+$");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private XSSAPI xssApi;

    @OSGiService
    private LegacyBlueprintConfig config;

    private String configJson = "{}";
    private String componentId;
    private boolean editorPreview;
    private boolean configured;

    @PostConstruct
    protected void init() {
        WCMMode mode = WCMMode.fromRequest(request);
        editorPreview = mode == WCMMode.EDIT || mode == WCMMode.PREVIEW;

        Resource settingsResource = resource.getChild(LegacyBlueprintAuthoredContent.NODE_SETTINGS);
        ValueMap settings = settingsResource == null ? ValueMap.EMPTY : settingsResource.getValueMap();
        String authoredId = settings.get("componentId", String.class);
        componentId = authoredId != null && COMPONENT_ID.matcher(authoredId).matches() ? authoredId : null;
        configured = StringUtils.isNotBlank(settings.get(LegacyBlueprintAuthoredContent.PROP_EMAIL_TEMPLATE, String.class))
                && StringUtils.isNotBlank(settings.get(LegacyBlueprintAuthoredContent.PROP_PRIVACY_PAGE, String.class));

        ObjectNode root = MAPPER.createObjectNode();
        root.set(LegacyBlueprintAuthoredContent.NODE_SETTINGS, exportSettings(settings));
        for (String name : CONTENT_NODES) {
            Resource tab = resource.getChild(name);
            root.set(name, tab == null ? MAPPER.createObjectNode() : exportResource(tab));
        }
        resolvePrivacyLink(root);
        root.set("rules", rules());

        try {
            configJson = MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("LsoCalculator: unable to serialise the authored configuration for {}", resource.getPath(), e);
        }
    }

    public String getConfigJson() {
        return configJson;
    }

    /** Null when the authored id isn't a safe HTML id. */
    public String getComponentId() {
        return componentId;
    }

    public String getSubmitEndpoint() {
        return resource.getPath() + "." + LegacyBlueprintConstants.SELECTOR_SUBMIT + ".json";
    }

    /** Edit or preview mode in the author editor, where submissions are blocked. */
    public boolean isEditorPreview() {
        return editorPreview;
    }

    /** Whether the email template and privacy page are set; drives the editor placeholder. */
    public boolean isConfigured() {
        return configured;
    }

    private ObjectNode exportSettings(ValueMap settings) {
        ObjectNode node = MAPPER.createObjectNode();
        for (String key : EXPORTED_SETTINGS) {
            String value = settings.get(key, String.class);
            if (value != null) {
                node.put(key, toLink(value));
            }
        }
        boolean trackingAllowed = config == null || config.isTrackingAllowed();
        node.put("gaEnabled", trackingAllowed && settings.get("gaEnabled", false));
        if (editorPreview) {
            node.put("previewScreen", settings.get("previewScreen", "intro"));
        }
        node.put("editorPreview", editorPreview);
        return node;
    }

    private ObjectNode exportResource(Resource source) {
        ObjectNode node = MAPPER.createObjectNode();
        for (Map.Entry<String, Object> entry : source.getValueMap().entrySet()) {
            String key = entry.getKey();
            if (key.contains(":")) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                node.put(key, exportString(key, (String) value));
            } else if (value instanceof String[]) {
                ArrayNode array = node.putArray(key);
                for (String item : (String[]) value) {
                    array.add(exportString(key, item));
                }
            } else if (value instanceof Boolean) {
                node.put(key, (Boolean) value);
            } else if (value instanceof Long || value instanceof Integer) {
                node.put(key, ((Number) value).longValue());
            } else if (value instanceof Number) {
                node.put(key, ((Number) value).doubleValue());
            }
        }
        for (Resource child : source.getChildren()) {
            List<Resource> items = new ArrayList<>();
            child.getChildren().forEach(items::add);
            boolean multifield = !items.isEmpty() && items.stream().allMatch(item -> MULTIFIELD_ITEM.matcher(item.getName()).matches());
            if (multifield) {
                ArrayNode array = node.putArray(child.getName());
                items.forEach(item -> array.add(exportResource(item)));
            } else {
                node.set(child.getName(), exportResource(child));
            }
        }
        return node;
    }

    private String exportString(String key, String value) {
        if (key.endsWith(RTE_SUFFIX)) {
            return xssApi == null ? StringUtils.EMPTY : xssApi.filterHTML(value);
        }
        return toLink(value);
    }

    private String toLink(String value) {
        if (!value.startsWith("/content/")) {
            return value;
        }
        String path = value.startsWith("/content/dam/") ? value : value + ".html";
        return resourceResolver.map(request, path);
    }

    // The privacy page is picked once in Settings, so the consent text uses a {privacyLink} token
    // rather than a link typed into the RTE.
    private void resolvePrivacyLink(ObjectNode root) {
        ObjectNode contact = (ObjectNode) root.get("contact");
        String consent = contact.path("consentRte").asText(null);
        if (consent == null || !consent.contains(PRIVACY_TOKEN)) {
            return;
        }
        String href = root.path(LegacyBlueprintAuthoredContent.NODE_SETTINGS).path(LegacyBlueprintAuthoredContent.PROP_PRIVACY_PAGE).asText("");
        String text = contact.path("privacyLinkText").asText("Privacy Notice");
        String link = xssApi == null ? text : "<a href=\"" + xssApi.encodeForHTMLAttr(xssApi.getValidHref(href))
                + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + xssApi.encodeForHTML(text) + "</a>";
        contact.put("consentRte", consent.replace(PRIVACY_TOKEN, link));
    }

    private static ObjectNode rules() {
        ObjectNode rules = MAPPER.createObjectNode();
        rules.put("legacyMinimum", LegacyBlueprintConstants.LEGACY_MINIMUM);
        rules.put("planStep", LegacyBlueprintConstants.PLAN_STEP);
        rules.put("maxPeople", LegacyBlueprintConstants.MAX_PEOPLE);
        rules.put("maxMilestones", LegacyBlueprintConstants.MAX_MILESTONES);
        rules.put("monthlyPaymentMinimum", LegacyBlueprintConstants.MONTHLY_PAYMENT_MINIMUM);
        rules.put("yearlyPaymentMinimum", LegacyBlueprintConstants.YEARLY_PAYMENT_MINIMUM);
        rules.put("defaultMilestoneAge", LegacyBlueprintConstants.DEFAULT_MILESTONE_AGE);
        rules.put("defaultWithinYears", LegacyBlueprintConstants.DEFAULT_WITHIN_YEARS);
        rules.set("relationshipLimits", MAPPER.valueToTree(LegacyBlueprintConstants.RELATIONSHIP_LIMITS));
        rules.set("needs", MAPPER.valueToTree(LegacyBlueprintConstants.NEEDS.keySet()));
        rules.set("peaceOfMind", MAPPER.valueToTree(LegacyBlueprintConstants.PEACE_OF_MIND.keySet()));
        rules.set("ageRanges", MAPPER.valueToTree(LegacyBlueprintConstants.AGE_RANGES.keySet()));
        rules.set("relationshipStatuses", MAPPER.valueToTree(LegacyBlueprintConstants.RELATIONSHIP_STATUSES.keySet()));
        rules.set("milestones", MAPPER.valueToTree(LegacyBlueprintConstants.MILESTONES.keySet()));
        rules.set("milestoneGuidePicks", MAPPER.valueToTree(LegacyBlueprintConstants.MILESTONE_GUIDE_PICKS));
        rules.set("legacyAmountChips", MAPPER.valueToTree(LegacyBlueprintConstants.LEGACY_AMOUNT_CHIPS));
        rules.set("putAsideChips", MAPPER.valueToTree(LegacyBlueprintConstants.PUT_ASIDE_CHIPS));
        rules.set("lumpAmountChips", MAPPER.valueToTree(LegacyBlueprintConstants.LUMP_AMOUNT_CHIPS));
        rules.set("yearChips", MAPPER.valueToTree(LegacyBlueprintConstants.YEAR_CHIPS));
        rules.set("withinYearChips", MAPPER.valueToTree(LegacyBlueprintConstants.WITHIN_YEAR_CHIPS));
        rules.set("stepUpRateChips", MAPPER.valueToTree(LegacyBlueprintConstants.STEP_UP_RATE_CHIPS));
        rules.set("stepUpIntervalChips", MAPPER.valueToTree(LegacyBlueprintConstants.STEP_UP_INTERVAL_CHIPS));
        rules.set("startAgeChips", MAPPER.valueToTree(LegacyBlueprintConstants.START_AGE_CHIPS));
        rules.set("countryCodes", MAPPER.valueToTree(LegacyBlueprintConstants.COUNTRY_CODES));
        rules.set("states", MAPPER.valueToTree(LegacyBlueprintConstants.STATES));
        return rules;
    }
}
