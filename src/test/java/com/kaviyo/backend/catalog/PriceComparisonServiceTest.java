package com.kaviyo.backend.catalog;

import com.kaviyo.catalog.Availability;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.PageScanStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceComparisonServiceTest {
    private static final PriceComparisonService.CatalogBaseline BASELINE =
            new PriceComparisonService.CatalogBaseline("pack-1", "Trail Pack",
                    new BigDecimal("129.99"), "USD", Availability.IN_STOCK);

    @Test
    void matchingPriceAndAvailability() {
        PriceComparisonService.SourceComparison comparison = describe(fetched(
                new BigDecimal("129.99"), "USD", Availability.IN_STOCK));
        assertEquals(PriceComparisonService.Match.MATCH, comparison.priceMatch());
        assertEquals(PriceComparisonService.Match.MATCH, comparison.availabilityMatch());
        assertTrue(comparison.notes().isEmpty());
    }

    @Test
    void differingPriceExplainsDirection() {
        PriceComparisonService.SourceComparison comparison = describe(fetched(
                new BigDecimal("149.99"), "usd", Availability.IN_STOCK));
        assertEquals(PriceComparisonService.Match.DIFFER, comparison.priceMatch());
        assertEquals(PriceComparisonService.Match.MATCH, comparison.availabilityMatch());
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("20 USD above")),
                "expected a directional delta note, got: " + comparison.notes());
    }

    @Test
    void databaseScaleDoesNotLeakIntoNotes() {
        PriceComparisonService.CatalogBaseline scaledBaseline =
                new PriceComparisonService.CatalogBaseline("pack-1", "Trail Pack",
                        new BigDecimal("899.0000"), "USD", Availability.IN_STOCK);
        PriceComparisonService.SourceComparison comparison = PriceComparisonService.describe(
                fetched(new BigDecimal("59.99"), "USD", Availability.IN_STOCK), scaledBaseline);
        assertEquals(PriceComparisonService.Match.DIFFER, comparison.priceMatch());
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("839.01 USD below")),
                "expected stripped decimals, got: " + comparison.notes());
    }

    @Test
    void currencyMismatchIsUnknownNeverGuessed() {
        PriceComparisonService.SourceComparison comparison = describe(fetched(
                new BigDecimal("129.99"), "EUR", Availability.IN_STOCK));
        assertEquals(PriceComparisonService.Match.UNKNOWN, comparison.priceMatch());
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("Currencies differ")));
    }

    @Test
    void missingValuesAreUnknownWithReasons() {
        PriceComparisonService.SourceComparison comparison = describe(fetched(null, null, null));
        assertEquals(PriceComparisonService.Match.UNKNOWN, comparison.priceMatch());
        assertEquals(PriceComparisonService.Match.UNKNOWN, comparison.availabilityMatch());
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("No readable price")));
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("No readable availability")));
    }

    @Test
    void unfetchedPageComparesNothing() {
        PageScanSnapshot snapshot = new PageScanSnapshot("pack-1", URI.create("https://shop.example/pack"),
                URI.create("https://shop.example/pack"), PageScanStatus.DESTINATION_BLOCKED, null, null,
                "title", null, new BigDecimal("9.99"), "USD", Availability.IN_STOCK, true, List.of(),
                null, "text", "sha", Map.of(), Instant.parse("2026-11-02T12:00:00Z"));
        PriceComparisonService.SourceComparison comparison = describe(snapshot);
        assertEquals(PriceComparisonService.Match.UNKNOWN, comparison.priceMatch());
        assertEquals(PriceComparisonService.Match.UNKNOWN, comparison.availabilityMatch());
        assertTrue(comparison.notes().stream().anyMatch(note -> note.contains("could not be fetched")));
    }

    @Test
    void catalogAvailabilityTextMapsToKnownValues() {
        assertEquals(Availability.IN_STOCK, PriceComparisonService.availabilityOf("IN_STOCK"));
        assertEquals(Availability.OUT_OF_STOCK, PriceComparisonService.availabilityOf(" out_of_stock "));
        assertEquals(Availability.UNKNOWN, PriceComparisonService.availabilityOf(null));
        assertEquals(Availability.UNKNOWN, PriceComparisonService.availabilityOf("backorder-ish"));
    }

    private static PriceComparisonService.SourceComparison describe(PageScanSnapshot snapshot) {
        return PriceComparisonService.describe(snapshot, BASELINE);
    }

    private static PageScanSnapshot fetched(BigDecimal price, String currency, Availability availability) {
        return new PageScanSnapshot("pack-1", URI.create("https://shop.example/pack"),
                URI.create("https://shop.example/pack"), PageScanStatus.FETCHED, 200, "text/html",
                "Trail Pack", null, price, currency, availability, true, List.of(), null,
                "sample", "sha", Map.of(), Instant.parse("2026-11-02T12:00:00Z"));
    }
}
