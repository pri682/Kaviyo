package com.kaviyo.crawler;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsRulesTest {
    @Test
    void blocksDisallowedPathAndHonorsLongestAllowRule() {
        String robots = """
                User-agent: *
                Disallow: /private
                Allow: /private/product
                """;
        assertFalse(RobotsRules.allows(robots, URI.create("https://shop.example.test/private/cart"),
                "KaviyoProductAuditBot/0.1"));
        assertTrue(RobotsRules.allows(robots, URI.create("https://shop.example.test/private/product/1"),
                "KaviyoProductAuditBot/0.1"));
        assertTrue(RobotsRules.allows(robots, URI.create("https://shop.example.test/products/1"),
                "KaviyoProductAuditBot/0.1"));
    }

    @Test
    void prefersSpecificUserAgentGroupOverWildcardRules() {
        String robots = """
                User-agent: *
                Disallow: /

                User-agent: KaviyoProductAuditBot
                Disallow: /
                Allow: /products
                """;
        assertTrue(RobotsRules.allows(robots, URI.create("https://shop.example.test/products/1"),
                "KaviyoProductAuditBot/0.1"));
        assertFalse(RobotsRules.allows(robots, URI.create("https://shop.example.test/other"),
                "KaviyoProductAuditBot/0.1"));
    }
}
