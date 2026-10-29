package com.kaviyo.audit;

import java.util.List;
import java.util.Objects;

public record AuditIssue(
        String code,
        Severity severity,
        Confidence confidence,
        String productId,
        String productTitle,
        String title,
        String observation,
        String recommendedAction,
        List<Evidence> evidence) {

    public AuditIssue {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        confidence = Objects.requireNonNull(confidence, "confidence");
        productId = requireText(productId, "productId");
        productTitle = requireText(productTitle, "productTitle");
        title = requireText(title, "title");
        observation = requireText(observation, "observation");
        recommendedAction = requireText(recommendedAction, "recommendedAction");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("an issue must include evidence");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
