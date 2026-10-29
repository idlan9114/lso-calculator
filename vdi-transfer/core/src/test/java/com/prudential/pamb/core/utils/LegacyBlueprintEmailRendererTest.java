package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.utils.LegacyBlueprintEmailRenderer.RenderedEmail;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.distribution;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.known;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.lump;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.person;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.split;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.allocated;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class LegacyBlueprintEmailRendererTest {

    private static final String TEMPLATE = "<html><head><title>Your plan, ${name} &amp; family</title></head><body>"
            + "<p>Hi ${name},</p><p>${legacyAmount}|${putAside}|${gap}|${recommendedPlan}</p>"
            + "<table>${peopleRows}</table><p>${priorities}</p><a href=\"${privacyLink}\">Privacy</a>"
            + "<p>${email}|${mobile}|${state}|${unknownToken}</p></body></html>";

    private final AemContext context = new AemContext();

    private LegacyBlueprintSubmission submission;
    private LegacyBlueprintPlan plan;
    private LegacyBlueprintAuthoredContent authored;

    @BeforeEach
    void setUp() {
        Resource component = context.create().resource("/content/pamb/en/plan/jcr:content/root/lso");
        context.create().resource(component, "priorities", "option1Label", "Everyday needs", "option2Label", "Education");
        context.create().resource(component, "distribution", "lumpTitle", "All at once", "milestonesTitle", "Life moments");
        context.create().resource(component.getPath() + "/aboutPeople/relationshipOptions/item0", "label", "Pasangan", "value", "Spouse");
        context.create().resource(component.getPath() + "/aboutPeople/relationshipOptions/item1", "label", "Anak", "value", "Child");
        authored = LegacyBlueprintAuthoredContent.from(context.resourceResolver().getResource(component.getPath()));

        submission = known(2_000_000);
        submission.setPutAside(250_000L);
        submission.getContact().setFullName("Aisyah <b>Rahman</b> ${gap}");
        Distribution combo = distribution("lump", "milestones");
        combo.setSplit(split(50, null, 50));
        combo.setMilestones(allocated("college", 100));
        person(submission, "a", "Adam & <i>Son</i>", "Spouse", 60, combo);
        person(submission, "b", "Sofia", "Child", 40, lump());
        plan = LegacyBlueprintCalculator.calculate(submission);
    }

    @Test
    void fillsPlaceholdersAndEscapesVisitorInput() {
        RenderedEmail email = LegacyBlueprintEmailRenderer.render(TEMPLATE, submission, plan, authored, "https://www.prudential.com.my/privacy.html");
        String html = email.getHtml();

        assertTrue(html.contains("Hi Aisyah &lt;b&gt;Rahman&lt;/b&gt; ${gap},"), "visitor input is escaped and never re-expanded");
        assertTrue(html.contains("RM 2,000,000|RM 250,000|RM 1,750,000|RM 2,000,000"));
        assertTrue(html.contains("Adam &amp; &lt;i&gt;Son&lt;/i&gt;"));
        assertTrue(html.contains(">Pasangan<"), "relationship uses the authored label");
        assertTrue(html.contains("60% &middot; RM 1,200,000"));
        assertTrue(html.contains("All at once + Life moments"));
        assertTrue(html.contains("<p>Everyday needs, Education, Healthcare or caregiving</p>"), "top 3 priorities, authored labels with fallback");
        assertTrue(html.contains("href=\"https://www.prudential.com.my/privacy.html\""));
        assertTrue(html.contains("aisyah@example.com|+60 123456789|Selangor|</p>"), "unknown placeholders render blank");
        assertFalse(html.contains("<b>Rahman"));
    }

    @Test
    void subjectComesFromTitleAsPlainText() {
        RenderedEmail email = LegacyBlueprintEmailRenderer.render(TEMPLATE, submission, plan, authored, "");
        assertEquals("Your plan, Aisyah <b>Rahman</b> ${gap} & family", email.getSubject());
    }

    @Test
    void subjectIgnoresCommentsMentioningTitle() {
        String template = "<head><!-- The <title> is the subject. Uses ${name} ${email} --><title>Your <b>Legacy</b> Blueprint</title></head>";
        RenderedEmail email = LegacyBlueprintEmailRenderer.render(template, submission, plan, authored, "");
        assertEquals("Your Legacy Blueprint", email.getSubject());
    }

    @Test
    void subjectFallsBackAndStaysOnOneLine() {
        RenderedEmail noTitle = LegacyBlueprintEmailRenderer.render("<p>${name}</p>", submission, plan, authored, "");
        assertEquals("Your Legacy Blueprint", noTitle.getSubject());

        submission.getContact().setFullName("Line\r\nBcc: someone@example.com");
        RenderedEmail injected = LegacyBlueprintEmailRenderer.render("<title>Hi ${name}</title>", submission, plan, authored, "");
        assertFalse(injected.getSubject().contains("\n"));
    }

    @Test
    void formatsAmounts() {
        assertEquals("RM 0", LegacyBlueprintEmailRenderer.formatAmount(0));
        assertEquals("RM 1,250,000", LegacyBlueprintEmailRenderer.formatAmount(1_250_000));
    }
}
