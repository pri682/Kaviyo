package com.kaviyo.audit;

import com.kaviyo.catalog.Availability;
import com.kaviyo.catalog.FetchStatus;
import com.kaviyo.catalog.PageSnapshot;
import com.kaviyo.catalog.Product;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Deterministic checks for catalog completeness and catalog-to-page agreement. */
public final class ProductAuditService {
    private static final int MIN_DESCRIPTION_LENGTH = 80;

    private static final Map<String, List<String>> REQUIRED_ATTRIBUTES = Map.of(
            "Coffee Gear", List.of("brewMethod", "material"),
            "Daypacks", List.of("capacity", "material", "laptopCompatibility"),
            "Kitchen Storage", List.of("material", "capacity", "dishwasherSafe"));

    private final Clock clock;

    public ProductAuditService(Clock clock) {
        this.clock = clock;
    }

    public AuditReport audit(List<Product> products, List<PageSnapshot> snapshots) {
        Map<String, PageSnapshot> pagesByProduct = new HashMap<>();
        for (PageSnapshot snapshot : snapshots) {
            if (pagesByProduct.put(snapshot.productId(), snapshot) != null) {
                throw new IllegalArgumentException(
                        "multiple page snapshots for product " + snapshot.productId());
            }
        }

        List<AuditIssue> issues = new ArrayList<>();
        for (Product product : products) {
            PageSnapshot snapshot = pagesByProduct.get(product.id());
            checkDescription(product, issues);
            checkRequiredAttributes(product, issues);
            checkPage(product, snapshot, issues);
        }

        Set<String> catalogIds = products.stream().map(Product::id).collect(Collectors.toSet());
        Set<String> orphanedSnapshotIds = new HashSet<>(pagesByProduct.keySet());
        orphanedSnapshotIds.removeAll(catalogIds);
        if (!orphanedSnapshotIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "page snapshots refer to unknown products: " + orphanedSnapshotIds);
        }

        issues.sort(Comparator
                .comparingInt((AuditIssue issue) -> issue.severity().sortOrder())
                .thenComparing(AuditIssue::productTitle)
                .thenComparing(AuditIssue::code));
        return new AuditReport(Instant.now(clock), products.size(), issues);
    }

    private void checkDescription(Product product, List<AuditIssue> issues) {
        if (product.description() == null || product.description().isBlank()) {
            issues.add(issue(
                    "DESCRIPTION_MISSING", Severity.MEDIUM, Confidence.HIGH, product,
                    "Product description is missing",
                    "The merchant catalog has no product description.",
                    "Add a clear description using confirmed product details. Do not guess missing specifications.",
                    catalogEvidence(product, "description", "Missing")));
            return;
        }

        if (product.description().trim().length() < MIN_DESCRIPTION_LENGTH) {
            issues.add(issue(
                    "DESCRIPTION_THIN", Severity.LOW, Confidence.MEDIUM, product,
                    "Product description has little detail",
                    "The catalog description is shorter than " + MIN_DESCRIPTION_LENGTH + " characters.",
                    "Consider adding verified use cases and product attributes so shoppers can distinguish this item.",
                    catalogEvidence(product, "description", product.description())));
        }
    }

    private void checkRequiredAttributes(Product product, List<AuditIssue> issues) {
        List<String> required = REQUIRED_ATTRIBUTES.get(product.category());
        if (required == null) {
            return;
        }

        List<String> missing = required.stream()
                .filter(attribute -> {
                    String value = product.attributes().get(attribute);
                    return value == null || value.isBlank();
                })
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        List<Evidence> evidence = missing.stream()
                .map(attribute -> catalogEvidence(product, "attributes." + attribute, "Missing"))
                .toList();
        issues.add(issue(
                "REQUIRED_ATTRIBUTES_MISSING", Severity.MEDIUM, Confidence.HIGH, product,
                "Important product details are missing",
                "The catalog is missing " + String.join(", ", missing) + ".",
                "Fill these fields only with specifications confirmed by the merchant or product source.",
                evidence));
    }

    private void checkPage(Product product, PageSnapshot snapshot, List<AuditIssue> issues) {
        if (snapshot == null) {
            issues.add(issue(
                    "PAGE_NOT_OBSERVED", Severity.MEDIUM, Confidence.LOW, product,
                    "No page observation is available",
                    "Kaviyo has no page snapshot for this product, so page content was not evaluated.",
                    "Run an authorized page scan before drawing conclusions about storefront content.",
                    new Evidence("Kaviyo scan state", "page snapshot", "Not available",
                            product.sourceUri(), Instant.now(clock))));
            return;
        }

        if (snapshot.fetchStatus() != FetchStatus.FETCHED) {
            issues.add(issue(
                    "PAGE_SCAN_INCOMPLETE", Severity.MEDIUM, Confidence.LOW, product,
                    "Product page could not be checked",
                    "The synthetic scan fixture returned " + snapshot.fetchStatus() + "; page content is unknown.",
                    "Check the URL and scan permissions, then retry. This result does not establish how an AI channel sees the page.",
                    new Evidence("Synthetic page snapshot", "fetch status",
                            snapshot.fetchStatus().name(), snapshot.pageUri(), snapshot.observedAt())));
            return;
        }

        checkPrice(product, snapshot, issues);
        checkAvailability(product, snapshot, issues);
        checkStructuredData(product, snapshot, issues);
    }

    private void checkPrice(Product product, PageSnapshot snapshot, List<AuditIssue> issues) {
        if (product.price() == null || snapshot.price() == null) {
            return;
        }
        boolean sameCurrency = product.currency().equalsIgnoreCase(snapshot.currency());
        boolean samePrice = product.price().compareTo(snapshot.price()) == 0;
        if (sameCurrency && samePrice) {
            return;
        }

        issues.add(issue(
                "PRICE_MISMATCH", Severity.HIGH, Confidence.HIGH, product,
                "Catalog and page prices do not match",
                "The catalog lists " + money(product.price(), product.currency())
                        + " while the page snapshot shows " + money(snapshot.price(), snapshot.currency()) + ".",
                "Confirm which value is current, then update the incorrect source. Kaviyo does not infer the correct price.",
                List.of(
                        catalogEvidence(product, "price", money(product.price(), product.currency())),
                        pageEvidence(snapshot, "price", money(snapshot.price(), snapshot.currency())))));
    }

    private void checkAvailability(Product product, PageSnapshot snapshot, List<AuditIssue> issues) {
        Availability pageAvailability = snapshot.availability();
        if (pageAvailability == null || pageAvailability == Availability.UNKNOWN
                || product.availability() == Availability.UNKNOWN
                || product.availability() == pageAvailability) {
            return;
        }

        issues.add(issue(
                "AVAILABILITY_MISMATCH", Severity.HIGH, Confidence.HIGH, product,
                "Catalog and page availability do not match",
                "The catalog says " + product.availability().displayName().toLowerCase()
                        + " while the page snapshot says " + pageAvailability.displayName().toLowerCase() + ".",
                "Confirm current inventory, then update the incorrect source before sharing availability with shoppers.",
                List.of(
                        catalogEvidence(product, "availability", product.availability().displayName()),
                        pageEvidence(snapshot, "availability", pageAvailability.displayName()))));
    }

    private void checkStructuredData(Product product, PageSnapshot snapshot, List<AuditIssue> issues) {
        if (Boolean.TRUE.equals(snapshot.hasProductStructuredData())) {
            return;
        }
        issues.add(issue(
                "PRODUCT_STRUCTURED_DATA_MISSING", Severity.MEDIUM, Confidence.HIGH, product,
                "Product structured data was not found",
                "The page snapshot did not contain Product structured data.",
                "Consider adding valid Product structured data populated from confirmed catalog values.",
                pageEvidence(snapshot, "Product structured data", "Not found")));
    }

    private AuditIssue issue(
            String code,
            Severity severity,
            Confidence confidence,
            Product product,
            String title,
            String observation,
            String recommendedAction,
            Evidence evidence) {
        return issue(code, severity, confidence, product, title, observation,
                recommendedAction, List.of(evidence));
    }

    private AuditIssue issue(
            String code,
            Severity severity,
            Confidence confidence,
            Product product,
            String title,
            String observation,
            String recommendedAction,
            List<Evidence> evidence) {
        return new AuditIssue(code, severity, confidence, product.id(), product.title(), title,
                observation, recommendedAction, evidence);
    }

    private Evidence catalogEvidence(Product product, String field, String value) {
        return new Evidence("Synthetic merchant catalog", field, value,
                product.sourceUri(), Instant.now(clock));
    }

    private Evidence pageEvidence(PageSnapshot snapshot, String field, String value) {
        return new Evidence("Synthetic page snapshot", field, value,
                snapshot.pageUri(), snapshot.observedAt());
    }

    private static String money(BigDecimal amount, String currency) {
        return currency + " " + amount.stripTrailingZeros().toPlainString();
    }
}
