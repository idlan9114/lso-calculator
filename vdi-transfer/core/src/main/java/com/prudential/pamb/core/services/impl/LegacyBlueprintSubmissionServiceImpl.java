package com.prudential.pamb.core.services.impl;

import com.day.cq.commons.Externalizer;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.prudential.pamb.core.configurations.LegacyBlueprintConfig;
import com.prudential.pamb.core.constants.LegacyBlueprintConstants;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.exceptions.LegacyBlueprintApiException;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintApiResult;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintPlan;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmission;
import com.prudential.pamb.core.models.dto.legacyblueprint.LegacyBlueprintSubmissionResult;
import com.prudential.pamb.core.services.LegacyBlueprintApiService;
import com.prudential.pamb.core.services.LegacyBlueprintSubmissionService;
import com.prudential.pamb.core.services.PAMBEmailService;
import com.prudential.pamb.core.utils.LegacyBlueprintAuthoredContent;
import com.prudential.pamb.core.utils.LegacyBlueprintCalculator;
import com.prudential.pamb.core.utils.LegacyBlueprintEmailRenderer;
import com.prudential.pamb.core.utils.LegacyBlueprintEmailRenderer.RenderedEmail;
import com.prudential.pamb.core.utils.LegacyBlueprintPayloadMapper;
import com.prudential.pamb.core.utils.LegacyBlueprintValidator;
import com.prudential.pamb.core.utils.RsaJwtSigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default {@link LegacyBlueprintSubmissionService}. The client API call and the email are
 * independent: the email is always attempted, and both outcomes are reported to the frontend.
 * Failed API calls are logged (without personal data) for follow-up; there is no automatic retry.
 */
@Component(service = LegacyBlueprintSubmissionService.class, immediate = true)
@Slf4j
public class LegacyBlueprintSubmissionServiceImpl implements LegacyBlueprintSubmissionService {

    private static final int MAX_TEMPLATE_BYTES = 512 * 1024;

    @Reference
    private LegacyBlueprintConfig config;

    @Reference
    private LegacyBlueprintApiService apiService;

    @Reference
    private PAMBEmailService emailService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private volatile Externalizer externalizer;

    @Override
    public LegacyBlueprintSubmissionResult submit(LegacyBlueprintSubmission submission, Resource component) {
        String path = component.getPath();
        LegacyBlueprintAuthoredContent authored = LegacyBlueprintAuthoredContent.from(component);

        List<String> errors = LegacyBlueprintValidator.validate(submission, authored.getRelationshipValues());
        if (!errors.isEmpty()) {
            log.info("LegacyBlueprint | rejected submission at {}: {}", path, errors);
            return LegacyBlueprintSubmissionResult.invalid(errors);
        }

        LegacyBlueprintPlan plan = LegacyBlueprintCalculator.calculate(submission);
        LegacyBlueprintSubmissionResult result = new LegacyBlueprintSubmissionResult();
        result.setPlan(plan);

        try {
            String language = language(component);
            LegacyBlueprintApiResult api = apiService.submit(LegacyBlueprintPayloadMapper.toApiPayload(
                    submission, plan, authored.getProductCode(), language));
            result.setApiSubmitted(true);
            result.setApiStatus(api.getStatus());
            result.setApiResponse(api.getBody());
            result.setApiReference(api.getReferenceId());
        } catch (LegacyBlueprintApiException e) {
            result.setApiStatus(e.getStatus());
            result.setApiResponse(e.getResponseBody());
            // Logged for manual follow-up; the visitor still receives their email below.
            log.error("LegacyBlueprint | API submission failed at {} (status={}, leadSource={}): {}",
                    path, e.getStatus(), authored.getLeadSourceCode(), e.getMessage());
        }

        result.setEmailSent(sendEmail(submission, plan, authored, component.getResourceResolver(), path));

        if (result.isEmailSent() && result.isApiSubmitted()) {
            result.setStatus("ok");
        } else if (result.isEmailSent() || result.isApiSubmitted()) {
            result.setStatus("partial");
        } else {
            result.setStatus("failed");
        }
        log.info("LegacyBlueprint | submission at {}: status={}, apiSubmitted={}, apiReference={}, emailSent={}, people={}, mode={}",
                path, result.getStatus(), result.isApiSubmitted(), result.getApiReference(), result.isEmailSent(),
                plan.getPeople().size(), plan.getAmountMode());
        return result;
    }

    private boolean sendEmail(LegacyBlueprintSubmission submission, LegacyBlueprintPlan plan,
                              LegacyBlueprintAuthoredContent authored, ResourceResolver resolver, String path) {
        String from = config.getMailFrom();
        if (RsaJwtSigner.isUnresolved(from)) {
            log.error("LegacyBlueprint | email not sent at {}: sender (LEGACY_BP_MAIL_FROM) is not configured.", path);
            return false;
        }
        String template = loadTemplate(resolver, authored.getEmailTemplatePath(), path);
        if (template == null) {
            return false;
        }

        RenderedEmail email = LegacyBlueprintEmailRenderer.render(template, submission, plan, authored,
                privacyLink(resolver, authored.getPrivacyPagePath()));
        Map<String, String> parameters = new HashMap<>();
        parameters.put(PAMBConstants.TO_EMAIL_IDS, StringUtils.trim(submission.getContact().getEmail()));
        parameters.put(PAMBConstants.FROM_EMAIL_ID, from);
        parameters.put(PAMBConstants.SUBJECT, email.getSubject());
        parameters.put(PAMBConstants.MSG_BODY, email.getHtml());
        boolean sent = emailService.sendEmail(parameters, Collections.emptyList());
        if (!sent) {
            log.error("LegacyBlueprint | email service could not send the blueprint email at {}.", path);
        }
        return sent;
    }

    // Only HTML files under the authored template folder are read, whatever is stored in the dialog.
    private static String loadTemplate(ResourceResolver resolver, String templatePath, String path) {
        if (templatePath == null || !templatePath.startsWith(LegacyBlueprintConstants.EMAIL_TEMPLATE_ROOT)
                || templatePath.contains("..") || !templatePath.toLowerCase(Locale.ROOT).endsWith(".html")) {
            log.error("LegacyBlueprint | email not sent at {}: no valid email template selected ({}).", path, templatePath);
            return null;
        }
        Resource resource = resolver.getResource(templatePath);
        if (resource == null) {
            log.error("LegacyBlueprint | email not sent at {}: template {} not found or not readable.", path, templatePath);
            return null;
        }
        Asset asset = resource.adaptTo(Asset.class);
        Rendition original = asset != null ? asset.getOriginal() : null;
        try (InputStream stream = original != null ? original.getStream() : resource.adaptTo(InputStream.class)) {
            if (stream == null) {
                log.error("LegacyBlueprint | email not sent at {}: template {} has no content.", path, templatePath);
                return null;
            }
            return readBounded(stream);
        } catch (IOException e) {
            log.error("LegacyBlueprint | email not sent at {}: could not read template {}.", path, templatePath, e);
            return null;
        }
    }

    private static String readBounded(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            if (out.size() + read > MAX_TEMPLATE_BYTES) {
                throw new IOException("Email template exceeds " + MAX_TEMPLATE_BYTES + " bytes.");
            }
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String privacyLink(ResourceResolver resolver, String pagePath) {
        if (StringUtils.isBlank(pagePath)) {
            return StringUtils.EMPTY;
        }
        if (!pagePath.startsWith("/content/")) {
            return pagePath;
        }
        Externalizer linkExternalizer = externalizer;
        return linkExternalizer != null
                ? linkExternalizer.publishLink(resolver, pagePath + ".html")
                : resolver.map(pagePath + ".html");
    }

    // Language of the page: the nearest jcr:language, else a language-root segment such as /en/.
    private static String language(Resource component) {
        PageManager pageManager = component.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager == null ? null : pageManager.getContainingPage(component);
        if (page == null) {
            return LegacyBlueprintConstants.LANGUAGE_DEFAULT;
        }
        for (Page current = page; current != null; current = current.getParent()) {
            String locale = current.getProperties().get(JcrConstants.JCR_LANGUAGE, String.class);
            if (StringUtils.isNotBlank(locale)) {
                String code = locale.split("[_-]")[0].toLowerCase(Locale.ROOT);
                return LegacyBlueprintConstants.LANGUAGES.getOrDefault(code, LegacyBlueprintConstants.LANGUAGE_DEFAULT);
            }
        }
        for (String segment : page.getPath().split("/")) {
            if (LegacyBlueprintConstants.LANGUAGES.containsKey(segment)) {
                return LegacyBlueprintConstants.LANGUAGES.get(segment);
            }
        }
        return LegacyBlueprintConstants.LANGUAGE_DEFAULT;
    }
}
