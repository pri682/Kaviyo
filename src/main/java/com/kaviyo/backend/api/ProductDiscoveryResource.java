package com.kaviyo.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaviyo.backend.ai.BasetenDiscoverySimulationService;
import com.kaviyo.backend.ai.DiscoverySimulationRepository;
import com.kaviyo.backend.ai.DraftRepository;
import com.kaviyo.backend.ai.DraftRevisionService;
import com.kaviyo.backend.ai.MetaEvidenceDraftService;
import com.kaviyo.backend.ai.ModelProviderException;
import com.kaviyo.backend.ai.ModelRunRepository;
import com.kaviyo.backend.ai.ShopperQuestionSetService;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.backend.scan.ScanRepository;
import com.kaviyo.importer.CatalogImportMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Local demo workflows for evidence-backed copy drafts, controlled simulation, and human review. */
@Path("/api/v1/catalog")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public final class ProductDiscoveryResource {
    @Inject
    MetaEvidenceDraftService metaEvidenceDraftService;

    @Inject
    BasetenDiscoverySimulationService simulationService;

    @Inject
    ShopperQuestionSetService shopperQuestionSetService;

    @Inject
    DraftRevisionService draftRevisionService;

    @Inject
    ModelRunRepository modelRunRepository;

    @Inject
    DraftRepository draftRepository;

    @Inject
    ScanRepository scanRepository;

    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/scans/{scanRunId}/products/{productId}/drafts")
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
                            CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID,
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
    @Path("/scans/{scanRunId}/products/{productId}/shopper-question-set")
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
                            CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID,
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

    @GET
    @Path("/drafts")
    public Response listDrafts(@QueryParam("scanRunId") String scanRunId) {
        UUID scanId = null;
        if (scanRunId != null && !scanRunId.isBlank()) {
            scanId = uuidOrNull(scanRunId);
            if (scanId == null) return badRequest("SCAN_ID_INVALID", "scanRunId must be a UUID.");
        }
        try {
            return Response.ok(new DraftList("SYNTHETIC_DEMO", draftRepository.list(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, scanId),
                    "Drafts are scoped to the local demo workspace. Authentication and tenant isolation are not implemented."))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFTS_UNAVAILABLE", "Saved drafts could not be loaded.");
        }
    }

    @GET
    @Path("/drafts/{draftId}")
    public Response getDraft(@PathParam("draftId") String draftId) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        try {
            return draftRepository.get(CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id)
                    .<Response>map(draft -> Response.ok(new DraftResponse("SYNTHETIC_DEMO", draft)).build())
                    .orElseGet(() -> error(404, "DRAFT_NOT_FOUND", "No saved draft exists with that ID."));
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFTS_UNAVAILABLE", "The saved draft could not be loaded.");
        }
    }

    @GET
    @Path("/model-runs/{modelRunId}")
    public Response getModelRun(@PathParam("modelRunId") String modelRunId) {
        UUID id = uuidOrNull(modelRunId);
        if (id == null) return badRequest("MODEL_RUN_ID_INVALID", "modelRunId must be a UUID.");
        try {
            return modelRunRepository.get(CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id)
                    .<Response>map(run -> Response.ok(new ModelRunResponse("SYNTHETIC_DEMO", run,
                            "Audit trace for a controlled Meta-model task. It is not a live assistant result."))
                            .build())
                    .orElseGet(() -> error(404, "MODEL_RUN_NOT_FOUND", "No saved model run exists with that ID."));
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "MODEL_RUN_UNAVAILABLE", "The saved model trace could not be loaded.");
        }
    }

    @GET
    @Path("/drafts/{draftId}/preview")
    public Response previewDraft(@PathParam("draftId") String draftId) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        try {
            DraftRepository.DraftRecord draft = draftRepository.get(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id).orElse(null);
            if (draft == null) return error(404, "DRAFT_NOT_FOUND", "No saved draft exists with that ID.");
            CatalogImportRepository.CatalogProductSnapshot product = scanRepository.capturedProduct(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, draft.scanRunId(), draft.productId()).orElse(null);
            if (product == null) return error(409, "PRODUCT_SNAPSHOT_NOT_FOUND", "The saved product facts are unavailable.");
            ObjectNode structuredData = productJsonLd(product, draft.revision().proposedValue());
            return Response.ok(new DraftPreview("SYNTHETIC_DEMO", draft, product.description(),
                    draft.revision().proposedValue(), structuredData,
                    "Preview only. This does not fetch or modify the product page. Price and availability are omitted from the JSON-LD suggestion because they may need separate confirmation."))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFT_PREVIEW_UNAVAILABLE", "The saved preview facts could not be loaded.");
        }
    }

    @POST
    @Path("/drafts/{draftId}/decision")
    public Response decide(@PathParam("draftId") String draftId, DecisionRequest request) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        if (request == null || request.decision() == null
                || (!"APPROVE".equals(request.decision()) && !"REJECT".equals(request.decision())
                && !"REQUEST_REVISION".equals(request.decision()))) {
            return badRequest("DRAFT_DECISION_INVALID", "decision must be APPROVE, REJECT, or REQUEST_REVISION.");
        }
        if (request.note() != null && request.note().length() > 1000) {
            return badRequest("DRAFT_NOTE_TOO_LONG", "note must be 1000 characters or fewer.");
        }
        if ("REQUEST_REVISION".equals(request.decision())
                && (request.note() == null || request.note().isBlank())) {
            return badRequest("REVISION_NOTE_REQUIRED", "Explain what should change before requesting a revision.");
        }
        try {
            DraftRepository.DraftRecord current = draftRepository.get(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id).orElse(null);
            if (current == null) return error(404, "DRAFT_NOT_FOUND", "No saved draft exists with that ID.");
            if (!"NEEDS_REVIEW".equals(current.status())) {
                return error(409, "DRAFT_ALREADY_DECIDED", "Only a draft awaiting review can receive a decision.");
            }
            DraftRepository.DraftRecord updated = draftRepository.decide(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id,
                    request.decision(), request.note(), Instant.now()).orElse(null);
            if (updated == null) return error(409, "DRAFT_ALREADY_DECIDED", "The draft changed before the decision was saved.");
            return Response.ok(new DraftResponse("SYNTHETIC_DEMO", updated,
                    "This local demo records a merchant review decision. It does not update a live store."))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFT_DECISION_UNAVAILABLE", "The merchant review decision could not be saved.");
        }
    }

    @POST
    @Path("/drafts/{draftId}/revisions")
    public Response reviseDraft(@PathParam("draftId") String draftId,
                                DraftRevisionService.RevisionRequest request) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(draftRevisionService.createRevision(
                            CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id, request))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFT_REVISION_UNAVAILABLE", "The merchant revision could not be saved.");
        }
    }

    @POST
    @Path("/drafts/{draftId}/export")
    public Response export(@PathParam("draftId") String draftId) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        try {
            DraftRepository.ChangePacket packet = draftRepository.exportApproved(
                    CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id, Instant.now()).orElse(null);
            if (packet == null) {
                return error(409, "DRAFT_NOT_APPROVED", "Approve the draft before exporting a change packet.");
            }
            return Response.ok(new ExportResponse("SYNTHETIC_DEMO", packet,
                    "Export only. No publishing or live product update occurred.")).build();
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "DRAFT_EXPORT_UNAVAILABLE", "The approved change packet could not be saved.");
        }
    }

    @POST
    @Path("/drafts/{draftId}/discovery-simulation")
    public Response simulate(@PathParam("draftId") String draftId, SimulationRequest request) {
        UUID id = uuidOrNull(draftId);
        if (id == null) return badRequest("DRAFT_ID_INVALID", "draftId must be a UUID.");
        if (request == null || request.shopperQuestion() == null) {
            return badRequest("SIMULATION_REQUEST_INVALID", "shopperQuestion is required.");
        }
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(simulationService.simulate(
                            CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID, id, request.shopperQuestion()))
                    .build();
        } catch (MetaEvidenceDraftService.WorkflowRequestException exception) {
            return error(exception.status(), exception.code(), exception.getMessage());
        } catch (ModelProviderException exception) {
            return error(exception.httpStatus(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "SIMULATION_PERSISTENCE_UNAVAILABLE", "The simulation could not access saved data.");
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

    private ObjectNode productJsonLd(CatalogImportRepository.CatalogProductSnapshot product, String description) {
        ObjectNode jsonLd = objectMapper.createObjectNode();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "Product");
        jsonLd.put("name", product.title());
        jsonLd.put("url", product.productUrl());
        if (description != null) jsonLd.put("description", description);
        if (product.sku() != null) jsonLd.put("sku", product.sku());
        if (product.brand() != null) jsonLd.putObject("brand").put("@type", "Brand").put("name", product.brand());
        if (product.category() != null) jsonLd.put("category", product.category());
        if (!product.attributes().isEmpty()) {
            ArrayNode properties = jsonLd.putArray("additionalProperty");
            product.attributes().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(attribute -> {
                        ObjectNode property = properties.addObject();
                        property.put("@type", "PropertyValue");
                        property.put("name", attribute.getKey());
                        property.put("value", attribute.getValue());
                    });
        }
        return jsonLd;
    }

    public record DraftRequest(String findingCode, String shopperQuestion) { }
    public record ShopperQuestionSetRequest(String shopperGoal) { }
    public record SimulationRequest(String shopperQuestion) { }
    public record DecisionRequest(String decision, String note) { }
    public record ApiError(String code, String message) { }
    public record DraftResponse(String dataClassification, DraftRepository.DraftRecord draft, String limitation) {
        public DraftResponse(String dataClassification, DraftRepository.DraftRecord draft) {
            this(dataClassification, draft, "Drafts are review-only. No product data has been published.");
        }
    }
    public record DraftList(String dataClassification, List<DraftRepository.DraftRecord> drafts, String limitation) {
        public DraftList { drafts = List.copyOf(drafts); }
    }
    public record ModelRunResponse(String dataClassification, ModelRunRepository.ModelRunRecord modelRun,
                                   String limitation) { }
    public record DraftPreview(String dataClassification, DraftRepository.DraftRecord draft,
                               String currentDescription, String proposedDescription,
                               ObjectNode productStructuredDataPreview, String limitation) { }
    public record ExportResponse(String dataClassification, DraftRepository.ChangePacket packet, String limitation) { }
}
