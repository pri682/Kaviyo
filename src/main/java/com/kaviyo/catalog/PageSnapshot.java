package com.kaviyo.catalog;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/** A point-in-time page observation; it is not a live assistant ranking result. */
public record PageSnapshot(
        String productId,
        URI pageUri,
        FetchStatus fetchStatus,
        String title,
        BigDecimal price,
        String currency,
        Availability availability,
        Boolean hasProductStructuredData,
        Instant observedAt) {

    public PageSnapshot {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        productId = productId.trim();
        pageUri = Objects.requireNonNull(pageUri, "pageUri");
        fetchStatus = Objects.requireNonNull(fetchStatus, "fetchStatus");
        observedAt = Objects.requireNonNull(observedAt, "observedAt");

        if (currency != null) {
            currency = currency.trim().toUpperCase();
        }
        if (price != null && (price.signum() < 0 || currency == null || currency.isBlank())) {
            throw new IllegalArgumentException("a page price must be nonnegative and include a currency");
        }

        if (fetchStatus == FetchStatus.FETCHED && hasProductStructuredData == null) {
            throw new IllegalArgumentException(
                    "structured-data presence must be observed when a page is fetched");
        }
        if (fetchStatus != FetchStatus.FETCHED
                && (title != null || price != null || currency != null
                || availability != null || hasProductStructuredData != null)) {
            throw new IllegalArgumentException(
                    "page content must be empty when the page was not fetched");
        }
    }
}
