package com.prudential.pamb.core.services;

public final class EmailAttachment {

    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public EmailAttachment(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}
