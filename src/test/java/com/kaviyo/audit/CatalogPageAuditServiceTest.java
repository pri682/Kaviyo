package com.kaviyo.audit;

import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.catalog.Availability;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.PageScanStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogPageAuditServiceTest {
    private static final Instant OBSERVED_AT = Instant.parse("2026-10-28T20:00:00Z");

    @Test
    void reportsOnlyObservedPriceAvailabilityAndStructuredDataDifferences() {
        CatalogImportRepository.CatalogProductSnapshot catalog = product();
        PageScanSnapshot page = new PageScanSnapshot(
                catalog.productId(), URI.create(catalog.productUrl()), URI.create(catalog.productUrl()),
                PageScanStatus.FETCHED, 200, "text/html", "Northstar Studio 14", null,
                new BigDecimal("1399.00"), "USD", Availability.OUT_OF_STOCK, false,
                List.of("noindex"), null, "Laptop listing", "ab".repeat(32), Map.of(), OBSERVED_AT);

        AuditReport report = new CatalogPageAuditService(Clock.fixed(OBSERVED_AT, ZoneOffset.UTC))
                .audit(catalog, page);

        assertEquals(4, report.issues().size());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PRICE_MISMATCH")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("AVAILABILITY_MISMATCH")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PRODUCT_STRUCTURED_DATA_MISSING")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PAGE_NOINDEX_DIRECTIVE")));
    }

    @Test
    void failedFetchIsUnknownAndDoesNotCreatePageFactFindings() {
        CatalogImportRepository.CatalogProductSnapshot catalog = product();
        PageScanSnapshot page = new PageScanSnapshot(
                catalog.productId(), URI.create(catalog.productUrl()), URI.create(catalog.productUrl()),
                PageScanStatus.DESTINATION_BLOCKED, null, null, null, null, null, null, null, null,
                List.of(), null, null, null, Map.of(), OBSERVED_AT);

        AuditReport report = new CatalogPageAuditService(Clock.fixed(OBSERVED_AT, ZoneOffset.UTC))
                .audit(catalog, page);

        assertEquals(1, report.issues().size());
        assertEquals("PAGE_SCAN_INCOMPLETE", report.issues().get(0).code());
        assertTrue(report.issues().get(0).observation().contains("page content is unknown"));
    }

    private static CatalogImportRepository.CatalogProductSnapshot product() {
        return new CatalogImportRepository.CatalogProductSnapshot(
                UUID.randomUUID(), "laptop-northstar-14", "Northstar Studio 14", "NS-14-32",
                "Northern Light", "Computers",
                "A capable laptop for photo editing, research, and travel with verified specifications.",
                new BigDecimal("1299.00"), "USD", "IN_STOCK",
                "https://shop.example.invalid/products/northstar-studio-14", UUID.randomUUID(), OBSERVED_AT,
                Map.of("ram_gb", "32", "display", "OLED", "battery_hours", "12"));
    }
}
