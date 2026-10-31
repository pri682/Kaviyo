package com.kaviyo.backend.scan;

import com.kaviyo.backend.ai.MetaEvidenceDraftService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Records a merchant's guided decision for a price or availability mismatch finding. */
@ApplicationScoped
public class FindingResolutionService {
    public static final Set<String> RESOLVABLE_FINDINGS = Set.of("PRICE_MISMATCH", "AVAILABILITY_MISMATCH");
    private static final Set<String> RESOLUTIONS = Set.of("ACCEPT_CATALOG", "ACCEPT_PAGE", "DISMISSED");
    private static final int MAX_NOTE_LENGTH = 2000;
    private static final String LIMITATION =
            "A merchant decision about which side to fix; Kaviyo does not change the catalog or the page.";

    @Inject
    ScanRepository scanRepository;

    @Inject
    FindingResolutionRepository resolutionRepository;

    public static String validationError(String findingCode, String resolution, String note) {
        if (findingCode == null || findingCode.isBlank()) return "Finding code is required.";
        if (!RESOLVABLE_FINDINGS.contains(findingCode)) {
            return "Guided resolution supports PRICE_MISMATCH and AVAILABILITY_MISMATCH only.";
        }
        if (resolution == null || resolution.isBlank()) return "Resolution is required.";
        if (!RESOLUTIONS.contains(resolution)) {
            return "Resolution must be ACCEPT_CATALOG, ACCEPT_PAGE, or DISMISSED.";
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            return "Resolution note must be 2000 characters or fewer.";
        }
        return null;
    }

    public ResolutionResult saveResolution(UUID workspaceId, UUID scanRunId, String externalProductId,
            String findingCode, String resolution, String note) {
        String invalid = validationError(findingCode, resolution, note);
        if (invalid != null) {
            throw new MetaEvidenceDraftService.WorkflowRequestException(
                    "RESOLUTION_REQUEST_INVALID", invalid, 400);
        }
        ScanRunDetail scan = scanRepository.get(workspaceId, scanRunId)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException("SCAN_NOT_FOUND",
                        "No saved scan run exists with that ID.", 404));
        boolean inScan = scan.products().stream().anyMatch(item -> item.productId().equals(externalProductId));
        if (!inScan) {
            throw new MetaEvidenceDraftService.WorkflowRequestException("PRODUCT_NOT_IN_SCAN",
                    "The requested product was not part of this saved scan.", 404);
        }
        UUID productId = scanRepository.capturedProduct(workspaceId, scanRunId, externalProductId)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException(
                        "PRODUCT_SNAPSHOT_NOT_FOUND", "The scan-time product facts are unavailable.", 409))
                .id();
        UUID findingId = scanRepository.findingId(workspaceId, scanRunId, productId, findingCode).orElse(null);
        FindingResolutionRepository.ResolutionRecord saved = resolutionRepository.save(
                workspaceId, scanRunId, productId, findingId, findingCode, resolution, note, Instant.now());
        return new ResolutionResult("MERCHANT_SUPPLIED", LIMITATION,
                new Resolution(saved.findingCode(), saved.resolution(), saved.note(), saved.decidedAt()));
    }

    public ResolutionResult findResolution(UUID workspaceId, UUID scanRunId, String externalProductId,
            String findingCode) {
        ScanRunDetail scan = scanRepository.get(workspaceId, scanRunId)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException("SCAN_NOT_FOUND",
                        "No saved scan run exists with that ID.", 404));
        boolean inScan = scan.products().stream().anyMatch(item -> item.productId().equals(externalProductId));
        if (!inScan) {
            throw new MetaEvidenceDraftService.WorkflowRequestException("PRODUCT_NOT_IN_SCAN",
                    "The requested product was not part of this saved scan.", 404);
        }
        UUID productId = scanRepository.capturedProduct(workspaceId, scanRunId, externalProductId)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException(
                        "PRODUCT_SNAPSHOT_NOT_FOUND", "The scan-time product facts are unavailable.", 409))
                .id();
        FindingResolutionRepository.ResolutionRecord saved = resolutionRepository
                .find(workspaceId, scanRunId, productId, findingCode)
                .orElseThrow(() -> new MetaEvidenceDraftService.WorkflowRequestException("RESOLUTION_NOT_FOUND",
                        "No guided decision is recorded for this finding yet.", 404));
        return new ResolutionResult("MERCHANT_SUPPLIED", LIMITATION,
                new Resolution(saved.findingCode(), saved.resolution(), saved.note(), saved.decidedAt()));
    }

    public record ResolutionResult(String dataClassification, String limitation, Resolution resolution) {
    }

    public record Resolution(String findingCode, String resolution, String note, Instant decidedAt) {
    }
}
