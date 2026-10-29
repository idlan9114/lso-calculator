package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Contact;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Distribution;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Milestones;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Person;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Split;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Structured;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builders for Legacy Blueprint test submissions. */
public final class LegacyBlueprintTestData {

    private LegacyBlueprintTestData() {
    }

    /** A valid "amount known" submission with no people yet; add them with {@link #person}. */
    public static LegacyBlueprintSubmission known(long legacyAmount) {
        LegacyBlueprintSubmission submission = base();
        submission.setAmountMode("known");
        submission.setLegacyAmount(legacyAmount);
        return submission;
    }

    /** A valid calculator ("not sure") submission with no people yet. */
    public static LegacyBlueprintSubmission unsure() {
        LegacyBlueprintSubmission submission = base();
        submission.setAmountMode("unsure");
        return submission;
    }

    public static Person person(LegacyBlueprintSubmission submission, String id, String name, String relationship,
                                Integer share, Distribution distribution) {
        Person person = new Person();
        person.setId(id);
        person.setName(name);
        person.setRelationship(relationship);
        person.setShare(share);
        person.setDistribution(distribution);
        submission.getPeople().add(person);
        return person;
    }

    public static Distribution lump() {
        return distribution("lump");
    }

    public static Distribution lump(long amount) {
        Distribution distribution = distribution("lump");
        distribution.setLumpAmount(amount);
        return distribution;
    }

    public static Distribution structured(Structured structured) {
        Distribution distribution = distribution("structured");
        distribution.setStructured(structured);
        return distribution;
    }

    public static Distribution milestones(Milestones milestones) {
        Distribution distribution = distribution("milestones");
        distribution.setMilestones(milestones);
        return distribution;
    }

    public static Distribution distribution(String... parts) {
        Distribution distribution = new Distribution();
        distribution.setParts(new ArrayList<>(Arrays.asList(parts)));
        return distribution;
    }

    public static Split split(Integer lump, Integer structured, Integer milestones) {
        Split split = new Split();
        split.setLump(lump);
        split.setStructured(structured);
        split.setMilestones(milestones);
        return split;
    }

    public static Structured level(String frequency, int years) {
        Structured structured = new Structured();
        structured.setFrequency(frequency);
        structured.setYears(years);
        structured.setStepUp(false);
        structured.setStart("now");
        return structured;
    }

    public static Structured stepUp(String frequency, int years, int rate, int interval) {
        Structured structured = level(frequency, years);
        structured.setStepUp(true);
        structured.setRate(rate);
        structured.setInterval(interval);
        return structured;
    }

    public static Structured withAmount(Structured structured, long amount) {
        structured.setAmount(amount);
        return structured;
    }

    /** Milestones with a percent allocation, given as id/percent pairs. */
    public static Milestones allocated(Object... idPercentPairs) {
        Milestones milestones = new Milestones();
        milestones.setSelected(new ArrayList<>());
        Map<String, Integer> allocation = new LinkedHashMap<>();
        for (int i = 0; i < idPercentPairs.length; i += 2) {
            milestones.getSelected().add((String) idPercentPairs[i]);
            allocation.put((String) idPercentPairs[i], (Integer) idPercentPairs[i + 1]);
        }
        milestones.setAllocation(allocation);
        return milestones;
    }

    /** Milestones with an amount per milestone (calculator mode), given as id/amount pairs. */
    public static Milestones amounts(Object... idAmountPairs) {
        Milestones milestones = new Milestones();
        milestones.setSelected(new ArrayList<>());
        Map<String, Long> amounts = new LinkedHashMap<>();
        for (int i = 0; i < idAmountPairs.length; i += 2) {
            milestones.getSelected().add((String) idAmountPairs[i]);
            amounts.put((String) idAmountPairs[i], ((Number) idAmountPairs[i + 1]).longValue());
        }
        milestones.setAmounts(amounts);
        return milestones;
    }

    private static LegacyBlueprintSubmission base() {
        LegacyBlueprintSubmission submission = new LegacyBlueprintSubmission();
        submission.setImportance(5);
        submission.setPreparedness(4);
        submission.setNeeds(new ArrayList<>(Arrays.asList("everyday", "education", "care", "family", "retirement")));
        submission.setPeaceOfMind("timing");
        submission.setAgeRange("35-44");
        submission.setRelationshipStatus("married");
        submission.setPeople(new ArrayList<>());
        submission.setPutAside(0L);

        Contact contact = new Contact();
        contact.setFullName("Aisyah Rahman");
        contact.setEmail("aisyah@example.com");
        contact.setCountryCode("60");
        contact.setMobile("012-345 6789");
        contact.setNric("900101-14-5678");
        contact.setState("Selangor");
        contact.setReferralCode("AB12CD34");
        contact.setConsent(true);
        submission.setContact(contact);
        return submission;
    }
}
