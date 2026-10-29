package com.prudential.pamb.core.servlets;

import com.google.gson.JsonObject;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.services.EmailAttachment;
import com.prudential.pamb.core.services.PAMBEmailService;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.adobe.granite.rest.Constants.CT_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = PAMBEmailPocServlet.RESOURCE_TYPE,
        selectors = PAMBEmailPocServlet.SELECTOR,
        extensions = "json",
        methods = METHOD_POST)
@ServiceDescription("PAMB Email POC Servlet")
@Designate(ocd = PAMBEmailPocServlet.Configuration.class)
public class PAMBEmailPocServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PAMBEmailPocServlet.class);

    static final String RESOURCE_TYPE = "prudential-aem-malaysia/components/content/emailPoc";
    static final String SELECTOR = "emailpoc";

    static final String ATTACHMENT_FILE_NAME = "testing-email-service-poc.pdf";
    static final String ATTACHMENT_CLASSPATH = "/poc/" + ATTACHMENT_FILE_NAME;
    static final String ATTACHMENT_CONTENT_TYPE = "application/pdf";

    static final String DEFAULT_SUBJECT = "PAMB AEM Email POC";
    static final String DEFAULT_BODY = "This is a test email from the PAMB AEM Email POC. The sample PDF is attached.";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");

    @Reference
    private transient PAMBEmailService emailService;

    private boolean enabled;
    private String fromAddress;
    private List<String> allowedRecipientDomains;

    @Activate
    @Modified
    protected void activate(Configuration config) {
        this.enabled = config.enabled();
        this.fromAddress = config.fromAddress();
        this.allowedRecipientDomains = config.allowedRecipientDomains() == null
                ? Collections.emptyList()
                : Arrays.stream(config.allowedRecipientDomains())
                        .filter(domain -> !isBlank(domain))
                        .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList());
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType(CT_JSON);
        response.setCharacterEncoding("UTF-8");

        if (!enabled) {
            response.sendError(SC_NOT_FOUND);
            return;
        }

        String email = trimToEmpty(request.getParameter(PAMBConstants.EMAIL));
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            writeResult(response, SC_BAD_REQUEST, false, "Please enter a valid email address.");
            return;
        }
        if (!isAllowedDomain(email)) {
            LOG.warn("Email POC | Rejected recipient outside allowed domains: {}", email);
            writeResult(response, SC_BAD_REQUEST, false, "This email address is not allowed for the POC.");
            return;
        }

        EmailAttachment attachment = loadAttachment();
        if (attachment == null) {
            writeResult(response, SC_INTERNAL_SERVER_ERROR, false, "Sample PDF could not be loaded.");
            return;
        }

        ValueMap properties = request.getResource().getValueMap();
        Map<String, String> emailParameters = new HashMap<>();
        emailParameters.put(PAMBConstants.TO_EMAIL_IDS, email);
        emailParameters.put(PAMBConstants.FROM_EMAIL_ID, fromAddress);
        emailParameters.put(PAMBConstants.SUBJECT, properties.get("subject", DEFAULT_SUBJECT));
        emailParameters.put(PAMBConstants.MSG_BODY, "<p>" + escapeHtml4(properties.get("body", DEFAULT_BODY)) + "</p>");

        boolean sent = emailService.sendEmail(emailParameters, Collections.singletonList(attachment));
        LOG.info("Email POC | sent={} to={} from={} component={}", sent, email, fromAddress, request.getResource().getPath());

        if (sent) {
            writeResult(response, SC_OK, true, "Email sent. Please check your inbox.");
        } else {
            writeResult(response, SC_INTERNAL_SERVER_ERROR, false, "Email could not be sent. Please try again later.");
        }
    }

    private boolean isAllowedDomain(String email) {
        return allowedRecipientDomains.isEmpty()
                || allowedRecipientDomains.contains(substringAfter(email, "@").toLowerCase(Locale.ROOT));
    }

    private EmailAttachment loadAttachment() {
        try (InputStream inputStream = PAMBEmailPocServlet.class.getResourceAsStream(ATTACHMENT_CLASSPATH)) {
            if (inputStream == null) {
                LOG.error("Email POC | Attachment not found on classpath: {}", ATTACHMENT_CLASSPATH);
                return null;
            }
            return new EmailAttachment(ATTACHMENT_FILE_NAME, ATTACHMENT_CONTENT_TYPE, IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            LOG.error("Email POC | Failed to read attachment {}", ATTACHMENT_CLASSPATH, e);
            return null;
        }
    }

    private void writeResult(SlingHttpServletResponse response, int status, boolean sent, String message) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("sent", sent);
        result.addProperty(PAMBConstants.MESSAGE, message);
        response.setStatus(status);
        response.getWriter().write(result.toString());
    }

    @ObjectClassDefinition(name = "PAMB Email POC Servlet")
    public @interface Configuration {

        @AttributeDefinition(name = "Enabled")
        boolean enabled() default false;

        @AttributeDefinition(name = "From address")
        String fromAddress() default "no-reply@pamb-poc.local";

        @AttributeDefinition(name = "Allowed recipient domains",
                description = "Leave empty to allow any domain.")
        String[] allowedRecipientDomains() default {};
    }
}
