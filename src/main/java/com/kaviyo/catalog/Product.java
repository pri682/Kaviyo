package com.kaviyo.catalog;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** A product record supplied by the merchant's catalog. */
public record Product(
        String id,
        String title,
        String sku,
        String brand,
        String category,
        String description,
        BigDecimal price,
        String currency,
        Availability availability,
        Map<String, String> attributes,
        URI sourceUri) {

    public Product {
        id = requireText(id, "id");
        title = requireText(title, "title");
        sku = requireText(sku, "sku");
        brand = requireText(brand, "brand");
        category = requireText(category, "category");
        currency = requireText(currency, "currency").toUpperCase();
        availability = Objects.requireNonNull(availability, "availability");
        attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
        sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");

        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("price cannot be negative");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
