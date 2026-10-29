package com.kaviyo.app;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.ProductAuditService;
import com.kaviyo.catalog.DemoCatalog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Starts a loopback-only browser preview of the synthetic audit report. */
public final class KaviyoWebDemo {
    private KaviyoWebDemo() {
    }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8080 : parsePort(args[0]);
        ProductAuditService auditService = new ProductAuditService(Clock.systemUTC());
        HtmlReportRenderer reportRenderer = new HtmlReportRenderer();
        HomePageRenderer homeRenderer = new HomePageRenderer();
        AuditFlowRenderer flowRenderer = new AuditFlowRenderer();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/", exchange -> handle(exchange, homeRenderer, flowRenderer,
                reportRenderer, auditService));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            executor.shutdown();
        }, "kaviyo-demo-shutdown"));

        server.start();
        System.out.println("Kaviyo browser preview is running at http://127.0.0.1:" + port);
        System.out.println("Synthetic demo data only. Press Ctrl+C to stop.");
    }

    private static void handle(HttpExchange exchange, HomePageRenderer homeRenderer,
                               AuditFlowRenderer flowRenderer, HtmlReportRenderer reportRenderer,
                               ProductAuditService auditService) throws IOException {
        try (exchange) {
            String path = exchange.getRequestURI().getPath();
            if ("/favicon.ico".equals(path)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String html = switch (path) {
                case "/" -> homeRenderer.render();
                case "/assets/home-scroll-story.js" -> homeRenderer.scrollStoryScript();
                case "/how-it-works" -> flowRenderer.renderHowItWorks();
                case "/audit/start" -> flowRenderer.renderSourceStep(DemoCatalog.products());
                case "/audit/review" -> flowRenderer.renderReviewStep(DemoCatalog.products());
                case "/audit/results" -> reportRenderer.render(auditService.audit(
                        DemoCatalog.products(), DemoCatalog.pageSnapshots()));
                default -> null;
            };
            if (html == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] page = html.getBytes(StandardCharsets.UTF_8);
            String contentType = path.endsWith(".js")
                    ? "text/javascript; charset=utf-8"
                    : "text/html; charset=utf-8";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; base-uri 'none'; frame-ancestors 'none'");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
        }
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535.");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Usage: KaviyoWebDemo [port]", exception);
        }
    }
}
