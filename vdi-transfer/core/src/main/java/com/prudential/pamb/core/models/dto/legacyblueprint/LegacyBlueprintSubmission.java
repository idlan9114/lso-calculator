package com.prudential.pamb.core.models.dto.legacyblueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Raw answers posted by the frontend to {@code <component>.submit.json}. Only the visitor's choices
 * are accepted; every amount, share split (in calculator mode) and payment is recalculated on the
 * server, so browser-calculated figures are never part of this contract.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegacyBlueprintSubmission {

    /** Intro question 1, 1-5: how much leaving a legacy matters. */
    private Integer importance;
    /** Intro question 2, 1-5: whether current plans would be enough. */
    private Integer preparedness;
    /** All five need IDs in ranked order. */
    private List<String> needs;
    private String peaceOfMind;
    private String ageRange;
    private String relationshipStatus;
    private List<Person> people;
    /** {@code known} (visitor enters the amount) or {@code unsure} (built from each person's needs). */
    private String amountMode;
    /** Amount to leave, required in {@code known} mode; ignored in {@code unsure} mode. */
    private Long legacyAmount;
    /** Amount already put aside (savings, insurance, EPF); 0 when none. */
    private Long putAside;
    private Contact contact;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Person {
        private String id;
        private String name;
        /** Authored relationship option value, e.g. {@code Spouse}. */
        private String relationship;
        /** Share of the legacy in whole percent ({@code known} mode only). */
        private Integer share;
        private Distribution distribution;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Distribution {
        /** Any of {@code lump}, {@code structured}, {@code milestones}. */
        private List<String> parts;
        /** Percent split between parts when more than one is chosen ({@code known} mode only). */
        private Split split;
        /** All-at-once amount ({@code unsure} mode only). */
        private Long lumpAmount;
        private Structured structured;
        private Milestones milestones;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Split {
        private Integer lump;
        private Integer structured;
        private Integer milestones;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Structured {
        /** {@code monthly} or {@code yearly}. */
        private String frequency;
        private Integer years;
        private Boolean stepUp;
        /** Step-up percent per interval. */
        private Integer rate;
        /** Step-up interval in years. */
        private Integer interval;
        /** {@code now} or {@code age}. */
        private String start;
        private Integer startAge;
        /** Payment per period ({@code unsure} mode only; calculated from the years in {@code known} mode). */
        private Long amount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Milestones {
        /** Milestone IDs in the order picked. */
        private List<String> selected;
        /** Percent of the milestone portion per milestone ({@code known} mode). */
        private Map<String, Integer> allocation;
        /** Amount per milestone ({@code unsure} mode). */
        private Map<String, Long> amounts;
        /** Age for the "turning a special age" milestone. */
        private Map<String, Integer> ages;
        /** All moments covered within N years (display only). */
        private Integer within;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        private String fullName;
        private String email;
        /** Dialling code without '+', e.g. {@code 60}. */
        private String countryCode;
        private String mobile;
        private String nric;
        private String state;
        private String referralCode;
        private Boolean consent;
    }
}
