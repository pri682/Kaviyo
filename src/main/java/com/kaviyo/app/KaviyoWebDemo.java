package com.kaviyo.app;

import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.ProductAuditService;
import com.kaviyo.catalog.DemoCatalog;
import com.kaviyo.importer.CatalogCsvPreviewService;
import com.kaviyo.importer.CatalogImportRowPreview;
import com.kaviyo.importer.CsvPreviewException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

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
        Map<String, DiscoverySession> discoverySessions = new ConcurrentHashMap<>();
        CatalogCsvPreviewService csvPreviewService = new CatalogCsvPreviewService();
        HttpClient apiClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build();
        String apiBaseUrl = System.getenv().getOrDefault("KAVIYO_API_BASE_URL", "http://127.0.0.1:8083");

        HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/", exchange -> handle(exchange, homeRenderer, flowRenderer,
                reportRenderer, auditService, discoverySessions, csvPreviewService, apiClient, apiBaseUrl));
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
                               ProductAuditService auditService,
                               Map<String, DiscoverySession> discoverySessions,
                               CatalogCsvPreviewService csvPreviewService, HttpClient apiClient,
                               String apiBaseUrl) throws IOException {
        try (exchange) {
            handleSafe(exchange, homeRenderer, flowRenderer, reportRenderer, auditService,
                    discoverySessions, csvPreviewService, apiClient, apiBaseUrl);
        } catch (Throwable fatal) {
            // The JDK server closes failed exchanges silently; log loudly instead.
            System.err.println("Kaviyo demo request failed: " + fatal);
            fatal.printStackTrace(System.err);
        }
    }

    private static void handleSafe(HttpExchange exchange, HomePageRenderer homeRenderer,
                                   AuditFlowRenderer flowRenderer, HtmlReportRenderer reportRenderer,
                                   ProductAuditService auditService,
                                   Map<String, DiscoverySession> discoverySessions,
                                   CatalogCsvPreviewService csvPreviewService, HttpClient apiClient,
                                   String apiBaseUrl) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/favicon.ico".equals(path)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            boolean createDiscoverySession = "/discovery/start".equals(path)
                    && "POST".equals(exchange.getRequestMethod());
            boolean confirmDiscoverySession = "/discovery/confirm".equals(path)
                    && "POST".equals(exchange.getRequestMethod());
            boolean createAccountStep = "/discovery/account".equals(path)
                    && "POST".equals(exchange.getRequestMethod());
            if (!"GET".equals(exchange.getRequestMethod()) && !createDiscoverySession && !confirmDiscoverySession
                    && !createAccountStep) {
                exchange.getResponseHeaders().set("Allow", "/discovery/start".equals(path)
                        || "/discovery/confirm".equals(path) || "/discovery/account".equals(path) ? "GET, POST" : "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            if ("/discovery/governance".equals(path)) {
                exchange.getResponseHeaders().set("Location", "/discovery/observe#review-policy");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            String html;
            if (createAccountStep) {
                byte[] formBody = exchange.getRequestBody().readNBytes(8_001);
                if (formBody.length > 8_000) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                String form = new String(formBody, StandardCharsets.UTF_8);
                String accountEmail = queryParameter(form, "accountEmail");
                String accountPlan = "GROWTH".equals(queryParameter(form, "accountPlan")) ? "GROWTH" : "STARTER";
                if (!validAccountEmail(accountEmail)) {
                    html = flowRenderer.renderAccountStep("Enter a valid work email to continue.");
                } else {
                    html = flowRenderer.renderDiscoveryWorkspace(null, accountEmail.trim(), accountPlan);
                }
                sendHtml(exchange, path, html);
                return;
            }
            if (confirmDiscoverySession) {
                // Matches the backend 1M body cap so large catalogs confirm instead of 413ing here first.
                byte[] formBody = exchange.getRequestBody().readNBytes(1_000_001);
                if (formBody.length > 1_000_000) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                String sessionId = queryParameter(new String(formBody, StandardCharsets.UTF_8), "session");
                DiscoverySession session = sessionId == null ? null : discoverySessions.get(sessionId);
                if (session == null) {
                    html = flowRenderer.renderDiscoveryWorkspace("This catalog review expired. Start again to continue.");
                } else if (session.csv().isBlank()) {
                    html = needsProductsHtml(flowRenderer, sessionId, session, "Add products to continue.");
                } else {
                    try {
                        String previewHash = csvPreviewService.preview(session.csv()).previewHash();
                        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBaseUrl + "/api/v1/catalog/imports/confirm"))
                                .timeout(java.time.Duration.ofSeconds(15))
                                .header("Content-Type", "text/csv; charset=utf-8")
                                .header("X-Kaviyo-Confirm-Hash", previewHash)
                                .POST(HttpRequest.BodyPublishers.ofString(session.csv(), StandardCharsets.UTF_8))
                                .build();
                        HttpResponse<String> response = apiClient.send(request,
                                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            String reason = response.statusCode() == 503
                                    ? "The catalog database is unavailable. Start PostgreSQL and the Kaviyo API, then retry confirmation."
                                    : "The backend could not save this catalog (HTTP " + response.statusCode() + "). Review the CSV and retry.";
                            html = flowRenderer.renderCatalogReview(sessionId, session.business(), session.website(),
                                    session.businessType(), session.missions(), session.products(), session.totalRows(),
                                    session.needsReview(), session.truthSource(), session.accountEmail(), session.accountPlan(),
                                    session.goals(), false, reason);
                        } else {
                            DiscoverySession saved = new DiscoverySession(session.business(), session.website(),
                                    session.businessType(), session.missions(), session.goals(), session.products(), session.totalRows(),
                                    session.needsReview(), session.csv(), true, session.truthSource(),
                                    session.accountEmail(), session.accountPlan());
                            discoverySessions.put(sessionId, saved);
                            var scan = startFirstScan(sessionId,saved,csvPreviewService,apiClient,apiBaseUrl);
                            html = flowRenderer.renderCatalogReview(sessionId, saved.business(), saved.website(),
                                    saved.businessType(), saved.missions(), saved.products(), saved.totalRows(),
                                    saved.needsReview(), saved.truthSource(), saved.accountEmail(), saved.accountPlan(),
                                    saved.goals(), true, scan.message());
                            if (scan.id()!=null) html=html.replace("href=\"/discovery/run\"", "href=\"/discovery/run?scan="+scan.id()+"\"");
                        }
                    } catch (IOException | InterruptedException | CsvPreviewException | IllegalArgumentException error) {
                        if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                        html = flowRenderer.renderCatalogReview(sessionId, session.business(), session.website(),
                                session.businessType(), session.missions(), session.products(), session.totalRows(),
                                session.needsReview(), session.truthSource(), session.accountEmail(), session.accountPlan(),
                                session.goals(), false, "Could not reach the Kaviyo API. Start it and confirm the catalog again.");
                    }
                }
                sendHtml(exchange, path, html);
                return;
            }
            if (createDiscoverySession) {
                byte[] formBody = exchange.getRequestBody().readNBytes(3_200_001);
                if (formBody.length > 3_200_000) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                String form = new String(formBody, StandardCharsets.UTF_8);
                String business = queryParameter(form, "business");
                String website = queryParameter(form, "website");
                String businessType = queryParameter(form, "businessType");
                List<String> missions = shopperMissions(form);
                String goal = queryParameter(form, "goal");
                if (blank(goal) && !missions.isEmpty()) goal = missions.get(0);
                if (missions.isEmpty() && !blank(goal)) missions = List.of(goal);
                String product = queryParameter(form, "product");
                String productUrl = queryParameter(form, "productUrl");
                String facts = queryParameter(form, "facts");
                String csv = queryParameter(form, "csv");
                String truthSource = queryParameter(form, "truthSource");
                if (!"CSV".equals(truthSource) && !"WEBSITE".equals(truthSource) && !"OTHER".equals(truthSource))
                    truthSource = "CSV";
                String accountEmail = queryParameter(form, "accountEmail");
                String accountPlan = "GROWTH".equals(queryParameter(form, "accountPlan")) ? "GROWTH" : "STARTER";
                if (!validAccountEmail(accountEmail)) {
                    html = flowRenderer.renderAccountStep("Your sign-in expired. Enter your work email again to continue.");
                    sendHtml(exchange, path, html);
                    return;
                }
                accountEmail = accountEmail.trim();
                if (!"products".equals(queryParameter(form, "stage"))) {
                    String businessError = businessFieldError(business, website, missions);
                    if (businessError != null) {
                        html = flowRenderer.renderDiscoveryWorkspace(businessError, accountEmail, accountPlan);
                        sendHtml(exchange, path, html);
                        return;
                    }
                    List<GoalEntry> businessGoals = shopperGoals(form);
                    String businessGoalsError = goalsError(businessGoals);
                    if (businessGoalsError != null) {
                        html = flowRenderer.renderDiscoveryWorkspace(businessGoalsError, accountEmail, accountPlan);
                        sendHtml(exchange, path, html);
                        return;
                    }
                    String setupId = UUID.randomUUID().toString();
                    final String resolvedGoal = goal;
                    String profile = java.util.stream.Stream.of("business","businessType","website","background","policies","goal","cadence")
                            .map(key -> key + "=" + java.net.URLEncoder.encode(switch (key) { case "goal" -> resolvedGoal; case "businessType" -> java.util.Objects.requireNonNullElse(businessType,""); case "cadence" -> java.util.Objects.requireNonNullElse(queryParameter(form,key),"weekly"); default -> java.util.Objects.requireNonNullElse(queryParameter(form,key),""); }, StandardCharsets.UTF_8))
                            .collect(java.util.stream.Collectors.joining("&"));
                    try {
                        var savedProfile = apiClient.send(HttpRequest.newBuilder(URI.create(apiBaseUrl+"/api/v1/merchant-setups/"+setupId))
                                .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type","application/x-www-form-urlencoded")
                                .PUT(HttpRequest.BodyPublishers.ofString(profile)).build(),HttpResponse.BodyHandlers.discarding());
                        if (savedProfile.statusCode()!=204) throw new IOException("Profile unavailable");
                    } catch (IOException | InterruptedException error) {
                        if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                        sendHtml(exchange,path,flowRenderer.renderDiscoveryWorkspace("Your business profile could not be saved. Check the form and try again.",accountEmail,accountPlan)); return;
                    }
                    try {
                        var savedGoals = apiClient.send(HttpRequest.newBuilder(URI.create(apiBaseUrl+"/api/v1/merchant-setups/goals"))
                                .timeout(java.time.Duration.ofSeconds(10)).header("Content-Type","application/json")
                                .PUT(HttpRequest.BodyPublishers.ofString(goalsJson(businessGoals))).build(),HttpResponse.BodyHandlers.discarding());
                        if (savedGoals.statusCode()!=204) throw new IOException("Goals unavailable");
                    } catch (IOException | InterruptedException error) {
                        if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                        sendHtml(exchange,path,flowRenderer.renderDiscoveryWorkspace("Your goals could not be saved. Check the targets and try again.",accountEmail,accountPlan)); return;
                    }
                    List<String> goalLines = goalLines(businessGoals);
                    DiscoverySession fresh = new DiscoverySession(business, website, businessType, List.copyOf(missions),
                            List.copyOf(goalLines), List.of(), 0, 0, "", false, "CSV", accountEmail, accountPlan);
                    discoverySessions.put(setupId, fresh);
                    html = needsProductsHtml(flowRenderer, setupId, fresh, null);
                    sendHtml(exchange, path, html);
                    return;
                }
                String sessionId = queryParameter(form, "session");
                DiscoverySession existing = sessionId == null ? null : discoverySessions.get(sessionId);
                if (existing == null) {
                    html = flowRenderer.renderDiscoveryWorkspace("This setup session expired. Start again to continue.", accountEmail, accountPlan);
                    sendHtml(exchange, path, html);
                    return;
                }
                String fieldError = startFieldError(existing.business(), existing.website(), existing.missions(),
                        product, productUrl, facts);
                if (fieldError != null) {
                    html = needsProductsHtml(flowRenderer, sessionId, existing, fieldError);
                    sendHtml(exchange, path, html);
                    return;
                }
                List<AuditFlowRenderer.CatalogItem> items = new ArrayList<>();
                int totalRows = 1;
                int needsReview = 0;
                if (csv != null && !csv.isBlank()) {
                    try {
                        csv = CatalogCsvPreviewService.mapTitleColumn(csv, queryParameter(form, "titleColumn"));
                        var preview = csvPreviewService.preview(csv);
                        totalRows = preview.totalRows();
                        needsReview = preview.rowsNeedingReview();
                        for (CatalogImportRowPreview row : preview.rows()) {
                            if (!row.readyToImport()) continue;
                            String evidence = productFacts(row);
                            items.add(new AuditFlowRenderer.CatalogItem(row.title(), evidence, row.productUrl() == null ? null : row.productUrl().toString()));
                        }
                        if (items.isEmpty()) {
                            html = needsProductsHtml(flowRenderer, sessionId, existing, "No rows are ready. Fix the CSV errors and upload it again.");
                            sendHtml(exchange, path, html);
                            return;
                        }
                    } catch (CsvPreviewException error) {
                        html = needsProductsHtml(flowRenderer, sessionId, existing, error.getMessage());
                        sendHtml(exchange, path, html);
                        return;
                    }
                } else {
                    if (blank(product)) {
                        html = needsProductsHtml(flowRenderer, sessionId, existing, "Choose a CSV file or add a product manually.");
                        sendHtml(exchange, path, html);
                        return;
                    }
                    csv = "product_id,title,url,description\n"
                            + csvCell("manual-" + UUID.randomUUID()) + ","
                            + csvCell(product) + ","
                            + csvCell(productUrl == null ? "" : productUrl) + ","
                            + csvCell(facts == null ? "" : facts) + "\n";
                    items.add(new AuditFlowRenderer.CatalogItem(product, facts == null ? "" : facts, productUrl));
                }
                DiscoverySession updated = new DiscoverySession(existing.business(), existing.website(), existing.businessType(),
                        existing.missions(), existing.goals(), List.copyOf(items), totalRows, needsReview,
                        csv == null ? "" : csv, false, truthSource, existing.accountEmail(), existing.accountPlan());
                discoverySessions.put(sessionId, updated);
                html = flowRenderer.renderCatalogReview(sessionId, updated.business(), updated.website(),
                        updated.businessType(), updated.missions(), items, totalRows, needsReview, truthSource,
                        updated.accountEmail(), updated.accountPlan(), updated.goals(), false, null);
            } else {
                html = switch (path) {
                case "/" -> homeRenderer.render();
                case "/assets/home-scroll-story.js" -> homeRenderer.scrollStoryScript();
                case "/how-it-works" -> flowRenderer.renderHowItWorks();
                case "/discovery" -> flowRenderer.renderAccountStep();
                case "/assets/theme.js" -> flowRenderer.themeScript();
                case "/assets/onboarding.js" -> flowRenderer.onboardingScript();
                case "/assets/sample-catalog.csv" -> SampleCatalogGenerator.generate();
                case "/discovery/simulation" -> renderSimulation(exchange, flowRenderer, discoverySessions);
                case "/discovery/run" -> flowRenderer.renderScanRun(
                        queryParameter(exchange.getRequestURI().getRawQuery(), "scan"));
                case "/discovery/observe" -> flowRenderer.renderObservationCapture(
                        queryParameter(exchange.getRequestURI().getRawQuery(), "mission"));
                case "/assets/governance.js" -> flowRenderer.governanceScript();
                case "/discovery/compare" -> flowRenderer.renderPriceCompare(
                        queryParameter(exchange.getRequestURI().getRawQuery(), "product"));
                case "/assets/price-compare.js" -> flowRenderer.priceCompareScript();
                case "/discovery/drafts" -> flowRenderer.renderDraftReview(
                        queryParameter(exchange.getRequestURI().getRawQuery(), "scan"),
                        queryParameter(exchange.getRequestURI().getRawQuery(), "product"),
                        queryParameter(exchange.getRequestURI().getRawQuery(), "finding"));
                case "/assets/discovery-workspace.js" -> flowRenderer.discoveryWorkspaceScript();
                case "/assets/agent-simulation.js" -> flowRenderer.agentSimulationScript();
                case "/assets/scan-run.js" -> flowRenderer.scanRunScript();
                case "/assets/draft-review.js" -> flowRenderer.draftReviewScript();
                case "/audit/start" -> flowRenderer.renderSourceStep(DemoCatalog.products());
                case "/audit/review" -> flowRenderer.renderReviewStep(DemoCatalog.products());
                case "/audit/results" -> reportRenderer.render(auditService.audit(
                        DemoCatalog.products(), DemoCatalog.pageSnapshots()));
                default -> null;
        };
            }
            if (html == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] page = html.getBytes(StandardCharsets.UTF_8);
            String contentType = path.endsWith(".js") ? "text/javascript; charset=utf-8"
                    : path.endsWith(".csv") ? "text/csv; charset=utf-8" : "text/html; charset=utf-8";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; connect-src http://127.0.0.1:8083 http://localhost:8083 http://127.0.0.1:8090; base-uri 'none'; frame-ancestors 'none'");
            exchange.sendResponseHeaders(200, page.length);
            exchange.getResponseBody().write(page);
    }

    private static String productFacts(CatalogImportRowPreview row) {
        StringBuilder facts = new StringBuilder();
        addFact(facts, "SKU", row.sku()); addFact(facts, "Brand", row.brand());
        addFact(facts, "Category", row.category()); addFact(facts, "Description", row.description());
        if (row.price() != null) addFact(facts, "Price", row.price() + " " + (row.currency() == null ? "" : row.currency()));
        addFact(facts, "Availability", row.availability());
        addFact(facts, "Product URL", row.productUrl() == null ? null : row.productUrl().toString());
        row.attributes().forEach((key, value) -> addFact(facts, key, value));
        return facts.length() > 3_000 ? facts.substring(0, 3_000) : facts.toString();
    }

    private static void addFact(StringBuilder facts, String label, String value) {
        if (value == null || value.isBlank()) return;
        if (!facts.isEmpty()) facts.append("; ");
        facts.append(label).append(": ").append(value);
    }

    private static String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static void sendHtml(HttpExchange exchange, String path, String html) throws IOException {
        byte[] page = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'none'; script-src 'self'; style-src 'unsafe-inline'; connect-src http://127.0.0.1:8083 http://localhost:8083 http://127.0.0.1:8090; base-uri 'none'; frame-ancestors 'none'");
        exchange.sendResponseHeaders(200, page.length);
        exchange.getResponseBody().write(page);
    }

    private static String queryParameter(String query, String name) {
        if (query == null || query.isBlank()) return null;
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && name.equals(URLDecoder.decode(pair[0], StandardCharsets.UTF_8))) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private record FirstScan(String message, UUID id) {}
    private static FirstScan startFirstScan(String setupId, DiscoverySession session, CatalogCsvPreviewService service,
                                            HttpClient client, String api) {
        var ids = service.preview(session.csv()).rows().stream().filter(row -> row.productUrl()!=null)
                .limit(100).map(row -> AuditFlowRenderer.jsonString(row.productId())).toList();
        if (ids.isEmpty()) return new FirstScan("Catalog saved. Add product page URLs to start a website investigation.", null);
        try {
            var response = client.send(HttpRequest.newBuilder(URI.create(api+"/api/v1/catalog/scans"))
                    .timeout(java.time.Duration.ofSeconds(15)).header("Content-Type","application/json")
                    .header("X-Kaviyo-Setup-Run",setupId)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"productIds\":["+String.join(",",ids)+"]}")).build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode()==202) {
                String path=response.headers().firstValue("Location").orElseThrow();
                return new FirstScan("Catalog saved. First investigation queued for "+ids.size()+" product page(s).",
                        UUID.fromString(path.substring(path.lastIndexOf('/')+1)));
            }
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        catch (IOException | IllegalArgumentException error) { /* Catalog remains available for manual retry. */ }
        return new FirstScan("Catalog saved. The first scan could not be queued. Open the live scan to retry.", null);
    }

    private static String renderSimulation(HttpExchange exchange, AuditFlowRenderer flowRenderer,
                                           Map<String, DiscoverySession> sessions) {
        String query = exchange.getRequestURI().getRawQuery();
        String sessionId = queryParameter(query, "session");
        if (sessionId == null || sessionId.isBlank()) {
            return flowRenderer.renderSimulation(null, null, null);
        }
        DiscoverySession session = sessions.get(sessionId);
        if (session == null) return flowRenderer.renderSimulation(null, null, null);
        return flowRenderer.renderSimulation(session.business(), session.website(), session.primaryMission(), session.products());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean validAccountEmail(String value) {
        if (value == null || value.length() > 254) return false;
        String email = value.trim();
        int at = email.indexOf('@');
        return at > 0 && at == email.lastIndexOf('@') && email.indexOf('.', at) > at + 1
                && email.indexOf(' ') < 0 && at < email.length() - 1;
    }

    private static final int MAX_SHOPPER_MISSIONS = 3;
    private static final int MAX_MISSION_LENGTH = 600;

    private static String businessFieldError(String business, String website, List<String> missions) {
        if (blank(business)) return "Add your business name to continue.";
        if (business.length() > 100) return "Business name must be 100 characters or fewer.";
        if (missions.isEmpty())
            return "Enter a shopper question or topic.";
        for (int index = 0; index < missions.size(); index++) {
            if (missions.get(index).length() > MAX_MISSION_LENGTH)
                return "Shopper need " + (index + 1) + " must be 600 characters or fewer.";
        }
        if (website != null && website.length() > 300) return "Website URL must be 300 characters or fewer.";
        return null;
    }

    private static String startFieldError(String business, String website, List<String> missions,
                                          String product, String productUrl, String facts) {
        String businessError = businessFieldError(business, website, missions);
        if (businessError != null) return businessError;
        if (product != null && product.length() > 160) return "Product name must be 160 characters or fewer.";
        if (productUrl != null && productUrl.length() > 1_000) return "Product page URL must be 1000 characters or fewer.";
        if (facts != null && facts.length() > 3_000) return "Product facts must be 3000 characters or fewer.";
        return null;
    }

    private static String needsProductsHtml(AuditFlowRenderer flowRenderer, String sessionId,
                                              DiscoverySession session, String message) {
        return flowRenderer.renderCatalogReview(sessionId, session.business(), session.website(),
                session.businessType(), session.missions(), List.of(), 0, 0, session.truthSource(),
                session.accountEmail(), session.accountPlan(), session.goals(), false, message);
    }

    private static List<String> shopperMissions(String form) {
        List<String> missions = new ArrayList<>();
        for (int index = 1; index <= MAX_SHOPPER_MISSIONS; index++) {
            String mission = queryParameter(form, "mission" + index);
            if (!blank(mission)) missions.add(mission.trim());
        }
        return missions;
    }

    private record GoalEntry(String metric, String target, String period) {
    }

    private static final List<String> GOAL_METRICS =
            List.of("MENTION_RATE", "ACCURACY_RATE", "FINDINGS_FIXED", "OBSERVATIONS_RECORDED");
    private static final int MAX_GOALS = 5;

    private static List<GoalEntry> shopperGoals(String form) {
        int count;
        try {
            count = Integer.parseInt(String.valueOf(queryParameter(form, "goalCount")).trim());
        } catch (NumberFormatException e) {
            return List.of();
        }
        count = Math.min(Math.max(count, 0), MAX_GOALS + 1);
        List<GoalEntry> goals = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            goals.add(new GoalEntry(queryParameter(form, "goalMetric" + index),
                    queryParameter(form, "goalTarget" + index), queryParameter(form, "goalPeriod" + index)));
        }
        return goals;
    }

    private static String goalsError(List<GoalEntry> goals) {
        if (goals.isEmpty()) return "Set at least one goal with a target number.";
        if (goals.size() > MAX_GOALS) return "Set at most " + MAX_GOALS + " goals.";
        for (int index = 0; index < goals.size(); index++) {
            GoalEntry goal = goals.get(index);
            String where = "Goal " + (index + 1);
            if (goal.metric() == null || !GOAL_METRICS.contains(goal.metric())) return where + " needs a metric.";
            if (goal.period() == null || !List.of("daily", "weekly", "monthly").contains(goal.period())) {
                return where + " needs a daily, weekly, or monthly period.";
            }
            java.math.BigDecimal target;
            try {
                target = new java.math.BigDecimal(goal.target() == null ? "" : goal.target().trim());
            } catch (NumberFormatException e) {
                return where + " needs a target number.";
            }
            boolean rate = "MENTION_RATE".equals(goal.metric()) || "ACCURACY_RATE".equals(goal.metric());
            if (rate) {
                if (target.compareTo(java.math.BigDecimal.ONE) < 0
                        || target.compareTo(new java.math.BigDecimal("100")) > 0) {
                    return where + " target must be a percent between 1 and 100.";
                }
            } else if (target.compareTo(java.math.BigDecimal.ONE) < 0
                    || target.stripTrailingZeros().scale() > 0) {
                return where + " target must be a whole number of 1 or more.";
            }
        }
        return null;
    }

    private static String goalsJson(List<GoalEntry> goals) {
        StringBuilder json = new StringBuilder("{\"goals\":[");
        for (int index = 0; index < goals.size(); index++) {
            GoalEntry goal = goals.get(index);
            if (index > 0) json.append(',');
            json.append("{\"metric\":\"").append(goal.metric()).append("\",\"target\":")
                    .append(goal.target().trim()).append(",\"period\":\"").append(goal.period()).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static List<String> goalLines(List<GoalEntry> goals) {
        List<String> lines = new ArrayList<>();
        for (GoalEntry goal : goals) {
            boolean rate = "MENTION_RATE".equals(goal.metric()) || "ACCURACY_RATE".equals(goal.metric());
            String target = new java.math.BigDecimal(goal.target().trim()).stripTrailingZeros().toPlainString();
            lines.add(goalMetricLabel(goal.metric()) + " ≥ " + target + (rate ? "%" : "") + " · " + goal.period());
        }
        return lines;
    }

    private static String goalMetricLabel(String metric) {
        return switch (metric) {
            case "MENTION_RATE" -> "Mention rate";
            case "ACCURACY_RATE" -> "Accuracy rate";
            case "FINDINGS_FIXED" -> "Findings fixed";
            default -> "Observations recorded";
        };
    }

    private record DiscoverySession(String business, String website, String businessType, List<String> missions,
                                    List<String> goals,
                                    List<AuditFlowRenderer.CatalogItem> products, int totalRows, int needsReview,
                                    String csv, boolean catalogSaved, String truthSource,
                                    String accountEmail, String accountPlan) {
        /** First mission drives the simulation; intake validation guarantees at least one. */
        String primaryMission() {
            return missions.get(0);
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
