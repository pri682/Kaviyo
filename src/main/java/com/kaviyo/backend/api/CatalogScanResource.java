package com.kaviyo.backend.api;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.CatalogPageAuditService;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.backend.scan.ScanRepository;
import com.kaviyo.backend.scan.ScanRunDetail;
import com.kaviyo.backend.scan.ScanRunStatus;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.PageScanStatus;
import com.kaviyo.crawler.SafePageFetcher;
import com.kaviyo.importer.CatalogImportMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Synchronous demo scan endpoint; every completed result is saved to the local workspace. */
@Path("/api/v1/catalog/scans")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogScanResource {
    private static final int MAX_PRODUCTS_PER_REQUEST = 3;

    @Inject
    CatalogImportRepository importRepository;

    @Inject
    ScanRepository scanRepository;

    @Inject
    SafePageFetcher pageFetcher;

    @ConfigProperty(name = "kaviyo.crawler.enabled", defaultValue = "false")
    boolean crawlerEnabled;

    @POST
    public Response scan(ScanRequest request) {
        if (!crawlerEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SCANNER_DISABLED",
                            "Page scanning is disabled. Enable it explicitly for the local development server."))
                    .build();
        }
        if (request == null || request.productIds() == null || request.productIds().isEmpty()) {
            return badRequest("At least one imported product ID is required.");
        }
        if (request.productIds().size() > MAX_PRODUCTS_PER_REQUEST) {
            return badRequest("A scan is limited to " + MAX_PRODUCTS_PER_REQUEST + " products.");
        }
        if (request.productIds().stream().anyMatch(id -> id == null || id.isBlank())
                || new HashSet<>(request.productIds()).size() != request.productIds().size()) {
            return badRequest("Product IDs must be nonblank and unique.");
        }

        ScanRepository.StartedScan started = null;
        try {
            List<CatalogImportRepository.CatalogProductSnapshot> catalog = importRepository.listCurrentProducts(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID);
            Map<String, CatalogImportRepository.CatalogProductSnapshot> byExternalId = catalog.stream()
                    .collect(Collectors.toMap(CatalogImportRepository.CatalogProductSnapshot::productId,
                            product -> product));
            Set<String> unknownIds = new HashSet<>(request.productIds());
            unknownIds.removeAll(byExternalId.keySet());
            if (!unknownIds.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiError("PRODUCT_NOT_FOUND",
                                "One or more requested product IDs are not in the confirmed local catalog."))
                        .build();
            }

            Instant startedAt = Instant.now(Clock.systemUTC());
            started = scanRepository.begin(CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID,
                    request.productIds().size(), startedAt);
            CatalogPageAuditService auditService = new CatalogPageAuditService(Clock.systemUTC());
            List<ScanRunDetail.ProductObservation> results = new ArrayList<>();
            boolean hadTechnicalFailure = false;
            for (String productId : request.productIds()) {
                CatalogImportRepository.CatalogProductSnapshot product = byExternalId.get(productId);
                PageScanSnapshot page;
                try {
                    page = pageFetcher.scan(product.productId(), URI.create(product.productUrl()));
                } catch (RuntimeException exception) {
                    // Keep an unexpected parser/network failure as unknown page content, not a fact mismatch.
                    page = failedPage(product);
                    hadTechnicalFailure = true;
                }
                AuditReport report = auditService.audit(product, page);
                scanRepository.recordProduct(started, product, page, report);
                results.add(new ScanRunDetail.ProductObservation(product.productId(), product.title(), page, report));
            }
            ScanRunDetail saved = scanRepository.finish(started,
                    hadTechnicalFailure ? ScanRunStatus.PARTIAL : ScanRunStatus.COMPLETED,
                    Instant.now(Clock.systemUTC()));
            return Response.status(Response.Status.CREATED)
                    .entity(new ScanResponse("SYNTHETIC_DEMO", saved))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            if (started != null) {
                try {
                    scanRepository.fail(started, Instant.now(Clock.systemUTC()));
                } catch (RuntimeException ignored) {
                    // Preserve the primary error without exposing database details.
                }
            }
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SCAN_PERSISTENCE_UNAVAILABLE",
                            "The scan could not be saved. Confirm that PostgreSQL is running and migrations completed."))
                    .build();
        }
    }

    private static PageScanSnapshot failedPage(CatalogImportRepository.CatalogProductSnapshot product) {
        URI url = URI.create(product.productUrl());
        return new PageScanSnapshot(product.productId(), url, url, PageScanStatus.FETCH_FAILED,
                null, null, null, null, null, null, null, null, List.of(), null, null, null,
                Map.of(), Instant.now(Clock.systemUTC()));
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError("SCAN_REQUEST_INVALID", message))
                .build();
    }

    public record ScanRequest(List<String> productIds) {
    }

    public record ScanResponse(String dataClassification, ScanRunDetail scan) {
    }

    public record ApiError(String code, String message) {
    }
}
