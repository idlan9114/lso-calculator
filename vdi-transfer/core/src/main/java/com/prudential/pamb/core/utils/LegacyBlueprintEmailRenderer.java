package com.prudential.pamb.core.utils;

import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan.PersonPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission.Contact;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;

/**
 * Fills the DAM email template. Placeholders use {@code ${name}} syntax and are replaced in a single
 * pass, so a visitor value that itself looks like a placeholder is never expanded. Every visitor
 * value is HTML-escaped; unknown placeholders are left blank.
 */
public final class LegacyBlueprintEmailRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z]+)}");
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final int TOP_PRIORITIES = 3;
    private static final String CELL = "padding:10px 12px;border-top:1px solid #E5E7EA;font-size:14px;";

    private LegacyBlueprintEmailRenderer() {
    }

    public static final class RenderedEmail {
        private final String subject;
        private final String html;

        RenderedEmail(String subject, String html) {
            this.subject = subject;
            this.html = html;
        }

        public String getSubject() {
            return subject;
        }

        public String getHtml() {
            return html;
        }
    }

    /** {@code privacyLink} must be absolute; relative links don't work in an email client. */
    public static RenderedEmail render(String template, LegacyBlueprintSubmission submission, LegacyBlueprintPlan plan,
                                       LegacyBlueprintAuthoredContent authored, String privacyLink) {
        Map<String, String> text = textValues(submission, plan, authored, privacyLink);
        Map<String, String> html = new HashMap<>();
        text.forEach((key, value) -> html.put(key, escapeHtml4(value)));
        html.put("peopleRows", peopleRows(plan, authored));

        String body = replace(template, html);
        // Comments are removed first so a note that mentions <title> can't be taken as the subject.
        Matcher title = TITLE.matcher(COMMENT.matcher(template).replaceAll(""));
        String rawSubject = title.find() ? unescapeHtml4(TAG.matcher(title.group(1)).replaceAll("")).trim() : "";
        // The subject is a mail header: plain text, single line.
        String subject = replace(rawSubject, text).replaceAll("[\\r\\n]+", " ").trim();
        return new RenderedEmail(StringUtils.defaultIfBlank(subject, LegacyBlueprintConstants.DEFAULT_EMAIL_SUBJECT), body);
    }

    /** Formats whole ringgit as {@code RM 2,000,000}. */
    public static String formatAmount(long amount) {
        return "RM " + NumberFormat.getIntegerInstance(Locale.US).format(amount);
    }

    private static Map<String, String> textValues(LegacyBlueprintSubmission submission, LegacyBlueprintPlan plan,
                                                  LegacyBlueprintAuthoredContent authored, String privacyLink) {
        Contact contact = submission.getContact();
        Map<String, String> values = new HashMap<>();
        values.put("name", StringUtils.trimToEmpty(contact.getFullName()));
        values.put("email", StringUtils.trimToEmpty(contact.getEmail()));
        values.put("mobile", "+" + contact.getCountryCode() + " "
                + LegacyBlueprintValidator.normaliseMobile(contact.getCountryCode(), contact.getMobile()));
        values.put("state", StringUtils.defaultString(contact.getState()));
        values.put("legacyAmount", formatAmount(plan.getLegacyAmount()));
        values.put("putAside", formatAmount(plan.getPutAside()));
        values.put("gap", formatAmount(plan.getGap()));
        values.put("recommendedPlan", formatAmount(plan.getRecommendedPlanAmount()));
        values.put("priorities", submission.getNeeds().stream()
                .limit(TOP_PRIORITIES)
                .map(authored::needLabel)
                .collect(Collectors.joining(", ")));
        values.put("privacyLink", StringUtils.defaultString(privacyLink));
        return values;
    }

    private static String peopleRows(LegacyBlueprintPlan plan, LegacyBlueprintAuthoredContent authored) {
        StringBuilder rows = new StringBuilder();
        for (PersonPlan person : plan.getPeople()) {
            List<String> methods = new ArrayList<>();
            person.getParts().forEach(part -> methods.add(authored.partLabel(part)));
            rows.append("<tr>")
                    .append(cell(person.getName(), true))
                    .append(cell(authored.relationshipLabel(person.getRelationship()), false))
                    .append(cell(person.getShare() + "% · " + formatAmount(person.getAmount()), false))
                    .append(cell(methods.isEmpty() ? "–" : String.join(" + ", methods), false))
                    .append("</tr>");
        }
        return rows.toString();
    }

    private static String cell(String value, boolean bold) {
        return "<td style=\"" + CELL + (bold ? "font-weight:700;" : "") + "\">"
                + escapeHtml4(StringUtils.defaultString(value)) + "</td>";
    }

    private static String replace(String source, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
