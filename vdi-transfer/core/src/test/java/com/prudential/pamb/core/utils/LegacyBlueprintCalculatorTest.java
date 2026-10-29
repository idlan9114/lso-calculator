package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.PersonPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.ScheduleRow;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.StructuredPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Structured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Expected values were produced by running the v8 prototype's own functions (firstPayFor,
 * paySchedule, structTotal, toPcts, syncNeeds) in Node.js with the same inputs, and match the
 * worked examples in "Legacy Blueprint v5 vs v8 - Differences.md".
 */
class LegacyBlueprintCalculatorTest {

    @Test
    void firstPayment_matchesV8Examples() {
        assertEquals(4167, LegacyBlueprintCalculator.firstPaymentFor(500_000, level("monthly", 10), 10));
        assertEquals(3635, LegacyBlueprintCalculator.firstPaymentFor(500_000, stepUp("monthly", 10, 3, 1), 10));
        assertEquals(50_000, LegacyBlueprintCalculator.firstPaymentFor(1_000_000, level("yearly", 20), 20));
        assertEquals(1667, LegacyBlueprintCalculator.firstPaymentFor(200_000, level("monthly", 10), 10));
        assertEquals(64_053, LegacyBlueprintCalculator.firstPaymentFor(1_000_000, stepUp("yearly", 15, 2, 3), 15));
        assertEquals(0, LegacyBlueprintCalculator.firstPaymentFor(0, level("monthly", 10), 10));
    }

    @Test
    void levelSchedule_foldsRoundingIntoFinalYear() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 50, structured(level("monthly", 10)));
        person(submission, "b", "John", "Child", 50, lump());

        StructuredPlan plan = LegacyBlueprintCalculator.calculate(submission).getPeople().get(0).getStructured();

        assertEquals(4167, plan.getPayment());
        assertEquals(10, plan.getSchedule().size());
        for (int year = 0; year < 9; year++) {
            assertEquals(50_004, plan.getSchedule().get(year).getPaid());
        }
        ScheduleRow last = plan.getSchedule().get(9);
        assertEquals(49_964, last.getPaid());
        assertEquals(0, last.getRemaining());
        assertEquals(500_000, plan.getSchedule().stream().mapToLong(ScheduleRow::getPaid).sum());
    }

    @Test
    void stepUpSchedule_growsEveryInterval() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 100, structured(stepUp("yearly", 15, 2, 3)));

        StructuredPlan plan = LegacyBlueprintCalculator.calculate(submission).getPeople().get(0).getStructured();

        long[] expectedPaid = {64_053, 64_053, 64_053, 65_334, 65_334, 65_334, 66_641, 66_641, 66_641,
                67_974, 67_974, 67_974, 69_333, 69_333, 69_330};
        assertArrayEquals(expectedPaid, plan.getSchedule().stream().mapToLong(ScheduleRow::getPaid).toArray());
        assertEquals(64_053, plan.getPayment());
        assertTrue(plan.isStepUp());
        assertEquals(2, plan.getRate());
        assertEquals(3, plan.getInterval());
    }

    @Test
    void stepUpEveryFiveYears_matchesClosedFormFromBusiness() {
        // Business formula: first year = pot / (I x ((1+r)^(N/I) - 1) / r) = 606,000 / (5 x (1.02^2 - 1) / 0.02)
        // = 606,000 / 10.1 = 60,000 a year -> RM 5,000 a month, rising 2% after year 5.
        Structured st = stepUp("monthly", 10, 2, 5);
        assertEquals(5_000, LegacyBlueprintCalculator.firstPaymentFor(606_000, st, 10));

        LegacyBlueprintSubmission submission = known(1_010_000);
        person(submission, "a", "Jane", "Spouse", 60, structured(st));
        person(submission, "b", "John", "Child", 40, lump());
        StructuredPlan plan = LegacyBlueprintCalculator.calculate(submission).getPeople().get(0).getStructured();

        assertEquals(5_000, plan.getPayment());
        long[] paid = plan.getSchedule().stream().mapToLong(ScheduleRow::getPaid).toArray();
        assertArrayEquals(new long[]{60_000, 60_000, 60_000, 60_000, 60_000, 61_200, 61_200, 61_200, 61_200, 61_200}, paid);
        assertEquals(5_100, plan.getSchedule().get(5).getPayment());
        assertEquals(0, plan.getSchedule().get(9).getRemaining());
    }

    @Test
    void unsureMode_stepUpEveryFiveYears_needsTotal() {
        // Visitor enters RM 5,000 a month for 10 years, +2% every 5 years: 5 x 60,000 + 5 x 61,200 = 606,000.
        LegacyBlueprintSubmission submission = unsure();
        person(submission, "a", "Jane", "Spouse", null, structured(withAmount(stepUp("monthly", 10, 2, 5), 5_000)));

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);

        assertEquals(606_000, plan.getNeedsTotal());
        // Below RM 1M, so the plan is lifted to RM 1M and each payment grows in proportion (v8 behaviour).
        assertEquals(1_000_000, plan.getLegacyAmount());
        assertEquals(8_251, plan.getPeople().get(0).getStructured().getPayment());
    }

    @Test
    void monthlyStepUpSchedule_matchesComparisonDoc() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 50, structured(stepUp("monthly", 10, 3, 1)));
        person(submission, "b", "John", "Child", 50, lump());

        List<ScheduleRow> rows = LegacyBlueprintCalculator.calculate(submission).getPeople().get(0).getStructured().getSchedule();

        assertEquals(43_620, rows.get(0).getPaid());
        assertEquals(56_860, rows.get(9).getPaid());
    }

    @Test
    void combination_splitsShareAcrossPartsAndMilestones() {
        LegacyBlueprintSubmission submission = known(5_000_000);
        Distribution combo = distribution("lump", "structured", "milestones");
        combo.setSplit(split(33, 33, 34));
        combo.setStructured(level("monthly", 10));
        combo.setMilestones(allocated("college", 3, "marriage", 24, "business", 24, "property", 24, "care", 25));
        person(submission, "a", "Jane", "Spouse", 66, combo);
        person(submission, "b", "John", "Child", 34, lump());

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);
        PersonPlan jane = plan.getPeople().get(0);

        assertEquals(3_300_000, jane.getAmount());
        assertEquals(1_089_000, jane.getLumpAmount());
        assertEquals(1_089_000, jane.getStructuredAmount());
        assertEquals(1_122_000, jane.getMilestonesAmount());
        assertEquals(33_660, jane.getMilestones().get(0).getAmount());
        assertEquals(269_280, jane.getMilestones().get(1).getAmount());
        assertEquals(280_500, jane.getMilestones().get(4).getAmount());
        assertEquals(10, jane.getWithinYears());

        PersonPlan john = plan.getPeople().get(1);
        assertEquals(100, john.getLumpPercent());
        assertEquals(1_700_000, john.getLumpAmount());
    }

    @Test
    void zeroShare_hasNoDistribution() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Spouse", 100, lump());
        person(submission, "b", "John", "Child", 0, null);

        PersonPlan john = LegacyBlueprintCalculator.calculate(submission).getPeople().get(1);

        assertEquals(0, john.getAmount());
        assertTrue(john.getParts().isEmpty());
        assertNull(john.getLumpAmount());
    }

    @Test
    void specialAgeMilestone_defaultsTo21() {
        LegacyBlueprintSubmission submission = known(1_000_000);
        person(submission, "a", "Jane", "Child", 100, milestones(allocated("age", 100)));

        PersonPlan jane = LegacyBlueprintCalculator.calculate(submission).getPeople().get(0);

        assertEquals(21, jane.getMilestones().get(0).getAge());
        assertEquals(1_000_000, jane.getMilestones().get(0).getAmount());
    }

    @Test
    void unsureMode_liftsNeedsToMinimumAndRaisesPayment() {
        LegacyBlueprintSubmission submission = unsure();
        person(submission, "a", "Jane", "Spouse", null, structured(withAmount(level("monthly", 10), 2000)));

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);
        StructuredPlan structured = plan.getPeople().get(0).getStructured();

        assertEquals(240_000, plan.getNeedsTotal());
        assertEquals(1_000_000, plan.getLegacyAmount());
        assertEquals(100, plan.getPeople().get(0).getShare());
        assertEquals(8333, structured.getPayment());
        assertEquals(10, structured.getSchedule().size());
        assertEquals(100_036, structured.getSchedule().get(9).getPaid());
    }

    @Test
    void unsureMode_derivesSharesComboAndMilestoneSplits() {
        LegacyBlueprintSubmission submission = unsure();
        Distribution combo = distribution("lump", "structured", "milestones");
        combo.setLumpAmount(300_000L);
        combo.setStructured(withAmount(stepUp("yearly", 5, 5, 2), 30_000));
        combo.setMilestones(amounts("college", 100_000, "marriage", 50_000));
        person(submission, "a", "Jane", "Spouse", null, combo);
        person(submission, "b", "John", "Child", null, milestones(amounts("age", 20_000, "care", 250_000, "property", 100_000)));

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);
        PersonPlan jane = plan.getPeople().get(0);
        PersonPlan john = plan.getPeople().get(1);

        assertEquals(1_000_000, plan.getLegacyAmount());
        assertEquals(62, jane.getShare());
        assertEquals(38, john.getShare());
        assertEquals(49, jane.getLumpPercent());
        assertEquals(26, jane.getStructuredPercent());
        assertEquals(25, jane.getMilestonesPercent());
        assertEquals(303_800, jane.getLumpAmount());
        assertEquals(67, jane.getMilestones().get(0).getPercent());
        assertEquals(33, jane.getMilestones().get(1).getPercent());
        assertEquals(Arrays.asList(5, 68, 27), Arrays.asList(john.getMilestones().get(0).getPercent(),
                john.getMilestones().get(1).getPercent(), john.getMilestones().get(2).getPercent()));
        assertEquals(30_985, jane.getStructured().getPayment());
        long[] paid = jane.getStructured().getSchedule().stream().mapToLong(ScheduleRow::getPaid).toArray();
        assertArrayEquals(new long[]{30_985, 30_985, 32_534, 32_534, 34_162}, paid);
    }

    @Test
    void unsureMode_aboveMinimumKeepsNeedsTotal() {
        LegacyBlueprintSubmission submission = unsure();
        Distribution combo = distribution("lump", "structured");
        combo.setLumpAmount(900_000L);
        combo.setStructured(withAmount(level("monthly", 20), 5000));
        person(submission, "a", "Jane", "Spouse", null, combo);

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);

        assertEquals(2_100_000, plan.getLegacyAmount());
        assertEquals(43, plan.getPeople().get(0).getLumpPercent());
        assertEquals(57, plan.getPeople().get(0).getStructuredPercent());
        assertEquals(4988, plan.getPeople().get(0).getStructured().getPayment());
    }

    @ParameterizedTest(name = "gap {0} -> plan {1}")
    @CsvSource({
            "0, 1000000",
            "500000, 1000000",
            // v8 over-rounds exact multiples (comparison doc issue A) - pinned until the business confirms.
            "1000000, 2000000",
            "1500000, 2000000",
            "1700000, 2000000",
            "2000000, 3000000",
            "4750000, 5000000"
    })
    void recommendedPlanAmount_followsV8Formula(long gap, long expected) {
        assertEquals(expected, LegacyBlueprintCalculator.recommendedPlanAmount(gap));
    }

    @Test
    void gapAndPlanAmount_onPlan() {
        LegacyBlueprintSubmission submission = known(2_000_000);
        submission.setPutAside(300_000L);
        person(submission, "a", "Jane", "Spouse", 100, lump());

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);

        assertEquals(1_700_000, plan.getGap());
        assertEquals(2_000_000, plan.getRecommendedPlanAmount());
        assertFalse(plan.isGapBelowMinimum());
        assertEquals(0, LegacyBlueprintCalculator.gap(1_000_000, 1_500_000));
    }

    @Test
    void toPercentages_usesLargestRemainder() {
        assertArrayEquals(new int[]{34, 33, 33}, LegacyBlueprintCalculator.toPercentages(new double[]{1, 1, 1}));
        assertArrayEquals(new int[]{29, 59, 12}, LegacyBlueprintCalculator.toPercentages(new double[]{240_000, 500_000, 100_000}));
        assertArrayEquals(new int[]{33, 33, 34}, LegacyBlueprintCalculator.toPercentages(new double[]{1_089_000, 1_089_000, 1_122_000}));
        assertArrayEquals(new int[]{0, 0}, LegacyBlueprintCalculator.toPercentages(new double[]{0, 0}));
    }

    @Test
    void jsRound_roundsHalfUp() {
        assertEquals(3, LegacyBlueprintCalculator.jsRound(2.5));
        assertEquals(-2, LegacyBlueprintCalculator.jsRound(-2.5));
        assertEquals(4167, LegacyBlueprintCalculator.jsRound(4166.67));
    }

    @Test
    void structuredTotal_compoundsStepUp() {
        Structured st = stepUp("yearly", 5, 5, 2);
        assertEquals(156_075, LegacyBlueprintCalculator.structuredTotal(30_000, st, 5), 0.0001);
    }
}
