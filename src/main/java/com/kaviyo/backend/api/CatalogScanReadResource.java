package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.scan.ScanRepository;
import com.kaviyo.importer.CatalogImportMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/catalog/scans")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogScanReadResource {
    @Inject
    ScanRepository scanRepository;

    @GET
    @Path("/{scanRunId}")
    public Response get(@PathParam("scanRunId") String scanRunId) {
        UUID id;
        try {
            id = UUID.fromString(scanRunId);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("SCAN_ID_INVALID", "scanRunId must be a UUID."))
                    .build();
        }
        try {
            return scanRepository.get(CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id)
                    .<Response>map(detail -> Response.ok(new ScanResponse("SYNTHETIC_DEMO", detail)).build())
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                            .entity(new ApiError("SCAN_NOT_FOUND", "No scan run exists with that ID."))
                            .build());
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SCAN_PERSISTENCE_UNAVAILABLE", "Saved scan results could not be loaded."))
                    .build();
        }
    }

    public record ScanResponse(String dataClassification, Object scan) {
    }

    public record ApiError(String code, String message) {
    }
}
