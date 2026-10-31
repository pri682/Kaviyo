package com.kaviyo.backend.catalog;

import com.kaviyo.catalog.Availability;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.SafePageFetcher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Compares merchant-supplied price and availability against merchant-pasted outside URLs.
 * Each URL is fetched once on demand; outside data is never stored and never overrides the catalog.
 */
@ApplicationScoped
public class PriceComparisonService {
    public static final int MAX_SOURCES = 3;

    public enum Match {
        MATCH,
        DIFFER,
        UNKNOWN
    }

    public record CatalogBaseline(
            String productId,
            String title,
            BigDecimal price,
            String currency,
            Availability availability) {
    }

    public record SourceComparison(
            String sourceUrl,
            String finalUrl,
            Instant fetchedAt,
            String fetchStatus,
            Integer httpStatus,
            BigDecimal price,
            String currency,
            Availability availability,
            Boolean structuredData,
            Match priceMatch,
            Match availabilityMatch,
            List<String> notes) {
        public SourceComparison {
            notes = List.copyOf(notes);
        }
    }

    public record ComparisonReport(CatalogBaseline catalog, List<SourceComparison> comparisons) {
        public ComparisonReport {
            comparisons = List.copyOf(comparisons);
        }
    }

    @Inject
    SafePageFetcher pageFetcher;
    @Inject
    CatalogImportRepository importRepository;

    public Optional<ComparisonReport> compare(UUID workspaceId, String productId, List<URI> sourceUrls) {
        CatalogImportRepository.CatalogProductSnapshot product = importRepository
                .findCurrentProducts(workspaceId, List.of(productId)).stream().findFirst().orElse(null);
        if (product == null) return Optional.empty();
        CatalogBaseline baseline = new CatalogBaseline(product.productId(), product.title(),
                product.price(), product.currency(), availabilityOf(product.availability()));
        List<SourceComparison> comparisons = new ArrayList<>();
        for (URI sourceUrl : sourceUrls) {
            comparisons.add(describe(pageFetcher.scan(productId, sourceUrl), baseline));
        }
        return Optional.of(new ComparisonReport(baseline, comparisons));
    }

    static SourceComparison describe(PageScanSnapshot snapshot, CatalogBaseline baseline) {
        List<String> notes = new ArrayList<>();
        Match price;
        Match availability;
        if (!snapshot.fetched()) {
            price = Match.UNKNOWN;
            availability = Match.UNKNOWN;
            notes.add("This page could not be fetched ("
                    + snapshot.status().name().toLowerCase(Locale.ROOT).replace('_', ' ')
                    + "); nothing here was compared.");
        } else {
            price = priceMatch(baseline.price(), baseline.currency(), snapshot.price(), snapshot.currency());
            availability = availabilityMatch(baseline.availability(), snapshot.availability());
            priceNote(notes, baseline, snapshot);
            availabilityNote(notes, baseline, snapshot);
        }
        return new SourceComparison(
                snapshot.requestedUrl() == null ? null : snapshot.requestedUrl().toString(),
                snapshot.finalUrl() == null ? null : snapshot.finalUrl().toString(),
                snapshot.observedAt(), snapshot.status().name(), snapshot.httpStatus(),
                snapshot.price(), snapshot.currency(), snapshot.availability(),
                snapshot.hasProductStructuredData(), price, availability, notes);
    }

    static Match priceMatch(BigDecimal catalogPrice, String catalogCurrency,
                            BigDecimal sourcePrice, String sourceCurrency) {
        if (catalogPrice == null || sourcePrice == null) return Match.UNKNOWN;
        if (!sameCurrency(catalogCurrency, sourceCurrency)) return Match.UNKNOWN;
        return catalogPrice.compareTo(sourcePrice) == 0 ? Match.MATCH : Match.DIFFER;
    }

    static Match availabilityMatch(Availability catalog, Availability source) {
        if (catalog == null || catalog == Availability.UNKNOWN
                || source == null || source == Availability.UNKNOWN) return Match.UNKNOWN;
        return catalog == source ? Match.MATCH : Match.DIFFER;
    }

    static Availability availabilityOf(String raw) {
        if (raw == null || raw.isBlank()) return Availability.UNKNOWN;
        try {
            return Availability.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException expected) {
            return Availability.UNKNOWN;
        }
    }

    private static boolean sameCurrency(String first, String second) {
        return first != null && second != null && first.trim().equalsIgnoreCase(second.trim());
    }

    private static void priceNote(List<String> notes, CatalogBaseline baseline, PageScanSnapshot snapshot) {
        if (baseline.price() == null) {
            notes.add("Your catalog has no price, so prices can't be compared.");
        } else if (snapshot.price() == null) {
            notes.add("No readable price on this page (needs Product structured data or price meta tags).");
        } else if (!sameCurrency(baseline.currency(), snapshot.currency())) {
            notes.add("Currencies differ (" + orUnknown(baseline.currency()) + " vs "
                    + orUnknown(snapshot.currency()) + ") — prices not compared.");
        } else if (priceMatch(baseline.price(), baseline.currency(), snapshot.price(), snapshot.currency()) == Match.DIFFER) {
            BigDecimal delta = snapshot.price().subtract(baseline.price());
            notes.add("Outside price " + money(snapshot.price(), snapshot.currency()) + " is "
                    + money(delta.abs(), snapshot.currency()) + " "
                    + (delta.signum() > 0 ? "above" : "below") + " your catalog price.");
        }
    }

    private static void availabilityNote(List<String> notes, CatalogBaseline baseline, PageScanSnapshot snapshot) {
        if (baseline.availability() == null || baseline.availability() == Availability.UNKNOWN) {
            notes.add("Your catalog has no clear availability, so availability can't be compared.");
        } else if (snapshot.availability() == null || snapshot.availability() == Availability.UNKNOWN) {
            notes.add("No readable availability on this page.");
        } else if (baseline.availability() != snapshot.availability()) {
            notes.add("They show " + snapshot.availability().displayName().toLowerCase(Locale.ROOT)
                    + "; your catalog shows " + baseline.availability().displayName().toLowerCase(Locale.ROOT) + ".");
        }
    }

    private static String money(BigDecimal amount, String currency) {
        // Database decimals carry scale (899.0000); strip it so notes read like prices.
        return amount.stripTrailingZeros().toPlainString()
                + (currency == null ? "" : " " + currency.trim().toUpperCase(Locale.ROOT));
    }

    private static String orUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toUpperCase(Locale.ROOT);
    }
}
