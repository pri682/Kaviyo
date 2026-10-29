package com.kaviyo.backend.api;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.ProductAuditService;
import com.kaviyo.catalog.DemoCatalog;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Clock;

@Path("/api/v1/demo/audit")
@Produces(MediaType.APPLICATION_JSON)
public final class SyntheticAuditResource {
    private final ProductAuditService auditService = new ProductAuditService(Clock.systemUTC());

    @GET
    public SyntheticAuditResponse getSampleAudit() {
        AuditReport report = auditService.audit(
                DemoCatalog.products(), DemoCatalog.pageSnapshots());
        return new SyntheticAuditResponse(
                "SYNTHETIC",
                "catalog-versus-saved-page-fixtures",
                "This report uses fictional products and saved sample evidence. It is not a live store scan or an assistant ranking result.",
                report);
    }

    public record SyntheticAuditResponse(
            String dataClassification,
            String scope,
            String limitation,
            AuditReport report) {
    }
}
