package com.kaviyo.backend.scan;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.crawler.PageScanSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanRunDetail(
        UUID scanRunId,
        UUID workspaceId,
        String status,
        String observationType,
        String limitation,
        int requestedProducts,
        int completedProducts,
        Instant startedAt,
        Instant completedAt,
        List<ProductObservation> products,
        AuditReport report) {

    public ScanRunDetail {
        products = List.copyOf(products);
    }

    public record ProductObservation(
            String productId,
            String productTitle,
            PageScanSnapshot page,
            AuditReport report) {
    }
}
