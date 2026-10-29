package com.kaviyo.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A merchant-confirmed import batch ready for transactional persistence. */
public record ConfirmedCatalogImport(
        UUID importId,
        UUID workspaceId,
        String contentSha256,
        Instant confirmedAt,
        List<CatalogProductRecord> products) {

    public ConfirmedCatalogImport {
        products = List.copyOf(products);
    }
}
