package com.kaviyo.app;

import com.kaviyo.audit.AuditIssue;
import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.Evidence;
import com.kaviyo.audit.Severity;

import java.util.StringJoiner;

public final class ConsoleReportRenderer {
    public String render(AuditReport report) {
        StringBuilder output = new StringBuilder();
        output.append("KAVIYO | PRODUCT DATA AUDIT\n");
        output.append("SYNTHETIC DEMO DATA — NOT A LIVE STORE SCAN OR AI SHOPPING RESULT\n\n");
        output.append("Generated: ").append(report.generatedAt()).append('\n');
        output.append("Products checked: ").append(report.productsChecked()).append('\n');
        output.append("Findings: ").append(report.issues().size())
                .append(" (high ").append(report.count(Severity.HIGH))
                .append(", medium ").append(report.count(Severity.MEDIUM))
                .append(", low ").append(report.count(Severity.LOW)).append(")\n\n");

        if (report.issues().isEmpty()) {
            output.append("No issues were found in the checked sample data.\n");
            return output.toString();
        }

        for (int i = 0; i < report.issues().size(); i++) {
            appendIssue(output, i + 1, report.issues().get(i));
        }
        output.append("These findings describe catalog and page evidence only. They do not measure or predict third-party assistant rankings.\n");
        return output.toString();
    }

    private void appendIssue(StringBuilder output, int number, AuditIssue issue) {
        output.append("[").append(issue.severity()).append("] ")
                .append(number).append(". ").append(issue.title()).append('\n');
        output.append("Product: ").append(issue.productTitle())
                .append(" (").append(issue.productId()).append(")\n");
        output.append("Confidence in finding: ").append(issue.confidence()).append('\n');
        output.append("Observation: ").append(issue.observation()).append('\n');
        output.append("Suggested action: ").append(issue.recommendedAction()).append('\n');
        output.append("Evidence:\n");
        for (Evidence evidence : issue.evidence()) {
            output.append("  - ").append(evidence.source()).append(" | ")
                    .append(evidence.field()).append(" = ").append(evidence.value())
                    .append(" | ").append(evidence.sourceUri())
                    .append(" | ").append(evidence.observedAt()).append('\n');
        }
        output.append('\n');
    }
}
