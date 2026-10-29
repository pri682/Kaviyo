package com.kaviyo.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.backend.scan.ScanRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Saves merchant-edited copy as a new, cited revision without changing canonical catalog facts. */
@ApplicationScoped
public class DraftRevisionService {
    private static final Pattern NUMBER = Pattern.compile("\\b\\d+(?:[.,]\\d+)?\\b");
    private static final Pattern VOLATILE_CLAIM = Pattern.compile(
            "\\$|\\b(?:usd|price|prices|cost|in stock|out of stock|available now|guarantee|guaranteed)\\b",
            Pattern.CASE_INSENSITIVE);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DraftRepository draftRepository;

    @Inject
    ScanRepository scanRepository;

    public DraftRevisionResult createRevision(UUID workspaceId, UUID draftId,
                                               RevisionRequest request) {
        if (request == null || request.proposedDescription() == null || request.rationale() == null
                || request.claims() == null) {
            throw new MetaEvidenceDraftService.WorkflowRequestException("DRAFT_REVISION_INVALID",
                    "proposedDescription, rationale, and claims are required.", 400);
        }
        DraftRepository.DraftRecord draft = draftRepository.get(workspaceId, draftId)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException("DRAFT_NOT_FOUND",
                        "No saved draft exists with that ID.", 404));
        if (!"NEEDS_REVIEW".equals(draft.status()) && !"REVISION_REQUESTED".equals(draft.status())) {
            throw new MetaEvidenceDraftService.WorkflowRequestException("DRAFT_NOT_REVISIONABLE",
                    "Only a draft awaiting review or revision can be edited.", 409);
        }
        CatalogImportRepository.CatalogProductSnapshot product = scanRepository.capturedProduct(
                        workspaceId, draft.scanRunId(), draft.productId())
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException(
                        "PRODUCT_SNAPSHOT_NOT_FOUND", "The saved product facts are unavailable.", 409));

        ObjectNode validation = validate(request, product);
        if (!validation.path("valid").asBoolean()) {
            throw new MetaEvidenceDraftService.WorkflowRequestException("DRAFT_REVISION_REJECTED",
                    "The edit must cite exact saved product facts and cannot introduce price, stock, or unsupported numeric claims.", 422);
        }
        ArrayNode evidence = evidenceJson(request.claims());
        DraftRepository.DraftRecord saved = draftRepository.createMerchantRevision(
                        workspaceId, draftId, request.proposedDescription().trim(), request.rationale().trim(),
                        evidence, Instant.now())
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException(
                        "DRAFT_NOT_REVISIONABLE", "The draft changed before this revision was saved.", 409));
        return new DraftRevisionResult("SYNTHETIC_DEMO", saved,
                validation.put("merchantReviewRequired", true),
                "Merchant-authored revision saved. It remains a draft and must be reviewed again; nothing was published.");
    }

    private ObjectNode validate(RevisionRequest request,
                                CatalogImportRepository.CatalogProductSnapshot product) {
        ObjectNode validation = objectMapper.createObjectNode();
        List<String> errors = new ArrayList<>();
        String description = request.proposedDescription().trim();
        String rationale = request.rationale().trim();
        if (description.length() < 40 || description.length() > 800) errors.add("description_length");
        if (rationale.isBlank() || rationale.length() > 600) errors.add("rationale_length");
        if (request.claims().isEmpty() || request.claims().size() > 8) errors.add("claims_count");

        Map<String, String> facts = factMap(product);
        Set<String> citedSourceValues = new LinkedHashSet<>();
        Set<String> citedIds = new LinkedHashSet<>();
        for (EvidenceClaim claim : request.claims()) {
            if (claim == null) {
                errors.add("claim_invalid");
                continue;
            }
            String sourceValue = claim.sourceValue() == null ? "" : claim.sourceValue();
            String fragment = claim.copyFragment() == null ? "" : claim.copyFragment().trim();
            String actualValue = facts.get(claim.evidenceId());
            if (actualValue == null || !actualValue.equals(sourceValue)) errors.add("unsupported_source_value");
            if (claim.evidenceId() == null || !citedIds.add(claim.evidenceId())) errors.add("duplicate_citation");
            if (fragment.isBlank() || !containsIgnoreCase(sourceValue, fragment)
                    || !containsIgnoreCase(description, fragment)) errors.add("copy_fragment_missing");
            if (actualValue != null) citedSourceValues.add(actualValue);
        }
        if (VOLATILE_CLAIM.matcher(description).find()) errors.add("volatile_or_unverified_claim");
        Matcher numbers = NUMBER.matcher(description);
        while (numbers.find()) {
            String number = numbers.group();
            if (citedSourceValues.stream().noneMatch(value -> value.contains(number))) {
                errors.add("uncited_number");
            }
        }
        validation.put("valid", errors.isEmpty());
        validation.put("sourceIdAndValueVerified", errors.stream()
                .noneMatch(error -> error.equals("unsupported_source_value")));
        validation.put("semanticSupportVerified", false);
        ArrayNode errorList = validation.putArray("errors");
        errors.forEach(errorList::add);
        return validation;
    }

    private Map<String, String> factMap(CatalogImportRepository.CatalogProductSnapshot product) {
        Map<String, String> facts = new LinkedHashMap<>();
        addFact(facts, product, "title", product.title());
        addFact(facts, product, "brand", product.brand());
        addFact(facts, product, "category", product.category());
        addFact(facts, product, "description", product.description());
        product.attributes().forEach((key, value) -> addFact(facts, product, "attribute." + key, value));
        return facts;
    }

    private static void addFact(Map<String, String> facts, CatalogImportRepository.CatalogProductSnapshot product,
                                String field, String value) {
        if (value != null && !value.isBlank()) facts.put("catalog:" + product.id() + ":" + field, value.trim());
    }

    private ArrayNode evidenceJson(List<EvidenceClaim> claims) {
        ArrayNode result = objectMapper.createArrayNode();
        for (EvidenceClaim claim : claims) {
            ObjectNode item = result.addObject();
            item.put("evidence_id", claim.evidenceId());
            item.put("source_value", claim.sourceValue());
            item.put("copy_fragment", claim.copyFragment());
        }
        return result;
    }

    private static boolean containsIgnoreCase(String text, String fragment) {
        return text.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    public record RevisionRequest(String proposedDescription, String rationale, List<EvidenceClaim> claims) {
        public RevisionRequest {
            claims = claims == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(claims));
        }
    }

    public record EvidenceClaim(String evidenceId, String sourceValue, String copyFragment) { }

    public record DraftRevisionResult(String dataClassification, DraftRepository.DraftRecord draft,
                                      JsonNode validation, String limitation) { }
}
