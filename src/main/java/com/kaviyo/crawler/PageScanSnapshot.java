package com.kaviyo.crawler;

import com.kaviyo.catalog.Availability;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** In-memory evidence from a Kaviyo fetch; persistence is added with the audit-run slice. */
public record PageScanSnapshot(
        String productId,
        URI requestedUrl,
        URI finalUrl,
        PageScanStatus status,
        Integer httpStatus,
        String contentType,
        String title,
        URI canonicalUrl,
        BigDecimal price,
        String currency,
        Availability availability,
        Boolean hasProductStructuredData,
        List<String> robotsDirectives,
        String xRobotsTag,
        String visibleTextSample,
        String contentSha256,
        Map<String, String> relevantHeaders,
        Instant observedAt) {

    public PageScanSnapshot {
        robotsDirectives = robotsDirectives == null ? List.of() : List.copyOf(robotsDirectives);
        relevantHeaders = relevantHeaders == null ? Map.of() : Map.copyOf(relevantHeaders);
        if (status != PageScanStatus.FETCHED) {
            title = null;
            canonicalUrl = null;
            price = null;
            currency = null;
            availability = null;
            hasProductStructuredData = null;
            visibleTextSample = null;
            contentSha256 = null;
        }
    }

    public boolean fetched() {
        return status == PageScanStatus.FETCHED;
    }

    public boolean noindex() {
        return robotsDirectives.stream().anyMatch(value -> value.equalsIgnoreCase("noindex"))
                || (xRobotsTag != null && xRobotsTag.toLowerCase(java.util.Locale.ROOT).contains("noindex"));
    }
}
