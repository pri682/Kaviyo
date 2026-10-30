package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.backend.scan.ScanJobRepository;
import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Queues bounded, durable page-scan jobs; callers poll the returned scan resource for progress. */
@Path("/api/v1/catalog/scans")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogScanResource {
    private static final int MAX_PRODUCTS_PER_SCAN = 100;

    @Inject CatalogImportRepository importRepository;
    @Inject ScanJobRepository jobRepository;
    @Inject WorkspaceContext workspaceContext;

    @ConfigProperty(name = "kaviyo.crawler.enabled", defaultValue = "false")
    boolean crawlerEnabled;

    @POST
    public Response scan(ScanRequest request, @jakarta.ws.rs.HeaderParam("X-Kaviyo-Setup-Run") java.util.UUID setupId) {
        if (!crawlerEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SCANNER_DISABLED",
                            "Page scanning is disabled. Enable it explicitly for this deployment."))
                    .build();
        }
        if (request == null || request.productIds() == null || request.productIds().isEmpty()) {
            return badRequest("At least one imported product ID is required.");
        }
        if (request.productIds().size() > MAX_PRODUCTS_PER_SCAN) {
            return badRequest("A scan is limited to " + MAX_PRODUCTS_PER_SCAN + " products.");
        }
        if (request.productIds().stream().anyMatch(id -> id == null || id.isBlank())
                || new HashSet<>(request.productIds()).size() != request.productIds().size()) {
            return badRequest("Product IDs must be nonblank and unique.");
        }

        try {
            Map<String, CatalogImportRepository.CatalogProductSnapshot> byExternalId =
                    importRepository.findCurrentProducts(workspaceContext.workspaceId(), request.productIds()).stream()
                            .collect(Collectors.toMap(
                                    CatalogImportRepository.CatalogProductSnapshot::productId, product -> product));
            Set<String> unknownIds = new HashSet<>(request.productIds());
            unknownIds.removeAll(byExternalId.keySet());
            if (!unknownIds.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiError("PRODUCT_NOT_FOUND",
                                "One or more requested product IDs are not in the confirmed catalog."))
                        .build();
            }

            // Snapshot the exact confirmed catalog values at enqueue time so later imports cannot
            // silently change the inputs to a job waiting in the queue.
            List<CatalogImportRepository.CatalogProductSnapshot> selected = request.productIds().stream()
                    .map(byExternalId::get).toList();
            if (selected.stream().anyMatch(product -> product.productUrl() == null))
                return badRequest("Add a product page URL before scanning the selected products.");
            Instant now = Instant.now(Clock.systemUTC());
            ScanJobRepository.QueuedScan queued = jobRepository.enqueue(
                    workspaceContext.workspaceId(), selected, now, setupId);
            return Response.accepted(new ScanQueuedResponse(
                    "KAVIYO_FETCH", queued.runId(), "QUEUED", queued.productCount(), queued.queuedAt(),
                    "/api/v1/catalog/scans/" + queued.runId(),
                    "Scan queued. Poll the status URL for progress and saved findings."))
                    .header("Location", "/api/v1/catalog/scans/" + queued.runId())
                    .build();
        } catch (ScanJobRepository.QueueCapacityException exception) {
            return Response.status(429)
                    .entity(new ApiError("SCAN_QUEUE_LIMIT_REACHED", exception.getMessage()))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SCAN_PERSISTENCE_UNAVAILABLE",
                            "The scan could not be queued or the confirmed catalog could not be loaded."))
                    .build();
        }
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError("SCAN_REQUEST_INVALID", message)).build();
    }

    public record ScanRequest(List<String> productIds) { }
    public record ScanQueuedResponse(String observationType, java.util.UUID scanRunId, String status,
                                     int requestedProducts, Instant queuedAt, String statusUrl, String message) { }
    public record ApiError(String code, String message) { }
}
