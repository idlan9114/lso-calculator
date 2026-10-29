package com.prudential.pamb.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Structured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.allocated;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.distribution;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.known;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.lump;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.person;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.split;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.stepUp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyBlueprintPayloadMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode sample;
    private ObjectNode payload;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/legacyblueprint/Legacy-Blueprints-Request.json")) {
            sample = MAPPER.readTree(in);
        }

        LegacyBlueprintSubmission submission = known(5_000_000);
        submission.setPutAside(5_000L);
        Structured structured = stepUp("monthly", 20, 1, 2);
        structured.setStart("age");
        structured.setStartAge(18);
        Distribution combo = distribution("lump", "structured", "milestones");
        combo.setSplit(split(33, 33, 34));
        combo.setStructured(structured);
        combo.setMilestones(allocated("college", 3, "marriage", 24, "business", 24, "property", 24, "care", 25));
        person(submission, "a", " Jane Doe ", "Spouse", 66, combo);
        person(submission, "b", "John Doe", "Child", 34, lump());

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);
        payload = LegacyBlueprintPayloadMapper.toApiPayload(submission, plan, "PRULady", "English");
    }

    @Test
    void payloadHasExactlyTheSampleFields() {
        assertEquals(fieldNames(sample), fieldNames(payload));
        assertEquals(fieldNames(sample.get("questions")), fieldNames(payload.get("questions")));
        assertEquals(fieldNames(sample.get("questions").get("receiver").get(0)),
                fieldNames(payload.get("questions").get("receiver").get(0)));
    }

    @Test
    void questionsUseFixedEnglishValues() {
        JsonNode questions = payload.get("questions");
        assertEquals(5, questions.get("legacyMatterRate").asInt());
        assertEquals(4, questions.get("currentPlanRate").asInt());
        assertEquals("Everyday financial security", questions.get("covers").get(0).asText());
        assertEquals(5, questions.get("covers").size());
        assertEquals("Support at the right time", questions.get("peaceOfMind").asText());
        assertEquals("35-44", questions.get("age").asText());
        assertEquals("Married", questions.get("relationshipStatus").asText());
    }

    @Test
    void receiverAmountsComeFromTheServerPlan() {
        JsonNode jane = payload.get("questions").get("receiver").get(0);
        assertEquals("Jane Doe", jane.get("name").asText());
        assertEquals("Spouse", jane.get("relationship").asText());
        assertEquals(66, jane.get("amountPercent").asInt());
        assertEquals(3_300_000, jane.get("amount").asLong());
        assertTrue(jane.get("amountAlreadyPutAside").isNull());
        assertEquals("[\"All at once\",\"Little by little\",\"At big life moments\"]", jane.get("receiveGiftMethod").toString());
        assertEquals(1_089_000, jane.get("allAtOnceAmount").asLong());
        assertEquals(1_122_000, jane.get("bigLifeMomentsAmount").asLong());
        assertEquals("Every month", jane.get("littleByLittleFrequency").asText());
        assertEquals("Yes, increase it", jane.get("growOverTime").asText());
        assertEquals(1, jane.get("growPercent").asInt());
        assertEquals(2, jane.get("growYear").asInt());
        assertEquals("At a certain age", jane.get("paymentBegin").asText());
        assertEquals(18, jane.get("paymentAge").asInt());
        assertEquals(3, jane.get("bigLifeMomentUniversityPercent").asInt());
        assertEquals(33_660, jane.get("bigLifeMomentUniversityAmount").asLong());
        assertEquals(269_280, jane.get("bigLifeMomentHomeCareAmount").asLong());
        assertEquals(280_500, jane.get("bigLifeMomentMedicalAmount").asLong());
        assertTrue(jane.get("bigLifeMomentRetirementPercent").isNull());
        assertTrue(jane.get("littleByLittleAmount").asLong() > 0);

        JsonNode john = payload.get("questions").get("receiver").get(1);
        assertEquals("[\"All at once\"]", john.get("receiveGiftMethod").toString());
        assertEquals(100, john.get("allAtOncePercent").asInt());
        assertEquals(1_700_000, john.get("allAtOnceAmount").asLong());
        assertTrue(john.get("littleByLittleFrequency").isNull());
        assertTrue(john.get("growOverTime").isNull());
    }

    @Test
    void contactAndTotals() {
        assertEquals(5_000_000, payload.get("legacyAmount").asLong());
        assertEquals(4_995_000, payload.get("amountLeft").asLong());
        assertEquals("Aisyah Rahman", payload.get("fullName").asText());
        assertEquals("60", payload.get("countryCode").asText());
        assertEquals("123456789", payload.get("phoneNumber").asText());
        assertEquals("900101145678", payload.get("countryNationalId").asText());
        assertEquals("Selangor", payload.get("state").asText());
        assertEquals("PRULady", payload.get("productCode").asText());
        assertEquals("English", payload.get("language").asText());
    }

    private static TreeSet<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        Iterator<String> iterator = node.fieldNames();
        iterator.forEachRemaining(names::add);
        return new TreeSet<>(names);
    }
}
