package com.kaviyo.audit;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/** A traceable observation used to support one audit finding. */
public record Evidence(
        String source,
        String field,
        String value,
        URI sourceUri,
        Instant observedAt) {

    public Evidence {
        source = requireText(source, "source");
        field = requireText(field, "field");
        value = requireText(value, "value");
        sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
