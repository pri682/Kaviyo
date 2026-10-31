package com.kaviyo.backend.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Fetches product catalogs from merchant stores over their public APIs. Keys are used for one call and never stored. */
@ApplicationScoped
public class StoreConnectionService {
    static final String WIX_API_BASE = "https://www.wixapis.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_PRODUCTS = 200;

    private final HttpClient http;
    private final ObjectMapper json;
    private final String wixApiBase;

    public StoreConnectionService() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build(),
                new ObjectMapper(), WIX_API_BASE);
    }

    StoreConnectionService(HttpClient http, ObjectMapper json, String wixApiBase) {
        this.http = http;
        this.json = json;
        this.wixApiBase = wixApiBase;
    }

    /** Returns a human reason when the request is invalid, else null. Never echoes secrets. */
    public static String validate(String provider, String storeUrl, String consumerKey, String consumerSecret,
                                  String apiKey, String siteId) {
        if (!"WOOCOMMERCE".equals(provider) && !"WIX".equals(provider) && !"SHOPIFY".equals(provider)) {
            return "Choose WooCommerce, Wix, or Shopify.";
        }
        if ("SHOPIFY".equals(provider)) {
            if (shopifyHost(storeUrl) == null) return "Enter your Shopify store domain, like mystore.myshopify.com.";
            if (apiKey == null || apiKey.isBlank()) return "Enter your Shopify admin API access token.";
            if (apiKey.length() > 500) return "That access token looks too long.";
            return null;
        }
        if ("WOOCOMMERCE".equals(provider)) {
            if (storeUrl == null || storeUrl.isBlank()) return "Enter your store address.";
            URI store;
            try {
                store = URI.create(storeUrl.trim());
            } catch (IllegalArgumentException bad) {
                return "That store address is not a valid URL.";
            }
            if (store.getHost() == null) return "That store address is not a valid URL.";
            boolean loopback = "localhost".equalsIgnoreCase(store.getHost()) || "127.0.0.1".equals(store.getHost());
            if (!"https".equalsIgnoreCase(store.getScheme()) && !loopback) {
                return "Use an https store address so your keys stay encrypted.";
            }
            if (consumerKey == null || consumerKey.isBlank() || consumerSecret == null || consumerSecret.isBlank()) {
                return "Enter your WooCommerce consumer key and secret.";
            }
            if (consumerKey.length() > 200 || consumerSecret.length() > 500) return "Those credentials look too long.";
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) return "Enter your Wix API key.";
        if (apiKey.length() > 500) return "That API key looks too long.";
        if (siteId != null && siteId.length() > 100) return "That site ID looks too long.";
        return null;
    }

    public ConnectionResult fetch(String provider, String storeUrl, String consumerKey, String consumerSecret,
                                   String apiKey, String siteId) throws ConnectionFailed {
        try {
            if ("WOOCOMMERCE".equals(provider)) return fetchWooCommerce(storeUrl.trim(), consumerKey, consumerSecret);
            if ("SHOPIFY".equals(provider)) return fetchShopify(shopifyHost(storeUrl), apiKey);
            return fetchWix(apiKey, siteId);
        } catch (ConnectionFailed failed) {
            throw failed;
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ConnectionFailed("The store could not be reached. Check the address and try again.");
        }
    }

    private ConnectionResult fetchWooCommerce(String storeUrl, String key, String secret)
            throws IOException, InterruptedException, ConnectionFailed {
        String base = storeUrl.endsWith("/") ? storeUrl.substring(0, storeUrl.length() - 1) : storeUrl;
        String query = "?status=publish&per_page=100&consumer_key=" + encode(key) + "&consumer_secret=" + encode(secret);
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(base + "/wp-json/wc/v3/products" + query))
                .timeout(TIMEOUT).header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ConnectionFailed("The store rejected those keys. Check them in WooCommerce > Settings > Advanced > REST API.");
        }
        if (response.statusCode() != 200) throw new ConnectionFailed("The store returned HTTP " + response.statusCode() + ".");
        String currency = wooCurrency(base, query);
        JsonNode items;
        try {
            items = json.readTree(response.body());
        } catch (IOException bad) {
            throw new ConnectionFailed("The store did not return product data.");
        }
        if (!items.isArray()) throw new ConnectionFailed("The store did not return product data.");
        List<ConnectedProduct> products = new ArrayList<>();
        for (JsonNode item : items) {
            if (products.size() >= MAX_PRODUCTS) break;
            String price = text(item, "price");
            if (price == null || price.isBlank()) price = text(item, "regular_price");
            products.add(new ConnectedProduct(
                    orBlank(text(item, "name")), orBlank(text(item, "sku")),
                    price == null ? "" : price, currency,
                    wooAvailability(text(item, "stock_status")),
                    orBlank(text(item, "permalink")),
                    plain(orBlank(text(item, "short_description")))));
        }
        return new ConnectionResult("WOOCOMMERCE", hostOf(base), products.size(), List.copyOf(products), Instant.now());
    }

    private String wooCurrency(String base, String query) {
        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(base + "/wp-json/wc/v3/data/currencies/current" + query))
                            .timeout(TIMEOUT).header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "";
            return orBlank(json.readTree(response.body()).path("code").asText(""));
        } catch (IOException | InterruptedException | IllegalArgumentException error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            return "";
        }
    }

    private ConnectionResult fetchWix(String apiKey, String siteId) throws IOException, InterruptedException, ConnectionFailed {
        var request = HttpRequest.newBuilder(URI.create(wixApiBase + "/stores/v1/products/query"))
                .timeout(TIMEOUT).header("Content-Type", "application/json").header("Accept", "application/json")
                .header("Authorization", apiKey);
        if (siteId != null && !siteId.isBlank()) request.header("wix-site-id", siteId.trim());
        HttpResponse<String> response = http.send(request.POST(HttpRequest.BodyPublishers
                .ofString("{\"query\":{},\"paging\":{\"limit\":100,\"offset\":0}}")).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ConnectionFailed("Wix rejected that API key. Check it in your site dashboard under API keys.");
        }
        if (response.statusCode() != 200) throw new ConnectionFailed("Wix returned HTTP " + response.statusCode() + ".");
        JsonNode items;
        try {
            items = json.readTree(response.body()).path("products");
        } catch (IOException bad) {
            throw new ConnectionFailed("Wix did not return product data.");
        }
        if (!items.isArray()) throw new ConnectionFailed("Wix did not return product data.");
        List<ConnectedProduct> products = new ArrayList<>();
        for (JsonNode item : items) {
            if (products.size() >= MAX_PRODUCTS) break;
            JsonNode price = item.path("priceData");
            JsonNode stock = item.path("stock");
            products.add(new ConnectedProduct(
                    orBlank(text(item, "name")), orBlank(text(item, "sku")),
                    price.path("price").asText(""), price.path("currency").asText(""),
                    stock.isMissingNode() ? "UNKNOWN" : (stock.path("inStock").asBoolean(false) ? "IN_STOCK" : "OUT_OF_STOCK"),
                    absoluteOrBlank(text(item, "productPageUrl")),
                    plain(orBlank(item.path("description").asText("")))));
        }
        return new ConnectionResult("WIX", "wix", products.size(), List.copyOf(products), Instant.now());
    }

    private ConnectionResult fetchShopify(String host, String token) throws IOException, InterruptedException, ConnectionFailed {
        boolean loopback = host.startsWith("127.0.0.1") || host.startsWith("localhost");
        String base = (loopback ? "http://" : "https://") + host;
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(
                        URI.create(base + "/admin/api/2024-01/products.json?limit=100&status=active"))
                .timeout(TIMEOUT).header("Accept", "application/json")
                .header("X-Shopify-Access-Token", token).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ConnectionFailed("Shopify rejected that access token. Check your custom app permissions (read_products).");
        }
        if (response.statusCode() != 200) throw new ConnectionFailed("Shopify returned HTTP " + response.statusCode() + ".");
        JsonNode items;
        try {
            items = json.readTree(response.body()).path("products");
        } catch (IOException bad) {
            throw new ConnectionFailed("Shopify did not return product data.");
        }
        if (!items.isArray()) throw new ConnectionFailed("Shopify did not return product data.");
        String currency = shopifyCurrency(base, token);
        List<ConnectedProduct> products = new ArrayList<>();
        for (JsonNode item : items) {
            if (products.size() >= MAX_PRODUCTS) break;
            JsonNode variant = item.path("variants").path(0);
            products.add(new ConnectedProduct(
                    orBlank(text(item, "title")), orBlank(text(variant, "sku")),
                    variant.path("price").asText(""), currency,
                    shopifyAvailability(variant),
                    "https://" + host.replaceFirst(":[0-9]+$", "") + "/products/" + item.path("handle").asText(""),
                    plain(orBlank(text(item, "body_html")))));
        }
        return new ConnectionResult("SHOPIFY", host.replaceFirst(":[0-9]+$", ""), products.size(),
                List.copyOf(products), Instant.now());
    }

    private String shopifyCurrency(String base, String token) {
        try {
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(base + "/admin/api/2024-01/shop.json"))
                    .timeout(TIMEOUT).header("Accept", "application/json")
                    .header("X-Shopify-Access-Token", token).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "";
            return orBlank(json.readTree(response.body()).path("shop").path("currency").asText(""));
        } catch (IOException | InterruptedException | IllegalArgumentException error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            return "";
        }
    }

    private static String shopifyAvailability(JsonNode variant) {
        if (variant.isMissingNode()) return "UNKNOWN";
        if ("continue".equals(variant.path("inventory_policy").asText(""))) return "IN_STOCK";
        if (variant.path("inventory_quantity").isNumber()) {
            return variant.path("inventory_quantity").asInt(0) > 0 ? "IN_STOCK" : "OUT_OF_STOCK";
        }
        return "UNKNOWN";
    }

    static String shopifyHost(String storeUrl) {
        if (storeUrl == null || storeUrl.isBlank()) return null;
        String trimmed = storeUrl.trim();
        if (!trimmed.contains("://")) trimmed = "https://" + trimmed;
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() == null || !uri.getHost().contains(".")) return null;
            return uri.getPort() > 0 ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    private static String wooAvailability(String status) {
        if (status == null) return "UNKNOWN";
        return switch (status.trim().toLowerCase()) {
            case "instock" -> "IN_STOCK";
            case "outofstock" -> "OUT_OF_STOCK";
            case "onbackorder" -> "ON_BACKORDER";
            default -> "UNKNOWN";
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isTextual() ? child.asText() : (child.isNumber() ? child.asText() : null);
    }

    private static String orBlank(String value) {
        return value == null ? "" : value;
    }

    private static String plain(String html) {
        String stripped = html.replaceAll("<[^>]{0,200}>", " ").replaceAll("\\s+", " ").trim();
        return stripped.length() > 500 ? stripped.substring(0, 500) : stripped;
    }

    private static String absoluteOrBlank(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        return trimmed.startsWith("https://") || trimmed.startsWith("http://") ? trimmed : "";
    }

    private static String hostOf(String url) {
        try {
            return orBlank(URI.create(url).getHost());
        } catch (IllegalArgumentException bad) {
            return "";
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record ConnectedProduct(String name, String sku, String price, String currency, String availability,
                                   String url, String description) {
    }

    public record ConnectionResult(String provider, String store, int productCount, List<ConnectedProduct> products,
                                   Instant fetchedAt) {
    }

    public static class ConnectionFailed extends Exception {
        ConnectionFailed(String message) {
            super(message);
        }
    }
}
