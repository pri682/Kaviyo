package com.kaviyo.backend.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreConnectionServiceTest {
    private HttpServer stub;

    @AfterEach
    void stopStub() {
        if (stub != null) stub.stop(0);
    }

    @Test
    void validatesConnectionRequests() {
        assertNull(StoreConnectionService.validate("WOOCOMMERCE", "https://shop.example", "ck", "cs", null, null));
        assertNull(StoreConnectionService.validate("WIX", null, null, null, "key", null));
        assertTrue(StoreConnectionService.validate("MAGENTO", null, null, null, null, null).contains("WooCommerce, Wix, or Shopify"));
        assertTrue(StoreConnectionService.validate("WOOCOMMERCE", "http://shop.example", "ck", "cs", null, null)
                .contains("https"));
        assertTrue(StoreConnectionService.validate("WOOCOMMERCE", "https://shop.example", "", "cs", null, null)
                .contains("consumer key"));
        assertTrue(StoreConnectionService.validate("WIX", null, null, null, "", null).contains("API key"));
        assertNull(StoreConnectionService.validate("SHOPIFY", "mystore.myshopify.com", null, null, "shpat_x", null));
        assertTrue(StoreConnectionService.validate("SHOPIFY", "not a domain", null, null, "shpat_x", null)
                .contains("store domain"));
    }

    @Test
    void fetchesShopifyProductsOverHttp() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/admin/api/2024-01/products.json", exchange -> {
            assertEquals("shpat_x", exchange.getRequestHeaders().getFirst("X-Shopify-Access-Token"));
            byte[] body = """
                    {"products":[{"title":"Trail Pack","handle":"trail-pack","body_html":"<p>Weekend hauler.</p>",
                      "variants":[{"sku":"SKU-1","price":"120.00","inventory_quantity":3,"inventory_policy":"deny"}]}]}"""
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.createContext("/admin/api/2024-01/shop.json", exchange -> {
            byte[] body = "{\"shop\":{\"currency\":\"USD\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.setExecutor(Executors.newSingleThreadExecutor());
        stub.start();
        var service = new StoreConnectionService(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(), StoreConnectionService.WIX_API_BASE);
        var result = service.fetch("SHOPIFY", "http://127.0.0.1:" + stub.getAddress().getPort(), null, null, "shpat_x", null);

        assertEquals(1, result.productCount());
        assertEquals("Trail Pack", result.products().get(0).name());
        assertEquals("120.00", result.products().get(0).price());
        assertEquals("IN_STOCK", result.products().get(0).availability());
        assertEquals("Weekend hauler.", result.products().get(0).description());
    }

    @Test
    void fetchesWooCommerceProductsOverHttp() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/wp-json/wc/v3/products", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            assertTrue(query.contains("consumer_key="), "keys travel as query params");
            byte[] body = """
                    [{"name":"Trail Pack","sku":"SKU-1","price":"120","stock_status":"instock",
                      "permalink":"https://shop.example/pack","short_description":"<p>Carries <b>all</b> weekend gear.</p>"},
                     {"name":"Day Sling","sku":"SKU-2","price":"","regular_price":"45","stock_status":"outofstock",
                      "permalink":"https://shop.example/sling","short_description":""}]""".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.createContext("/wp-json/wc/v3/data/currencies/current", exchange -> {
            byte[] body = "{\"code\":\"USD\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        stub.setExecutor(Executors.newSingleThreadExecutor());
        stub.start();
        var service = new StoreConnectionService(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(), StoreConnectionService.WIX_API_BASE);
        var result = service.fetch("WOOCOMMERCE", "http://127.0.0.1:" + stub.getAddress().getPort(), "ck", "cs", null, null);

        assertEquals(2, result.productCount());
        assertEquals("Trail Pack", result.products().get(0).name());
        assertEquals("120", result.products().get(0).price());
        assertEquals("USD", result.products().get(0).currency());
        assertEquals("IN_STOCK", result.products().get(0).availability());
        assertEquals("Carries all weekend gear.", result.products().get(0).description());
        assertEquals("45", result.products().get(1).price());
        assertEquals("OUT_OF_STOCK", result.products().get(1).availability());
    }

    @Test
    void rejectsBadCredentialsHonestly() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/wp-json/wc/v3/products", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        stub.setExecutor(Executors.newSingleThreadExecutor());
        stub.start();
        var service = new StoreConnectionService(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(), StoreConnectionService.WIX_API_BASE);
        try {
            service.fetch("WOOCOMMERCE", "http://127.0.0.1:" + stub.getAddress().getPort(), "bad", "bad", null, null);
            throw new AssertionError("expected ConnectionFailed");
        } catch (StoreConnectionService.ConnectionFailed failed) {
            assertTrue(failed.getMessage().contains("rejected those keys"));
        }
    }
}
