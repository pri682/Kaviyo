package com.kaviyo.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaviyo.catalog.Availability;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parses bounded HTML into plain text and explicitly observed product fields. */
final class ProductPageParser {
    private static final int MAX_VISIBLE_TEXT_CHARACTERS = 4_000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    ParsedProductPage parse(String html, URI finalUrl, Map<String, String> headers) {
        Document document = Jsoup.parse(html, finalUrl.toString());
        String title = firstNonBlank(document.title(), metaContent(document, "property", "og:title"));
        URI canonicalUrl = canonicalUrl(document, finalUrl);
        List<String> robots = robotsDirectives(document);

        List<JsonNode> products = jsonLdProducts(document);
        ProductFacts facts = null;
        boolean hasProductStructuredData = !products.isEmpty();
        if (products.size() == 1) {
            facts = factsFromJsonLd(products.get(0));
        } else if (products.isEmpty()) {
            List<Element> microdataProducts = microdataProducts(document);
            hasProductStructuredData = !microdataProducts.isEmpty();
            if (microdataProducts.size() == 1) {
                facts = factsFromMicrodata(microdataProducts.get(0));
            }
        }

        BigDecimal price = facts == null ? null : facts.price();
        String currency = facts == null ? null : facts.currency();
        Availability availability = facts == null ? null : facts.availability();
        if (price == null) {
            price = parsePrice(firstNonBlank(
                    metaContent(document, "property", "product:price:amount"),
                    metaContent(document, "property", "og:price:amount")));
        }
        if (currency == null) {
            currency = normalizeCurrency(firstNonBlank(
                    metaContent(document, "property", "product:price:currency"),
                    metaContent(document, "property", "og:price:currency")));
        }

        document.select("script, style, noscript, svg, iframe").remove();
        String visibleText = document.body() == null ? "" : document.body().text();
        if (visibleText.length() > MAX_VISIBLE_TEXT_CHARACTERS) {
            visibleText = visibleText.substring(0, MAX_VISIBLE_TEXT_CHARACTERS);
        }

        return new ParsedProductPage(
                blankToNull(title), canonicalUrl, price, currency, availability,
                hasProductStructuredData, robots, visibleText);
    }

    private List<JsonNode> jsonLdProducts(Document document) {
        List<JsonNode> products = new ArrayList<>();
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                collectProducts(objectMapper.readTree(script.data()), products);
            } catch (Exception ignored) {
                // Invalid JSON-LD is observable as absent valid Product data, not a fetch failure.
            }
        }
        return products;
    }

    private static void collectProducts(JsonNode node, List<JsonNode> products) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectProducts(child, products));
            return;
        }
        if (!node.isObject()) {
            return;
        }
        JsonNode type = node.get("@type");
        if (isProductType(type)) {
            products.add(node);
        }
        Iterator<JsonNode> values = node.elements();
        while (values.hasNext()) {
            collectProducts(values.next(), products);
        }
    }

    private static boolean isProductType(JsonNode type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode item : type) {
                if (isProductType(item)) {
                    return true;
                }
            }
            return false;
        }
        String value = type.asText("").toLowerCase(Locale.ROOT);
        return value.equals("product") || value.endsWith("/product") || value.endsWith("#product");
    }

    private static ProductFacts factsFromJsonLd(JsonNode product) {
        JsonNode offers = product.get("offers");
        JsonNode offer = singleOffer(offers);
        if (offer == null) {
            return new ProductFacts(null, null, null);
        }
        BigDecimal price = parsePrice(text(offer.get("price")));
        String currency = normalizeCurrency(text(offer.get("priceCurrency")));
        Availability availability = parseAvailability(text(offer.get("availability")));
        return new ProductFacts(price, currency, availability);
    }

    private static JsonNode singleOffer(JsonNode offers) {
        if (offers == null || offers.isNull()) {
            return null;
        }
        if (offers.isArray()) {
            return offers.size() == 1 ? offers.get(0) : null;
        }
        return offers.isObject() ? offers : null;
    }

    private static List<Element> microdataProducts(Document document) {
        Elements scopes = document.select("[itemscope][itemtype]");
        return scopes.stream()
                .filter(element -> element.attr("itemtype").toLowerCase(Locale.ROOT).contains("schema.org/product"))
                .toList();
    }

    private static ProductFacts factsFromMicrodata(Element product) {
        Element priceElement = firstItemProp(product, "price");
        Element currencyElement = firstItemProp(product, "priceCurrency");
        Element availabilityElement = firstItemProp(product, "availability");
        return new ProductFacts(
                parsePrice(elementValue(priceElement)),
                normalizeCurrency(elementValue(currencyElement)),
                parseAvailability(elementValue(availabilityElement)));
    }

    private static Element firstItemProp(Element root, String property) {
        return root.select("[itemprop=" + property + "]").stream().findFirst().orElse(null);
    }

    private static String elementValue(Element element) {
        if (element == null) {
            return null;
        }
        return firstNonBlank(element.attr("content"), element.attr("value"), element.text());
    }

    private static URI canonicalUrl(Document document, URI finalUrl) {
        Element canonical = document.select("link[rel][href]").stream()
                .filter(link -> java.util.Arrays.stream(link.attr("rel").toLowerCase(Locale.ROOT).split("\\s+"))
                        .anyMatch(token -> token.equals("canonical")))
                .findFirst()
                .orElse(null);
        if (canonical == null) {
            return null;
        }
        try {
            URI candidate = finalUrl.resolve(canonical.attr("href").trim()).normalize();
            if (candidate.getHost() == null || candidate.getUserInfo() != null
                    || !("http".equalsIgnoreCase(candidate.getScheme())
                    || "https".equalsIgnoreCase(candidate.getScheme()))) {
                return null;
            }
            return candidate;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static List<String> robotsDirectives(Document document) {
        List<String> directives = new ArrayList<>();
        for (Element meta : document.select("meta[name][content]")) {
            String name = meta.attr("name").trim().toLowerCase(Locale.ROOT);
            if (name.equals("robots") || name.equals("googlebot") || name.equals("bingbot")) {
                for (String directive : meta.attr("content").split(",")) {
                    if (!directive.isBlank()) {
                        directives.add(directive.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return List.copyOf(directives);
    }

    private static String metaContent(Document document, String key, String value) {
        Element meta = document.selectFirst("meta[" + key + "=" + value + "][content]");
        return meta == null ? null : meta.attr("content").trim();
    }

    private static Availability parseAvailability(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.endsWith("instock") || normalized.equals("available")) {
            return Availability.IN_STOCK;
        }
        if (normalized.endsWith("outofstock") || normalized.endsWith("discontinued")
                || normalized.endsWith("soldout")) {
            return Availability.OUT_OF_STOCK;
        }
        if (normalized.endsWith("preorder") || normalized.endsWith("presale")) {
            return Availability.PREORDER;
        }
        return null;
    }

    private static BigDecimal parsePrice(String value) {
        if (value == null || value.isBlank() || value.length() > 32) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value.trim());
            return price.signum() >= 0 && price.precision() <= 15 && price.scale() <= 4 ? price : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String normalizeCurrency(String value) {
        if (value == null) {
            return null;
        }
        String currency = value.trim().toUpperCase(Locale.ROOT);
        return currency.matches("[A-Z]{3}") ? currency : null;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isNull() || node.isContainerNode()) {
            return null;
        }
        return node.asText();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ProductFacts(BigDecimal price, String currency, Availability availability) {
    }
}
