package com.benson.dto;

public class FormDataItem {
    private String value;
    private String contentType;
    private String filename;

    public FormDataItem(String value) {
        this.value = value;
    }

    public FormDataItem(String value, String contentType, String filename) {
        this.value = value;
        this.contentType = contentType;
        this.filename = filename;
    }

    public String getValue() {
        return value;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return "FormDataItem{" +
                "value='" + value + '\'' +
                ", contentType='" + contentType + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }
}
