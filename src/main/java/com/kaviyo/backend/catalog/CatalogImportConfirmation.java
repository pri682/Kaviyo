package com.kaviyo.backend.catalog;

import java.time.Instant;
import java.util.UUID;

public record CatalogImportConfirmation(
        String dataClassification,
        UUID workspaceId,
        UUID importId,
        int productsConfirmed,
        Instant confirmedAt,
        String message) {
}
