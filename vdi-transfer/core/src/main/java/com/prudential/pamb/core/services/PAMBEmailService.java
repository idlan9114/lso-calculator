package com.prudential.pamb.core.services;

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.List;
import java.util.Map;

public interface PAMBEmailService {

    boolean sendEmail(Map<String, String> emailParameterMap, SlingHttpServletRequest request);

    boolean sendEmail(Map<String, String> emailParameterMap, List<EmailAttachment> attachments);
}
