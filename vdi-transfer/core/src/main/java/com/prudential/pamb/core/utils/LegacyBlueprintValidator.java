package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Contact;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Milestones;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Person;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Split;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Structured;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.AGE_RANGES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.COUNTRY_CODES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_MONTHLY;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.FREQUENCY_YEARLY;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.INTRO_SCALE_MAX;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.LEGACY_MAXIMUM;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.LEGACY_MINIMUM;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MALAYSIA_CODE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_MILESTONES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_NAME_LENGTH;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_PAYMENT_YEARS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_PEOPLE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_START_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_STEP_UP_RATE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MAX_WITHIN_YEARS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MILESTONES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MILESTONE_SPECIAL_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MODE_KNOWN;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.MODE_UNSURE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.NEEDS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_LUMP;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_MILESTONES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PART_STRUCTURED;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.PEACE_OF_MIND;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.RELATIONSHIP_LIMITS;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.RELATIONSHIP_STATUSES;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_AGE;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.START_NOW;
import static com.prudential.pamb.core.constants.LegacyBlueprintConstants.STATES;

/**
 * Re-validates every answer on the server with the v8 rules. Returns error codes (field path plus
 * rule, e.g. {@code people[1].structured.paymentBelowMinimum}) and never echoes visitor input, so
 * the codes are safe to log and return.
 */
public final class LegacyBlueprintValidator {

    // v8 email rule, also rejecting ',' and ';' so a single field can't address several recipients.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@,;]+@[^\\s@,;]+\\.[^\\s@,;]{2,}$");
    private static final Pattern MY_MOBILE = Pattern.compile("^1\\d{8,9}$");
    private static final Pattern REFERRAL = Pattern.compile("^[A-Za-z0-9]{0,8}$");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}<>]");
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_ID_LENGTH = 40;

    private LegacyBlueprintValidator() {
    }

    /** Returns an empty list when valid. Relationships are checked against the authored option values. */
    public static List<String> validate(LegacyBlueprintSubmission submission, Collection<String> allowedRelationships) {
        List<String> errors = new ArrayList<>();
        if (submission == null) {
            errors.add("request.empty");
            return errors;
        }

        checkScale(submission.getImportance(), "importance", errors);
        checkScale(submission.getPreparedness(), "preparedness", errors);
        List<String> needs = submission.getNeeds();
        if (needs == null || needs.size() != NEEDS.size() || !NEEDS.keySet().containsAll(needs)
                || new HashSet<>(needs).size() != NEEDS.size()) {
            errors.add("needs.rankAll");
        }
        checkOption(submission.getPeaceOfMind(), PEACE_OF_MIND.keySet(), "peaceOfMind", errors);
        checkOption(submission.getAgeRange(), AGE_RANGES.keySet(), "ageRange", errors);
        checkOption(submission.getRelationshipStatus(), RELATIONSHIP_STATUSES.keySet(), "relationshipStatus", errors);

        String mode = submission.getAmountMode();
        boolean unsure = MODE_UNSURE.equals(mode);
        if (!unsure && !MODE_KNOWN.equals(mode)) {
            errors.add("amountMode.invalid");
        }
        if (!inRange(submission.getPutAside() == null ? 0L : submission.getPutAside(), 0, LEGACY_MAXIMUM)) {
            errors.add("putAside.invalid");
        }

        List<Person> people = submission.getPeople();
        if (people == null || people.isEmpty() || people.size() > MAX_PEOPLE) {
            errors.add("people.count");
        } else {
            checkPeople(people, allowedRelationships, errors);
            if (MODE_KNOWN.equals(mode)) {
                checkKnownAmount(submission, errors);
            }
            if (errors.isEmpty()) {
                checkDistributions(submission, unsure, errors);
            }
        }

        checkContact(submission.getContact(), errors);
        return errors;
    }

    /** v8 Malaysian mobile rule after removing a typed 60 prefix and leading 0. */
    public static String normaliseMobile(String countryCode, String mobile) {
        String digits = StringUtils.defaultString(mobile).replaceAll("\\D", "");
        if (MALAYSIA_CODE.equals(countryCode) && digits.startsWith(MALAYSIA_CODE)) {
            digits = digits.substring(MALAYSIA_CODE.length());
        }
        return StringUtils.stripStart(digits, "0");
    }

    private static void checkPeople(List<Person> people, Collection<String> allowedRelationships, List<String> errors) {
        Set<String> ids = new HashSet<>();
        Map<String, Integer> relationshipCounts = new HashMap<>();
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            String prefix = "people[" + i + "]";
            if (person == null) {
                errors.add(prefix + ".missing");
                continue;
            }
            if (StringUtils.isBlank(person.getId()) || person.getId().length() > MAX_ID_LENGTH || !ids.add(person.getId())) {
                errors.add(prefix + ".id");
            }
            if (!isValidName(person.getName())) {
                errors.add(prefix + ".name");
            }
            String relationship = person.getRelationship();
            if (relationship == null || allowedRelationships == null || !allowedRelationships.contains(relationship)) {
                errors.add(prefix + ".relationship");
            } else {
                String key = relationship.toLowerCase(Locale.ROOT);
                int count = relationshipCounts.merge(key, 1, Integer::sum);
                Integer limit = RELATIONSHIP_LIMITS.get(key);
                if (limit != null && count > limit) {
                    errors.add(prefix + ".relationshipLimit");
                }
            }
        }
    }

    private static void checkKnownAmount(LegacyBlueprintSubmission submission, List<String> errors) {
        Long legacy = submission.getLegacyAmount();
        if (legacy == null || legacy < LEGACY_MINIMUM || legacy > LEGACY_MAXIMUM) {
            errors.add("legacyAmount.belowMinimum");
        }
        int total = 0;
        for (int i = 0; i < submission.getPeople().size(); i++) {
            Integer share = submission.getPeople().get(i).getShare();
            if (share == null || share < 0 || share > 100) {
                errors.add("people[" + i + "].share");
            } else {
                total += share;
            }
        }
        if (total != 100) {
            errors.add("shares.total");
        }
    }

    private static void checkDistributions(LegacyBlueprintSubmission submission, boolean unsure, List<String> errors) {
        List<Person> people = submission.getPeople();
        double needsTotal = 0;
        for (int i = 0; i < people.size(); i++) {
            Person person = people.get(i);
            // v8 only asks people with a share how to receive it (calculator mode asks everyone).
            if (!unsure && person.getShare() == 0) {
                continue;
            }
            String prefix = "people[" + i + "]";
            Distribution distribution = person.getDistribution();
            List<String> parts = LegacyBlueprintCalculator.partsOf(distribution);
            if (parts.isEmpty() || parts.size() != new HashSet<>(distribution.getParts()).size()) {
                errors.add(prefix + ".method");
                continue;
            }
            boolean multi = parts.size() > 1;
            double personPot = unsure ? 0 : submission.getLegacyAmount() * (double) person.getShare() / 100;

            if (!unsure && multi) {
                checkSplit(distribution.getSplit(), parts, prefix, errors);
            }
            if (parts.contains(PART_LUMP) && unsure) {
                Long lump = distribution.getLumpAmount();
                if (lump == null || lump <= 0 || lump > LEGACY_MAXIMUM) {
                    errors.add(prefix + ".lumpAmount");
                } else {
                    needsTotal += lump;
                }
            }
            if (parts.contains(PART_STRUCTURED)) {
                double pot = multi && distribution.getSplit() != null && distribution.getSplit().getStructured() != null
                        ? personPot * distribution.getSplit().getStructured() / 100 : personPot;
                needsTotal += checkStructured(distribution.getStructured(), pot, unsure, prefix + ".structured", errors);
            }
            if (parts.contains(PART_MILESTONES)) {
                needsTotal += checkMilestones(distribution.getMilestones(), unsure, prefix + ".milestones", errors);
            }
        }
        if (unsure && needsTotal > LEGACY_MAXIMUM) {
            errors.add("needs.total");
        }
    }

    private static void checkSplit(Split split, List<String> parts, String prefix, List<String> errors) {
        if (split == null) {
            errors.add(prefix + ".split.total");
            return;
        }
        Integer[] values = {split.getLump(), split.getStructured(), split.getMilestones()};
        String[] keys = {PART_LUMP, PART_STRUCTURED, PART_MILESTONES};
        int total = 0;
        for (int k = 0; k < keys.length; k++) {
            int value = values[k] == null ? 0 : values[k];
            if (value < 0 || value > 100) {
                errors.add(prefix + ".split.invalid");
                return;
            }
            if (parts.contains(keys[k])) {
                if (value < 1) {
                    errors.add(prefix + ".split.partMissing");
                }
                total += value;
            } else if (value != 0) {
                errors.add(prefix + ".split.unexpected");
            }
        }
        if (total != 100) {
            errors.add(prefix + ".split.total");
        }
    }

    /** Validates regular payments; returns the calculator-mode total so the needs sum can be bounded. */
    private static double checkStructured(Structured st, double pot, boolean unsure, String prefix, List<String> errors) {
        if (st == null) {
            errors.add(prefix + ".missing");
            return 0;
        }
        int before = errors.size();
        if (!FREQUENCY_MONTHLY.equals(st.getFrequency()) && !FREQUENCY_YEARLY.equals(st.getFrequency())) {
            errors.add(prefix + ".frequency");
        }
        if (st.getYears() == null || st.getYears() < 1 || st.getYears() > MAX_PAYMENT_YEARS) {
            errors.add(prefix + ".years");
        }
        if (Boolean.TRUE.equals(st.getStepUp())
                && (st.getRate() == null || st.getRate() < 1 || st.getRate() > MAX_STEP_UP_RATE
                || st.getInterval() == null || st.getInterval() < 1 || st.getInterval() > MAX_PAYMENT_YEARS)) {
            errors.add(prefix + ".stepUp");
        }
        if (!START_NOW.equals(st.getStart()) && !START_AGE.equals(st.getStart())) {
            errors.add(prefix + ".start");
        } else if (START_AGE.equals(st.getStart())
                && (st.getStartAge() == null || st.getStartAge() < 1 || st.getStartAge() > MAX_START_AGE)) {
            errors.add(prefix + ".startAge");
        }
        if (errors.size() > before) {
            return 0;
        }

        long minimum = LegacyBlueprintCalculator.paymentMinimum(st);
        if (unsure) {
            Long amount = st.getAmount();
            if (amount == null || amount < minimum || amount > LEGACY_MAXIMUM) {
                errors.add(prefix + ".paymentBelowMinimum");
                return 0;
            }
            return LegacyBlueprintCalculator.structuredTotal(amount, st, st.getYears());
        }
        long first = LegacyBlueprintCalculator.firstPaymentFor(pot, st, st.getYears());
        if (first < minimum) {
            errors.add(prefix + ".paymentBelowMinimum");
        } else if (first * LegacyBlueprintCalculator.periodsPerYear(st) > pot) {
            errors.add(prefix + ".paymentExceedsShare");
        }
        return 0;
    }

    /** Validates life-moment payouts; returns the calculator-mode total so the needs sum can be bounded. */
    private static double checkMilestones(Milestones milestones, boolean unsure, String prefix, List<String> errors) {
        List<String> selected = milestones == null ? null : milestones.getSelected();
        if (selected == null || selected.isEmpty() || selected.size() > MAX_MILESTONES
                || !MILESTONES.keySet().containsAll(selected) || new HashSet<>(selected).size() != selected.size()) {
            errors.add(prefix + ".selection");
            return 0;
        }
        if (milestones.getWithin() != null && (milestones.getWithin() < 1 || milestones.getWithin() > MAX_WITHIN_YEARS)) {
            errors.add(prefix + ".within");
        }
        if (selected.contains(MILESTONE_SPECIAL_AGE) && milestones.getAges() != null) {
            Integer age = milestones.getAges().get(MILESTONE_SPECIAL_AGE);
            if (age != null && (age < 1 || age > MAX_START_AGE)) {
                errors.add(prefix + ".age");
            }
        }

        double total = 0;
        if (unsure) {
            Map<String, Long> amounts = milestones.getAmounts();
            for (String id : selected) {
                Long amount = amounts == null ? null : amounts.get(id);
                if (amount == null || amount <= 0 || amount > LEGACY_MAXIMUM) {
                    errors.add(prefix + ".amount");
                    return 0;
                }
                total += amount;
            }
            return total;
        }
        Map<String, Integer> allocation = milestones.getAllocation();
        int sum = 0;
        for (String id : selected) {
            Integer pct = allocation == null ? null : allocation.get(id);
            if (pct == null || pct < 0 || pct > 100) {
                errors.add(prefix + ".allocation");
                return 0;
            }
            sum += pct;
        }
        if (sum != 100) {
            errors.add(prefix + ".allocationTotal");
        }
        return 0;
    }

    private static void checkContact(Contact contact, List<String> errors) {
        if (contact == null) {
            errors.add("contact.missing");
            return;
        }
        if (!isValidName(contact.getFullName())) {
            errors.add("contact.fullName");
        }
        String email = StringUtils.trimToEmpty(contact.getEmail());
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
            errors.add("contact.email");
        }
        String countryCode = contact.getCountryCode();
        if (!COUNTRY_CODES.contains(countryCode)) {
            errors.add("contact.countryCode");
        } else {
            String digits = StringUtils.defaultString(contact.getMobile()).replaceAll("\\D", "");
            boolean valid = MALAYSIA_CODE.equals(countryCode)
                    ? MY_MOBILE.matcher(normaliseMobile(countryCode, contact.getMobile())).matches()
                    : digits.length() >= 6 && digits.length() <= 14;
            if (!valid) {
                errors.add("contact.mobile");
            }
        }
        if (StringUtils.defaultString(contact.getNric()).replaceAll("\\D", "").length() != 12) {
            errors.add("contact.nric");
        }
        if (!STATES.contains(contact.getState())) {
            errors.add("contact.state");
        }
        if (!REFERRAL.matcher(StringUtils.defaultString(contact.getReferralCode())).matches()) {
            errors.add("contact.referralCode");
        }
        if (!Boolean.TRUE.equals(contact.getConsent())) {
            errors.add("contact.consent");
        }
    }

    private static boolean isValidName(String name) {
        String trimmed = StringUtils.trimToEmpty(name);
        return !trimmed.isEmpty() && trimmed.length() <= MAX_NAME_LENGTH && !CONTROL_CHARS.matcher(trimmed).find();
    }

    private static void checkScale(Integer value, String field, List<String> errors) {
        if (value == null || value < 1 || value > INTRO_SCALE_MAX) {
            errors.add(field + ".invalid");
        }
    }

    private static void checkOption(String value, Collection<String> allowed, String field, List<String> errors) {
        if (value == null || !allowed.contains(value)) {
            errors.add(field + ".invalid");
        }
    }

    private static boolean inRange(long value, long min, long max) {
        return value >= min && value <= max;
    }
}
