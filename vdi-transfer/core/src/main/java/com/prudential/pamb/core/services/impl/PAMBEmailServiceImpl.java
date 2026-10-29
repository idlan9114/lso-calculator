package com.prudential.pamb.core.services.impl;

import com.day.cq.mailer.MailingException;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.prudential.pamb.core.constants.PAMBConstants;
import com.prudential.pamb.core.services.EmailAttachment;
import com.prudential.pamb.core.services.PAMBEmailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.*;

@Slf4j
@Component(service = PAMBEmailService.class, immediate = true)
public class PAMBEmailServiceImpl implements PAMBEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(PAMBEmailServiceImpl.class);

    @Reference
    MessageGatewayService messageGatewayService;

    private static Map<String, Object> getAttachmentMap(RequestParameter file) throws IOException {
        Map<String, Object> attachments = new HashMap<>();
        if (isNotBlank(file.getFileName()) && isNotBlank(file.getContentType()) && file.getInputStream() != null) {
            attachments.put(PAMBConstants.FILE_NAME, file.getFileName().replace("attachment/", EMPTY));
            attachments.put(PAMBConstants.FILE_CONTENT, IOUtils.toByteArray(file.getInputStream()));
            attachments.put(PAMBConstants.FILE_TYPE, file.getContentType());
        }
        return attachments;
    }

    private Optional<String[]> separateString(String emailString) {
        if (isBlank(emailString)) {
            return Optional.empty();
        }
        String[] emails = emailString.trim().split("\\s*,\\s*");
        return emails.length > 0 ? Optional.of(emails) : Optional.empty();
    }

    private void addRecipients(HtmlEmail email, Map<String, String> emailParameterMap) {
        separateString(emailParameterMap.getOrDefault(PAMBConstants.TO_EMAIL_IDS, EMPTY))
                .ifPresent(str -> {
                    try {
                        email.addTo(str);
                    } catch (EmailException e) {
                        LOG.error("Error while setting up To: ", e);
                    }
                });

        separateString(emailParameterMap.getOrDefault(PAMBConstants.CC_EMAIL_IDS, EMPTY))
                .ifPresent(str -> {
                    try {
                        email.addCc(str);
                    } catch (EmailException e) {
                        LOG.error("Error while setting up CC: ", e);
                    }
                });

        separateString(emailParameterMap.getOrDefault(PAMBConstants.BCC_EMAIL_IDS, EMPTY))
                .ifPresent(str -> {
                    try {
                        email.addBcc(str);
                    } catch (EmailException e) {
                        LOG.error("Error while setting up BCC: ", e);
                    }
                });
    }

    /**
     *
     * @param emailParameterMap emailParameterMap a map containing:
     *                          subject – the subject of the email
     *                          messageBody – the HTML content of the email
     *                          toEmails – a comma-separated list of recipient email addresses
     *                          ccEmails – a comma-separated list of CC email addresses
     *                          bccEmails – a comma-separated list of BCC email addresses
     *                          fromEmail – the sender's email address
     * @param request           The SlingHttpServletRequest for attachments
     * @return true, if mail is sent successfully; false otherwise
     */
    @Override
    public boolean sendEmail(Map<String, String> emailParameterMap, SlingHttpServletRequest request) {
        return send(emailParameterMap, request, null);
    }

    @Override
    public boolean sendEmail(Map<String, String> emailParameterMap, List<EmailAttachment> attachments) {
        return send(emailParameterMap, null, attachments);
    }

    private boolean send(Map<String, String> emailParameterMap, SlingHttpServletRequest request, List<EmailAttachment> attachments) {
        try {
            LOG.info("into sendEmail");
            // Create a SimpleEmail instance
            HtmlEmail email = new HtmlEmail();
            email.setCharset(String.valueOf(UTF_8));

            email.setHtmlMsg(emailParameterMap.getOrDefault(PAMBConstants.MSG_BODY, EMPTY));
            email.setSubject(emailParameterMap.getOrDefault(PAMBConstants.SUBJECT, EMPTY));
            addRecipients(email, emailParameterMap);
            email.setFrom(emailParameterMap.getOrDefault(PAMBConstants.FROM_EMAIL_ID, EMPTY)); // This must match the authenticated sender
            if (null != request) {
                attachFile(fetchAttachments(request), email);
            }
            if (null != attachments) {
                attachFile(toAttachmentMaps(attachments), email);
            }

            // Get the configured gateway
            MessageGateway<Email> messageGateway = messageGatewayService.getGateway(SimpleEmail.class);
            if (messageGateway != null) {
                messageGateway.send(email);
                LOG.info("EmailServiceImpl Log | Mail triggered successfully for: {}", emailParameterMap.getOrDefault(PAMBConstants.TO_EMAIL_IDS, EMPTY));
                return true;
            } else {
                LOG.info("EmailServiceImpl Log | No message gateway found");
            }
        } catch (EmailException | MailingException e) {
            LOG.error("EmailServiceImpl Log | Error : ", e);
        }
        return false;
    }

    private void attachFile(List<Map<String, Object>> files, HtmlEmail email) throws EmailException {
        for (Map<String, Object> file : files) {
            try {
                DataSource ds = new ByteArrayDataSource((byte[]) file.get(PAMBConstants.FILE_CONTENT), (String) file.get(PAMBConstants.FILE_TYPE));
                email.attach(ds, (String) file.get(PAMBConstants.FILE_NAME), EMPTY);
            } catch (EmailException e) {
                LOG.error("EmailServiceImpl Log | Error while adding attachment to email: ", e);
            }
        }
    }

    private List<Map<String, Object>> toAttachmentMaps(List<EmailAttachment> attachments) {
        List<Map<String, Object>> attachmentMapList = new ArrayList<>();
        for (EmailAttachment attachment : attachments) {
            if (attachment != null && isNotBlank(attachment.getFileName()) && isNotBlank(attachment.getContentType())
                    && attachment.getContent() != null) {
                Map<String, Object> attachmentMap = new HashMap<>();
                attachmentMap.put(PAMBConstants.FILE_NAME, attachment.getFileName());
                attachmentMap.put(PAMBConstants.FILE_CONTENT, attachment.getContent());
                attachmentMap.put(PAMBConstants.FILE_TYPE, attachment.getContentType());
                attachmentMapList.add(attachmentMap);
            } else {
                LOG.warn("EmailServiceImpl Log | Skipping attachment with missing name, type or content");
            }
        }
        return attachmentMapList;
    }

    private List<Map<String, Object>> fetchAttachments(SlingHttpServletRequest request) {
        List<Map<String, Object>> attachmentMapList = new ArrayList<>();
        RequestParameter[] requestParameters = request.getRequestParameters("attachments");
        if (requestParameters != null) {
            for (RequestParameter file : requestParameters) {
                try {
                    Map<String, Object> attachments = getAttachmentMap(file);
                    attachmentMapList.add(attachments);
                } catch (IOException e) {
                    return attachmentMapList;
                }
            }
        }
        return attachmentMapList;
    }
}
