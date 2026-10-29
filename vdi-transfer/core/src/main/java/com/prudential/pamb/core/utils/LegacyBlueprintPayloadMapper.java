package com.prudential.pamb.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.MilestonePlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.PersonPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.StructuredPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Contact;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_MONTHLY;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_MONTHLY_API;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_YEARLY_API;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.GROW_NO_API;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.GROW_YES_API;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MILESTONES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_API_VALUES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_AGE_API;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_NOW_API;

/**
 * Maps validated answers and the server-calculated plan to the client API request body. This is
 * the only place that knows the external field names, which follow Legacy-Blueprints-Request.json
 * exactly; no undocumented fields are sent.
 */
public final class LegacyBlueprintPayloadMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LegacyBlueprintPayloadMapper() {
    }

    /** {@code language} is the API's language name (e.g. {@code English}), not a locale code. */
    public static ObjectNode toApiPayload(LegacyBlueprintSubmission submission, LegacyBlueprintPlan plan,
                                          String productCode, String language) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode questions = root.putObject("questions");
        questions.put("legacyMatterRate", submission.getImportance());
        questions.put("currentPlanRate", submission.getPreparedness());
        ArrayNode covers = questions.putArray("covers");
        submission.getNeeds().forEach(id -> covers.add(LegacyBlueprintConstants.NEEDS.get(id)));
        questions.put("peaceOfMind", LegacyBlueprintConstants.PEACE_OF_MIND.get(submission.getPeaceOfMind()));
        questions.put("age", LegacyBlueprintConstants.AGE_RANGES.get(submission.getAgeRange()));
        questions.put("relationshipStatus", LegacyBlueprintConstants.RELATIONSHIP_STATUSES.get(submission.getRelationshipStatus()));
        ArrayNode receivers = questions.putArray("receiver");
        plan.getPeople().forEach(person -> receivers.add(receiver(person)));

        Contact contact = submission.getContact();
        root.put("legacyAmount", plan.getLegacyAmount());
        root.put("amountLeft", plan.getGap());
        root.put("fullName", StringUtils.trim(contact.getFullName()));
        root.put("email", StringUtils.trim(contact.getEmail()));
        root.put("countryCode", contact.getCountryCode());
        root.put("phoneNumber", LegacyBlueprintValidator.normaliseMobile(contact.getCountryCode(), contact.getMobile()));
        root.put("state", contact.getState());
        root.put("countryNationalId", StringUtils.defaultString(contact.getNric()).replaceAll("\\D", ""));
        root.put("productCode", productCode);
        root.put("language", language);
        return root;
    }

    private static ObjectNode receiver(PersonPlan person) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("name", person.getName());
        node.put("relationship", person.getRelationship());
        node.put("amountPercent", person.getShare());
        node.put("amount", person.getAmount());
        // Put-aside is captured once for the whole plan in v8, not per person.
        node.putNull("amountAlreadyPutAside");

        ArrayNode methods = node.putArray("receiveGiftMethod");
        person.getParts().forEach(part -> methods.add(PART_API_VALUES.get(part)));

        putNumber(node, "allAtOncePercent", person.getLumpPercent());
        putNumber(node, "littleByLittlePercent", person.getStructuredPercent());
        putNumber(node, "bigLifeMomentsPercent", person.getMilestonesPercent());
        putNumber(node, "allAtOnceAmount", person.getLumpAmount());
        putNumber(node, "bigLifeMomentsAmount", person.getMilestonesAmount());

        StructuredPlan structured = person.getStructured();
        boolean hasStructured = structured != null;
        putText(node, "littleByLittleFrequency", hasStructured
                ? (FREQUENCY_MONTHLY.equals(structured.getFrequency()) ? FREQUENCY_MONTHLY_API : FREQUENCY_YEARLY_API) : null);
        putNumber(node, "littleByLittleAmount", hasStructured ? structured.getPayment() : null);
        putText(node, "growOverTime", hasStructured ? (structured.isStepUp() ? GROW_YES_API : GROW_NO_API) : null);
        putNumber(node, "growPercent", hasStructured ? structured.getRate() : null);
        putNumber(node, "growYear", hasStructured ? structured.getInterval() : null);
        putText(node, "paymentBegin", hasStructured
                ? (START_AGE.equals(structured.getStart()) ? START_AGE_API : START_NOW_API) : null);
        putNumber(node, "paymentAge", hasStructured ? structured.getStartAge() : null);

        Map<String, MilestonePlan> chosen = new HashMap<>();
        List<MilestonePlan> milestones = person.getMilestones();
        if (milestones != null) {
            milestones.forEach(milestone -> chosen.put(milestone.getId(), milestone));
        }
        MILESTONES.forEach((id, infix) -> {
            MilestonePlan milestone = chosen.get(id);
            putNumber(node, "bigLifeMoment" + infix + "Percent", milestone == null ? null : milestone.getPercent());
            putNumber(node, "bigLifeMoment" + infix + "Amount", milestone == null ? null : milestone.getAmount());
        });
        return node;
    }

    private static void putNumber(ObjectNode node, String field, Number value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.longValue());
        }
    }

    private static void putText(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }
}
