package com.kaviyo.backend.api;

import com.kaviyo.backend.ai.ModelProviderException;
import com.kaviyo.backend.ai.ObservationExtractionService;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.discovery.DiscoveryRepository;
import com.kaviyo.backend.workspace.WorkspaceContext;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/discovery")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public final class DiscoveryWorkspaceResource {
    private static final String LIMITATION = "This local demo stores merchant-entered observations. It does not query or control third-party shopping agents.";

    @Inject DiscoveryRepository repository;
    @Inject WorkspaceContext workspaceContext;
    @Inject ObservationExtractionService extractionService;

    @GET
    @Path("/missions")
    public Response missions() {
        try { return Response.ok(new MissionsResponse("SYNTHETIC_DEMO", LIMITATION, repository.listMissions(workspaceContext.workspaceId()))).build(); }
        catch (CatalogImportPersistenceException e) { return error(503, "MISSIONS_UNAVAILABLE", "Discovery missions could not be loaded."); }
    }

    @POST
    @Path("/missions")
    public Response createMission(MissionRequest request) {
        if (request == null || request.missionText() == null || request.missionText().isBlank() || request.missionText().length() > 1000)
            return error(400, "MISSION_INVALID", "missionText must contain 1–1000 characters.");
        try { return Response.status(201).entity(new MissionResponse("MERCHANT_AUTHORED", LIMITATION,
                repository.createMission(workspaceContext.workspaceId(), request.missionText().trim(), "MERCHANT_AUTHORED"))).build(); }
        catch (CatalogImportPersistenceException e) { return error(503, "MISSION_SAVE_UNAVAILABLE", "The mission could not be saved."); }
    }

    @GET
    @Path("/observations")
    public Response observations(@QueryParam("missionId") String missionId,
                                 @QueryParam("limit") Integer requestedLimit,
                                 @QueryParam("cursorCreatedAt") String cursorCreatedAt,
                                 @QueryParam("cursorId") String cursorId) {
        UUID id = null;
        if (missionId != null && !missionId.isBlank()) {
            try { id = UUID.fromString(missionId); } catch (IllegalArgumentException e) { return error(400, "MISSION_ID_INVALID", "missionId must be a UUID."); }
        }
        int limit = requestedLimit == null ? 50 : requestedLimit;
        if (limit < 1 || limit > 100) return error(400, "OBSERVATION_LIMIT_INVALID", "limit must be between 1 and 100.");
        if ((cursorCreatedAt == null || cursorCreatedAt.isBlank()) != (cursorId == null || cursorId.isBlank()))
            return error(400, "OBSERVATION_CURSOR_INVALID", "cursorCreatedAt and cursorId must be provided together.");
        Instant beforeCreatedAt = null;
        UUID beforeId = null;
        if (cursorCreatedAt != null && !cursorCreatedAt.isBlank()) {
            try {
                beforeCreatedAt = Instant.parse(cursorCreatedAt);
                beforeId = UUID.fromString(cursorId);
            } catch (IllegalArgumentException e) {
                return error(400, "OBSERVATION_CURSOR_INVALID", "The observation cursor is invalid.");
            }
        }
        try {
            var page = repository.listObservationPage(workspaceContext.workspaceId(), id, limit,
                    beforeCreatedAt, beforeId);
            return Response.ok(new ObservationsResponse("OBSERVATION_LEDGER", LIMITATION,
                    page.observations(), page.hasMore(),
                    page.nextCursorCreatedAt() == null ? null : page.nextCursorCreatedAt().toString(),
                    page.nextCursorId() == null ? null : page.nextCursorId().toString())).build();
        }
        catch (CatalogImportPersistenceException e) { return error(503, "OBSERVATIONS_UNAVAILABLE", "Discovery observations could not be loaded."); }
    }

    @GET
    @Path("/metrics")
    public Response metrics(@QueryParam("missionId") String missionId, @QueryParam("target") String target) {
        UUID id = null;
        if (missionId != null && !missionId.isBlank()) {
            try { id = UUID.fromString(missionId); } catch (IllegalArgumentException e) { return error(400, "MISSION_ID_INVALID", "missionId must be a UUID."); }
        }
        try {
            List<DiscoveryRepository.Observation> observations = repository.listObservations(workspaceContext.workspaceId(), id);
            int observed = 0, simulated = 0, mentions = 0, supported = 0, contradicted = 0, unknown = 0, stale = 0;
            int observedWithMentions = 0;
            for (var observation : observations) {
                if ("SIMULATED".equals(observation.status())) simulated++;
                if (!"OBSERVED".equals(observation.status())) continue;
                observed++;
                int matches = targetMentions(observation, target);
                mentions += matches;
                if (matches > 0) observedWithMentions++;
                for (var claim : observation.claims()) switch (claim.verdict()) {
                    case "SUPPORTED" -> supported++;
                    case "CONTRADICTED" -> contradicted++;
                    case "UNKNOWN" -> unknown++;
                    case "STALE" -> stale++;
                    default -> { }
                }
            }
            int assessed = supported + contradicted + stale;
            return Response.ok(new MetricsResponse("OBSERVATION_LEDGER", LIMITATION, observations.size(), observed,
                    simulated, mentions, supported, contradicted, unknown, stale,
                    observedWithMentions, blank(target) ? null : rate(observedWithMentions, observed),
                    assessed, rate(supported, assessed),
                    "Counts and rates cover the most recent 200 recorded observations and their manually assessed claims. "
                    + "Mention rate = observed answers with a matching target product reference / observed answers. "
                    + "Accuracy rate = supported claims / assessed claims (supported, contradicted, stale); unknown claims are excluded. "
                    + "Rates describe only this recorded sample, not all assistant traffic, and no causal lift is claimed.")).build();
        } catch (CatalogImportPersistenceException e) { return error(503, "METRICS_UNAVAILABLE", "Discovery metrics could not be loaded."); }
    }

    @GET
    @Path("/trends")
    public Response trends(@QueryParam("missionId") String missionId,
                           @QueryParam("points") Integer requestedPoints, @QueryParam("target") String target) {
        UUID id = null;
        if (missionId != null && !missionId.isBlank()) {
            try { id = UUID.fromString(missionId); } catch (IllegalArgumentException e) { return error(400, "MISSION_ID_INVALID", "missionId must be a UUID."); }
        }
        int points = requestedPoints == null ? 12 : requestedPoints;
        if (points < 1 || points > 26) return error(400, "TREND_POINTS_INVALID", "points must be between 1 and 26.");
        try {
            List<DiscoveryRepository.Observation> observations = repository.listObservations(workspaceContext.workspaceId(), id);
            java.util.Map<java.time.LocalDate, int[]> buckets = new java.util.TreeMap<>();
            for (var observation : observations) {
                if (!"OBSERVED".equals(observation.status())) continue;
                Instant when = observation.observedAt() != null ? observation.observedAt() : observation.createdAt();
                java.time.LocalDate week = when.atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                int[] counts = buckets.computeIfAbsent(week, key -> new int[5]);
                counts[0]++;
                if (targetMentions(observation, target) > 0) counts[1]++;
                for (var claim : observation.claims()) switch (claim.verdict()) {
                    case "SUPPORTED" -> counts[2]++;
                    case "CONTRADICTED" -> counts[3]++;
                    case "STALE" -> counts[4]++;
                    default -> { }
                }
            }
            List<java.time.LocalDate> weeks = new ArrayList<>(buckets.keySet());
            if (weeks.size() > points) weeks = weeks.subList(weeks.size() - points, weeks.size());
            List<TrendBucket> trend = new ArrayList<>();
            for (java.time.LocalDate week : weeks) {
                int[] counts = buckets.get(week);
                int assessed = counts[2] + counts[3] + counts[4];
                trend.add(new TrendBucket(week.toString(), counts[0], counts[1],
                        blank(target) ? null : rate(counts[1], counts[0]), assessed, rate(counts[2], assessed)));
            }
            return Response.ok(new TrendsResponse("OBSERVATION_LEDGER", LIMITATION, trend,
                    "Weekly buckets (Monday 00:00 UTC) over the most recent 200 recorded observations. "
                    + "Simulations and unknown-verdict claims are excluded. Rates describe only this recorded sample.")).build();
        } catch (CatalogImportPersistenceException e) { return error(503, "TRENDS_UNAVAILABLE", "Discovery trends could not be loaded."); }
    }

    @GET
    @Path("/contradictions")
    public Response contradictions(@QueryParam("limit") Integer requestedLimit) {
        int limit = requestedLimit == null ? 20 : requestedLimit;
        if (limit < 1 || limit > 50) return error(400, "CONTRADICTION_LIMIT_INVALID", "limit must be between 1 and 50.");
        try {
            List<DiscoveryRepository.Observation> observations =
                    repository.listObservations(workspaceContext.workspaceId(), null);
            List<Contradiction> alerts = new ArrayList<>();
            for (DiscoveryRepository.Observation observation : observations) {
                if (!"OBSERVED".equals(observation.status())) continue;
                for (DiscoveryRepository.Claim claim : observation.claims()) {
                    if (!"CONTRADICTED".equals(claim.verdict())) continue;
                    alerts.add(new Contradiction(claim.claimText(), observation.id(), observation.missionId(),
                            observation.provider(), observation.observedAt(), claim.productReference() == null ? List.of() : List.of(claim.productReference()),
                            claim.evidenceReference(), claim.evidenceValue()));
                    if (alerts.size() >= limit) break;
                }
                if (alerts.size() >= limit) break;
            }
            return Response.ok(new ContradictionsResponse("OBSERVATION_LEDGER", LIMITATION, alerts,
                    "Newest contradicted claims first, drawn from the most recent 200 recorded observations. "
                    + "Simulations are excluded. Each alert names the recorded answer that contradicts saved facts.")).build();
        } catch (CatalogImportPersistenceException e) { return error(503, "CONTRADICTIONS_UNAVAILABLE", "Contradiction alerts could not be loaded."); }
    }

    @POST
    @Path("/observations/extract")
    public Response extractObservation(ExtractionRequest request) {
        if (request == null || blank(request.answer()) || request.answer().length() > 20000
                || (request.prompt() != null && request.prompt().length() > 2000)) {
            return error(400, "EXTRACTION_REQUEST_INVALID",
                    "answer is required (max 20000 characters); prompt must fit 2000 characters.");
        }
        try {
            ObservationExtractionService.ExtractionResult result = extractionService.extract(
                    workspaceContext.workspaceId(), request.prompt(), request.answer());
            return Response.ok(new ExtractionResponse("MERCHANT_SUPPLIED", LIMITATION, result,
                    ObservationExtractionService.PROMPT_VERSION,
                    "Suggestions only — nothing was saved. Confirm each mention and verdict before recording the observation.")).build();
        } catch (ModelProviderException exception) {
            return error(exception.httpStatus(), exception.code(), exception.getMessage());
        } catch (CatalogImportPersistenceException exception) {
            return error(503, "EXTRACTION_UNAVAILABLE", "Catalog facts for grounding could not be loaded.");
        }
    }

    @POST
    @Path("/missions/{missionId}/observations")
    public Response createObservation(@PathParam("missionId") String missionId, ObservationRequest request) {
        UUID id;
        try { id = UUID.fromString(missionId); } catch (IllegalArgumentException e) { return error(400, "MISSION_ID_INVALID", "missionId must be a UUID."); }
        if (request == null || blank(request.provider()) || request.provider().length() > 80
                || blank(request.prompt()) || request.prompt().length() > 2000
                || blank(request.answer()) || request.answer().length() > 20000
                || (request.sourceUrl() != null && request.sourceUrl().length() > 2000))
            return error(400, "OBSERVATION_INVALID", "provider, prompt, and answer are required and must fit the documented limits.");
        Instant observedAt;
        try { observedAt = Instant.parse(request.observedAt()); }
        catch (DateTimeParseException | NullPointerException e) { return error(400, "OBSERVED_AT_INVALID", "observedAt must be an ISO-8601 timestamp."); }
        List<DiscoveryRepository.MentionInput> mentions = request.mentions() == null ? List.of() : request.mentions();
        List<DiscoveryRepository.ClaimInput> claims = request.claims() == null ? List.of() : request.claims();
        if (mentions.size() > 50 || claims.size() > 100) return error(400, "OBSERVATION_EVIDENCE_LIMIT", "An observation can contain up to 50 mentions and 100 claims.");
        for (DiscoveryRepository.MentionInput mention : mentions) {
            if (mention == null || blank(mention.productReference()) || mention.productReference().length() > 240
                    || (mention.position() != null && mention.position() < 1)
                    || (mention.brandName() != null && mention.brandName().length() > 240)
                    || (mention.productName() != null && mention.productName().length() > 500)
                    || (mention.context() != null && mention.context().length() > 2000))
                return error(400, "MENTION_INVALID", "Each mention requires a productReference (max 240 chars) and a positive optional position; brandName, productName, and context must fit 240, 500, and 2000 characters.");
        }
        for (DiscoveryRepository.ClaimInput claim : claims) {
            if (claim == null || blank(claim.claimText()) || claim.claimText().length() > 1200
                    || blank(claim.verdict())
                    || !List.of("SUPPORTED", "CONTRADICTED", "UNKNOWN", "STALE").contains(claim.verdict())
                    || (claim.evidenceReference() != null && claim.evidenceReference().length() > 1000)
                    || (claim.evidenceValue() != null && claim.evidenceValue().length() > 1200))
                return error(400, "CLAIM_INVALID", "Each claim needs text (max 1200 chars) and a verdict: SUPPORTED, CONTRADICTED, UNKNOWN, or STALE. Evidence reference and value must fit 1000 and 1200 characters.");
            if (!blank(claim.productReference()) && mentions.stream().noneMatch(m -> m.productReference().equals(claim.productReference())))
                return error(400, "CLAIM_PRODUCT_INVALID", "Link the claim to a product mentioned in this answer.");
            if ("CONTRADICTED".equals(claim.verdict()) && (blank(claim.productReference()) || blank(claim.evidenceReference())
                    || blank(claim.evidenceValue()) || !request.answer().contains(claim.claimText())))
                return error(400, "CONTRADICTION_EVIDENCE_REQUIRED", "Provide the exact answer excerpt, product reference, merchant fact and evidence source.");
        }
        try {
            boolean missionExists = repository.listMissions(workspaceContext.workspaceId()).stream().anyMatch(m -> m.id().equals(id));
            if (!missionExists) return error(404, "MISSION_NOT_FOUND", "No discovery mission exists with that ID.");
            var observation = repository.createManualObservation(workspaceContext.workspaceId(), id, request.provider().trim(),
                    request.prompt().trim(), request.answer().trim(), request.sourceUrl(), observedAt, mentions, claims);
            return Response.status(201).entity(new ObservationResponse("MANUAL_OBSERVATION", LIMITATION, observation)).build();
        } catch (CatalogImportPersistenceException e) { return error(503, "OBSERVATION_SAVE_UNAVAILABLE", "The discovery observation could not be saved."); }
    }

    static int targetMentions(DiscoveryRepository.Observation observation, String target) {
        if (blank(target)) return 0;
        return (int) observation.mentions().stream()
                .filter(mention -> target.trim().equalsIgnoreCase(mention.productReference().trim())).count();
    }

    private static Double rate(int numerator, int denominator) {
        return denominator == 0 ? null : (double) numerator / denominator;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static Response error(int status, String code, String message) { return Response.status(status).entity(new ApiError(code, message)).build(); }

    public record MissionRequest(String missionText) { }
    public record ObservationRequest(String provider, String prompt, String answer, String sourceUrl,
            String observedAt, List<DiscoveryRepository.MentionInput> mentions,
            List<DiscoveryRepository.ClaimInput> claims) { }
    public record MissionsResponse(String dataClassification, String limitation, List<DiscoveryRepository.Mission> missions) { }
    public record MissionResponse(String dataClassification, String limitation, DiscoveryRepository.Mission mission) { }
    public record ObservationsResponse(String dataClassification, String limitation,
            List<DiscoveryRepository.Observation> observations, boolean hasMore,
            String nextCursorCreatedAt, String nextCursorId) { }
    public record ObservationResponse(String dataClassification, String limitation, DiscoveryRepository.Observation observation) { }
    public record MetricsResponse(String dataClassification, String limitation, int recordedObservations,
            int observedAnswers, int simulations, int productMentions, int supportedClaims,
            int contradictedClaims, int unknownClaims, int staleClaims,
            int observedWithMentions, Double mentionRate, int assessedClaims, Double accuracyRate,
            String interpretation) { }
    public record TrendBucket(String weekStart, int observedAnswers, int observedWithMentions,
            Double mentionRate, int assessedClaims, Double accuracyRate) { }
    public record TrendsResponse(String dataClassification, String limitation,
            List<TrendBucket> trend, String interpretation) {
        public TrendsResponse { trend = List.copyOf(trend); }
    }
    public record Contradiction(String claimText, java.util.UUID observationId, java.util.UUID missionId,
            String provider, Instant observedAt, List<String> products,
            String evidenceReference, String evidenceValue) {
        public Contradiction { products = List.copyOf(products); }
    }
    public record ContradictionsResponse(String dataClassification, String limitation,
            List<Contradiction> contradictions, String interpretation) {
        public ContradictionsResponse { contradictions = List.copyOf(contradictions); }
    }
    public record ExtractionRequest(String prompt, String answer) {
    }
    public record ExtractionResponse(String dataClassification, String limitation,
            ObservationExtractionService.ExtractionResult suggestion, String promptVersion,
            String interpretation) {
    }
    public record ApiError(String code, String message) { }
}
