package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Milestones;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.allocated;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.amounts;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.distribution;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.known;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.level;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.lump;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.milestones;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.person;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.split;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.stepUp;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.structured;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.unsure;
import static com.prudential.pamb.core.utils.LegacyBlueprintTestData.withAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyBlueprintValidatorTest {

    private static final List<String> RELATIONSHIPS = Arrays.asList("Spouse", "Child", "Parent", "Relative", "Other");

    private static List<String> validate(LegacyBlueprintSubmission submission) {
        return LegacyBlueprintValidator.validate(submission, RELATIONSHIPS);
    }

    private static LegacyBlueprintSubmission validKnown() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 100, lump());
        return submission;
    }

    @Test
    void validKnownSubmission_passes() {
        assertEquals(Collections.emptyList(), validate(validKnown()));
    }

    @Test
    void nullSubmission_isRejected() {
        assertEquals(Collections.singletonList("request.empty"), LegacyBlueprintValidator.validate(null, RELATIONSHIPS));
    }

    @Test
    void legacyBelowMinimum_isBlockedNotCorrected() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.setLegacyAmount(999_999L);
        assertTrue(validate(submission).contains("legacyAmount.belowMinimum"));
    }

    @Test
    void mustRankAllFivePriorities() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.setNeeds(Arrays.asList("everyday", "education", "care"));
        assertTrue(validate(submission).contains("needs.rankAll"));

        submission.setNeeds(Arrays.asList("everyday", "everyday", "care", "family", "retirement"));
        assertTrue(validate(submission).contains("needs.rankAll"));
    }

    @Test
    void introScoresMustBeOneToFive() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.setImportance(0);
        submission.setPreparedness(6);
        List<String> errors = validate(submission);
        assertTrue(errors.contains("importance.invalid"));
        assertTrue(errors.contains("preparedness.invalid"));
    }

    @Test
    void unknownOptionIds_areRejected() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.setPeaceOfMind("other");
        submission.setAgeRange("35–44");
        submission.setRelationshipStatus("Married");
        List<String> errors = validate(submission);
        assertTrue(errors.contains("peaceOfMind.invalid"));
        assertTrue(errors.contains("ageRange.invalid"));
        assertTrue(errors.contains("relationshipStatus.invalid"));
    }

    @Test
    void peopleLimits_areEnforced() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        for (int i = 0; i < 6; i++) {
            person(submission, "p" + i, "Person " + i, "Child", i == 0 ? 100 : 0, i == 0 ? lump() : null);
        }
        assertTrue(validate(submission).contains("people.count"));

        LegacyBlueprintSubmission spouses = known(1_000_000);
        person(spouses, "a", "A", "Spouse", 50, lump());
        person(spouses, "b", "B", "Spouse", 50, lump());
        assertTrue(validate(spouses).contains("people[1].relationshipLimit"));

        LegacyBlueprintSubmission parents = known(1_000_000);
        person(parents, "a", "A", "Parent", 34, lump());
        person(parents, "b", "B", "Parent", 33, lump());
        person(parents, "c", "C", "Parent", 33, lump());
        assertTrue(validate(parents).contains("people[2].relationshipLimit"));
    }

    @Test
    void relationshipMustBeAnAuthoredValue() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.getPeople().get(0).setRelationship("Cousin");
        assertTrue(validate(submission).contains("people[0].relationship"));
    }

    @Test
    void personNamesAndIds_areChecked() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "  ", "Spouse", 50, lump());
        person(submission, "a", "<script>", "Child", 50, lump());
        List<String> errors = validate(submission);
        assertTrue(errors.contains("people[0].name"));
        assertTrue(errors.contains("people[1].name"));
        assertTrue(errors.contains("people[1].id"));
    }

    @Test
    void sharesMustTotal100() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 60, lump());
        person(submission, "b", "B", "Child", 30, lump());
        assertTrue(validate(submission).contains("shares.total"));
    }

    @Test
    void zeroSharePerson_needsNoDistribution() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 100, lump());
        person(submission, "b", "B", "Child", 0, null);
        assertEquals(Collections.emptyList(), validate(submission));
    }

    @Test
    void methodIsRequired_andUnknownPartsRejected() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 100, distribution());
        assertTrue(validate(submission).contains("people[0].method"));

        submission.getPeople().get(0).setDistribution(distribution("lump", "cash"));
        assertTrue(validate(submission).contains("people[0].method"));
    }

    @Test
    void comboSplit_mustTotal100WithEveryPartAtLeast1() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        Distribution combo = distribution("lump", "milestones");
        combo.setSplit(split(60, null, 30));
        combo.setMilestones(allocated("college", 100));
        person(submission, "a", "A", "Spouse", 100, combo);
        assertTrue(validate(submission).contains("people[0].split.total"));

        combo.setSplit(split(100, null, 0));
        assertTrue(validate(submission).contains("people[0].split.partMissing"));

        combo.setSplit(split(50, 10, 40));
        assertTrue(validate(submission).contains("people[0].split.unexpected"));

        combo.setSplit(split(50, null, 50));
        assertEquals(Collections.emptyList(), validate(submission));
    }

    @Test
    void structured_calculatedPaymentBelowMinimum_isBlocked() {
        // RM 200,000 over 10 years monthly works out to RM 1,667 (< RM 2,000): "Try fewer years."
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 20, structured(level("monthly", 10)));
        person(submission, "b", "B", "Child", 80, lump());
        assertTrue(validate(submission).contains("people[0].structured.paymentBelowMinimum"));

        submission.getPeople().get(0).getDistribution().getStructured().setYears(8);
        assertEquals(Collections.emptyList(), validate(submission));
    }

    @Test
    void structured_requiresYearsAndValidStepUp() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 100, structured(level("monthly", 0)));
        assertTrue(validate(submission).contains("people[0].structured.years"));

        submission.getPeople().get(0).getDistribution().setStructured(stepUp("monthly", 10, 0, 1));
        assertTrue(validate(submission).contains("people[0].structured.stepUp"));

        submission.getPeople().get(0).getDistribution().setStructured(level("weekly", 10));
        assertTrue(validate(submission).contains("people[0].structured.frequency"));
    }

    @Test
    void structured_startAgeRequiredWhenStartingAtAge() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "A", "Spouse", 100, structured(level("monthly", 10)));
        submission.getPeople().get(0).getDistribution().getStructured().setStart("age");
        assertTrue(validate(submission).contains("people[0].structured.startAge"));
    }

    @Test
    void milestones_selectionAndAllocation() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        Milestones milestones = allocated("college", 50, "marriage", 40);
        person(submission, "a", "A", "Spouse", 100, milestones(milestones));
        assertTrue(validate(submission).contains("people[0].milestones.allocationTotal"));

        submission.getPeople().get(0).getDistribution().setMilestones(allocated("college", 50, "yacht", 50));
        assertTrue(validate(submission).contains("people[0].milestones.selection"));

        submission.getPeople().get(0).getDistribution().setMilestones(allocated("college", 20, "marriage", 20,
                "business", 20, "property", 20, "care", 10, "retirement", 10));
        assertTrue(validate(submission).contains("people[0].milestones.selection"));

        Milestones within = allocated("college", 100);
        within.setWithin(51);
        submission.getPeople().get(0).getDistribution().setMilestones(within);
        assertTrue(validate(submission).contains("people[0].milestones.within"));
    }

    @Test
    void unsureMode_validatesNeedsInputs() {
        LegacyBlueprintSubmission submission = unsure();
        person(submission, "a", "A", "Spouse", null, lump(0));
        person(submission, "b", "B", "Child", null, structured(withAmount(level("monthly", 10), 1500)));
        person(submission, "c", "C", "Parent", null, milestones(amounts("college", 0)));
        List<String> errors = validate(submission);
        assertTrue(errors.contains("people[0].lumpAmount"));
        assertTrue(errors.contains("people[1].structured.paymentBelowMinimum"));
        assertTrue(errors.contains("people[2].milestones.amount"));
    }

    @Test
    void unsureMode_validSubmissionPasses() {
        LegacyBlueprintSubmission submission = unsure();
        person(submission, "a", "A", "Spouse", null, lump(500_000));
        person(submission, "b", "B", "Child", null, structured(withAmount(level("yearly", 10), 24_000)));
        assertEquals(Collections.emptyList(), validate(submission));
    }

    @ParameterizedTest(name = "{0} {1} valid={2}")
    @CsvSource({
            "60, 0123456789, true",
            "60, 60123456789, true",
            "60, 12345678, false",
            "60, 0223456789, false",
            "65, 81234567, true",
            "65, 12345, false",
            "99, 81234567, false"
    })
    void mobileRules(String countryCode, String mobile, boolean valid) {
        LegacyBlueprintSubmission submission = validKnown();
        submission.getContact().setCountryCode(countryCode);
        submission.getContact().setMobile(mobile);
        List<String> errors = validate(submission);
        assertEquals(valid, !errors.contains("contact.mobile") && !errors.contains("contact.countryCode"));
    }

    @Test
    void contactRules() {
        LegacyBlueprintSubmission submission = validKnown();
        submission.getContact().setEmail("a@b.com,c@d.com");
        submission.getContact().setNric("900101-14-567");
        submission.getContact().setState("Singapore");
        submission.getContact().setReferralCode("TOO-LONG-CODE");
        submission.getContact().setConsent(false);
        submission.getContact().setFullName("");
        List<String> errors = validate(submission);
        assertTrue(errors.containsAll(Arrays.asList("contact.email", "contact.nric", "contact.state",
                "contact.referralCode", "contact.consent", "contact.fullName")));
    }

    @Test
    void normaliseMobile_stripsTypedPrefixes() {
        assertEquals("123456789", LegacyBlueprintValidator.normaliseMobile("60", "+60 12-345 6789"));
        assertEquals("123456789", LegacyBlueprintValidator.normaliseMobile("60", "012 345 6789"));
        assertEquals("81234567", LegacyBlueprintValidator.normaliseMobile("65", "081234567"));
    }
}
