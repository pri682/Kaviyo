package com.kaviyo.importer;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

public record CatalogImportRowPreview(
        int rowNumber,
        String productId,
        String title,
        String sku,
        String brand,
        String category,
        String description,
        BigDecimal price,
        String currency,
        String availability,
        URI productUrl,
        Map<String, String> attributes,
        boolean readyToImport,
        List<String> errors,
        List<String> warnings) {

    public CatalogImportRowPreview {
        attributes = Map.copyOf(attributes);
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }
}
