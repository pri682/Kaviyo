package com.kaviyo.crawler;

import com.kaviyo.catalog.Availability;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bounded, robots-aware fetcher for selected public product pages. It never runs page scripts. */
@ApplicationScoped
public class SafePageFetcher {
    private static final String USER_AGENT = "KaviyoProductAuditBot/0.1";
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_PAGE_BYTES = 1_048_576;
    private static final int MAX_ROBOTS_BYTES = 65_536;

    private final PublicDnsResolver dnsResolver;
    private final PublicUrlPolicy urlPolicy;
    private final ProductPageParser parser;
    private final CloseableHttpClient httpClient;

    public SafePageFetcher() {
        dnsResolver = new PublicDnsResolver();
        urlPolicy = new PublicUrlPolicy(dnsResolver);
        parser = new ProductPageParser();

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(4))
                .setSocketTimeout(Timeout.ofSeconds(5))
                .build();
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(8)
                .setMaxConnPerRoute(2)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .setResponseTimeout(Timeout.ofSeconds(5))
                .setRedirectsEnabled(false)
                .build();
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableContentCompression()
                .build();
    }

    public PageScanSnapshot scan(String productId, URI requestedUrl) {
        Instant observedAt = Instant.now();
        URI safeUrl;
        try {
            safeUrl = urlPolicy.validate(requestedUrl);
        } catch (BlockedDestinationException exception) {
            return failed(productId, requestedUrl, requestedUrl, PageScanStatus.DESTINATION_BLOCKED,
                    null, observedAt, Map.of());
        } catch (PublicUrlPolicy.InvalidScanUrlException exception) {
            return failed(productId, requestedUrl, requestedUrl, PageScanStatus.FETCH_FAILED,
                    null, observedAt, Map.of());
        }

        FetchResponse robots = fetch(safeUrl.resolve("/robots.txt"), MAX_ROBOTS_BYTES, "text/plain");
        if (robots.status() == PageScanStatus.FETCHED
                && !RobotsRules.allows(decode(robots.body(), robots.contentType()), safeUrl, USER_AGENT)) {
            return failed(productId, safeUrl, safeUrl, PageScanStatus.ROBOTS_BLOCKED,
                    robots.httpStatus(), observedAt, robots.headers());
        }
        if (robots.status() != PageScanStatus.FETCHED
                && robots.status() != PageScanStatus.NOT_FOUND
                && !(robots.status() == PageScanStatus.HTTP_ERROR
                && (robots.httpStatus() == 404 || robots.httpStatus() == 410))) {
            return failed(productId, safeUrl, robots.finalUrl(), PageScanStatus.ROBOTS_UNAVAILABLE,
                    robots.httpStatus(), observedAt, robots.headers());
        }

        FetchResponse page = fetch(safeUrl, MAX_PAGE_BYTES, "text/html");
        if (page.status() != PageScanStatus.FETCHED) {
            return failed(productId, safeUrl, page.finalUrl(), page.status(),
                    page.httpStatus(), observedAt, page.headers());
        }

        String html = decode(page.body(), page.contentType());
        ParsedProductPage parsed = parser.parse(html, page.finalUrl(), page.headers());
        String xRobotsTag = page.headers().get("x-robots-tag");
        List<String> robotsDirectives = new ArrayList<>(parsed.robotsDirectives());
        return new PageScanSnapshot(
                productId,
                safeUrl,
                page.finalUrl(),
                PageScanStatus.FETCHED,
                page.httpStatus(),
                page.contentType(),
                parsed.title(),
                parsed.canonicalUrl(),
                parsed.price(),
                parsed.currency(),
                parsed.availability(),
                parsed.hasProductStructuredData(),
                robotsDirectives,
                xRobotsTag,
                parsed.visibleTextSample(),
                sha256(page.body()),
                page.headers(),
                observedAt);
    }

    @PreDestroy
    void close() throws IOException {
        httpClient.close();
    }

    private FetchResponse fetch(URI start, int maxBytes, String expectedMediaType) {
        URI current = start;
        URI original = start;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            try {
                current = urlPolicy.validate(current);
            } catch (BlockedDestinationException exception) {
                return response(PageScanStatus.DESTINATION_BLOCKED, current, null, null, null, Map.of());
            } catch (PublicUrlPolicy.InvalidScanUrlException exception) {
                return response(PageScanStatus.FETCH_FAILED, current, null, null, null, Map.of());
            }
            if ("https".equalsIgnoreCase(original.getScheme())
                    && "http".equalsIgnoreCase(current.getScheme())) {
                return response(PageScanStatus.CROSS_ORIGIN_REDIRECT, current, null, null, null, Map.of());
            }

            HttpGet request = new HttpGet(current);
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept", expectedMediaType + ", application/xhtml+xml;q=0.9");
            request.setHeader("Accept-Encoding", "identity");
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getCode();
                Map<String, String> headers = relevantHeaders(response);
                if (isRedirect(status)) {
                    if (redirects == MAX_REDIRECTS) {
                        return response(PageScanStatus.REDIRECT_LIMIT, current, status,
                                null, null, headers);
                    }
                    String location = header(response, "location");
                    if (location == null || location.isBlank()) {
                        return response(PageScanStatus.HTTP_ERROR, current, status,
                                null, null, headers);
                    }
                    URI next;
                    try {
                        next = current.resolve(location.trim());
                    } catch (IllegalArgumentException exception) {
                        return response(PageScanStatus.HTTP_ERROR, current, status,
                                null, null, headers);
                    }
                    try {
                        next = urlPolicy.validate(next);
                    } catch (BlockedDestinationException exception) {
                        return response(PageScanStatus.DESTINATION_BLOCKED, next, status,
                                null, null, headers);
                    } catch (PublicUrlPolicy.InvalidScanUrlException exception) {
                        return response(PageScanStatus.CROSS_ORIGIN_REDIRECT, current, status,
                                null, null, headers);
                    }
                    if (!urlPolicy.sameHost(original, next)) {
                        return response(PageScanStatus.CROSS_ORIGIN_REDIRECT, next, status,
                                null, null, headers);
                    }
                    current = next;
                    continue;
                }

                if (status == 404 || status == 410) {
                    return response(PageScanStatus.NOT_FOUND, current, status,
                            null, null, headers);
                }
                if (status == 401 || status == 403 || status == 429) {
                    return response(PageScanStatus.HTTP_ERROR, current, status,
                            null, null, headers);
                }
                if (status < 200 || status >= 300) {
                    return response(PageScanStatus.HTTP_ERROR, current, status,
                            null, null, headers);
                }

                String contentType = header(response, "content-type");
                if (!isExpectedMediaType(contentType, expectedMediaType)) {
                    return response(PageScanStatus.UNSUPPORTED_CONTENT_TYPE, current, status,
                            contentType, null, headers);
                }
                String contentEncoding = header(response, "content-encoding");
                if (contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity")) {
                    return response(PageScanStatus.UNSUPPORTED_CONTENT_TYPE, current, status,
                            contentType, null, headers);
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return response(PageScanStatus.FETCH_FAILED, current, status,
                            contentType, null, headers);
                }
                if (entity.getContentLength() > maxBytes) {
                    return response(PageScanStatus.RESPONSE_TOO_LARGE, current, status,
                            contentType, null, headers);
                }
                byte[] body;
                try (InputStream stream = entity.getContent()) {
                    body = readBounded(stream, maxBytes);
                } catch (ResponseTooLargeException exception) {
                    return response(PageScanStatus.RESPONSE_TOO_LARGE, current, status,
                            contentType, null, headers);
                }
                return response(PageScanStatus.FETCHED, current, status,
                        contentType, body, headers);
            } catch (SocketTimeoutException exception) {
                return response(PageScanStatus.TIMEOUT, current, null, null, null, Map.of());
            } catch (IOException exception) {
                if (causedByBlockedDestination(exception)) {
                    return response(PageScanStatus.DESTINATION_BLOCKED, current, null, null, null, Map.of());
                }
                if (causedByTimeout(exception)) {
                    return response(PageScanStatus.TIMEOUT, current, null, null, null, Map.of());
                }
                return response(PageScanStatus.FETCH_FAILED, current, null, null, null, Map.of());
            }
        }
        return response(PageScanStatus.REDIRECT_LIMIT, current, null, null, null, Map.of());
    }

    private static PageScanSnapshot failed(
            String productId,
            URI requested,
            URI finalUrl,
            PageScanStatus status,
            Integer httpStatus,
            Instant observedAt,
            Map<String, String> headers) {
        return new PageScanSnapshot(productId, requested, finalUrl, status, httpStatus,
                headers.get("content-type"), null, null, null, null, null, null,
                List.of(), headers.get("x-robots-tag"), null, null, headers, observedAt);
    }

    private static FetchResponse response(
            PageScanStatus status,
            URI finalUrl,
            Integer httpStatus,
            String contentType,
            byte[] body,
            Map<String, String> headers) {
        return new FetchResponse(status, finalUrl, httpStatus, contentType, body, headers);
    }

    private static byte[] readBounded(InputStream input, int maxBytes) throws IOException, ResponseTooLargeException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8_192));
        byte[] buffer = new byte[8_192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > maxBytes) {
                throw new ResponseTooLargeException();
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static String decode(byte[] body, String contentType) {
        Charset charset = StandardCharsets.UTF_8;
        if (contentType != null) {
            try {
                Charset declared = ContentType.parse(contentType).getCharset();
                if (declared != null) {
                    charset = declared;
                }
            } catch (RuntimeException ignored) {
                // Use UTF-8 when the server sends an invalid charset label.
            }
        }
        return new String(body, charset);
    }

    private static boolean isExpectedMediaType(String value, String expected) {
        if (value == null) {
            return false;
        }
        String mediaType;
        try {
            mediaType = ContentType.parse(value).getMimeType().toLowerCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            return false;
        }
        if (expected.equals("text/html")) {
            return mediaType.equals("text/html") || mediaType.equals("application/xhtml+xml");
        }
        return mediaType.equals(expected);
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String header(CloseableHttpResponse response, String name) {
        var header = response.getFirstHeader(name);
        return header == null ? null : header.getValue();
    }

    private static Map<String, String> relevantHeaders(CloseableHttpResponse response) {
        Map<String, String> selected = new LinkedHashMap<>();
        for (String name : List.of("content-type", "x-robots-tag", "etag", "last-modified")) {
            String value = header(response, name);
            if (value != null) {
                selected.put(name, value.substring(0, Math.min(value.length(), 500)));
            }
        }
        return Map.copyOf(selected);
    }

    private static boolean causedByBlockedDestination(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof BlockedDestinationException) {
                return true;
            }
        }
        return false;
    }

    private static boolean causedByTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable in this Java runtime.", exception);
        }
    }

    private record FetchResponse(
            PageScanStatus status,
            URI finalUrl,
            Integer httpStatus,
            String contentType,
            byte[] body,
            Map<String, String> headers) {
    }

    private static final class ResponseTooLargeException extends Exception {
    }
}
