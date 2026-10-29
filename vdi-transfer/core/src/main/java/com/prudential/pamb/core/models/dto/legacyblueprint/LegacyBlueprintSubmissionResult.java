package com.prudential.pamb.core.models.dto.legacyblueprint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a submission, returned to the frontend as JSON. {@code emailSent} and
 * {@code apiSubmitted} are reported separately: the email is always attempted even when the API fails.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegacyBlueprintSubmissionResult {

    /** {@code ok} (both succeeded), {@code partial} (one failed), {@code failed} or {@code invalid}. */
    private String status;
    private boolean emailSent;
    private boolean apiSubmitted;
    /** Reference id returned by the client API, when provided. */
    private String apiReference;
    /** HTTP status from the client API; 0 when no response was received (timeout, network, configuration). */
    private Integer apiStatus;
    /** JSON body returned by the client API (success or error), passed through unchanged. */
    private JsonNode apiResponse;
    /** Validation error codes when {@code status} is {@code invalid}. */
    private List<String> errors = new ArrayList<>();
    private LegacyBlueprintPlan plan;

    public static LegacyBlueprintSubmissionResult invalid(List<String> errors) {
        LegacyBlueprintSubmissionResult result = new LegacyBlueprintSubmissionResult();
        result.setStatus("invalid");
        result.setErrors(errors);
        return result;
    }

    @JsonIgnore
    public boolean isValid() {
        return !"invalid".equals(status);
    }
}
