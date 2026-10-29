package com.kaviyo.app;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.ProductAuditService;
import com.kaviyo.catalog.DemoCatalog;

import java.time.Clock;

public final class KaviyoDemo {
    private KaviyoDemo() {
    }

    public static void main(String[] args) {
        ProductAuditService auditService = new ProductAuditService(Clock.systemUTC());
        AuditReport report = auditService.audit(
                DemoCatalog.products(),
                DemoCatalog.pageSnapshots());
        System.out.print(new ConsoleReportRenderer().render(report));
    }
}
