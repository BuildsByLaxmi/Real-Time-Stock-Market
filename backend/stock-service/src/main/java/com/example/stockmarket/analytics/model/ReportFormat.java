package com.example.stockmarket.analytics.model;

import java.util.Locale;

/** Downloadable report formats. */
public enum ReportFormat {
    TEXT("txt", "text/plain; charset=UTF-8"),
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String contentType;

    ReportFormat(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** Accepts "text"/"txt", "pdf", "excel"/"xlsx" (case-insensitive). */
    public static ReportFormat parse(String value) {
        String v = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "text", "txt", "plain" -> TEXT;
            case "pdf" -> PDF;
            case "excel", "xlsx" -> EXCEL;
            default -> throw new IllegalArgumentException("Unknown report format '" + value + "'. Use text, pdf or excel.");
        };
    }
}
