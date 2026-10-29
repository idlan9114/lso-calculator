package com.prudential.pamb.core.models;

import com.adobe.granite.xss.XSSAPI;
import com.day.cq.wcm.api.WCMMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class LsoCalculatorModelTest {

    private static final String COMPONENT = "/content/pamb/en/legacy/jcr:content/root/lso";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AemContext context = new AemContext();
    private LegacyBlueprintConfig config;

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(LsoCalculatorModel.class);
        XSSAPI xssApi = mock(XSSAPI.class);
        lenient().when(xssApi.filterHTML(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).replaceAll("<script>.*?</script>", ""));
        lenient().when(xssApi.encodeForHTML(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(xssApi.encodeForHTMLAttr(anyString())).thenAnswer(i -> i.getArgument(0));
        lenient().when(xssApi.getValidHref(anyString())).thenAnswer(i -> i.getArgument(0));
        context.registerService(XSSAPI.class, xssApi, "service.ranking", Integer.MAX_VALUE);
        config = mock(LegacyBlueprintConfig.class);
        when(config.isTrackingAllowed()).thenReturn(true);
        context.registerService(LegacyBlueprintConfig.class, config);

        context.create().page("/content/pamb/en/privacy-notice");
        context.create().resource(COMPONENT, "sling:resourceType", "prudential-aem-malaysia/components/content/lsoCalculator");
        context.create().resource(COMPONENT + "/settings",
                "componentId", "legacy-blueprint",
                "previewScreen", "s5",
                "privacyPage", "/content/pamb/en/privacy-notice",
                "errorPage", "/content/pamb/en/sorry",
                "emailTemplate", "/content/dam/pamb/email-templates/legacy-blueprint/en.html",
                "leadSourceCode", "LEGACY_BP_WEB",
                "productCode", "PRULady",
                "gaEnabled", true,
                "gaComponentName", "legacy_blueprint");
        context.create().resource(COMPONENT + "/intro",
                "headline", "If life changes tomorrow",
                "subHeadlineRte", "<p>Select <script>alert(1)</script>one</p>",
                "welcomeImage", "/content/dam/pamb/legacy/welcome.jpg",
                "beginGaLabel", "lb_intro_begin");
        context.create().resource(COMPONENT + "/contact",
                "consentRte", "<p>I agree to the {privacyLink}.</p>",
                "privacyLinkText", "Privacy Notice");
        context.create().resource(COMPONENT + "/aboutPeople/relationshipOptions/item0", "label", "Spouse", "value", "Spouse");
        context.create().resource(COMPONENT + "/aboutPeople/relationshipOptions/item1", "label", "Child", "value", "Child");
        context.currentResource(COMPONENT);
    }

    private LsoCalculatorModel model() {
        // createModel (unlike adaptTo) surfaces injection errors instead of returning null.
        return context.getService(ModelFactory.class).createModel(context.request(), LsoCalculatorModel.class);
    }

    private JsonNode config(LsoCalculatorModel model) throws Exception {
        return MAPPER.readTree(model.getConfigJson());
    }

    @Test
    void exportsAuthoredCopyPerTab() throws Exception {
        JsonNode json = config(model());

        assertEquals("If life changes tomorrow", json.at("/intro/headline").asText());
        assertEquals("lb_intro_begin", json.at("/intro/beginGaLabel").asText());
        assertEquals("/content/dam/pamb/legacy/welcome.jpg", json.at("/intro/welcomeImage").asText());
        assertEquals("Child", json.at("/aboutPeople/relationshipOptions/1/value").asText());
        assertTrue(json.at("/blueprint").isObject(), "tabs without content export as empty objects");
    }

    @Test
    void filtersRichTextAndResolvesPrivacyLink() throws Exception {
        JsonNode json = config(model());

        assertEquals("<p>Select one</p>", json.at("/intro/subHeadlineRte").asText());
        assertEquals("<p>I agree to the <a href=\"/content/pamb/en/privacy-notice.html\" target=\"_blank\" "
                + "rel=\"noopener noreferrer\">Privacy Notice</a>.</p>", json.at("/contact/consentRte").asText());
    }

    @Test
    void neverExportsServerOnlySettings() throws Exception {
        JsonNode settings = config(model()).get("settings");

        assertFalse(settings.has("emailTemplate"));
        assertFalse(settings.has("leadSourceCode"));
        assertFalse(settings.has("productCode"));
        assertEquals("/content/pamb/en/privacy-notice.html", settings.get("privacyPage").asText());
        assertEquals("/content/pamb/en/sorry.html", settings.get("errorPage").asText());
        assertEquals("legacy_blueprint", settings.get("gaComponentName").asText());
        assertTrue(settings.get("gaEnabled").asBoolean());
    }

    @Test
    void previewScreenOnlyInEditor() throws Exception {
        assertFalse(config(model()).get("settings").has("previewScreen"));
        assertFalse(model().isEditorPreview());

        WCMMode.EDIT.toRequest(context.request());
        LsoCalculatorModel editModel = model();
        assertTrue(editModel.isEditorPreview());
        assertEquals("s5", config(editModel).at("/settings/previewScreen").asText());
    }

    @Test
    void trackingCanBeSwitchedOffPerInstance() throws Exception {
        when(config.isTrackingAllowed()).thenReturn(false);
        assertFalse(config(model()).at("/settings/gaEnabled").asBoolean());
    }

    @Test
    void exportsFixedRules() throws Exception {
        JsonNode rules = config(model()).get("rules");
        assertEquals(1_000_000, rules.get("legacyMinimum").asLong());
        assertEquals(5, rules.get("maxPeople").asInt());
        assertEquals(1, rules.at("/relationshipLimits/spouse").asInt());
        assertEquals("everyday", rules.at("/needs/0").asText());
        assertEquals(16, rules.get("states").size());
    }

    @Test
    void endpointIdAndConfiguredFlag() {
        LsoCalculatorModel model = model();
        assertEquals(COMPONENT + ".submit.json", model.getSubmitEndpoint());
        assertEquals("legacy-blueprint", model.getComponentId());
        assertTrue(model.isConfigured());
    }

    @Test
    void unsafeComponentIdIsDropped() {
        context.resourceResolver().getResource(COMPONENT + "/settings").adaptTo(org.apache.sling.api.resource.ModifiableValueMap.class)
                .put("componentId", "\" onclick=\"x");
        assertNull(model().getComponentId());
    }
}
