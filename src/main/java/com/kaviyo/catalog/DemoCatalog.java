package com.kaviyo.catalog;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Synthetic fixtures for the local demo. The .invalid URLs are never fetched. */
public final class DemoCatalog {
    private static final Instant OBSERVED_AT = Instant.parse("2026-10-28T15:00:00Z");

    private DemoCatalog() {
    }

    public static List<Product> products() {
        return List.of(
                new Product(
                        "demo-dripper",
                        "Demo Pour-Over Coffee Dripper",
                        "DEMO-COF-001",
                        "Sample Kitchen Co.",
                        "Coffee Gear",
                        "A reusable pour-over dripper for brewing one to two cups at home or on the go. "
                                + "Its stainless steel mesh filter needs no paper filter, and the wide base "
                                + "fits most standard mugs. Rinse after use and let it dry fully.",
                        new BigDecimal("34.00"),
                        "USD",
                        Availability.IN_STOCK,
                        Map.of("brewMethod", "Pour-over", "material", "Stainless steel"),
                        URI.create("https://demo.kaviyo.invalid/products/demo-dripper")),
                new Product(
                        "demo-ridgepack",
                        "Demo Ridge 18L Daypack",
                        "DEMO-BAG-204",
                        "Sample Trail Co.",
                        "Daypacks",
                        "A lightweight bag for everyday adventures.",
                        new BigDecimal("68.00"),
                        "USD",
                        Availability.IN_STOCK,
                        Map.of("capacity", "18 L", "laptopCompatibility", "Fits laptops up to 13 inches"),
                        URI.create("https://demo.kaviyo.invalid/products/demo-ridgepack")),
                new Product(
                        "demo-storage-set",
                        "Demo Stackable Ceramic Storage Set",
                        "DEMO-HOME-118",
                        "Sample Home Co.",
                        "Kitchen Storage",
                        null,
                        new BigDecimal("42.00"),
                        "USD",
                        Availability.IN_STOCK,
                        Map.of("material", "Glazed stoneware", "capacity", "Three containers, 1.2 L each"),
                        URI.create("https://demo.kaviyo.invalid/products/demo-storage-set")));
    }

    public static List<PageSnapshot> pageSnapshots() {
        return List.of(
                new PageSnapshot(
                        "demo-dripper",
                        URI.create("https://demo.kaviyo.invalid/products/demo-dripper"),
                        FetchStatus.FETCHED,
                        "Demo Pour-Over Coffee Dripper | Sample Kitchen Co.",
                        new BigDecimal("34.00"),
                        "USD",
                        Availability.IN_STOCK,
                        true,
                        OBSERVED_AT),
                new PageSnapshot(
                        "demo-ridgepack",
                        URI.create("https://demo.kaviyo.invalid/products/demo-ridgepack"),
                        FetchStatus.FETCHED,
                        "Demo Ridge 18L Daypack | Sample Trail Co.",
                        new BigDecimal("78.00"),
                        "USD",
                        Availability.OUT_OF_STOCK,
                        false,
                        OBSERVED_AT),
                new PageSnapshot(
                        "demo-storage-set",
                        URI.create("https://demo.kaviyo.invalid/products/demo-storage-set"),
                        FetchStatus.TIMEOUT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        OBSERVED_AT));
    }
}
