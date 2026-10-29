package com.kaviyo.crawler;

import com.kaviyo.catalog.Availability;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductPageParserTest {
    @Test
    void extractsProductOfferCanonicalAndVisibleTextFromJsonLd() throws Exception {
        String html = Files.readString(Path.of("examples/page-fixtures/northstar-studio-14.html"));
        ParsedProductPage parsed = new ProductPageParser().parse(
                html,
                URI.create("https://shop.example.test/products/northstar-studio-14"),
                Map.of("x-robots-tag", "index, follow"));

        assertEquals("Northstar Studio 14 | Northern Light", parsed.title());
        assertEquals("https://shop.example.invalid/products/northstar-studio-14", parsed.canonicalUrl().toString());
        assertEquals("1299.00", parsed.price().toPlainString());
        assertEquals("USD", parsed.currency());
        assertEquals(Availability.IN_STOCK, parsed.availability());
        assertTrue(parsed.hasProductStructuredData());
        assertTrue(parsed.visibleTextSample().contains("32 GB memory"));
    }

    @Test
    void reportsUnknownValuesRatherThanGuessingWhenProductJsonIsAmbiguous() {
        String html = """
                <html><head><title>Compare</title>
                <script type="application/ld+json">[
                  {"@type":"Product","name":"A","offers":{"price":"10","priceCurrency":"USD"}},
                  {"@type":"Product","name":"B","offers":{"price":"20","priceCurrency":"USD"}}
                ]</script></head><body><p>Two choices</p></body></html>
                """;
        ParsedProductPage parsed = new ProductPageParser().parse(
                html, URI.create("https://shop.example.test/compare"), Map.of());

        assertTrue(parsed.hasProductStructuredData());
        org.junit.jupiter.api.Assertions.assertNull(parsed.price());
        assertFalse(parsed.visibleTextSample().isBlank());
    }

    @Test
    void extractsNoindexMetaDirective() {
        String html = "<html><head><meta name=robots content='index, noindex'></head><body>item</body></html>";
        ParsedProductPage parsed = new ProductPageParser().parse(
                html, URI.create("https://shop.example.test/item"), Map.of());
        assertTrue(parsed.robotsDirectives().contains("noindex"));
    }
}
