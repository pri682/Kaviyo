package com.kaviyo.crawler;

import com.kaviyo.catalog.Availability;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

record ParsedProductPage(
        String title,
        URI canonicalUrl,
        BigDecimal price,
        String currency,
        Availability availability,
        boolean hasProductStructuredData,
        List<String> robotsDirectives,
        String visibleTextSample) {
}
