package com.kaviyo.backend.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FindingResolutionServiceTest {
    @Test
    void acceptsGuidedPriceMismatchResolution() {
        assertNull(FindingResolutionService.validationError(
                "PRICE_MISMATCH", "ACCEPT_CATALOG", "Page shows stale sale price."));
        assertNull(FindingResolutionService.validationError(
                "AVAILABILITY_MISMATCH", "ACCEPT_PAGE", null));
    }

    @Test
    void rejectsUnknownResolutionsAndOversizeNotes() {
        assertEquals("Resolution is required.",
                FindingResolutionService.validationError("PRICE_MISMATCH", "  ", null));
        assertEquals("Resolution must be ACCEPT_CATALOG, ACCEPT_PAGE, or DISMISSED.",
                FindingResolutionService.validationError("PRICE_MISMATCH", "MAYBE", null));
        assertEquals("Resolution note must be 2000 characters or fewer.",
                FindingResolutionService.validationError("PRICE_MISMATCH", "ACCEPT_CATALOG", "x".repeat(2001)));
    }

    @Test
    void rejectsFindingCodesOutsideGuidedResolution() {
        assertEquals("Guided resolution supports PRICE_MISMATCH and AVAILABILITY_MISMATCH only.",
                FindingResolutionService.validationError("DESCRIPTION_THIN", "ACCEPT_CATALOG", null));
        assertEquals("Finding code is required.",
                FindingResolutionService.validationError("  ", "ACCEPT_CATALOG", null));
    }
}
