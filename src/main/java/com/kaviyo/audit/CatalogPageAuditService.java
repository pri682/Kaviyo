package com.kaviyo.audit;

import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.catalog.Availability;
import com.kaviyo.crawler.PageScanSnapshot;
import com.kaviyo.crawler.PageScanStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic checks for confirmed laptop facts and one observed product page. */
public final class CatalogPageAuditService {
    private static final Map<String, List<String>> REQUIRED_ATTRIBUTES = Map.of(
            "computers", List.of("ram_gb", "display", "battery_hours"));

    private final Clock clock;

    public CatalogPageAuditService(Clock clock) {
        this.clock = clock;
    }

    public AuditReport audit(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page) {
        Instant now = Instant.now(clock);
        List<AuditIssue> issues = new ArrayList<>();
        checkDescription(product, issues);
        checkRequiredAttributes(product, issues);
        if (!page.fetched()) {
            issues.add(issue(product, "PAGE_SCAN_INCOMPLETE", Severity.MEDIUM, Confidence.LOW,
                    "Product page could not be checked",
                    "Kaviyo's fetch status is " + page.status() + "; page content is unknown.",
                    "Check the selected URL and access rules, then retry. This status says nothing about external assistant access.",
                    evidence("Kaviyo URL fetch", "fetch status", page.status().name(),
                            page.requestedUrl(), page.observedAt())));
        } else {
            checkPrice(product, page, issues);
            checkAvailability(product, page, issues);
            checkStructuredData(product, page, issues);
            checkCanonical(product, page, issues);
            checkNoindex(product, page, issues);
        }
        issues.sort(Comparator
                .comparingInt((AuditIssue item) -> item.severity().sortOrder())
                .thenComparing(AuditIssue::productTitle)
                .thenComparing(AuditIssue::code));
        return new AuditReport(now, 1, issues);
    }

    private static void checkDescription(
            CatalogImportRepository.CatalogProductSnapshot product,
            List<AuditIssue> issues) {
        if (product.description() == null || product.description().isBlank()) {
            issues.add(issue(product, "DESCRIPTION_MISSING", Severity.MEDIUM, Confidence.HIGH,
                    "Product description is missing",
                    "The confirmed catalog import has no product description.",
                    "Add a description using details confirmed by the merchant or product source.",
                    evidence("Merchant catalog import", "description", "Missing",
                            uri(product.productUrl()), product.updatedAt())));
        } else if (product.description().trim().length() < 80) {
            issues.add(issue(product, "DESCRIPTION_THIN", Severity.LOW, Confidence.MEDIUM,
                    "Product description has little detail",
                    "The confirmed catalog description is shorter than 80 characters.",
                    "Consider adding verified use cases and product attributes so shoppers can distinguish this item.",
                    evidence("Merchant catalog import", "description", product.description(),
                            uri(product.productUrl()), product.updatedAt())));
        }
    }

    private static void checkRequiredAttributes(
            CatalogImportRepository.CatalogProductSnapshot product,
            List<AuditIssue> issues) {
        List<String> required = REQUIRED_ATTRIBUTES.get(
                product.category() == null ? "" : product.category().toLowerCase(java.util.Locale.ROOT));
        if (required == null) {
            return;
        }
        List<String> missing = required.stream()
                .filter(key -> product.attributes().get(key) == null || product.attributes().get(key).isBlank())
                .toList();
        if (missing.isEmpty()) {
            return;
        }
        List<Evidence> evidence = missing.stream()
                .map(key -> evidence("Merchant catalog import", "attribute." + key, "Missing",
                        uri(product.productUrl()), product.updatedAt()))
                .toList();
        issues.add(issue(product, "REQUIRED_ATTRIBUTES_MISSING", Severity.MEDIUM, Confidence.HIGH,
                "Important product details are missing",
                "The confirmed catalog is missing " + String.join(", ", missing) + ".",
                "Fill these attributes only with specifications confirmed by the merchant or product source.",
                evidence));
    }

    private static void checkPrice(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            List<AuditIssue> issues) {
        if (product.price() == null || page.price() == null
                || product.currency() == null || page.currency() == null) {
            return;
        }
        if (product.price().compareTo(page.price()) == 0
                && product.currency().equalsIgnoreCase(page.currency())) {
            return;
        }
        issues.add(issue(product, "PRICE_MISMATCH", Severity.HIGH, Confidence.HIGH,
                "Catalog and page prices do not match",
                "The catalog lists " + product.currency() + " " + product.price().toPlainString()
                        + " while Kaviyo observed " + page.currency() + " " + page.price().toPlainString() + " on the page.",
                "Confirm the current price, then update the incorrect source. Kaviyo does not choose which value is correct.",
                List.of(
                        evidence("Merchant catalog import", "price", product.currency() + " " + product.price(),
                                uri(product.productUrl()), product.updatedAt()),
                        evidence("Kaviyo-fetched product page", "price", page.currency() + " " + page.price(),
                                page.finalUrl(), page.observedAt()))));
    }

    private static void checkAvailability(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            List<AuditIssue> issues) {
        Availability catalogAvailability = availability(product.availability());
        if (catalogAvailability == null || catalogAvailability == Availability.UNKNOWN
                || page.availability() == null || page.availability() == Availability.UNKNOWN
                || catalogAvailability == page.availability()) {
            return;
        }
        issues.add(issue(product, "AVAILABILITY_MISMATCH", Severity.HIGH, Confidence.HIGH,
                "Catalog and page availability do not match",
                "The catalog says " + catalogAvailability.displayName().toLowerCase()
                        + " while Kaviyo observed " + page.availability().displayName().toLowerCase() + " on the page.",
                "Confirm current inventory, then update the incorrect source before sharing availability with shoppers.",
                List.of(
                        evidence("Merchant catalog import", "availability", catalogAvailability.displayName(),
                                uri(product.productUrl()), product.updatedAt()),
                        evidence("Kaviyo-fetched product page", "availability", page.availability().displayName(),
                                page.finalUrl(), page.observedAt()))));
    }

    private static void checkStructuredData(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            List<AuditIssue> issues) {
        if (Boolean.TRUE.equals(page.hasProductStructuredData())) {
            return;
        }
        issues.add(issue(product, "PRODUCT_STRUCTURED_DATA_MISSING", Severity.MEDIUM, Confidence.HIGH,
                "Product structured data was not found",
                "Kaviyo did not find Product JSON-LD or Product microdata in the fetched page.",
                "Consider adding valid Product structured data populated from confirmed catalog values.",
                evidence("Kaviyo-fetched product page", "Product structured data", "Not found",
                        page.finalUrl(), page.observedAt())));
    }

    private static void checkCanonical(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            List<AuditIssue> issues) {
        if (page.canonicalUrl() == null || equivalentUrl(uri(product.productUrl()), page.canonicalUrl())) {
            return;
        }
        issues.add(issue(product, "CANONICAL_URL_DIFFERS", Severity.LOW, Confidence.HIGH,
                "The page declares a different canonical URL",
                "The selected product URL and the fetched page's canonical URL differ.",
                "Confirm that the canonical URL points to the intended product page.",
                List.of(
                        evidence("Merchant catalog import", "product URL", product.productUrl(),
                                uri(product.productUrl()), product.updatedAt()),
                        evidence("Kaviyo-fetched product page", "canonical URL", page.canonicalUrl().toString(),
                                page.finalUrl(), page.observedAt()))));
    }

    private static void checkNoindex(
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            List<AuditIssue> issues) {
        if (!page.noindex()) {
            return;
        }
        String directive = page.xRobotsTag() != null && page.xRobotsTag().toLowerCase().contains("noindex")
                ? "X-Robots-Tag: " + page.xRobotsTag()
                : "meta robots: " + String.join(", ", page.robotsDirectives());
        issues.add(issue(product, "PAGE_NOINDEX_DIRECTIVE", Severity.MEDIUM, Confidence.HIGH,
                "The page includes a noindex directive",
                "Kaviyo observed " + directive + " on the fetched page.",
                "Check whether the directive is intentional for this product page.",
                evidence("Kaviyo-fetched product page", "robots directive", directive,
                        page.finalUrl(), page.observedAt())));
    }

    private static AuditIssue issue(
            CatalogImportRepository.CatalogProductSnapshot product,
            String code,
            Severity severity,
            Confidence confidence,
            String title,
            String observation,
            String recommendation,
            Evidence evidence) {
        return issue(product, code, severity, confidence, title, observation, recommendation, List.of(evidence));
    }

    private static AuditIssue issue(
            CatalogImportRepository.CatalogProductSnapshot product,
            String code,
            Severity severity,
            Confidence confidence,
            String title,
            String observation,
            String recommendation,
            List<Evidence> evidence) {
        return new AuditIssue(code, severity, confidence, product.productId(), product.title(),
                title, observation, recommendation, evidence);
    }

    private static Evidence evidence(String source, String field, Object value,
                                     java.net.URI sourceUri, Instant observedAt) {
        return new Evidence(source, field, String.valueOf(value), sourceUri, observedAt);
    }

    private static Availability availability(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Availability.valueOf(value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static java.net.URI uri(String value) {
        return java.net.URI.create(value);
    }

    private static boolean equivalentUrl(java.net.URI left, java.net.URI right) {
        return left.normalize().toString().replaceAll("/$", "")
                .equalsIgnoreCase(right.normalize().toString().replaceAll("/$", ""));
    }
}
