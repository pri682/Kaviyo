package com.kaviyo.catalog;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

/** Framework-independent confirmed catalog snapshot and the source facts that support it. */
public record CatalogProductRecord(
        String externalProductId,
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
        List<CatalogFactObservation> observations) {

    public CatalogProductRecord {
        attributes = Map.copyOf(attributes);
        observations = List.copyOf(observations);
    }
}
