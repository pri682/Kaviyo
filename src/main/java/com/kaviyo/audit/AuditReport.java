package com.kaviyo.audit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AuditReport(
        Instant generatedAt,
        int productsChecked,
        List<AuditIssue> issues) {

    public AuditReport {
        generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        if (productsChecked < 0) {
            throw new IllegalArgumentException("productsChecked cannot be negative");
        }
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public long count(Severity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).count();
    }
}
