package com.kaviyo.catalog;

import java.time.Instant;

/** Immutable statement about a product value observed in a named source. */
public record CatalogFactObservation(
        String fieldKey,
        String value,
        String sourceType,
        String sourceReference,
        Instant observedAt,
        Instant confirmedAt) {
}
