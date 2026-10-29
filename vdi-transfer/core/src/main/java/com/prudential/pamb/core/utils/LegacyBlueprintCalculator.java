package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.MilestonePlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.PersonPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.ScheduleRow;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.StructuredPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Milestones;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Person;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Split;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Structured;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.DEFAULT_MILESTONE_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.DEFAULT_WITHIN_YEARS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_MONTHLY;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_YEARLY;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.LEGACY_MINIMUM;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MILESTONE_SPECIAL_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MODE_UNSURE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MONTHLY_PAYMENT_MINIMUM;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PARTS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_LUMP;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_MILESTONES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_STRUCTURED;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PLAN_STEP;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_NOW;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.YEARLY_PAYMENT_MINIMUM;

/**
 * Server-side port of the v8 prototype's calculations. Arithmetic deliberately mirrors the
 * prototype (double precision, JavaScript {@code Math.round} at the same points) so the server
 * figures match what the frontend shows; every output amount is rounded to whole ringgit.
 * Expects input that has passed {@link LegacyBlueprintValidator}.
 */
public final class LegacyBlueprintCalculator {

    private LegacyBlueprintCalculator() {
    }

    public static LegacyBlueprintPlan calculate(LegacyBlueprintSubmission submission) {
        boolean unsure = MODE_UNSURE.equals(submission.getAmountMode());
        List<Person> people = submission.getPeople();
        LegacyBlueprintPlan plan = new LegacyBlueprintPlan();
        plan.setAmountMode(submission.getAmountMode());

        int[] shares = new int[people.size()];
        List<int[]> splits = new ArrayList<>();
        List<Map<String, Integer>> allocations = new ArrayList<>();
        long legacy;

        if (unsure) {
            // v8 syncNeeds: sum each person's needs, lift to the minimum, derive every percentage.
            double sum = 0;
            double[] totals = new double[people.size()];
            List<Needs> needs = new ArrayList<>();
            for (int i = 0; i < people.size(); i++) {
                Needs need = needsOf(people.get(i).getDistribution());
                needs.add(need);
                totals[i] = need.total();
                sum += totals[i];
            }
            shares = toPercentages(totals);
            for (Needs need : needs) {
                int[] pc = toPercentages(new double[]{need.lump, need.structured, need.milestones});
                splits.add(new int[]{
                        need.parts.contains(PART_LUMP) ? pc[0] : 0,
                        need.parts.contains(PART_STRUCTURED) ? pc[1] : 0,
                        need.parts.contains(PART_MILESTONES) ? pc[2] : 0});
                allocations.add(allocationFor(need.milestoneAmounts));
            }
            legacy = Math.max(jsRound(sum), LEGACY_MINIMUM);
            plan.setNeedsTotal(jsRound(sum));
        } else {
            legacy = submission.getLegacyAmount();
            for (int i = 0; i < people.size(); i++) {
                Person person = people.get(i);
                shares[i] = intValue(person.getShare());
                Distribution distribution = person.getDistribution();
                Split split = distribution != null && distribution.getSplit() != null ? distribution.getSplit() : new Split();
                splits.add(new int[]{intValue(split.getLump()), intValue(split.getStructured()), intValue(split.getMilestones())});
                Milestones milestones = distribution != null ? distribution.getMilestones() : null;
                allocations.add(milestones != null && milestones.getAllocation() != null
                        ? milestones.getAllocation() : Collections.emptyMap());
            }
        }

        long putAside = Math.max(0L, longValue(submission.getPutAside()));
        long gap = gap(legacy, putAside);
        plan.setLegacyAmount(legacy);
        plan.setPutAside(putAside);
        plan.setGap(gap);
        plan.setRecommendedPlanAmount(recommendedPlanAmount(gap));
        plan.setGapBelowMinimum(gap < LEGACY_MINIMUM);

        for (int i = 0; i < people.size(); i++) {
            plan.getPeople().add(personPlan(people.get(i), legacy, shares[i], splits.get(i), allocations.get(i), unsure));
        }
        return plan;
    }

    /** Still to plan for: {@code max(amount to leave - already put aside, 0)}. */
    public static long gap(long legacyAmount, long putAside) {
        return Math.max(legacyAmount - putAside, 0L);
    }

    /**
     * Recommended plan amount, exactly as v8: {@code (floor(gap / 1M) + 1) x 1M}. Note this adds an
     * extra RM 1M when the gap is an exact multiple of RM 1M (comparison doc issue A, open with the
     * business); change only this method once the intended rule is confirmed.
     */
    public static long recommendedPlanAmount(long gap) {
        return (Math.max(0L, gap) / PLAN_STEP + 1) * PLAN_STEP;
    }

    /** v8 {@code firstPayFor}: first payment per period so the payments over {@code years} use up {@code pot}. */
    public static long firstPaymentFor(double pot, Structured st, int years) {
        int n = Math.max(1, years);
        if (!(pot > 0)) {
            return 0;
        }
        boolean stepUp = isStepUp(st);
        double rate = intValue(st.getRate()) / 100.0;
        int interval = Math.max(1, intValue(st.getInterval()));
        double denominator = 0;
        for (int y = 0; y < n; y++) {
            denominator += stepUp ? Math.pow(1 + rate, Math.floor((double) y / interval)) : 1;
        }
        double firstYear = pot / denominator;
        return jsRound(isMonthly(st) ? firstYear / 12 : firstYear);
    }

    /** v8 {@code structTotal}: total paid over {@code years} starting at {@code amount} per period. */
    public static double structuredTotal(double amount, Structured st, int years) {
        int perYear = periodsPerYear(st);
        boolean stepUp = isStepUp(st);
        double rate = intValue(st.getRate()) / 100.0;
        int interval = Math.max(1, intValue(st.getInterval()));
        double pay = amount;
        double total = 0;
        for (int y = 0; y < Math.max(0, years); y++) {
            if (stepUp && y > 0 && y % interval == 0) {
                pay *= 1 + rate;
            }
            total += pay * perYear;
        }
        return total;
    }

    /**
     * v8 {@code paySchedule} with the chosen years as the cap and the remainder folded into the final
     * year, so the rows always pay out exactly {@code pot}.
     */
    public static List<ScheduleRow> paySchedule(double pot, double amount, Structured st, int years) {
        List<ScheduleRow> rows = new ArrayList<>();
        if (!(amount > 0) || !(pot > 0) || years < 1) {
            return rows;
        }
        int perYear = periodsPerYear(st);
        boolean stepUp = isStepUp(st);
        double rate = intValue(st.getRate()) / 100.0;
        int interval = Math.max(1, intValue(st.getInterval()));
        double left = pot;
        double pay = amount;
        int year = 0;
        while (left > 0.5 && year < years) {
            if (stepUp && year > 0 && year % interval == 0) {
                pay = pay * (1 + rate);
            }
            double paid = Math.min(left, pay * perYear);
            if (year == years - 1) {
                paid = left;
            }
            left -= paid;
            year++;
            ScheduleRow row = new ScheduleRow();
            row.setYear(year);
            row.setPayment(jsRound(pay));
            row.setPaid(jsRound(paid));
            row.setRemaining(jsRound(Math.max(left, 0)));
            rows.add(row);
        }
        return rows;
    }

    /**
     * v8 {@code toPcts}: whole percentages proportional to {@code values} that add up to exactly 100,
     * handing the leftover points to the largest fractional remainders (ties keep input order).
     */
    public static int[] toPercentages(double[] values) {
        int[] result = new int[values.length];
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        if (!(sum > 0)) {
            return result;
        }
        double[] raw = new double[values.length];
        int floorSum = 0;
        for (int i = 0; i < values.length; i++) {
            raw[i] = values[i] / sum * 100;
            result[i] = (int) Math.floor(raw[i]);
            floorSum += result[i];
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            order.add(i);
        }
        // List.sort is stable, matching Array.prototype.sort for equal remainders.
        order.sort(Comparator.comparingDouble((Integer i) -> raw[i] - result[i]).reversed());
        int remaining = 100 - floorSum;
        for (int k = 0; k < remaining; k++) {
            result[order.get(k % order.size())]++;
        }
        return result;
    }

    public static long paymentMinimum(Structured st) {
        return isMonthly(st) ? MONTHLY_PAYMENT_MINIMUM : YEARLY_PAYMENT_MINIMUM;
    }

    public static int periodsPerYear(Structured st) {
        return isMonthly(st) ? 12 : 1;
    }

    /** The chosen parts in canonical order (lump, structured, milestones), ignoring unknown values. */
    public static List<String> partsOf(Distribution distribution) {
        List<String> parts = new ArrayList<>();
        if (distribution == null || distribution.getParts() == null) {
            return parts;
        }
        for (String part : PARTS) {
            if (distribution.getParts().contains(part)) {
                parts.add(part);
            }
        }
        return parts;
    }

    /** Same as JavaScript {@code Math.round} (half-up towards +infinity), so amounts match the v8 frontend. */
    public static long jsRound(double value) {
        return (long) Math.floor(value + 0.5);
    }

    private static PersonPlan personPlan(Person person, long legacy, int share, int[] split,
                                         Map<String, Integer> allocation, boolean unsure) {
        PersonPlan personPlan = new PersonPlan();
        personPlan.setId(person.getId());
        personPlan.setName(person.getName() == null ? null : person.getName().trim());
        personPlan.setRelationship(person.getRelationship());
        personPlan.setShare(share);
        double amount = legacy * (double) share / 100;
        personPlan.setAmount(jsRound(amount));
        if (share <= 0) {
            // v8 skips the distribution step for people given 0%; nothing to plan for them.
            return personPlan;
        }

        Distribution distribution = person.getDistribution();
        List<String> parts = partsOf(distribution);
        boolean multi = parts.size() > 1;
        personPlan.setParts(parts);

        if (parts.contains(PART_LUMP)) {
            int pct = multi ? split[0] : 100;
            personPlan.setLumpPercent(pct);
            personPlan.setLumpAmount(jsRound(multi ? amount * pct / 100 : amount));
        }
        if (parts.contains(PART_STRUCTURED)) {
            int pct = multi ? split[1] : 100;
            double pot = multi ? amount * pct / 100 : amount;
            personPlan.setStructuredPercent(pct);
            personPlan.setStructuredAmount(jsRound(pot));
            personPlan.setStructured(structuredPlan(distribution.getStructured(), pot, unsure));
        }
        if (parts.contains(PART_MILESTONES)) {
            int pct = multi ? split[2] : 100;
            double pot = multi ? amount * pct / 100 : amount;
            personPlan.setMilestonesPercent(pct);
            personPlan.setMilestonesAmount(jsRound(pot));
            Milestones milestones = distribution.getMilestones();
            List<MilestonePlan> milestonePlans = new ArrayList<>();
            for (String id : milestones.getSelected()) {
                MilestonePlan milestonePlan = new MilestonePlan();
                milestonePlan.setId(id);
                int milestonePct = intValue(allocation.get(id));
                milestonePlan.setPercent(milestonePct);
                milestonePlan.setAmount(jsRound(pot * milestonePct / 100));
                if (MILESTONE_SPECIAL_AGE.equals(id)) {
                    Integer age = milestones.getAges() != null ? milestones.getAges().get(id) : null;
                    milestonePlan.setAge(age != null && age > 0 ? age : DEFAULT_MILESTONE_AGE);
                }
                milestonePlans.add(milestonePlan);
            }
            personPlan.setMilestones(milestonePlans);
            personPlan.setWithinYears(milestones.getWithin() != null ? milestones.getWithin() : DEFAULT_WITHIN_YEARS);
        }
        return personPlan;
    }

    private static StructuredPlan structuredPlan(Structured st, double pot, boolean unsure) {
        int years = intValue(st.getYears());
        long first = unsure ? longValue(st.getAmount()) : firstPaymentFor(pot, st, years);
        // Rounding (and the RM 1M top-up in calculator mode) means the payments rarely add up to the
        // pot exactly, so v8 rescales them before building the schedule.
        double payment = first;
        double base = structuredTotal(first, st, years);
        if (base > 0 && Math.abs(base - pot) > 1) {
            payment = jsRound(first * pot / base);
        }

        StructuredPlan plan = new StructuredPlan();
        plan.setFrequency(isMonthly(st) ? FREQUENCY_MONTHLY : FREQUENCY_YEARLY);
        plan.setYears(years);
        plan.setStepUp(isStepUp(st));
        if (isStepUp(st)) {
            plan.setRate(intValue(st.getRate()));
            plan.setInterval(intValue(st.getInterval()));
        }
        boolean atAge = START_AGE.equals(st.getStart());
        plan.setStart(atAge ? START_AGE : START_NOW);
        if (atAge) {
            plan.setStartAge(intValue(st.getStartAge()));
        }
        plan.setPayment(jsRound(payment));
        plan.setSchedule(paySchedule(pot, payment, st, years));
        return plan;
    }

    private static Needs needsOf(Distribution distribution) {
        Needs needs = new Needs();
        needs.parts = partsOf(distribution);
        if (needs.parts.contains(PART_LUMP)) {
            needs.lump = longValue(distribution.getLumpAmount());
        }
        if (needs.parts.contains(PART_STRUCTURED)) {
            Structured st = distribution.getStructured();
            needs.structured = structuredTotal(longValue(st.getAmount()), st, intValue(st.getYears()));
        }
        if (needs.parts.contains(PART_MILESTONES)) {
            Milestones milestones = distribution.getMilestones();
            for (String id : milestones.getSelected()) {
                long value = milestones.getAmounts() != null ? longValue(milestones.getAmounts().get(id)) : 0L;
                needs.milestoneAmounts.put(id, (double) value);
                needs.milestones += value;
            }
        }
        return needs;
    }

    private static Map<String, Integer> allocationFor(Map<String, Double> amounts) {
        List<String> ids = new ArrayList<>(amounts.keySet());
        double[] values = new double[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            values[i] = amounts.get(ids.get(i));
        }
        int[] pcts = toPercentages(values);
        Map<String, Integer> allocation = new LinkedHashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            allocation.put(ids.get(i), pcts[i]);
        }
        return allocation;
    }

    private static boolean isMonthly(Structured st) {
        return !FREQUENCY_YEARLY.equals(st.getFrequency());
    }

    private static boolean isStepUp(Structured st) {
        return Boolean.TRUE.equals(st.getStepUp());
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private static long longValue(Long value) {
        return value == null ? 0L : value;
    }

    /** One person's needs in calculator ("not sure") mode, mirroring v8 {@code personNeedParts}. */
    private static final class Needs {
        private List<String> parts = new ArrayList<>();
        private double lump;
        private double structured;
        private double milestones;
        private final Map<String, Double> milestoneAmounts = new LinkedHashMap<>();

        private double total() {
            return lump + structured + milestones;
        }
    }
}
