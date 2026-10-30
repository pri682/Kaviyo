package com.kaviyo.backend.api;

import com.kaviyo.backend.ai.MetaEvidenceDraftService;
import com.kaviyo.backend.ai.ModelProviderException;
import com.kaviyo.backend.ai.ShopperQuestionSetService;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.scan.FindingResolutionService;
import com.kaviyo.backend.scan.ScanRepository;
import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Owns the full {@code /api/v1/catalog/scans} subtree. Scan-product actions live here
 * (not in a broader {@code /api/v1/catalog} resource) so routing stays unambiguous.
 */
@Path("/api/v1/catalog/scans")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogScanReadResource {
    @Inject
    ScanRepository scanRepository;
    @Inject
    WorkspaceContext workspaceContext;
    @Inject
    MetaEvidenceDraftService metaEvidenceDraftService;
    @Inject
    ShopperQuestionSetService shopperQuestionSetService;
    @Inject
    FindingResolutionService findingResolutionService;

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
            return scanRepository.get(workspaceContext.workspaceId(), id)
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

    @POST
    @Path("/{scanRunId}/products/{productId}/drafts")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createDraft(
            @PathParam("scanRunId") String scanRunId,
            @PathParam("productId") String productId,
            DraftRequest request) {
        UUID scanId = uuidOrNull(scanRunId);
        if (scanId == null) return badRequest("SCAN_ID_INVALID", "scanRunId must be a UUID.");
        if (productId == null || productId.isBlank() || productId.length() > 180) {
            return badRequest("PRODUCT_ID_INVALID", "productId must be a nonblank catalog identifier.");
        }
        if (request == null || request.findingCode() == null || request.shopperQuestion() == null) {
            return badRequest("DRAFT_REQUEST_INVALID", "findingCode and shopperQuestion are required.");
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(metaEvidenceDraftService.createDraft(
                            workspaceContext.workspaceId(),
                            scanId, productId, request.findingCode(), request.shopperQuestion()))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (ModelProviderException exception) {
            return error(exception.httpStatus(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFT_PERSISTENCE_UNAVAILABLE", "The draft workflow could not access saved evidence.");
        }
    }

    @POST
    @Path("/{scanRunId}/products/{productId}/shopper-question-set")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response generateShopperQuestionSet(
            @PathParam("scanRunId") String scanRunId,
            @PathParam("productId") String productId,
            ShopperQuestionSetRequest request) {
        UUID scanId = uuidOrNull(scanRunId);
        if (scanId == null) return badRequest("SCAN_ID_INVALID", "scanRunId must be a UUID.");
        if (productId == null || productId.isBlank() || productId.length() > 180) {
            return badRequest("PRODUCT_ID_INVALID", "productId must be a nonblank catalog identifier.");
        }
        if (request == null || request.shopperGoal() == null) {
            return badRequest("QUESTION_SET_REQUEST_INVALID", "shopperGoal is required.");
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(shopperQuestionSetService.generate(
                            workspaceContext.workspaceId(),
                            scanId, productId, request.shopperGoal()))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (ModelProviderException exception) {
            return error(exception.httpStatus(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "QUESTION_SET_PERSISTENCE_UNAVAILABLE",
                    "The question-set workflow could not access saved evidence.");
        }
    }

    @PUT
    @Path("/{scanRunId}/products/{productId}/findings/{findingCode}/resolution")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveResolution(
            @PathParam("scanRunId") String scanRunId,
            @PathParam("productId") String productId,
            @PathParam("findingCode") String findingCode,
            ResolutionRequest request) {
        UUID scanId = uuidOrNull(scanRunId);
        if (scanId == null) return badRequest("SCAN_ID_INVALID", "scanRunId must be a UUID.");
        if (productId == null || productId.isBlank() || productId.length() > 180) {
            return badRequest("PRODUCT_ID_INVALID", "productId must be a nonblank catalog identifier.");
        }
        if (findingCode == null || findingCode.isBlank()) {
            return badRequest("FINDING_CODE_INVALID", "findingCode must be a nonblank finding code.");
        }
        if (request == null || request.resolution() == null) {
            return badRequest("RESOLUTION_REQUEST_INVALID", "resolution is required.");
        }
        try {
            return Response.ok(findingResolutionService.saveResolution(
                            workspaceContext.workspaceId(), scanId, productId,
                            findingCode, request.resolution(), request.note()))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "RESOLUTION_PERSISTENCE_UNAVAILABLE",
                    "The guided resolution workflow could not access saved evidence.");
        }
    }

    @GET
    @Path("/{scanRunId}/products/{productId}/findings/{findingCode}/resolution")
    public Response getResolution(
            @PathParam("scanRunId") String scanRunId,
            @PathParam("productId") String productId,
            @PathParam("findingCode") String findingCode) {
        UUID scanId = uuidOrNull(scanRunId);
        if (scanId == null) return badRequest("SCAN_ID_INVALID", "scanRunId must be a UUID.");
        if (productId == null || productId.isBlank() || productId.length() > 180) {
            return badRequest("PRODUCT_ID_INVALID", "productId must be a nonblank catalog identifier.");
        }
        if (findingCode == null || findingCode.isBlank()) {
            return badRequest("FINDING_CODE_INVALID", "findingCode must be a nonblank finding code.");
        }
        try {
            return Response.ok(findingResolutionService.findResolution(
                            workspaceContext.workspaceId(), scanId, productId, findingCode))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "RESOLUTION_PERSISTENCE_UNAVAILABLE",
                    "The guided resolution workflow could not access saved evidence.");
        }
    }

    private static Response badRequest(String code, String message) {
        return error(400, code, message);
    }

    private static Response error(int status, String code, String message) {
        return Response.status(status).entity(new ApiError(code, message)).build();
    }

    private static UUID uuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    public record ScanResponse(String dataClassification, Object scan) {
    }

    public record DraftRequest(String findingCode, String shopperQuestion) { }

    public record ShopperQuestionSetRequest(String shopperGoal) { }

    public record ResolutionRequest(String resolution, String note) { }

    public record ApiError(String code, String message) {
    }
}
