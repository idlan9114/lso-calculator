package com.prudential.pamb.core.models.dto.legacyblueprint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-calculated blueprint. Feeds the client API payload and the email, and is returned to the
 * frontend so the blueprint report shows the authoritative figures. Amounts are whole ringgit.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegacyBlueprintPlan {

    private String amountMode;
    /** Amount to leave: as entered ({@code known}) or the needs total lifted to the minimum ({@code unsure}). */
    private long legacyAmount;
    /** Sum of every person's needs before the minimum is applied ({@code unsure} mode only). */
    private Long needsTotal;
    private long putAside;
    /** Still to plan for: {@code max(legacyAmount - putAside, 0)}. */
    private long gap;
    private long recommendedPlanAmount;
    private boolean gapBelowMinimum;
    private List<PersonPlan> people = new ArrayList<>();

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PersonPlan {
        private String id;
        /** Kept for the API payload and email; not echoed back to the browser. */
        @JsonIgnore
        private String name;
        private String relationship;
        private int share;
        private long amount;
        private List<String> parts = new ArrayList<>();
        private Integer lumpPercent;
        private Integer structuredPercent;
        private Integer milestonesPercent;
        private Long lumpAmount;
        private Long structuredAmount;
        private Long milestonesAmount;
        private StructuredPlan structured;
        private List<MilestonePlan> milestones;
        private Integer withinYears;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StructuredPlan {
        private String frequency;
        private int years;
        private boolean stepUp;
        private Integer rate;
        private Integer interval;
        private String start;
        private Integer startAge;
        /** First payment per period, scaled so the schedule pays out exactly the pot. */
        private long payment;
        private List<ScheduleRow> schedule = new ArrayList<>();
    }

    @Data
    public static class ScheduleRow {
        private int year;
        /** Payment per period during this year. */
        private long payment;
        private long paid;
        private long remaining;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MilestonePlan {
        private String id;
        private int percent;
        private long amount;
        private Integer age;
    }
}
