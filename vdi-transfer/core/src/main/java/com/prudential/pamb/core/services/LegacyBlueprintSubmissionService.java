package com.prudential.pamb.core.services;

import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmissionResult;
import org.apache.sling.api.resource.Resource;

/**
 * Processes a completed Legacy Blueprint: validates and recalculates the answers, posts them to
 * the client API and emails the visitor their blueprint from the authored DAM template.
 */
public interface LegacyBlueprintSubmissionService {

    /** {@code component} is the lsoCalculator resource; its authored settings drive the API and email. */
    LegacyBlueprintSubmissionResult submit(LegacyBlueprintSubmission submission, Resource component);
}
