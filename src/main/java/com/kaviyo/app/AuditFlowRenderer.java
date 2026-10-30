package com.kaviyo.app;

import com.kaviyo.catalog.Product;

import java.util.List;

/** Renders the guided, synthetic-data walkthrough shown after leaving the home page. */
public final class AuditFlowRenderer {
    public String renderDiscoveryWorkspace() {
        return renderDiscoveryWorkspace(null);
    }

    public String renderDiscoveryWorkspace(String errorMessage) {
        return renderDiscoveryWorkspace(errorMessage, null, null);
    }

    public String renderAccountStep() {
        return renderAccountStep(null);
    }

    public String renderAccountStep(String errorMessage) {
        String error = errorMessage == null ? "" : "<p class=\"onboarding-status onboarding-error\" role=\"alert\">" + escape(errorMessage) + "</p>";
        return page("Create your Kaviyo account", "Discovery", """
                <main class="flow-main discovery-main">
                  %s
                  %s
                  <section class="discovery-welcome" aria-labelledby="account-title">
                    <div>
                      <p class="eyebrow">Your account</p>
                      <h1 id="account-title">Check one product page.</h1>
                      <p class="flow-intro">Bring a product name, its page link, and a few facts you know are correct. Kaviyo checks what shoppers can see and shows you suggested wording to review.</p>
                    </div>
                  </section>
                  <form class="discovery-setup" action="/discovery/account" method="post">
                    <p class="step-guidance">Start with your work email</p>
                    <div class="setup-profile">
                      <label>Work email<input name="accountEmail" type="email" maxlength="254" placeholder="you@yourcompany.com" required></label>
                      <input type="hidden" name="accountPlan" value="STARTER">
                    </div>
                    <p class="field-note">This local preview uses your email to identify your setup form.</p>
                    <button class="action-link" type="submit">Continue to business setup <span aria-hidden="true">↗</span></button>
                  </form>
                </main>
                """.formatted(discoveryProgress("account"), error));
    }

    public String renderDiscoveryWorkspace(String errorMessage, String accountEmail, String accountPlan) {
        String error = errorMessage == null ? "" : "<p class=\"onboarding-status onboarding-error\" role=\"alert\">" + escape(errorMessage) + "</p>";
        String hidden = accountEmail == null || accountEmail.isBlank() ? ""
                : "<input type=\"hidden\" name=\"accountEmail\" value=\"" + escape(accountEmail) + "\">"
                + "<input type=\"hidden\" name=\"accountPlan\" value=\"" + escape(orEmpty(accountPlan)) + "\">";
        return page("Discovery missions", "Discovery", """
                <main class="flow-main discovery-main">
                  %s
                  %s
                  <section class="discovery-welcome" aria-labelledby="discovery-title">
                    <div>
                      <p class="eyebrow">Business onboarding</p>
                      <h1 id="discovery-title">Set up Kaviyo for your business.</h1>
                      <p class="flow-intro">Tell us what you sell and share the website you want checked.</p>
                    </div>
                    <aside class="workspace-note">For product businesses of every size: direct-to-consumer, B2B, wholesale, or both.</aside>
                  </section>
                  <form class="discovery-setup" action="/discovery/start" method="post">
                    %s
                    <input type="hidden" name="stage" value="business">
                    <section class="form-chapter" aria-labelledby="chapter-business">
                      <p class="chapter-index">01</p>
                      <h2 id="chapter-business">Your business</h2>
                      <p class="step-guidance">Add your business. You will add your products on the next step.</p>
                      <div class="setup-profile">
                        <label>Business name<input name="business" maxlength="100" placeholder="Your company name" required></label>
                        <label>Website<input name="website" type="url" maxlength="300" placeholder="https://yourcompany.com"></label>
                        <label>Who do you sell to?<select name="businessType"><option value="DTC">Direct to customers</option><option value="B2B">To other businesses</option><option value="BOTH">Both</option><option value="OTHER">Another product business</option></select></label>
                      </div>
                    </section>
                    <section class="form-chapter" aria-labelledby="chapter-profile">
                      <p class="chapter-index">02</p>
                      <h2 id="chapter-profile">Business profile</h2>
                      <div class="mission-fields">
                        <label>What do you sell, and who buys it?<textarea name="background" maxlength="3000"></textarea></label>
                        <label>Policies or policy page link (optional)<textarea name="policies" maxlength="3000"></textarea></label>
                      </div>
                    </section>
                    <section class="form-chapter" aria-labelledby="chapter-goals">
                      <p class="chapter-index">03</p>
                      <h2 id="chapter-goals">Your goals</h2>
                      <div class="goal-editor"><p>What should Kaviyo help you achieve? Set one to five measurable goals.</p>
                      <div id="goal-list"><div class="goal-row"><label>Metric<select name="goalMetric0"><option value="MENTION_RATE">Mention rate (pct)</option><option value="ACCURACY_RATE">Accuracy rate (pct)</option><option value="FINDINGS_FIXED">Findings fixed</option><option value="OBSERVATIONS_RECORDED">Observations recorded</option></select></label><label>Target<input name="goalTarget0" type="number" min="1" required></label><label>Period<select name="goalPeriod0"><option value="weekly">Weekly</option><option value="daily">Daily</option><option value="monthly">Monthly</option></select></label><button class="goal-remove action-link action-link--secondary" type="button">Remove</button></div></div>
                      <input type="hidden" name="goalCount" id="goal-count" value="1">
                      <button id="goal-add" class="action-link action-link--secondary" type="button">Add another goal</button></div>
                    </section>
                    <section class="form-chapter" aria-labelledby="chapter-questions">
                      <p class="chapter-index">04</p>
                      <h2 id="chapter-questions">Customer questions</h2>
                      <div class="mission-fields"><p>What would a customer ask before buying your product?</p>
                      <label>Customer question<textarea name="mission1" maxlength="600" rows="3" placeholder="Optional shopper question"></textarea></label>
                      <details><summary>Add more questions (optional)</summary><label>Question 2<textarea name="mission2" maxlength="600" rows="2" placeholder="e.g. A helmet lock that survives daily commuting"></textarea></label>
                      <label>Question 3<textarea name="mission3" maxlength="600" rows="2" placeholder="e.g. A kids helmet with a pinch-free buckle"></textarea></label></details></div>
                    </section>
                    <div class="form-submit">
                      <button class="action-link" type="submit">Continue to products <span aria-hidden="true">↗</span></button>
                      <p class="field-note">You will add products next, then review everything before saving.</p>
                      <p id="onboarding-status" class="onboarding-status" role="status" aria-live="polite"></p>
                    </div>
                  </form>
                  <p class="demo-disclosure">Product facts help agents understand fit. Kaviyo cannot guarantee that an outside assistant will recommend a product.</p>
                </main>
                """.formatted(discoveryProgress("mission"), error, hidden));
    }

    private String productsForm(String hiddenInputs) {
        return """
                  <form class="discovery-setup" action="/discovery/start" method="post">
                    %s
                    <section class="form-chapter" aria-labelledby="chapter-truth">
                      <p class="chapter-index">05</p>
                      <h2 id="chapter-truth">Source of truth</h2>
                      <label>Which source is the truth for price and availability?<select name="truthSource"><option value="CSV">This catalog upload</option><option value="WEBSITE">Your website</option><option value="OTHER">Another system (ERP, feed, marketplace)</option></select></label>
                    </section>
                    <section class="form-chapter" aria-labelledby="chapter-products">
                      <p class="chapter-index">06</p>
                      <h2 id="chapter-products">Your products</h2>
                      <div class="expand-panel store-connect-panel">
                        <button class="expand-panel__button" type="button" aria-expanded="false" aria-controls="store-connect-body" id="store-connect-toggle"><span class="expand-panel__text"><strong>Connect your store (optional)</strong><span>Pull products straight from your store, or skip this and use the spreadsheet below. Keys are used for one fetch and never stored.</span></span><span class="expand-panel__icon" aria-hidden="true"></span></button>
                        <div class="expand-panel__body" id="store-connect-body" role="region" aria-labelledby="store-connect-toggle" hidden>
                        <div class="store-connect">
                        <div class="connect-fields">
                          <label>Platform<select id="connect-provider"><option value="WOOCOMMERCE">WooCommerce</option><option value="SHOPIFY">Shopify</option><option value="WIX">Wix</option></select></label>
                          <label data-connect="WOOCOMMERCE">Store address<input id="connect-store-url" type="url" inputmode="url" placeholder="https://yourstore.com" autocomplete="off"></label>
                          <label data-connect="WOOCOMMERCE">Consumer key<input id="connect-key" autocomplete="off" placeholder="ck_…"></label>
                          <label data-connect="WOOCOMMERCE">Consumer secret<input id="connect-secret" type="password" autocomplete="off" placeholder="cs_…"></label>
                          <label data-connect="SHOPIFY" hidden>Store domain<input id="connect-shopify-domain" inputmode="url" placeholder="mystore.myshopify.com" autocomplete="off"></label>
                          <label data-connect="SHOPIFY" hidden>Admin API access token<input id="connect-shopify-token" type="password" autocomplete="off"></label>
                          <label data-connect="WIX" hidden>API key<input id="connect-apikey" type="password" autocomplete="off"></label>
                          <label data-connect="WIX" hidden>Site ID (optional)<input id="connect-site" autocomplete="off"></label>
                        </div>
                        <p class="field-note" data-connect="WOOCOMMERCE">The consumer key and secret let Kaviyo read your products. Find them in WooCommerce under Settings, then Advanced, then REST API. Give the key read access only.</p>
                        <p class="field-note" data-connect="SHOPIFY" hidden>The access token lets Kaviyo read your products. Create a custom app under Settings, then Apps and sales channels, then Develop apps, with read_products access.</p>
                        <p class="field-note" data-connect="WIX" hidden>The API key lets Kaviyo read your products. Create one in your Wix site dashboard under API keys.</p>
                        <button id="connect-fetch" class="action-link action-link--secondary" type="button">Fetch products</button>
                        <p id="connect-status" class="onboarding-status" role="status" aria-live="polite"></p>
                        </div>
                        </div>
                      </div>
                      <details class="manual-entry" open><summary>Add one product</summary><div class="setup-product"><label>Product name<input name="product" maxlength="160" placeholder="Your product"></label><label>Product page URL<input name="productUrl" type="url" maxlength="1000" placeholder="https://yourcompany.com/products/example"></label><label>Product facts<textarea name="facts" maxlength="3000" rows="3" placeholder="Key specs, compatibility, price, or availability"></textarea></label></div></details>
                      <div class="expand-panel">
                        <button class="expand-panel__button" type="button" aria-expanded="false" aria-controls="spreadsheet-body" id="spreadsheet-toggle"><span class="expand-panel__text"><strong>Upload a product spreadsheet instead</strong><span>Bring your own .csv file — check names and prices before saving.</span></span><span class="expand-panel__icon" aria-hidden="true"></span></button>
                        <div class="expand-panel__body" id="spreadsheet-body" role="region" aria-labelledby="spreadsheet-toggle" hidden>
                      <section class="catalog-upload" aria-label="Product catalog">
                        <div><strong>Bring in your product catalog</strong><span>Choose your own product spreadsheet (.csv), or enter one product below. You can check the names and prices before saving.</span></div>
                        <label class="file-drop">Choose a CSV file<input id="catalog-file" name="catalog" type="file" accept=".csv,.tsv,text/csv,text/tab-separated-values"><small id="file-name">No file selected · up to 1 MB</small></label>
                        <label>Product name column (optional)<input name="titleColumn" placeholder="For example, Listing headline"></label><small>Common headers are recognized automatically. Other columns are kept as product details.</small>
                        <textarea id="catalog-csv" name="csv" hidden></textarea>
                        <a class="template-link" href="/assets/sample-catalog.csv" download>Explore the demo CSV (2,000 fictional products)</a>
                      </section>
                        </div>
                      </div>
                    </section>
                    <div class="form-submit">
                      <button class="action-link" type="submit">Continue to product review <span aria-hidden="true">↗</span></button>
                      <p class="field-note">Your file is checked locally first. Product facts are sent to a model only if you choose to run an agent simulation.</p>
                      <p id="onboarding-status" class="onboarding-status" role="status" aria-live="polite"></p>
                    </div>
                  </form>
                """.formatted(hiddenInputs);
    }

    public String renderCatalogReview(String sessionId, String business, String website, String businessType,
                                      List<String> missions, List<CatalogItem> products, int totalRows, int needsReview,
                                      String truthSource, String accountEmail, String accountPlan,
                                      List<String> goals) {
        return renderCatalogReview(sessionId, business, website, businessType, missions, products, totalRows,
                needsReview, truthSource, accountEmail, accountPlan, goals, false, null);
    }

    public String renderCatalogReview(String sessionId, String business, String website, String businessType,
                                      List<String> missions, List<CatalogItem> products, int totalRows, int needsReview,
                                      String truthSource, String accountEmail, String accountPlan, List<String> goals,
                                      boolean saved, String message) {
        if (!saved && products.isEmpty()) {
            String banner = message == null || message.isBlank() ? ""
                    : "<p class=\"onboarding-status onboarding-error\" role=\"alert\">" + escape(message) + "</p>";
            String hidden = "<input type=\"hidden\" name=\"stage\" value=\"products\">"
                    + "<input type=\"hidden\" name=\"session\" value=\"" + escape(sessionId) + "\">"
                    + "<input type=\"hidden\" name=\"accountEmail\" value=\"" + escape(orEmpty(accountEmail)) + "\">"
                    + "<input type=\"hidden\" name=\"accountPlan\" value=\"" + escape(orEmpty(accountPlan)) + "\">"
                    + "<input type=\"hidden\" name=\"website\" value=\"" + escape(orEmpty(website)) + "\">";
            return page("Review your product catalog", "Discovery", """
                    <main class="flow-main discovery-main">
                      %s
                      <p class="eyebrow">Setup review · %s</p>
                      <h1 class="catalog-review-title">Add products to finish setup.</h1>
                      %s
                      <p class="review-meta">%s · %s · %s · %s</p>
                      %s
                      %s
                      %s
                      <p class="demo-disclosure">Product facts help agents understand fit. Kaviyo cannot guarantee that an outside assistant will recommend a product.</p>
                      <a class="back-link" href="/discovery">← Update your business setup</a>
                    </main>
                    """.formatted(discoveryProgress("fit"), escape(business), banner,
                    escape(websiteHost(website)), escape(businessTypeLabel(businessType)),
                    escape(orEmpty(accountEmail)), escape(planLabel(accountPlan)),
                    goalsSection(goals), missionList(missions), productsForm(hidden)));
        }
        StringBuilder rows = new StringBuilder();
        for (int index = 0; index < products.size() && index < MAX_REVIEW_ROWS_SHOWN; index++) {
            CatalogItem product = products.get(index);
            rows.append("<li><strong>").append(escape(product.name())).append("</strong><span>")
                    .append(escape(product.facts())).append("</span></li>");
        }
        if (products.size() > MAX_REVIEW_ROWS_SHOWN) {
            rows.append("<li><span>…and ").append(products.size() - MAX_REVIEW_ROWS_SHOWN)
                    .append(" more ready products (all ").append(products.size()).append(" will import).</span></li>");
        }
        StringBuilder matches = new StringBuilder();
        for (String mission : missions) {
            var matching = products.stream().filter(item -> nameMatches(item, mission)).toList();
            matches.append("<section><h2>Products for “").append(escape(mission)).append("”</h2>");
            if (matching.isEmpty()) matches.append("<p>No ready product name matches this question. You can still choose a product from your catalog.</p>");
            else {
                matches.append("<ul class='catalog-review-list'>");
                matching.stream().limit(10).forEach(item -> matches.append("<li><strong>").append(escape(item.name())).append("</strong></li>"));
                matches.append("</ul>");
                if (matching.size() > 10) matches.append("<p>Showing 10 of ").append(matching.size()).append(" matching names.</p>");
            }
            matches.append("</section>");
        }
        String savePanel = saved
                ? "<p class=\"onboarding-status\" role=\"status\">" + escape(orEmpty(message)) + "</p>"
                    + "<section class=\"pipeline-strip\" aria-label=\"Your pipeline\"><p class=\"step-label\">Your pipeline</p><ol>"
                    + "<li class=\"is-done\"><strong>Catalog confirmed</strong><span>" + totalRows + " "
                    + (totalRows == 1 ? "product" : "products") + " saved as your source of truth.</span></li>"
                    + "<li><strong>Check your website</strong><span><a href=\"/discovery/run\">Open the live scan and watch each page get checked</a></span></li>"
                    + "<li><strong>Run a simulation</strong><span><a href=\"/discovery/simulation?session=" + query(sessionId)
                    + "\">Compare your products against the web</a></span></li>"
                    + "<li><strong>Measure results</strong><span><a href=\"/discovery/observe\">Track your goals over time</a></span></li>"
                    + "</ol></section>"
                : needsReview > 0
                    ? "<p class=\"onboarding-status onboarding-error\" role=\"alert\">" + needsReview
                        + " row(s) need correction or removal. Update the CSV and upload it again before saving.</p>"
                    : "<p class=\"onboarding-status\" role=\"status\">" + escape(orEmpty(message)) + "</p>"
                    + "<form action=\"/discovery/confirm\" method=\"post\"><input type=\"hidden\" name=\"session\" value=\"" + escape(sessionId)
                    + "\"><button class=\"action-link\" type=\"submit\">Finish setup and start investigation</button></form>";
        return page("Review your product catalog", "Discovery", """
                <main class="flow-main discovery-main">
                  %s
                  <p class="eyebrow">Product catalog · %s</p>
                  <h1 class="catalog-review-title">Your catalog is ready to review.</h1>
                  %s
                  <p class="review-meta">%s · %s · %s · %s</p>
                  <div class="catalog-review-summary"><strong>%d %s found</strong><span>%d ready for this preview · %d need review</span><span>Source of truth for price and availability: %s</span></div>
                  %s
                  %s
                  <details><summary>Inspect uploaded catalog rows</summary><ul class="catalog-review-list">%s</ul></details>
                  %s
                  <a class="back-link" href="/discovery">← Update your business setup</a>
                </main>
                """.formatted(discoveryProgress("fit"), escape(business), missionList(missions),
                escape(websiteHost(website)), escape(businessTypeLabel(businessType)),
                escape(orEmpty(accountEmail)), escape(planLabel(accountPlan)),
                totalRows, totalRows == 1 ? "product" : "products", totalRows - needsReview, needsReview,
                escape(truthSourceLabel(truthSource)), goalsSection(goals), matches, rows, savePanel));
    }

    private static boolean nameMatches(CatalogItem item, String mission) {
        var names = java.util.Arrays.asList(item.name().toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+"));
        var ignored = java.util.Set.of("the","for","with","and","what","which","need","best","product","products","find","that","under","can","are","buy");
        return java.util.Arrays.stream(orEmpty(mission).toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() > 1 && !ignored.contains(word))
                .anyMatch(word -> names.contains(word) || names.contains(word + "s")
                        || (word.endsWith("s") && names.contains(word.substring(0, word.length()-1))));
    }

    private static String goalsSection(List<String> goals) {
        if (goals == null || goals.isEmpty()) return "";
        return "<p class=\"review-goals\">Tracking: " + goals.stream().map(AuditFlowRenderer::escape)
                .collect(java.util.stream.Collectors.joining(" · ")) + "</p>";
    }

    private static final int MAX_REVIEW_ROWS_SHOWN = 100;

    private static String missionList(List<String> missions) {
        StringBuilder list = new StringBuilder("<ol class=\"review-missions\">");
        for (int index = 0; index < missions.size(); index++) {
            list.append("<li><span class=\"mission-count\">Need ").append(index + 1).append("</span>")
                    .append("<blockquote class=\"review-mission\" title=\"").append(escape(missions.get(index))).append("\">")
                    .append(escape(missions.get(index))).append("</blockquote></li>");
        }
        return list.append("</ol>").toString();
    }

    public record CatalogItem(String name, String facts, String url) {
        public CatalogItem(String name, String facts) { this(name, facts, null); }
    }

    private static String truthSourceLabel(String truthSource) {
        return switch (truthSource == null ? "" : truthSource) {
            case "WEBSITE" -> "Your website";
            case "OTHER" -> "Another system (ERP, feed, marketplace)";
            default -> "This catalog upload";
        };
    }

    private static String businessTypeLabel(String businessType) {
        return switch (businessType == null ? "" : businessType) {
            case "B2B" -> "Sells to businesses";
            case "BOTH" -> "Sells direct and to businesses";
            case "OTHER" -> "Another product business";
            default -> "Sells direct to customers";
        };
    }

    private static String planLabel(String accountPlan) {
        return "GROWTH".equals(accountPlan) ? "Growth plan" : "Starter plan";
    }

    private static String websiteHost(String website) {
        if (website == null || website.isBlank()) return "No website added";
        try {
            String host = java.net.URI.create(website.trim()).getHost();
            if (host != null && !host.isBlank()) return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException ignored) { }
        String raw = website.trim();
        return raw.length() > 48 ? raw.substring(0, 48) + "…" : raw;
    }

    public String renderSimulation(String business, String mission, List<CatalogItem> products) {
        return renderSimulation(business, null, mission, products);
    }

    public String renderSimulation(String business, String website, String mission, List<CatalogItem> products) {
        List<CatalogItem> items = products == null ? List.of() : products;
        boolean empty = items.isEmpty()
                && (business == null || business.isBlank())
                && (mission == null || mission.isBlank());
        if (empty) {
            return page("Simulated agent answer", "Discovery", """
                    <main class="flow-main discovery-main">
                      <p class="eyebrow">Controlled shopping agent</p>
                      <h1>Bring your products to simulate.</h1>
                      <p class="flow-intro">Simulations run on your catalog and your shopper need — never on sample data. Set up your business first, then come back to preview an agent answer.</p>
                      <a class="action-link" href="/discovery">Set up your business</a>
                    </main>
                    """);
        }
        StringBuilder options = new StringBuilder();
        int suggested = -1;
        for (int i = 0; i < items.size(); i++) {
            if (nameMatches(items.get(i), mission)) { suggested = i; break; }
        }
        if (suggested < 0 && items.size() > 1) options.append("<option value='' selected disabled>Choose a catalog product</option>");
        for (int i = 0; i < items.size(); i++) {
            options.append("<option value='").append(i).append(i == suggested ? "' selected>" : "'>").append(escape(items.get(i).name())).append("</option>");
        }
        String payload = """
                <main id="simulation-workspace" class="flow-main discovery-main" data-business="%s" data-website="%s" data-mission="%s" data-products="%s">
                  %s
                  <style>
                    #simulation-workspace {max-width:58rem;margin-inline:auto;padding:clamp(1rem,4vw,3rem);font:1rem/1.55 system-ui}
                    #simulation-workspace h1 {font-size:clamp(2rem,5vw,3.4rem);line-height:1.08;margin:.25em 0}
                    #simulation-workspace h2 {font-size:1.45rem;margin:.35em 0}
                    .review-step {padding:1.35rem 0;border-top:1px solid currentColor;margin-top:1.3rem}
                    .review-step label {display:grid;gap:.4rem;font-weight:600;margin:.85rem 0}
                    .review-step :is(select,input,textarea),#simulation-result textarea {box-sizing:border-box;width:100%%;border:1px solid #82888e;border-radius:.4rem;background:Canvas;color:CanvasText;padding:.7rem;font:inherit}
                    .review-step summary {cursor:pointer}.review-step button {margin-top:.8rem}
                    #simulation-workspace [hidden] {display:none}
                    #simulation-workspace .simulation-result {max-width:none;overflow-wrap:anywhere}
                    #simulation-result .setup-profile {display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%%,16rem),1fr));gap:1rem}
                    #simulation-result article {border:1px solid #85858c;border-radius:.5rem;padding:1rem}
                  </style>
                  <header><small>%s</small><h1>Check your product page</h1><p>Shopper question · %s</p></header>
                  <section class="review-step"><small>Optional price check</small><h2>Compare another seller’s price</h2>
                  <label>Product from your catalog<select id="comparison-product">%s</select></label>
                  <p id="listing-guidance">Paste a link to a listing for the same or a similar item. Kaviyo compares that listing with your catalog. You can skip this and go straight to your website.</p><label>Other seller’s product link<textarea id="listing-urls" rows="2" placeholder="https://seller.com/product"></textarea></label>
                  <button id="run-simulation" class="action-link" type="button">Compare prices</button>
                  <div id="market-report" aria-live="polite"></div></section>
                  <section class="review-step"><small>Website check</small><h2>What does your page tell shoppers?</h2>
                  <p>Use your onboarding website, or enter the page you want Muse to review.</p>
                  <label>Merchant website<input id="merchant-website" type="url" value="%s" placeholder="https://your-store.com"></label>
                  <button id="review-site" class="action-link" type="button">Review website</button></section>
                  <section class="review-step"><small>Your decision</small><h2>Review the suggested wording</h2>
                  <p id="preview-guide">You’ll see text from your website beside any suggested change. You decide whether to save a draft. Your live website stays as it is.</p>
                  %s
                  <p><a href="/discovery/observe">Measure a real assistant answer</a></p>
                  <section id="simulation-result" class="simulation-result" aria-live="polite" tabindex="-1" hidden></section></section>
                </main>
                """.formatted(escape(orEmpty(business)), escape(orEmpty(website)), escape(orEmpty(mission)), escape(productsJson(items)),
                        discoveryProgress("simulate"), escape(orEmpty(business)), escape(orEmpty(mission)), options, escape(orEmpty(website)), reviewPolicy());
        String response = page("Simulated agent answer", "Discovery", payload);
        String script = "<script src=\"/assets/agent-simulation.js\" defer></script>";
        return response.replace("</body>", script + "</body>");
    }

    public String renderScanRun(String scanRunId) {
        String scan = scanRunId == null ? "" : scanRunId.trim();
        String startSection = scan.isEmpty() ? """
                <section id="run-start" class="run-start" aria-label="Start a scan">
                  <div><p class="step-label">Live run</p><h2>Scan your confirmed catalog.</h2>
                  <p id="run-start-note">Kaviyo fetches each saved product page, checks it against your catalog facts, and reports findings here as they land.</p>
                  <ul id="run-target-list" class="run-target-list" aria-label="Products this scan will check"></ul></div>
                  <button id="start-scan" class="action-link" type="button">Scan catalog now</button>
                </section>
                """ : "";
        String payload = """
                <main id="scan-run-workspace" class="flow-main discovery-main" data-scan="%s">
                  %s
                  <p class="eyebrow">Live catalog scan</p>
                  <header class="flow-heading discovery-heading" aria-labelledby="run-title">
                    <h1 id="run-title">Watch Kaviyo check your catalog.</h1>
                    <p class="flow-intro">Every fetch, finding, and decision appears below as it happens. This run only reads your pages; nothing here changes your store.</p>
                  </header>
                  %s
                  <section class="run-status" aria-label="Scan progress">
                    <div class="run-status-row"><span id="run-pill" class="run-pill" role="status">Ready</span><strong id="run-count">–</strong></div>
                    <div class="run-bar" aria-hidden="true"><span id="run-bar-fill"></span></div>
                    <p id="run-note" class="run-note">%s</p>
                  </section>
                  <section class="run-feed" aria-label="Live activity"><h2>Activity</h2><ol id="run-events" class="run-events" aria-live="polite"></ol></section>
                  <section class="run-findings" aria-label="Findings so far"><h2>Findings</h2><div id="run-finding-list"><p class="run-empty">Findings appear here as products finish.</p></div></section>
                  <section id="run-summary" class="run-summary" aria-live="polite" hidden></section>
                  <a class="back-link" href="/discovery">← Back to shopper missions</a>
                  <p class="demo-disclosure">A Kaviyo fetch says nothing about what outside assistants can access. Record real assistant answers separately under “Already saw an answer from an assistant?” on the simulation page.</p>
                </main>
                """.formatted(escape(scan), discoveryProgress("simulate"), startSection,
                scan.isEmpty() ? "Start a scan to watch each product get checked." : "Resuming live view for scan " + escape(scan) + ".");
        String response = page("Watch a live catalog scan", "Discovery", payload);
        String script = "<script src=\"/assets/scan-run.js\" defer></script>";
        return response.replace("</body>", script + "</body>");
    }

    public String renderDraftReview(String scanId, String productId, String findingCode) {
        String scan = orEmpty(scanId).trim();
        String product = orEmpty(productId).trim();
        String finding = orEmpty(findingCode).trim();
        if (scan.isEmpty() || product.isEmpty()) {
            String payload = """
                    <main class="flow-main discovery-main">
                      <p class="eyebrow">Draft review</p>
                      <h1>No finding selected.</h1>
                      <p class="flow-intro">Drafts and resolutions start from a scan finding. Run a catalog scan first, then choose “Draft a fix” or “Resolve” on a finding.</p>
                      <a class="action-link" href="/discovery/run">Go to the live scan</a>
                    </main>
                    """;
            return page("Review a proposed fix", "Discovery", payload);
        }
        boolean resolvable = "PRICE_MISMATCH".equals(finding) || "AVAILABILITY_MISMATCH".equals(finding);
        String payload;
        if (resolvable) {
            payload = """
                    <main id="draft-workspace" class="flow-main discovery-main" data-scan="%s" data-product="%s" data-finding="%s">
                      <p class="eyebrow">Guided resolution</p>
                      <h1>Resolve the mismatch.</h1>
                      <p class="flow-intro">Compare what your catalog says against what the page shows, then record which side to fix. Kaviyo changes nothing itself.</p>
                      <section class="draft-finding" aria-label="Finding"><h2>Finding</h2><div id="draft-finding-body"><p class="run-empty">Loading the finding…</p></div></section>
                      <section class="resolve-compare" aria-label="Side-by-side evidence"><h2>Catalog against page</h2><div id="resolve-evidence"><p class="run-empty">Loading both values…</p></div></section>
                      <section class="resolve-decide discovery-setup" aria-label="Record your decision">
                        <h2>Your decision</h2>
                        <label><input type="radio" name="resolve-choice" value="ACCEPT_CATALOG"> The page is wrong — I will update the page to match the catalog</label>
                        <label><input type="radio" name="resolve-choice" value="ACCEPT_PAGE"> The catalog is wrong — I will update the catalog to match the page</label>
                        <label><input type="radio" name="resolve-choice" value="DISMISSED"> Dismiss — no action needed</label>
                        <label>Note (optional)<input id="resolve-note" maxlength="2000" placeholder="e.g. Supplier confirmed the new price"></label>
                        <div><button id="resolve-submit" class="action-link" type="button">Record decision</button></div>
                        <p id="resolve-status" class="run-note" aria-live="polite"></p>
                      </section>
                      <section class="resolve-existing" aria-label="Recorded decision"><h2>Recorded decision</h2><div id="resolve-existing-body"><p class="run-empty">Loading any recorded decision…</p></div></section>
                      <a class="back-link" href="/discovery/run">← Back to the live scan</a>
                    </main>
                    """.formatted(escape(scan), escape(product), escape(finding));
        } else {
            payload = """
                    <main id="draft-workspace" class="flow-main discovery-main" data-scan="%s" data-product="%s" data-finding="%s">
                      <p class="eyebrow">Draft review</p>
                      <h1>Review the proposed fix.</h1>
                      <p class="flow-intro">A Muse draft rewrites one product description from confirmed catalog facts. Nothing is published: you approve, reject, or export every change.</p>
                      <section class="draft-finding" aria-label="Finding"><h2>Finding</h2><div id="draft-finding-body"><p class="run-empty">Loading the finding…</p></div></section>
                      <section class="draft-existing" aria-label="Existing drafts"><h2>Existing drafts</h2><div id="draft-existing-body"><p class="run-empty">Loading saved drafts…</p></div></section>
                      <section class="draft-create discovery-setup" aria-label="Draft a fix">
                        <h2>Draft a fix</h2>
                        <label>Shopper need this description should serve<input id="draft-mission" maxlength="600" placeholder="e.g. A laptop for photo editing with 32 GB of RAM"></label>
                        <div><button id="draft-submit" class="action-link" type="button">Draft a fix</button></div>
                        <p id="draft-create-note" class="run-note" aria-live="polite"></p>
                      </section>
                      <section id="draft-detail" class="draft-detail" aria-live="polite" hidden></section>
                      <a class="back-link" href="/discovery/run">← Back to the live scan</a>
                    </main>
                    """.formatted(escape(scan), escape(product), escape(finding));
        }
        String response = page("Review a proposed fix", "Discovery", payload);
        String script = "<script src=\"/assets/draft-review.js\" defer></script>";
        return response.replace("</body>", script + "</body>");
    }

    private static String reviewPolicy() {
        return """
                <details id="review-policy"><summary>Evidence and approval rules</summary>
                <p>The merchant owns the confirmed catalog facts. Kaviyo checks answers against those facts and flags conflicts. Unresolved claims stay unknown.</p>
                <p>A reviewer decides whether to approve a proposed edit or keep the original. Approval records the decision and evidence; publishing is a separate action.</p>
                <p>Only observed assistant answers count toward measured visibility. Keep model simulations separate.</p></details>
                """;
    }

    public String renderPriceCompare(String productId) {
        String product = orEmpty(productId).trim();
        if (product.isEmpty()) {
            String payload = """
                    <main class="flow-main discovery-main">
                      <p class="eyebrow">Outside price check</p>
                      <h1>No product selected.</h1>
                      <p class="flow-intro">Price checks start from a confirmed product. Open the live scan and choose “Compare outside prices” on any listed product.</p>
                      <a class="action-link" href="/discovery/run">Go to the live scan</a>
                    </main>
                    """;
            return page("Compare outside prices", "Discovery", payload);
        }
        String payload = """
                <main id="compare-workspace" class="flow-main discovery-main" data-product="%s">
                  <p class="eyebrow">Outside price check</p>
                  <h1>How does this price compare out there?</h1>
                  <p class="flow-intro">Paste up to three pages where this product is sold. Kaviyo fetches each one once, reads its price and availability, and compares against your catalog — nothing is stored.</p>
                  <section class="discovery-status compare-baseline" aria-label="Your catalog baseline">
                    <div><span class="status-label">Your catalog baseline</span><strong id="compare-baseline">Loading…</strong><small id="compare-baseline-note">Merchant-supplied facts for this product</small></div>
                  </section>
                  <section class="compare-sources discovery-setup" aria-label="Outside sources">
                    <h2>Pages to check</h2>
                    <label>Page 1<input id="compare-url-1" type="url" maxlength="2000" placeholder="https://retailer.example/products/…"></label>
                    <label>Page 2 (optional)<input id="compare-url-2" type="url" maxlength="2000" placeholder="https://another-store.example/…"></label>
                    <label>Page 3 (optional)<input id="compare-url-3" type="url" maxlength="2000" placeholder="https://marketplace.example/…"></label>
                    <div><button id="compare-run" class="action-link" type="button">Run comparison</button></div>
                    <p id="compare-status" class="run-note" aria-live="polite"></p>
                  </section>
                  <section class="compare-results" aria-label="Comparison results"><h2>Results</h2><div id="compare-results"><p class="run-empty">Results appear here after you run a comparison.</p></div></section>
                  <p class="demo-disclosure">Each page is fetched once on demand and reflects that moment only. Pages without Product structured data report unknown instead of guessed values.</p>
                  <a class="back-link" href="/discovery/run">← Back to the live scan</a>
                </main>
                """.formatted(escape(product));
        String response = page("Compare outside prices", "Discovery", payload);
        String script = "<script src=\"/assets/price-compare.js\" defer></script>";
        return response.replace("</body>", script + "</body>");
    }

    public String renderObservationCapture(String missionId) {
        // Default mission; the page script replaces the options with live workspace missions on load.
        String selectedMission = "00000000-0000-4000-8000-000000000101";
        if (missionId != null && !missionId.isBlank()) {
            try {
                selectedMission = java.util.UUID.fromString(missionId.trim()).toString();
            } catch (IllegalArgumentException ignored) {
            }
        }
        return page("Record an observation", "Discovery", """
                <main class="flow-main discovery-main">
                  %s
                  <p class="eyebrow">Product discovery · optional measurement</p>
                  <header class="flow-heading discovery-heading" aria-labelledby="capture-title">
                    <h1 id="capture-title">Measure AI visibility.</h1>
                    <p class="flow-intro">Save the exact answer a shopper received. Check whether it mentions your product and whether its claims match your catalog. Repeat the same question after a page change to track the result.</p>
                  </header>
                  <p class="api-note" id="discovery-api-status" role="status" aria-live="polite">Saved observations require the Kaviyo API and database.</p>
                  <section class="discovery-status observation-metrics" aria-label="Observation summary">
                    <div><span class="status-label">Observed answers</span><strong id="observed-count">Loading…</strong><small>Manual records from this workspace</small></div>
                    <div><label for="visibility-target">Target catalog reference</label><input id="visibility-target" placeholder="Enter product SKU"><span class="status-label">Target product mentions</span><strong id="mention-count">Loading…</strong><small>Mentions in recorded answers</small></div>
                    <div><span class="status-label">Claim checks</span><strong id="claim-summary">Loading…</strong><small>Compared with saved product facts</small></div>
                  </section>
                  <section class="discovery-status observation-trend" aria-label="Weekly trend">
                    <div><span class="status-label">Mention rate by week</span><div id="trend-mentions" class="trend-chart" role="img" aria-label="Weekly mention rate chart">Loading…</div><small>Share of recorded answers with a product mention</small></div>
                    <div><span class="status-label">Accuracy rate by week</span><div id="trend-accuracy" class="trend-chart" role="img" aria-label="Weekly accuracy rate chart">Loading…</div><small>Supported claims out of assessed claims</small></div>
                  </section>
                  <details><summary>Use Muse to help fill in the observation</summary><section class="observation-step extract-panel" aria-label="Assist with a pasted answer">
                    <div><p class="step-label">Optional assist</p><h2>Paste an answer, get a head start.</h2><p>Spark reads the pasted answer and suggests mentions, claims, and verdicts against your catalog. Nothing saves until you review and submit below.</p></div>
                    <label>Assistant or source<input id="extract-provider" maxlength="80" placeholder="Meta AI, ChatGPT, Gemini…"></label>
                    <label>Prompt used, if any<textarea id="extract-prompt" maxlength="2000" rows="2"></textarea></label>
                    <label>Answer you saw<textarea id="extract-answer" maxlength="20000" rows="5"></textarea></label>
                    <div><button id="extract-run" class="action-link" type="button">Suggest structure</button></div>
                    <p id="extract-status" class="run-note" aria-live="polite"></p>
                    <div id="extract-results"></div>
                  </section></details>
                  <section class="capture-panel capture-step" aria-label="Observation details">
                    <form id="observation-form">
                      <label>Shopper mission<select id="mission-id" required>
                        <option value="%s">Loading missions…</option>
                      </select></label>
                      <label>Assistant or source<input id="provider" maxlength="80" placeholder="Meta AI, ChatGPT, Gemini…" required><small class="field-note">Any assistant works — Kaviyo records your pasted answer; it never queries assistants.</small></label>
                      <label>Exact prompt used<textarea id="prompt" maxlength="2000" rows="2" required></textarea></label>
                      <label>Answer you saw<textarea id="answer" maxlength="20000" rows="5" required></textarea></label>
                      <div class="capture-grid"><label>Product mentioned (optional)<input id="product-name" maxlength="500"></label><label>Product reference, if known<input id="product-ref" maxlength="240" placeholder="Catalog product ID"></label></div>
                      <div class="capture-grid"><label>Position in answer<input id="position" type="number" min="1" inputmode="numeric"></label><label>Observed at<input id="observed-at" type="datetime-local" required></label></div>
                      <label>Source URL, if cited<input id="source-url" type="url" maxlength="2000"></label>
                      <details class="claim-details"><summary>Check a product claim against saved evidence (optional)</summary>
                        <div class="claim-fields"><label>Claim to check<input id="claim-text" maxlength="1200"></label><label>Assessment<select id="claim-verdict"><option value="UNKNOWN">Unknown</option><option value="SUPPORTED">Supported by saved fact</option><option value="CONTRADICTED">Contradicted</option><option value="STALE">Out of date</option></select></label>
                        <label>Evidence reference<input id="evidence-reference" maxlength="1000" placeholder="Catalog field or page URL"></label><label>Confirmed source value<input id="evidence-value" maxlength="1200"></label></div>
                      </details>
                      <button class="action-link" type="submit">Save observed answer</button><p id="capture-status" role="status" aria-live="polite"></p>
                    </form>
                  </section>
                  <section class="observation-ledger observation-step" aria-labelledby="ledger-title"><div><p class="step-label">This mission</p><h2 id="ledger-title">Saved observations</h2><p id="observation-summary">Loading saved observations…</p></div><div id="observation-list"><p>Observation history will appear here when the API is connected.</p></div><button id="load-more-observations" class="load-more-observations" type="button" hidden>Load older observations</button></section>
                  <a class="back-link" href="/discovery">← Back to shopper missions</a>
                  <p class="demo-disclosure">Manual records reflect only the answers you enter. Model simulations are diagnostic and are never counted as actual assistant mentions.</p>
                </main>
                """.formatted(reviewPolicy() + "<details><summary>Workspace accuracy and contradiction alerts</summary><div id=\"governance-alerts\"></div><div id=\"governance-accuracy\"></div></details>", escape(selectedMission)));
    }

    public String renderHowItWorks() {
        return page("How Kaviyo works", "How it works", """
                <main class="flow-main overview-main">
                  <p class="eyebrow">A product discovery workspace</p>
                  <h1>Get your products discovered by shopping agents.</h1>
                  <p class="flow-intro">Tell Kaviyo about your business, connect your products, and watch as it checks your site, compares your products against the web, and shows you exactly what to fix.</p>
                  <div class="overview-steps">
                    <article><span>01 · Onboard</span><h2>Describe your business.</h2><p>Add your company profile, set measurable goals, and connect WooCommerce, Wix, or a CSV. Your catalog becomes the source of truth.</p></article>
                    <article><span>02 · Pipeline</span><h2>Watch the investigation.</h2><p>Kaviyo crawls your product pages and checks every detail against your catalog, live, as findings land.</p></article>
                    <article><span>03 · Simulations</span><h2>Compare against the web.</h2><p>Run Muse simulations that weigh your products against public listings, then review suggested wording before anything changes.</p></article>
                    <article><span>04 · Measure</span><h2>Track discovery over time.</h2><p>Record real assistant answers and watch mention and accuracy rates move toward your goals.</p></article>
                  </div>
                  <a class="action-link" href="/discovery">Onboard your business <span aria-hidden="true">↗</span></a>
                </main>
                """);
    }

    public String renderSourceStep(List<Product> products) {
        return page("Choose sample data", "Try the demo", """
                <main class="flow-main flow-main--sample">
                  %s
                  <header class="flow-heading" aria-labelledby="flow-title">
                    <p class="step-label">Sample audit · Choose data</p>
                    <h1 id="flow-title">See how a product detail becomes a decision.</h1>
                    <p class="flow-intro">Start with a fictional catalog and saved product pages. Kaviyo compares the details, shows its evidence, and gives you a review step—without connecting to a store.</p>
                  </header>
                  <div class="sample-workspace">
                    <section class="sample-catalog" aria-labelledby="catalog-title">
                      <header class="catalog-caption">
                        <div><p class="catalog-source">Kaviyo sample merchant</p><h2 id="catalog-title">Products in this walkthrough</h2></div>
                        <span>Fictional data</span>
                      </header>
                      <div class="catalog-table-wrap">
                        <table class="sample-product-table">
                          <thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">Catalog price</th></tr></thead>
                          <tbody>%s</tbody>
                        </table>
                      </div>
                    </section>
                    <aside class="flow-next-step" aria-labelledby="next-title">
                      <p class="step-label">What you will see</p>
                      <h2 id="next-title">A useful review, grounded in sources.</h2>
                      <ul class="flow-preview-list">
                        <li><strong>Catalog facts</strong><span>The merchant's reference details</span></li>
                        <li><strong>Saved product pages</strong><span>Evidence to compare against them</span></li>
                        <li><strong>Audit findings</strong><span>Gaps and conflicts for a person to verify</span></li>
                      </ul>
                      <a class="action-link" href="/audit/review">Continue with sample catalog <span aria-hidden="true">↗</span></a>
                    </aside>
                  </div>
                  <p class="demo-disclosure">Synthetic walkthrough only. No account is needed; no live store is scanned and no assistant ranking is measured.</p>
                </main>
                """.formatted(progress("Choose data"), productRows(products)));
    }

    public String renderReviewStep(List<Product> products) {
        return page("Review audit scope", "Try the demo", """
                <main class="flow-main flow-main--sample">
                  %s
                  <header class="flow-heading" aria-labelledby="flow-title">
                    <p class="step-label">Sample audit · Review scope</p>
                    <h1 id="flow-title">Here is what Kaviyo will compare.</h1>
                    <p class="flow-intro">The catalog is the source of product facts. Each saved page snapshot is evidence to compare against those facts.</p>
                  </header>
                  <div class="sample-workspace">
                    <section class="sample-catalog" aria-labelledby="included-title">
                      <header class="catalog-caption">
                        <div><p class="catalog-source">Included products</p><h2 id="included-title">%d fictional items</h2></div>
                        <span>Catalog reference</span>
                      </header>
                      <div class="catalog-table-wrap">
                        <table class="sample-product-table">
                          <thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">Catalog price</th></tr></thead>
                          <tbody>%s</tbody>
                        </table>
                      </div>
                    </section>
                    <aside class="flow-next-step" aria-labelledby="next-title">
                      <p class="step-label">Comparison scope</p>
                      <h2 id="next-title">Catalog records against saved evidence.</h2>
                      <dl class="scope-details">
                        <div><dt>Catalog</dt><dd>%d product records</dd></div>
                        <div><dt>Page evidence</dt><dd>Saved synthetic snapshots</dd></div>
                        <div><dt>Live store requests</dt><dd>None</dd></div>
                      </dl>
                      <a class="action-link" href="/audit/results">Run the sample audit <span aria-hidden="true">↗</span></a>
                    </aside>
                  </div>
                  <p class="demo-disclosure">The snapshots contain fictional mismatches and missing details for review. They do not describe a live shop or how an assistant ranks products.</p>
                </main>
                """.formatted(progress("Review scope"), products.size(), productRows(products), products.size()));
    }

    private static String productRows(List<Product> products) {
        StringBuilder rows = new StringBuilder();
        for (Product product : products) {
            String price = product.price() == null
                    ? "Not listed"
                    : escape(product.currency()) + " " + escape(product.price().stripTrailingZeros().toPlainString());
            rows.append("<tr><th scope=\"row\">").append(escape(product.title()))
                    .append("<small>").append(escape(product.brand())).append("</small></th><td>")
                    .append(escape(product.category())).append("</td><td>").append(price).append("</td></tr>");
        }
        return rows.toString();
    }

    private static final String FLOW_STYLES = """
            body.flow-page { min-height: 100vh; background: radial-gradient(ellipse at 82% 8%, rgba(65,161,207,.13), transparent 27%), #fefffc; color: #171717; }
            .flow-page .site-header { width: min(1240px, calc(100% - 64px)); min-height: 62px; margin: 18px auto 0; padding: 8px 0; border: 0; border-bottom: 1px solid #dee2de; border-radius: 0; background: transparent; }
            .flow-page .brand { color: #171717; }
            .flow-page .brand-mark { border-radius: 5px; background: #41a1cf; color: #fff; }
            .flow-page .site-header nav { gap: 19px; }
            .flow-page .site-header nav a { color: #444141; font-size: 12px; }
            .flow-page .site-header nav a:hover, .flow-page .site-header nav a:focus-visible { color: #0081c0; }
            .flow-page .nav-action { border: 0; border-radius: 4px; background: #282834; color: #fff; }
            .flow-main--sample { width: min(1240px, calc(100% - 64px)); max-width: none; margin: 28px auto 76px; }
            .flow-page .flow-progress { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: clamp(12px, 3vw, 36px); margin: 0 0 14px; color: #858985; font-size: 12px; }
            .flow-page .flow-progress span { position: static; padding: 0 0 12px; border-bottom: 1px solid #dee2de; }
            .flow-page .flow-progress span:not(:last-child)::after { content: none; }
            .flow-page .flow-progress .active { border-bottom: 2px solid #0081c0; color: #171717; }
            .flow-heading { padding: clamp(34px, 6vw, 72px) 0 0; }
            .flow-page .step-label { margin: 0 0 13px; color: #0081c0; font-size: 12px; font-weight: 600; }
            .flow-heading h1 { max-width: 890px; margin: 0 0 17px; color: #171717; font-family: "Iowan Old Style", Georgia, serif; font-size: clamp(48px, 7vw, 78px); font-weight: 400; letter-spacing: -.055em; line-height: .98; }
            .flow-page .flow-intro { max-width: 680px; margin: 0; color: #646464; font-size: 16px; line-height: 1.7; }
            .sample-workspace { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, .8fr); align-items: start; gap: clamp(32px, 6vw, 82px); margin-top: clamp(38px, 6vw, 66px); }
            .sample-catalog { min-width: 0; border-top: 1px solid #b4b8b4; border-bottom: 1px solid #b4b8b4; }
            .catalog-caption { display: flex; align-items: end; justify-content: space-between; gap: 16px; padding: 18px 0 16px; }
            .catalog-source { margin: 0 0 4px; color: #0081c0; font-size: 11px; }
            .catalog-caption h2 { margin: 0; font-size: 22px; font-weight: 550; letter-spacing: -.035em; }
            .catalog-caption > span { color: #646464; font-size: 11px; }
            .catalog-table-wrap { overflow-x: auto; }
            .sample-product-table { width: 100%; border-collapse: collapse; text-align: left; }
            .sample-product-table thead { border-top: 1px solid #dee2de; border-bottom: 1px solid #dee2de; }
            .sample-product-table th, .sample-product-table td { padding: 14px 12px 14px 0; vertical-align: top; }
            .sample-product-table thead th { color: #646464; font-size: 11px; font-weight: 500; }
            .sample-product-table tbody tr + tr { border-top: 1px solid #e8ebe8; }
            .sample-product-table tbody th { min-width: 200px; color: #171717; font-size: 13px; font-weight: 600; }
            .sample-product-table tbody th small { display: block; margin-top: 3px; color: #858985; font-size: 10px; font-weight: 400; }
            .sample-product-table tbody td { color: #444141; font-size: 12px; white-space: nowrap; }
            .flow-next-step { padding: 20px 0 0 25px; border-left: 3px solid #41a1cf; }
            .flow-next-step h2 { max-width: 340px; margin: 0 0 17px; font-family: "Iowan Old Style", Georgia, serif; font-size: clamp(28px, 3vw, 38px); font-weight: 400; letter-spacing: -.045em; line-height: 1.04; }
            .flow-preview-list { margin: 0 0 23px; padding: 0; list-style: none; }
            .flow-preview-list li { display: grid; gap: 2px; padding: 11px 0; border-top: 1px solid #dee2de; }
            .flow-preview-list strong { color: #171717; font-size: 12px; font-weight: 600; }
            .flow-preview-list span { color: #646464; font-size: 11px; line-height: 1.5; }
            .scope-details { margin: 3px 0 22px; border-top: 1px solid #dee2de; }
            .scope-details div { display: flex; justify-content: space-between; gap: 12px; padding: 11px 0; border-bottom: 1px solid #dee2de; font-size: 11px; }
            .scope-details dt { color: #646464; }
            .scope-details dd { margin: 0; color: #171717; text-align: right; }
            .flow-page .action-link { min-height: 46px; padding: 11px 15px; border: 0; border-radius: 4px; background: #0081c0; color: #fff; font-size: 12px; font-weight: 600; text-decoration: none; }
            .flow-page .action-link:hover { background: #006b9e; }
            .flow-page .action-link:focus-visible { outline-color: #171717; }
            .flow-page .demo-disclosure { max-width: 790px; margin: 26px 0 0; padding-top: 14px; border-top: 1px solid #dee2de; color: #646464; font-size: 11px; line-height: 1.6; }
            .flow-page .flow-footer { width: min(1240px, calc(100% - 64px)); margin-top: 0; }
            .discovery-main { width: min(1240px, calc(100% - 64px)); max-width: none; margin: 28px auto 76px; }
            .discovery-progress { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 8px 0 28px; border-bottom: 1px solid #dee2de; }
            .discovery-progress { padding: 0; list-style: none; }
            .discovery-progress li { display: flex; align-items: center; gap: 9px; min-height: 43px; border-bottom: 2px solid transparent; color: #777d79; font-size: 12px; }
            .discovery-progress li[aria-current="step"] { border-color: #0081c0; color: #171717; font-weight: 600; }
            .discovery-progress li span { display: grid; width: 22px; height: 22px; place-items: center; border: 1px solid #cbd0cb; border-radius: 50%; font-size: 10px; }
            .discovery-progress li[aria-current="step"] span, .discovery-progress li.is-complete span { border-color: #0081c0; background: #0081c0; color: #fff; }
            .discovery-progress li.is-complete { color: #315d49; }
            .discovery-progress { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin: 8px 0 28px; border-bottom: 1px solid #dee2de; }
            .discovery-progress a { display: flex; align-items: center; gap: 9px; min-height: 43px; border-bottom: 2px solid transparent; color: #777d79; font-size: 12px; text-decoration: none; }
            .discovery-progress a[aria-current="step"] { border-color: #0081c0; color: #171717; font-weight: 600; }
            .discovery-progress a span { display: grid; width: 22px; height: 22px; place-items: center; border: 1px solid #cbd0cb; border-radius: 50%; font-size: 10px; }
            .discovery-progress a[aria-current="step"] span, .discovery-progress a.is-complete span { border-color: #0081c0; background: #0081c0; color: #fff; }
            .discovery-progress a.is-complete { color: #315d49; }
            .discovery-heading { padding-top: 48px; }
            .discovery-heading h1 { max-width: 850px; margin: 0 0 17px; color: #171717; font-family: "Iowan Old Style", Georgia, serif; font-size: clamp(48px, 7vw, 76px); font-weight: 400; letter-spacing: -.055em; line-height: .98; }
            .discovery-status { display: grid; grid-template-columns: repeat(3, 1fr); margin-top: 38px; border-top: 1px solid #b4b8b4; border-bottom: 1px solid #b4b8b4; }
            .discovery-status > div { display: grid; align-content: start; gap: 6px; padding: 16px 20px 18px 0; }
            .discovery-status > div + div { padding-left: 20px; border-left: 1px solid #dee2de; }
            .status-label, .section-heading > span { color: #646464; font-size: 10px; }
            .discovery-status strong { color: #171717; font-size: 14px; font-weight: 600; }
            .discovery-status small { max-width: 280px; color: #646464; font-size: 11px; line-height: 1.5; }
            .observation-trend { grid-template-columns: repeat(2, 1fr); margin-top: 0; border-top: 0; }
            .trend-chart { min-height: 56px; color: #646464; font-size: 11px; }
            .trend-chart svg { display: block; width: 100%; max-width: 260px; height: auto; }
            .trend-chart polyline { fill: none; stroke-width: 2; }
            .trend-line--mentions polyline { stroke: #0081c0; }
            .trend-line--mentions circle { fill: #0081c0; }
            .trend-line--accuracy polyline { stroke: #1a7a4a; }
            .trend-line--accuracy circle { fill: #1a7a4a; }
            .compare-baseline { grid-template-columns: 1fr; }
            .compare-sources { margin-top: 30px; }
            .compare-sources h2, .compare-results h2 { margin: 0; font-size: 22px; }
            .compare-results { margin-top: 30px; display: grid; gap: 12px; max-width: 780px; }
            .compare-results .run-finding { display: grid; gap: 6px; }
            .compare-results .run-finding p { margin: 0; }
            .governance-statement, .governance-policy, .governance-alerts, .governance-accuracy { max-width: 780px; margin-top: 30px; display: grid; gap: 12px; }
            .governance-statement h2, .governance-policy h2, .governance-alerts h2, .governance-accuracy h2 { margin: 0; font-size: 22px; }
            .governance-statement p { margin: 0; color: rgba(255,255,255,.78); font-size: 14px; line-height: 1.65; }
            .policy-table { width: 100%; border-collapse: collapse; font-size: 12px; }
            .policy-table th, .policy-table td { padding: 9px 12px 9px 0; text-align: left; vertical-align: top; border-top: 1px solid rgba(255,255,255,.2); color: rgba(255,255,255,.78); }
            .policy-table thead th { border-top: 0; color: rgba(255,255,255,.55); font-size: 11px; letter-spacing: .04em; text-transform: uppercase; font-weight: 600; }
            .policy-table tbody tr:last-child td { border-bottom: 1px solid rgba(255,255,255,.2); }
            .flow-footer a { color: #8dd7ef; font-size: 12px; text-underline-offset: 3px; }
            .mission-layout { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(270px, .65fr); gap: clamp(30px, 6vw, 82px); margin-top: 58px; }
            .section-heading { display: flex; align-items: end; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
            .section-heading h2 { margin: 0; font-family: "Iowan Old Style", Georgia, serif; font-size: clamp(26px, 3vw, 36px); font-weight: 400; letter-spacing: -.04em; }
            .mission-row { display: grid; grid-template-columns: 36px 1fr auto; align-items: center; gap: 13px; padding: 17px 0; border-top: 1px solid #dee2de; color: inherit; text-decoration: none; }
            .mission-row:last-child { border-bottom: 1px solid #dee2de; }
            .mission-row:hover strong, .mission-row:focus-visible strong { color: #0081c0; }
            .mission-picker { max-width: 940px; margin-top: 46px; }
            .mission-picker .section-heading { margin-bottom: 18px; }
            .mission-choose { color: #0081c0; font-size: 11px; white-space: nowrap; }
            .mission-intro-note { max-width: 720px; margin-top: 56px; }
            .back-link { display: inline-block; margin-top: 24px; color: #006f9f; font-size: 12px; text-underline-offset: 3px; }
            .api-note { max-width: 850px; margin: 22px 0 0; padding: 12px 15px; border-left: 3px solid #c58a43; background: #f7f5ef; color: #594a35; font-size: 12px; line-height: 1.5; }
            .capture-step { grid-template-columns: minmax(0, 1fr); max-width: 930px; margin-top: 20px; padding: 0; border: 0; }
            .capture-step #observation-form { grid-template-columns: 1fr 1fr; gap: 17px 20px; }
            .capture-step #observation-form > label:nth-of-type(3), .capture-step #observation-form > label:nth-of-type(4), .capture-step #observation-form .claim-details, .capture-step #observation-form > button, .capture-step #capture-status { grid-column: 1 / -1; }
            .claim-details { padding: 13px 0; border-top: 1px solid #dee2de; border-bottom: 1px solid #dee2de; }
            .claim-details summary { color: #315d49; font-size: 12px; cursor: pointer; }
            .claim-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; margin-top: 15px; }
            .observation-step { max-width: 930px; margin-top: 42px; padding-top: 20px; border-top: 1px solid #b4b8b4; }
            .extract-panel { display: grid; gap: 12px; }
            .extract-panel label { display: grid; gap: 6px; font-size: 12px; font-weight: 550; }
            .extract-panel input, .extract-panel textarea { width: 100%; padding: 10px; border: 1px solid #cbd0cb; border-radius: 3px; background: #fff; color: #171717; font: inherit; font-size: 12px; }
            .extract-panel textarea { resize: vertical; line-height: 1.5; }
            .extract-suggestion { border-top: 1px solid #dee2de; padding: 10px 0; display: grid; gap: 4px; }
            .extract-suggestion p { margin: 0; font-size: 12px; }
            .extract-suggestion .action-link { justify-self: start; }
            .observation-step > div > p:not(.step-label) { color: #646464; font-size: 12px; }
            .mission-number { color: #0081c0; font-family: Georgia, serif; font-size: 18px; }
            .mission-row strong, .mission-row small { display: block; }
            .mission-row strong { color: #171717; font-size: 13px; font-weight: 600; line-height: 1.5; }
            .mission-row small { margin-top: 3px; color: #646464; font-size: 10px; }
            .discovery-next h2 { margin: 0 0 12px; font-family: "Iowan Old Style", Georgia, serif; font-size: clamp(27px, 3.4vw, 39px); font-weight: 400; letter-spacing: -.04em; line-height: 1.05; }
            .discovery-next p:not(.step-label) { color: #646464; font-size: 12px; line-height: 1.7; }
            .secondary-missions { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; }
            .secondary-missions article { padding: 20px 0; }
            .secondary-missions article + article { padding-left: 24px; border-left: 1px solid #dee2de; }
            .secondary-missions h2 { font-size: 29px; }
            .discovery-next { display: flex; align-items: center; justify-content: space-between; gap: 30px; margin-top: 36px; padding: 23px 0; border-top: 1px solid #b4b8b4; border-bottom: 1px solid #b4b8b4; }
            .discovery-next h2 { margin-bottom: 5px; font-size: 32px; }
            .discovery-next p:not(.step-label) { margin: 0; }
            .capture-panel { display: grid; grid-template-columns: minmax(220px, .72fr) minmax(0, 1.28fr); gap: clamp(24px, 5vw, 70px); margin-top: 52px; padding: 24px 0; border-top: 1px solid #b4b8b4; border-bottom: 1px solid #b4b8b4; }
            .capture-panel h2, .observation-ledger h2 { margin: 0 0 8px; font-family: "Iowan Old Style", Georgia, serif; font-size: 32px; font-weight: 400; letter-spacing: -.04em; }
            .capture-panel > div > p:not(.step-label) { color: #646464; font-size: 12px; line-height: 1.65; }
            #observation-form { display: grid; gap: 12px; }
            #observation-form label { display: grid; gap: 5px; color: #646464; font-size: 10px; }
            #observation-form input, #observation-form textarea, #observation-form select { width: 100%; padding: 10px; border: 1px solid #cbd0cb; border-radius: 3px; background: #fff; color: #171717; font: inherit; font-size: 12px; }
            #observation-form textarea { resize: vertical; line-height: 1.5; }
            #observation-form input:focus-visible, #observation-form textarea:focus-visible, #observation-form select:focus-visible { outline: 2px solid #0081c0; outline-offset: 2px; }
            #observation-form select { cursor: pointer; }
            .capture-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
            #capture-status { min-height: 18px; margin: 0; color: #276040; font-size: 11px; }
            .observation-ledger { margin-top: 44px; }
            .observation-card { display: grid; grid-template-columns: minmax(180px, .55fr) 1fr; gap: 20px; padding: 18px 0; border-top: 1px solid #dee2de; }
            .observation-card:last-child { border-bottom: 1px solid #dee2de; }
            .observation-card h3 { margin: 0 0 5px; font-size: 14px; }
            .observation-card p { margin: 5px 0; color: #646464; font-size: 11px; line-height: 1.55; }
            .observation-card blockquote { margin: 0; padding-left: 13px; border-left: 2px solid #41a1cf; color: #444141; font-size: 12px; line-height: 1.6; white-space: pre-wrap; }
            .observation-card .observation-meta { color: #0081c0; font-size: 10px; }
            .load-more-observations { margin-top: 12px; padding: 10px 0; border: 0; background: transparent; color: #8dd7ef; font: inherit; font-size: 12px; text-decoration: underline; text-underline-offset: 3px; cursor: pointer; }
            .load-more-observations:focus-visible { outline: 2px solid currentColor; outline-offset: 3px; }
            .load-more-observations:disabled { opacity: .6; cursor: wait; }
            /* Discovery uses the same plum-and-ink palette as the home hero. */
            body.flow-page { background: radial-gradient(ellipse at 78% 3%, rgba(122,74,119,.31), transparent 35%), #1f1f29; color: #f8f7f7; }
            .flow-page .site-header { position: relative; z-index: 2; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; width: min(1240px, calc(100% - 64px)); min-height: 68px; margin: 18px auto 0; padding: 7px 8px 7px 20px; border: 1px solid rgba(255,255,255,.32); border-radius: 999px; background: rgba(40,40,52,.88); color: #fff; box-shadow: 0 8px 28px rgba(0,0,0,.12); backdrop-filter: blur(18px); }
            .flow-page .brand { color: #fff; font-size: 20px; }
            .flow-page .brand-mark { display: none; }
            .flow-page .site-header nav { gap: 6px; }
            .flow-page .site-header nav a { padding: 10px 13px; border-radius: 999px; color: rgba(255,255,255,.88); font-size: 12px; }
            .flow-page .site-header nav a:hover, .flow-page .site-header nav a:focus-visible { background: rgba(255,255,255,.12); color: #fff; }
            .flow-page .theme-toggle { display: inline-grid; flex: 0 0 34px; width: 34px; height: 34px; place-items: center; border: 1px solid rgba(255,255,255,.28); border-radius: 50%; background: transparent; color: inherit; font-size: 17px; cursor: pointer; }
            .flow-page .nav-action { justify-self: end; padding: 11px 17px; border: 0; border-radius: 999px; background: #f7f6f4; color: #171717; }
            .flow-main--sample, .discovery-main, .overview-main { position: relative; width: min(1240px, calc(100% - 64px)); max-width: none; margin: 18px auto 54px; padding: clamp(28px, 5vw, 62px); overflow: hidden; border: 1px solid rgba(255,255,255,.25); border-radius: 30px; background: radial-gradient(ellipse at 85% 0%, rgba(115,73,112,.42), transparent 38%), linear-gradient(135deg, rgba(44,41,57,.97), rgba(31,31,41,.98) 68%); color: #f8f7f7; }
            .discovery-main h1, .discovery-main h2, .overview-main h1, .overview-main h2 { color: #fff; font-family: "ppmondwest", "Iowan Old Style", Georgia, serif; font-weight: 400; letter-spacing: -.045em; }
            .flow-page .eyebrow, .flow-page .step-label { color: #a9dff1; font-size: 12px; font-weight: 600; letter-spacing: .01em; text-transform: none; }
            .flow-page .flow-intro, .discovery-main .flow-intro { color: rgba(255,255,255,.78); }
            .discovery-progress { gap: clamp(10px, 3vw, 38px); margin: 0 0 38px; border-color: rgba(255,255,255,.2); }
            .discovery-progress li { color: rgba(255,255,255,.55); }
            .discovery-progress li[aria-current="step"] { border-color: #63c4e5; color: #fff; }
            .discovery-progress li span { border-color: rgba(255,255,255,.4); }
            .discovery-progress li[aria-current="step"] span, .discovery-progress li.is-complete span { border-color: #41a1cf; background: #41a1cf; }
            .discovery-welcome { display: grid; grid-template-columns: minmax(0,1.3fr) minmax(220px,.7fr); align-items: end; gap: 30px; padding: clamp(24px,5vw,60px) 0 38px; }
            .discovery-welcome h1, .simulation-intro h1 { max-width: 720px; margin: 0 0 14px; font-size: clamp(48px,7vw,82px); line-height: .98; }
            .discovery-welcome .flow-intro { max-width: 620px; margin: 0; font-size: 16px; line-height: 1.65; }
            .workspace-note { max-width: 250px; margin: 0; padding: 17px 0 0 20px; border-left: 2px solid #41a1cf; color: rgba(255,255,255,.7); font-size: 13px; line-height: 1.6; }
            .discovery-setup { max-width: 780px; display: grid; gap: 18px; }
            .step-guidance { margin: 0; color: rgba(255,255,255,.72); font-size: 12px; line-height: 1.5; }
            .setup-profile { display: grid; grid-template-columns: 1fr 1fr .8fr; gap: 14px; }
            .discovery-setup label { display: grid; gap: 7px; color: rgba(255,255,255,.85); font-size: 13px; font-weight: 550; }
            .discovery-setup input, .discovery-setup textarea, .discovery-setup select { width: 100%; padding: 13px 14px; border: 1px solid rgba(255,255,255,.27); border-radius: 10px; background: rgba(255,255,255,.08); color: #fff; font: inherit; font-size: 14px; }
            .discovery-setup select option { color: #171717; }
            .discovery-setup input::placeholder, .discovery-setup textarea::placeholder { color: rgba(255,255,255,.45); }
            .discovery-setup input:focus-visible, .discovery-setup textarea:focus-visible { outline: 2px solid #71c9e7; outline-offset: 2px; }
            .discovery-setup textarea { resize: vertical; line-height: 1.55; }
            .mission-fields { display: grid; gap: 12px; }
            .goal-editor { display: grid; gap: 10px; }
            .goal-editor > p { margin: 0; font-size: 13px; }
            .goal-editor > .action-link { justify-self: start; }
            #goal-list { display: grid; gap: 10px; }
            .goal-row { display: grid; grid-template-columns: 1.5fr 1fr 1fr auto; gap: 8px; align-items: end; }
            .goal-row label { display: grid; gap: 4px; font-size: 11px; }
            .review-goals { color: rgba(255,255,255,.78); font-size: 13px; }
            .mission-fields > p { margin: 0; color: rgba(255,255,255,.85); font-size: 13px; font-weight: 550; }
            .review-missions { list-style: none; max-width: 780px; margin: 0 0 10px; padding: 0; display: grid; gap: 10px; }
            .review-missions .review-mission { margin: 4px 0 0; }
            .mission-count { color: rgba(255,255,255,.58); font-size: 11px; letter-spacing: .04em; text-transform: uppercase; }
            .setup-product { display: grid; grid-template-columns: .75fr 1.25fr; gap: 18px; }
            .catalog-upload { display: grid; gap: 12px; max-width: 780px; padding: 18px 0; border-top: 1px solid rgba(255,255,255,.2); border-bottom: 1px solid rgba(255,255,255,.2); }
            .catalog-upload > div { display: grid; gap: 5px; }
            .catalog-upload strong { color: #fff; font-size: 14px; }
            .catalog-upload span, .file-drop small { color: rgba(255,255,255,.62); font-size: 12px; }
            .file-drop { display: grid !important; gap: 7px; padding: 18px; border: 1px dashed rgba(141,215,239,.65); border-radius: 12px; background: rgba(255,255,255,.04); cursor: pointer; }
            .file-drop input { padding: 0; border: 0; background: transparent; color: #fff; }
            .template-link { justify-self: start; color: #8dd7ef; font-size: 12px; text-underline-offset: 3px; }
            .manual-entry { max-width: 780px; color: rgba(255,255,255,.8); font-size: 12px; }
            .manual-entry summary { cursor: pointer; }
            .manual-entry .setup-product { margin-top: 14px; }
            .flow-page .discovery-setup .expand-panel { display: grid; gap: 0; max-width: 780px; }
            .flow-page .discovery-setup .expand-panel__button { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; padding: 12px 16px; border: 1px solid #8a8f98; border-radius: 8px; background: transparent; color: #f7f6f5; font-family: inherit; font-size: 15px; font-weight: 500; text-align: left; cursor: pointer; }
            .flow-page .discovery-setup .expand-panel__button:hover { background: rgba(255,255,255,.08); }
            .flow-page .discovery-setup .expand-panel__text { display: grid; gap: 2px; min-width: 0; }
            .flow-page .discovery-setup .expand-panel__text > span { font-size: 13px; font-weight: 400; opacity: .75; }
            .flow-page .discovery-setup .expand-panel__icon { flex: none; font-size: 18px; font-weight: 400; line-height: 1; }
            .flow-page .discovery-setup .expand-panel__icon::after { content: "+"; }
            .flow-page .discovery-setup .expand-panel__button[aria-expanded="true"] .expand-panel__icon::after { content: "\\2212"; }
            .flow-page .discovery-setup .expand-panel__body[hidden] { display: none !important; }
            .flow-page .discovery-setup .expand-panel .store-connect, .flow-page .discovery-setup .expand-panel .catalog-upload { margin-top: 12px; }
            .onboarding-status { min-height: 16px; margin: 0; color: #a9dff1; font-size: 12px; }
            .onboarding-error { max-width: 780px; padding: 12px 15px; border-left: 3px solid #ee9e89; background: rgba(238,158,137,.1); color: #ffd3c8; }
            .catalog-review-title { max-width: 820px; margin: 26px 0 14px; font-size: clamp(43px,6vw,70px); line-height: 1; }
            .review-mission { max-width: 780px; margin: 0 0 10px; padding: 0 0 0 14px; border-left: 2px solid rgba(255,255,255,.35); color: rgba(255,255,255,.88); font-size: 15px; line-height: 1.55; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
            .review-meta { max-width: 780px; margin: 0 0 4px; color: rgba(255,255,255,.58); font-size: 12px; }
            .draft-status { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600; }
            .draft-status--review { background: rgba(232,184,70,.16); color: #e8b846; }
            .draft-status--decided { background: rgba(110,200,140,.16); color: #6ec88c; }
            .draft-status--rejected { background: rgba(224,110,110,.16); color: #e06e6e; }
            .draft-diff { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 12px 0; }
            .draft-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
            .draft-pre { white-space: pre-wrap; word-break: break-word; font-size: 12px; line-height: 1.5; }
            @media (max-width: 760px) { .draft-diff { grid-template-columns: 1fr; } }
            .catalog-review-summary { display: flex; flex-wrap: wrap; justify-content: space-between; gap: 12px; margin-top: 28px; padding: 16px 0; border-top: 1px solid rgba(255,255,255,.22); border-bottom: 1px solid rgba(255,255,255,.22); }
            .catalog-review-summary strong { color: #fff; font-size: 14px; }
            .catalog-review-summary span { color: rgba(255,255,255,.62); font-size: 12px; }
            .catalog-review-list { display: grid; gap: 0; max-width: 900px; margin: 0; padding: 0; list-style: none; }
            .catalog-review-list li { display: grid; gap: 5px; padding: 15px 0; border-bottom: 1px solid rgba(255,255,255,.16); }
            .catalog-review-list strong { color: #fff; font-size: 13px; }
            .catalog-review-list span { color: rgba(255,255,255,.66); font-size: 12px; line-height: 1.6; }
            .field-note, .discovery-main .demo-disclosure { max-width: 780px; color: rgba(255,255,255,.58); font-size: 12px; line-height: 1.6; }
            .field-note { margin: -8px 0 0; }
            .flow-page .action-link { justify-self: start; min-height: 46px; padding: 13px 18px; border-radius: 999px; background: #f6f5f2; color: #201f28; font-size: 13px; }
            .flow-page .action-link:hover { background: #dceef4; }
            .back-link, .observation-link a { color: #8dd7ef; font-size: 13px; text-underline-offset: 4px; }
            .simulation-brief > div > span { color: #a9dff1; font-size: 12px; font-weight: 600; }
            .simulation-brief details p { color: rgba(255,255,255,.82); font-size: 14px; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
            .discovery-next { align-items: center; margin-top: 30px; border-color: rgba(255,255,255,.22); }
            .discovery-next h2, .agent-run h2 { max-width: 550px; margin: 0 0 7px; font-size: clamp(27px,3vw,39px); }
            .discovery-next p:not(.step-label), .agent-run p:not(.step-label) { color: rgba(255,255,255,.68); }
            .simulation-intro { padding: clamp(20px,4vw,42px) 0 24px; }
            .simulation-intro h1 { max-width: 780px; }
            .simulation-brief { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; padding: 20px 0; border-top: 1px solid rgba(255,255,255,.25); border-bottom: 1px solid rgba(255,255,255,.25); }
            .simulation-brief > div { display: grid; align-content: start; gap: 8px; }
            .simulation-brief strong { color: #fff; font-size: 14px; line-height: 1.5; overflow-wrap: anywhere; }
            .simulation-brief details { grid-column: 1 / -1; color: rgba(255,255,255,.75); font-size: 12px; }
            .simulation-brief summary, .observation-link summary { cursor: pointer; }
            .simulation-brief summary { color: rgba(255,255,255,.75); }
            .agent-run { display: flex; align-items: center; justify-content: space-between; gap: 30px; padding: 32px 0; }
            .agent-run > div { max-width: 560px; }
            .agent-run p:not(.step-label) { font-size: 13px; line-height: 1.6; }
            .agent-run .action-link { cursor: pointer; }
            .simulation-result { max-width: 850px; margin: 8px 0 25px; padding: 22px; border: 1px solid rgba(141,215,239,.6); border-radius: 14px; background: rgba(255,255,255,.08); color: #fff; white-space: pre-wrap; line-height: 1.7; }
            .simulation-result[hidden] { display: none; }
            .simulation-result h3 { margin: 0 0 8px; font-family: inherit; font-size: 17px; }
            .simulation-limit { max-width: 780px; color: rgba(255,255,255,.53); font-size: 11px; line-height: 1.55; }
            .observation-link { max-width: 780px; margin-top: 30px; padding-top: 18px; border-top: 1px solid rgba(255,255,255,.2); color: rgba(255,255,255,.78); font-size: 12px; }
            .flow-page .flow-footer { width: min(1240px, calc(100% - 64px)); color: rgba(255,255,255,.55); }
            .flow-page .flow-footer span:last-child { color: rgba(255,255,255,.45); }
            body.flow-page[data-theme="light"] { background: #fefffc; color: #171717; }
            body.flow-page[data-theme="light"] .flow-main--sample, body.flow-page[data-theme="light"] .overview-main { max-width: 1200px; margin: 0 auto 80px; padding: 32px 0 64px; border: 0; border-radius: 0; background: transparent; box-shadow: none; overflow: visible; color: #171717; }
            body.flow-page[data-theme="light"] .discovery-main h1, body.flow-page[data-theme="light"] .discovery-main h2, body.flow-page[data-theme="light"] .overview-main h1, body.flow-page[data-theme="light"] .overview-main h2, body.flow-page[data-theme="light"] .catalog-review-summary strong, body.flow-page[data-theme="light"] .catalog-review-list strong { color: #171717; }
            body.flow-page[data-theme="light"] .flow-intro, body.flow-page[data-theme="light"] .workspace-note, body.flow-page[data-theme="light"] .field-note, body.flow-page[data-theme="light"] .catalog-upload span, body.flow-page[data-theme="light"] .file-drop small, body.flow-page[data-theme="light"] .catalog-review-list span, body.flow-page[data-theme="light"] .catalog-review-summary span { color: #646464; }
            body.flow-page[data-theme="light"] .discovery-setup label { color: #444141; }
            body.flow-page[data-theme="light"] .step-guidance { color: #4d5955; }
            body.flow-page[data-theme="light"] .file-drop { background: #f7faf9; border-color: #79aec2; }
            body.flow-page[data-theme="light"] .manual-entry { color: #444141; }
            body.flow-page[data-theme="light"] .catalog-review-list li, body.flow-page[data-theme="light"] .catalog-review-summary { border-color: #dee2de; }
            body.flow-page[data-theme="light"] .review-mission { color: #33403c; border-left-color: rgba(0,0,0,.28); }
            body.flow-page[data-theme="light"] .mission-fields > p { color: #444141; }
            body.flow-page[data-theme="light"] .review-goals { color: #444141; }
            body.flow-page[data-theme="light"] .mission-count { color: #6b7571; }
            body.flow-page[data-theme="light"] .governance-statement p { color: #444141; }
            body.flow-page[data-theme="light"] .policy-table th, body.flow-page[data-theme="light"] .policy-table td { border-color: #dee2de; color: #444141; }
            body.flow-page[data-theme="light"] .policy-table thead th { color: #6b7571; }
            body.flow-page[data-theme="light"] .policy-table tbody tr:last-child td { border-color: #dee2de; }
            body.flow-page[data-theme="light"] .flow-footer a { color: #006f9f; }
            body.flow-page[data-theme="light"] .review-meta { color: #6b7571; }
            body.flow-page[data-theme="light"] .draft-status--review { background: rgba(150,110,20,.14); color: #7a5c10; }
            body.flow-page[data-theme="light"] .draft-status--decided { background: rgba(30,140,80,.12); color: #1a7a4a; }
            body.flow-page[data-theme="light"] .draft-status--rejected { background: rgba(190,60,60,.12); color: #a83a3a; }
            body.flow-page[data-theme="light"] .simulation-brief strong, body.flow-page[data-theme="light"] .mission-detail-screen .fit-table strong { color: #171717; }
            body.flow-page[data-theme="light"] .simulation-brief details, body.flow-page[data-theme="light"] .simulation-brief summary { color: #444141; }
            body.flow-page[data-theme="light"] .simulation-result { background: #f4f8f7; color: #171717; border-color: #9fcad8; }
            body.flow-page[data-theme="light"] .agent-run p:not(.step-label), body.flow-page[data-theme="light"] .discovery-next p:not(.step-label), body.flow-page[data-theme="light"] .merchant-facts p, body.flow-page[data-theme="light"] .mission-detail-screen .fit-table span { color: #646464; }
            body.flow-page[data-theme="light"] .eyebrow,
            body.flow-page[data-theme="light"] .step-label,
            body.flow-page[data-theme="light"] .mission-detail-screen .fit-table-head { color: #175d76; }
            body.flow-page[data-theme="light"] .discovery-progress { border-color: #d4dcd9; }
            body.flow-page[data-theme="light"] .discovery-progress li { color: #4d5955; }
            body.flow-page[data-theme="light"] .discovery-progress li[aria-current="step"] { color: #171717; }
            body.flow-page[data-theme="light"] .discovery-progress li.is-complete { color: #24563e; }
            body.flow-page[data-theme="light"] .discovery-progress li span { border-color: #929f9a; color: #34423d; }
            body.flow-page[data-theme="light"] .discovery-progress li[aria-current="step"] span,
            body.flow-page[data-theme="light"] .discovery-progress li.is-complete span { border-color: #cce9f2; background: #cce9f2; color: #164c60; }
            body.flow-page[data-theme="light"] .mission-detail-screen .fit-table em { color: #765000; }
            body.flow-page[data-theme="light"] .simulation-brief > div > span { color: #0081c0; }
            body.flow-page[data-theme="light"] .mission-detail-screen .interpretation-note { background: #f1f5f2; color: #343a37 !important; }
            body.flow-page[data-theme="light"] .mission-detail-screen .interpretation-note strong { color: #171717; }
            body.flow-page[data-theme="light"] .flow-footer { color: #646464; }
            body.flow-page[data-theme="light"] .load-more-observations { color: #006f9f; }
            body.flow-page[data-theme="light"] .site-header { background: rgba(255,255,255,.78); border-color: #dee2de; box-shadow: rgba(0,0,0,.08) 0px 1px 1px 0px, rgba(0,0,0,.08) 0px 4px 5px 0px; color: #171717; }
            body.flow-page[data-theme="light"] .brand { color: #171717; }
            body.flow-page[data-theme="light"] .brand-mark { display: inline-grid; }
            body.flow-page[data-theme="light"] .site-header nav a { color: #444141; }
            body.flow-page[data-theme="light"] .site-header nav a:hover, body.flow-page[data-theme="light"] .site-header nav a:focus-visible { background: #f9faf7; color: #171717; }
            body.flow-page[data-theme="light"] .theme-toggle { display: none; }
            body.flow-page[data-theme="light"] .nav-action { background: transparent; border: 1px solid #282834; color: #282834; }
            body.flow-page[data-theme="light"] .action-link { background: transparent; border: 1px solid #41a1cf; border-radius: 8px; color: #41a1cf; font-weight: 500; }
            body.flow-page[data-theme="light"] .action-link:hover { background: rgba(65,161,207,.08); }
            body.flow-page[data-theme="light"] .action-link--secondary { border-color: #282834; color: #282834; }
            body.flow-page[data-theme="light"] .action-link--secondary:hover { background: rgba(40,40,52,.06); }
            body.flow-page[data-theme="light"] .discovery-setup input, body.flow-page[data-theme="light"] .discovery-setup textarea, body.flow-page[data-theme="light"] .discovery-setup select, body.flow-page[data-theme="light"] #observation-form input, body.flow-page[data-theme="light"] #observation-form textarea, body.flow-page[data-theme="light"] #observation-form select, body.flow-page[data-theme="light"] .extract-panel input, body.flow-page[data-theme="light"] .extract-panel textarea, body.flow-page[data-theme="light"] .review-step select, body.flow-page[data-theme="light"] .review-step input, body.flow-page[data-theme="light"] .review-step textarea { background: #f9faf7; border: 0; border-bottom: 1px solid #444141; border-radius: 0; color: #171717; }
            body.flow-page[data-theme="light"] .file-drop input { border-bottom: 0; }
            body.flow-page[data-theme="light"] .discovery-setup input::placeholder, body.flow-page[data-theme="light"] .discovery-setup textarea::placeholder { color: #646464; }
            body.flow-page[data-theme="light"] .discovery-main h1 { text-wrap: balance; }
            body.flow-page[data-theme="light"] .discovery-main { max-width: 1200px; margin: 0 auto 80px; padding: 0 0 64px; border: 0; border-radius: 0; background: transparent; box-shadow: none; overflow: visible; }
            body.flow-page[data-theme="light"] .discovery-progress { margin: 32px 0 0; }
            body.flow-page[data-theme="light"] .discovery-welcome { grid-template-columns: minmax(0,1fr) minmax(240px,300px); align-items: start; padding: 64px 0 48px; }
            body.flow-page[data-theme="light"] .discovery-welcome .eyebrow { color: #646464; font-size: 13px; font-weight: 500; }
            body.flow-page[data-theme="light"] .discovery-welcome h1 { max-width: 12ch; color: #2c2c2c; font-size: 54px; line-height: 1.1; letter-spacing: -0.02em; }
            body.flow-page[data-theme="light"] .discovery-welcome .flow-intro { max-width: 60ch; color: #646464; font-size: 16px; line-height: 1.5; }
            body.flow-page[data-theme="light"] .workspace-note { max-width: none; margin: 0; padding: 16px; border: 1px solid #dee2de; border-radius: 12px; background: #f9faf7; color: #444141; font-size: 15px; line-height: 1.5; }
            body.flow-page[data-theme="light"] .discovery-setup { max-width: 960px; gap: 18px; }
            body.flow-page[data-theme="light"] .form-chapter { display: grid; gap: 16px; padding: 48px 0; border-top: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .chapter-index { margin: 0; color: #646464; font-size: 13px; }
            body.flow-page[data-theme="light"] .form-chapter h2 { margin: 0; color: #2c2c2c; font-family: "Iowan Old Style", Georgia, serif; font-size: 27px; font-weight: 400; line-height: 1.5; letter-spacing: -0.02em; }
            body.flow-page[data-theme="light"] .step-guidance { color: #646464; font-size: 15px; line-height: 1.5; }
            body.flow-page[data-theme="light"] .discovery-setup label { color: #444141; font-size: 13px; font-weight: 500; }
            body.flow-page[data-theme="light"] .mission-fields > p, body.flow-page[data-theme="light"] .goal-editor > p { color: #444141; font-size: 15px; }
            body.flow-page[data-theme="light"] .discovery-setup input, body.flow-page[data-theme="light"] .discovery-setup textarea, body.flow-page[data-theme="light"] .discovery-setup select { padding: 12px 2px; font-size: 16px; }
            body.flow-page[data-theme="light"] .discovery-setup details > summary { color: #444141; font-size: 15px; font-weight: 500; cursor: pointer; }
            body.flow-page[data-theme="light"] .goal-row label { font-size: 12px; }
            body.flow-page[data-theme="light"] .form-submit { display: grid; justify-items: start; gap: 12px; padding: 48px 0 16px; border-top: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .action-link { padding: 12px 16px; font-size: 15px; }
            body.flow-page[data-theme="light"] .goal-remove.action-link, body.flow-page[data-theme="light"] #goal-add.action-link { padding: 8px 12px; font-size: 13px; }
            body.flow-page[data-theme="light"] .field-note { margin: 0; color: #646464; font-size: 13px; line-height: 1.5; }
            body.flow-page[data-theme="light"] .onboarding-status { color: #175d76; }
            body.flow-page[data-theme="light"] .onboarding-error { border-left: 0; border: 1px solid #dee2de; border-radius: 12px; background: #fff; color: #a83a3a; }
            body.flow-page[data-theme="light"] .catalog-upload { border-color: #dee2de; }
            body.flow-page[data-theme="light"] .catalog-upload strong { color: #2c2c2c; font-size: 16px; font-weight: 500; }
            body.flow-page[data-theme="light"] .file-drop { border-style: dashed; border-color: #b4b8b4; border-radius: 12px; background: #f9faf7; }
            body.flow-page[data-theme="light"] .store-connect { display: grid; gap: 12px; padding: 16px; border: 1px solid #dee2de; border-radius: 12px; background: #fff; box-shadow: rgba(0,0,0,.08) 0px 1px 1px 0px, rgba(0,0,0,.08) 0px 4px 5px 0px; }
            body.flow-page[data-theme="light"] .store-connect > p { display: grid; gap: 4px; margin: 0; }
            body.flow-page[data-theme="light"] .store-connect strong { color: #2c2c2c; font-size: 16px; font-weight: 500; }
            body.flow-page[data-theme="light"] .store-connect span { color: #646464; font-size: 13px; }
            body.flow-page[data-theme="light"] .connect-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 16px; }
            body.flow-page[data-theme="light"] .connect-fields label { display: grid; gap: 6px; }
            body.flow-page[data-theme="light"] .discovery-setup select, body.flow-page[data-theme="light"] #observation-form select, body.flow-page[data-theme="light"] .review-step select { -webkit-appearance: none; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M1 1l4 4 4-4' stroke='%23444141' fill='none' stroke-width='1.5'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 2px center; padding-right: 22px; }
            body.flow-page[data-theme="light"] #mission-id { padding: 12px 30px 12px 16px; border: 1px solid #929f9a; border-radius: 8px; background-color: transparent; background-position: right 12px center; color: #171717; font-size: 15px; font-weight: 500; }
            body.flow-page[data-theme="light"] #mission-id:hover { background-color: rgba(40,40,52,.06); }
            body.flow-page[data-theme="light"] .store-connect .field-note { margin: 0; }
            body.flow-page[data-theme="light"] .discovery-setup .expand-panel__button { border-color: #929f9a; color: #171717; }
            body.flow-page[data-theme="light"] .discovery-setup .expand-panel__button:hover { background: rgba(40,40,52,.06); }
            body.flow-page[data-theme="light"] .discovery-setup .expand-panel__text > span { color: #646464; opacity: 1; }
            body.flow-page[data-theme="light"] #connect-fetch { justify-self: start; }
            body.flow-page[data-theme="light"] .template-link { color: #0081c0; }
            body.flow-page[data-theme="light"] .discovery-main .demo-disclosure { max-width: none; margin: 0; padding: 24px 0 0; border-top: 1px solid #dee2de; color: #646464; font-size: 13px; }
            body.flow-page[data-theme="light"] .pipeline-strip { max-width: 780px; margin-top: 32px; padding: 24px 0; border-top: 1px solid #dee2de; border-bottom: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .pipeline-strip ol { display: grid; margin: 8px 0 0; padding: 0; list-style: none; }
            body.flow-page[data-theme="light"] .pipeline-strip li { position: relative; display: grid; gap: 2px; padding: 12px 0 12px 30px; border-top: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .pipeline-strip li:first-child { border-top: 0; }
            body.flow-page[data-theme="light"] .pipeline-strip li::before { position: absolute; left: 2px; top: 12px; content: "○"; color: #b4b8b4; font-size: 14px; }
            body.flow-page[data-theme="light"] .pipeline-strip li.is-done::before { content: "✓"; color: #1a7a4a; font-weight: 700; }
            body.flow-page[data-theme="light"] .pipeline-strip strong { color: #2c2c2c; font-size: 15px; font-weight: 500; }
            body.flow-page[data-theme="light"] .pipeline-strip span { color: #646464; font-size: 13px; }
            body.flow-page[data-theme="light"] .pipeline-strip a { color: #0081c0; text-underline-offset: 3px; }
            body.flow-page[data-theme="light"] .overview-main .eyebrow { color: #646464; font-size: 13px; font-weight: 500; }
            body.flow-page[data-theme="light"] .overview-main h1 { max-width: 14ch; color: #2c2c2c; font-family: "Iowan Old Style", Georgia, serif; font-size: 54px; font-weight: 400; line-height: 1.1; letter-spacing: -0.02em; text-wrap: balance; }
            body.flow-page[data-theme="light"] .overview-main .flow-intro { max-width: 60ch; color: #646464; font-size: 16px; line-height: 1.5; }
            body.flow-page[data-theme="light"] .overview-steps { display: grid; grid-template-columns: 1fr 1fr; gap: 0 32px; margin: 48px 0; border-top: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .overview-steps article { display: grid; gap: 6px; padding: 24px 0; border-bottom: 1px solid #dee2de; }
            body.flow-page[data-theme="light"] .overview-steps span { color: #646464; font-size: 13px; }
            body.flow-page[data-theme="light"] .overview-steps h2 { margin: 0; color: #2c2c2c; font-family: "Iowan Old Style", Georgia, serif; font-size: 27px; font-weight: 400; letter-spacing: -0.02em; }
            body.flow-page[data-theme="light"] .overview-steps p { margin: 0; color: #444141; font-size: 15px; line-height: 1.5; }
            @media (max-width: 760px) {
              .flow-page .site-header { width: calc(100% - 32px); }
              .flow-main--sample, .discovery-main, .overview-main { width: calc(100% - 24px); padding: 24px 19px; border-radius: 22px; }
              .discovery-welcome { grid-template-columns: 1fr; gap: 18px; }
              .workspace-note { max-width: 420px; }
              .setup-product { grid-template-columns: 1fr; }
              .setup-profile { grid-template-columns: 1fr; }
              .simulation-brief { grid-template-columns: 1fr 1fr; }
              .agent-run { align-items: start; flex-direction: column; }
              .sample-entry { align-items: start; flex-direction: column; }
              .flow-page .site-header, .flow-main--sample, .flow-page .flow-footer { width: calc(100% - 36px); }
              .sample-workspace { grid-template-columns: 1fr; gap: 28px; margin-top: 34px; }
              .flow-next-step { padding: 18px 0 0 19px; }
              .flow-heading { padding-top: 36px; }
              .flow-heading h1 { max-width: 640px; font-size: clamp(46px, 10vw, 64px); }
              .flow-page .flow-intro { font-size: 14px; }
              .discovery-main { width: calc(100% - 36px); }
              .mission-layout { grid-template-columns: 1fr; gap: 30px; margin-top: 38px; }
              .discovery-status { grid-template-columns: 1fr; }
              .discovery-status > div { padding: 12px 0; }
              .discovery-status > div + div { padding-left: 0; border-left: 0; border-top: 1px solid #dee2de; }
              .fit-table > div { grid-template-columns: 1fr 1fr; gap: 8px 14px; }
              .fit-table-head { display: none !important; }
              .fit-table > div > * { min-width: 0; }
              .secondary-missions { grid-template-columns: 1fr; gap: 0; }
              .secondary-missions article + article { padding-left: 0; border-left: 0; border-top: 1px solid #dee2de; }
              .discovery-next { align-items: start; flex-direction: column; }
              .capture-panel { grid-template-columns: 1fr; }
              .capture-step #observation-form { grid-template-columns: 1fr; }
              .capture-step #observation-form > * { grid-column: 1 !important; }
              .claim-fields { grid-template-columns: 1fr; }
              .observation-card { grid-template-columns: 1fr; gap: 10px; }
              .capture-grid { grid-template-columns: 1fr; }
            }
            @media (max-width: 480px) {
              .flow-page .site-header { grid-template-columns: 1fr auto; min-height: 58px; padding-left: 15px; }
              .flow-page .site-header nav { display: none; }
              .flow-page .nav-action { padding: 9px 12px; }
              .discovery-progress { gap: 6px; }
              .discovery-progress li { align-items: flex-start; flex-direction: column; gap: 4px; font-size: 9px; }
              .simulation-brief { grid-template-columns: 1fr; }
              .flow-page .site-header nav { gap: 8px; }
              .flow-page .site-header nav a { padding: 8px 5px; font-size: 10px; }
              .flow-page .nav-action { padding: 8px 9px; font-size: 10px; }
              .flow-page .flow-progress { gap: 9px; font-size: 10px; }
              .flow-page .catalog-caption { align-items: flex-start; flex-direction: column; gap: 5px; }
              .sample-product-table tbody th { min-width: 160px; }
            }
            .discovery-next-actions { display: flex; flex-direction: column; gap: 10px; align-items: flex-start; }
            .flow-page .action-link--secondary { background: transparent; border: 1px solid #8a8f98; color: #f7f6f5; }
            .flow-page .action-link--secondary:hover { background: rgba(255,255,255,.08); }
            [hidden] { display: none !important; }
            .run-start { display: flex; align-items: center; justify-content: space-between; gap: 22px; margin: 26px 0; padding: 22px 24px; border: 1px solid #494652; border-radius: 18px; background: #282834; }
            .run-start h2 { margin: 6px 0 8px; font-size: 24px; }
            .run-start p { margin: 0; color: #c0bcc2; font-size: 14px; line-height: 1.6; }
            .run-status { margin: 26px 0; padding: 20px 22px; border: 1px solid #494652; border-radius: 18px; background: #282834; }
            .run-status-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
            .run-status-row strong { font-size: 15px; }
            .run-pill { display: inline-block; padding: 6px 14px; border-radius: 999px; background: #3a3a48; color: #f7f6f5; font-size: 12px; font-weight: 700; letter-spacing: .04em; text-transform: uppercase; }
            .run-pill[data-tone="busy"] { background: #164c60; }
            .run-pill[data-tone="good"] { background: #24563e; }
            .run-pill[data-tone="warn"] { background: #6b4a12; }
            .run-pill[data-tone="bad"] { background: #7a2a24; }
            .run-bar { height: 10px; margin: 14px 0 10px; border-radius: 999px; background: #3a3a48; overflow: hidden; }
            .run-bar span { display: block; height: 100%; width: 0; border-radius: 999px; background: #41a1cf; transition: width .5s ease; }
            .run-note { margin: 0; color: #c0bcc2; font-size: 13px; }
            .run-feed h2, .run-findings h2, .run-summary h2 { font-size: 20px; margin: 26px 0 12px; }
            .run-events { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
            .run-event { display: grid; grid-template-columns: 40px 1fr; gap: 12px; align-items: start; padding: 14px 16px; border: 1px solid #494652; border-radius: 14px; background: #282834; animation: run-slide .3s ease; }
            @keyframes run-slide { from { opacity: 0; transform: translateY(10px); } }
            .run-event-icon { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 10px; background: #164c60; color: #fff; font-size: 17px; font-weight: 700; }
            .run-event--good .run-event-icon { background: #24563e; }
            .run-event--warn .run-event-icon { background: #6b4a12; }
            .run-event--bad .run-event-icon { background: #7a2a24; }
            .run-event-head { display: flex; align-items: baseline; justify-content: space-between; gap: 10px; }
            .run-event-head strong { font-size: 14px; }
            .run-event-head span { flex-shrink: 0; color: #8f8a95; font-size: 12px; }
            .run-event p { margin: 4px 0 0; color: #c0bcc2; font-size: 13px; line-height: 1.55; }
            .run-finding { padding: 14px 16px; border: 1px solid #494652; border-radius: 14px; background: #282834; }
            .run-finding + .run-finding { margin-top: 10px; }
            .run-finding strong { display: block; margin: 8px 0 2px; font-size: 14px; }
            .run-finding small { color: #8f8a95; font-size: 12px; }
            .run-finding p { margin: 8px 0 0; color: #c0bcc2; font-size: 13px; line-height: 1.6; }
            .run-severity { display: inline-block; padding: 3px 10px; border: 1px solid; border-radius: 999px; font-size: 11px; font-weight: 700; letter-spacing: .05em; }
            .run-severity--high { border-color: #e57373; color: #ef9a94; }
            .run-severity--medium { border-color: #e0a63f; color: #eec27a; }
            .run-severity--low { border-color: #7fc4e3; color: #a5d8ec; }
            .run-summary { margin-top: 26px; padding: 22px 24px; border: 1px solid #494652; border-radius: 18px; background: #282834; }
            .run-summary h2 { margin-top: 0; }
            .run-summary p { color: #c0bcc2; font-size: 14px; line-height: 1.6; }
            .run-summary-actions { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-top: 14px; }
            .run-empty { color: #8f8a95; font-size: 13px; }
            .run-target-list { list-style: none; margin: 14px 0 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
            .run-target-list li { font-size: 12px; color: #c0bcc2; }
            .run-target-list strong { display: block; font-size: 13px; color: #f7f6f5; }
            .run-target-list span { display: block; word-break: break-all; color: #8f8a95; }
            body.flow-page[data-theme="light"] .action-link--secondary { border-color: #929f9a; color: #171717; }
            body.flow-page[data-theme="light"] .run-start, body.flow-page[data-theme="light"] .run-status,
            body.flow-page[data-theme="light"] .run-event, body.flow-page[data-theme="light"] .run-finding,
            body.flow-page[data-theme="light"] .run-summary { background: #fff; border-color: #dee2de; }
            body.flow-page[data-theme="light"] .run-start p, body.flow-page[data-theme="light"] .run-note,
            body.flow-page[data-theme="light"] .run-event p, body.flow-page[data-theme="light"] .run-finding p,
            body.flow-page[data-theme="light"] .run-summary p { color: #4d5955; }
            body.flow-page[data-theme="light"] .run-bar { background: #dee2de; }
            body.flow-page[data-theme="light"] .run-pill { background: #e4e8e6; color: #2c2c2c; }
            body.flow-page[data-theme="light"] .run-event-head span, body.flow-page[data-theme="light"] .run-finding small,
            body.flow-page[data-theme="light"] .run-empty { color: #6b7571; }
            body.flow-page[data-theme="light"] .run-target-list li { color: #4d5955; }
            body.flow-page[data-theme="light"] .run-target-list strong { color: #171717; }
            body.flow-page[data-theme="light"] .run-target-list span { color: #6b7571; }
            body.flow-page[data-theme="light"] .run-severity--high { border-color: #b3261e; color: #8f1d17; }
            body.flow-page[data-theme="light"] .run-severity--medium { border-color: #8a6100; color: #6b4a12; }
            body.flow-page[data-theme="light"] .run-severity--low { border-color: #006f9f; color: #175d76; }
            @media (prefers-reduced-motion: reduce) { .run-event { animation: none; } .run-bar span { transition: none; } }
            @media (max-width: 760px) { .run-start { align-items: start; flex-direction: column; } }
            """;

    private static String page(String title, String current, String content) {
        StringBuilder html = new StringBuilder(12_000);
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light dark">
                  <title>Kaviyo | %s</title>
                  <style>
                """.formatted(escape(title)));
        html.append(HtmlReportRenderer.STYLES);
        html.append(FLOW_STYLES);
        html.append("""
                  </style>
                </head>
                <body class="flow-page">
                  <header class="site-header flow-header">
                    <a class="brand" href="/" aria-label="Kaviyo home"><span class="brand-mark" aria-hidden="true">k</span><span>kaviyo</span></a>
                    <nav aria-label="Page navigation">
                      <a href="/">Home</a>
                      <a href="/how-it-works">How it works</a>
                      <a href="/discovery">Onboard</a>
                      <a href="/discovery/observe">Measure</a>
                      <button class="theme-toggle" data-theme-toggle type="button" aria-pressed="false" aria-label="Switch to light mode">◐</button>
                    </nav>
                    <a class="nav-action" href="/discovery">%s</a>
                  </header>
                """.formatted(escape(current)));
        html.append(content);
        html.append("""
                  <footer class="footer flow-footer"><span>Kaviyo</span><span>Product discovery workspace</span><a href="/discovery/observe#review-policy">Evidence and approval rules</a></footer>
                </body>
                </html>
                """);
        int bodyEnd = html.lastIndexOf("</body>");
        html.insert(bodyEnd, "<script src=\"/assets/theme.js\" defer></script>\n");
        if ("Record an observation".equals(title)) html.insert(bodyEnd, "<script src=\"/assets/discovery-workspace.js\" defer></script><script src=\"/assets/governance.js\" defer></script>\n");
        if ("Discovery missions".equals(title) || "Review your product catalog".equals(title)) html.insert(bodyEnd, "<script src=\"/assets/onboarding.js\" defer></script>\n");
        return html.toString();
    }

    public String discoveryWorkspaceScript() {
        return """
                (() => {
                  const api = `http://${location.hostname}:8083/api/v1/discovery`;
                  const missionSelect = document.querySelector('#mission-id');
                  const status = document.querySelector('#capture-status');
                  const saveButton = document.querySelector('#observation-form button[type="submit"]');
                  const esc = (value) => String(value ?? '').replace(/[&<>\"']/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;',"'":'&#39;'}[c]));
                  async function request(path, options = {}) {
                    const response = await fetch(`${api}${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
                    const data = await response.json().catch(() => ({}));
                    if (!response.ok) throw new Error(data.message || `Request failed (${response.status})`);
                    return data;
                  }
                  let observationCursor = null;
                  function renderTrend(container, buckets, rateKey, emptyLabel) {
                    container.replaceChildren();
                    const timeline = Array.isArray(buckets) ? buckets : [];
                    if (!timeline.some((bucket) => bucket[rateKey] != null)) { container.textContent = emptyLabel; return; }
                    const width = 240, height = 56, pad = 6;
                    const x = (index) => timeline.length < 2 ? width / 2 : pad + (index * (width - 2 * pad)) / (timeline.length - 1);
                    const y = (rate) => height - pad - rate * (height - 2 * pad);
                    const svgNS = 'http://www.w3.org/2000/svg';
                    const svg = document.createElementNS(svgNS, 'svg');
                    svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
                    svg.classList.add(rateKey === 'accuracyRate' ? 'trend-line--accuracy' : 'trend-line--mentions');
                    let run = [];
                    const flush = () => {
                      if (run.length > 1) {
                        const line = document.createElementNS(svgNS, 'polyline');
                        line.setAttribute('points', run.map((p) => p.join(',')).join(' '));
                        svg.append(line);
                      }
                      run = [];
                    };
                    timeline.forEach((bucket, index) => {
                      const rate = bucket[rateKey];
                      if (rate == null) { flush(); return; }
                      run.push([x(index), y(rate)]);
                      const dot = document.createElementNS(svgNS, 'circle');
                      dot.setAttribute('cx', x(index)); dot.setAttribute('cy', y(rate)); dot.setAttribute('r', 3);
                      const tip = document.createElementNS(svgNS, 'title');
                      tip.textContent = `${bucket.weekStart}: ${Math.round(rate * 100)}%`;
                      dot.append(tip); svg.append(dot);
                    });
                    flush();
                    container.append(svg);
                  }
                  async function refresh(loadOlder = false) {
                    try {
                      if (!loadOlder) observationCursor = null;
                      const missionId = encodeURIComponent(missionSelect.value);
                      const target = encodeURIComponent(document.querySelector('#visibility-target').value.trim());
                      const cursorQuery = loadOlder && observationCursor
                        ? `&cursorCreatedAt=${encodeURIComponent(observationCursor.createdAt)}&cursorId=${encodeURIComponent(observationCursor.id)}` : '';
                      const [missions, observations, metrics, trends] = await Promise.all([
                        request('/missions'),
                        request(`/observations?missionId=${missionId}&limit=50${cursorQuery}`),
                        request(`/metrics?missionId=${missionId}&target=${target}`),
                        request(`/trends?missionId=${missionId}&points=12&target=${target}`)
                      ]);
                      const selected = missionSelect.value;
                      if (!missions.missions.length) {
                        const empty = document.createElement('option');
                        empty.textContent = 'No missions yet';
                        empty.disabled = true; empty.selected = true;
                        missionSelect.replaceChildren(empty);
                        if (saveButton) saveButton.disabled = true;
                        status.textContent = 'Finish onboarding to create your first shopper mission.';
                        document.querySelector('#observation-summary').textContent = 'No shopper missions yet.';
                        document.querySelector('#observation-list').innerHTML = '<p>Finish onboarding to create your first mission, then recorded answers will appear here.</p>';
                        document.querySelector('#observed-count').textContent = '0';
                        document.querySelector('#mention-count').textContent = 'No observed answers yet';
                        document.querySelector('#claim-summary').textContent = 'No assessed claims yet';
                        document.querySelector('#trend-mentions').textContent = 'No missions yet.';
                        document.querySelector('#trend-accuracy').textContent = 'No missions yet.';
                        return;
                      }
                      if (saveButton) saveButton.disabled = false;
                      missionSelect.replaceChildren(...missions.missions.map((m) => {
                        const option = document.createElement('option'); option.value = m.id;
                        option.textContent = m.missionText; option.selected = m.id === selected; return option;
                      }));
                      document.querySelector('#observed-count').textContent = metrics.observedAnswers;
                      document.querySelector('#mention-count').textContent = metrics.mentionRate == null
                        ? (target ? "No observed answers yet" : "Enter a target catalog reference")
                        : `${metrics.productMentions} · mentioned in ${metrics.observedWithMentions} of ${metrics.observedAnswers} answers (${Math.round(metrics.mentionRate * 100)}%)`;
                      document.querySelector('#claim-summary').textContent = metrics.accuracyRate == null
                        ? `${metrics.supportedClaims} supported · ${metrics.contradictedClaims} contradicted · ${metrics.unknownClaims} unknown · ${metrics.staleClaims} stale claims (no assessed claims yet)`
                        : `${metrics.supportedClaims} supported · ${metrics.contradictedClaims} contradicted · ${metrics.unknownClaims} unknown · ${metrics.staleClaims} stale claims · ${Math.round(metrics.accuracyRate * 100)}% accurate (${metrics.assessedClaims} assessed)`;
                      renderTrend(document.querySelector('#trend-mentions'), trends.trend, 'mentionRate', 'No weekly mention data yet — record answers across weeks to see a trend.');
                      renderTrend(document.querySelector('#trend-accuracy'), trends.trend, 'accuracyRate', 'No weekly accuracy data yet — record answers across weeks to see a trend.');
                      const list = document.querySelector('#observation-list');
                      if (!loadOlder) list.replaceChildren();
                      if (!observations.observations.length && !loadOlder) list.innerHTML = '<p>No answers have been recorded for this mission yet.</p>';
                      for (const item of observations.observations) {
                        const card = document.createElement('article'); card.className = 'observation-card';
                        const details = document.createElement('div');
                        const mission = missions.missions.find((m) => m.id === item.missionId);
                      details.innerHTML = `<span class="observation-meta">${esc(item.collectionMethod)} · ${esc(item.provider)} · ${esc(item.observedAt || item.createdAt)}</span><h3>${esc(mission?.missionText || 'Shopper mission')}</h3><p>${item.mentions.length} product mentions · ${item.claims.length} checked claims</p>`;
                        const answer = document.createElement('blockquote'); answer.textContent = item.answer;
                      const evidence = document.createElement('div');
                      for (const mention of item.mentions) { const p = document.createElement('p'); p.textContent = `Mention: ${mention.productName || mention.brandName || mention.productReference}${mention.position ? ` · position ${mention.position}` : ''}`; evidence.append(p); }
                      for (const claim of item.claims) { const p = document.createElement('p'); p.textContent = `${claim.verdict}: ${claim.claimText}${claim.evidenceReference ? ` · ${claim.evidenceReference}` : ''}${claim.evidenceValue ? ` = ${claim.evidenceValue}` : ''}`; evidence.append(p); }
                      card.append(details, answer, evidence); list.append(card);
                      }
                      observationCursor = observations.hasMore
                        ? { createdAt: observations.nextCursorCreatedAt, id: observations.nextCursorId } : null;
                      const loadMore = document.querySelector('#load-more-observations');
                      loadMore.hidden = !observationCursor;
                      loadMore.disabled = false;
                    } catch (error) {
                      if (loadOlder) document.querySelector('#load-more-observations').disabled = false;
                      const failed = document.createElement('option');
                      failed.textContent = 'Missions unavailable';
                      missionSelect.replaceChildren(failed);
                      document.querySelector('#observed-count').textContent = 'API unavailable';
                      document.querySelector('#mention-count').textContent = '—';
                      document.querySelector('#claim-summary').textContent = 'No saved claim checks are available.';
                      document.querySelector('#trend-mentions').textContent = 'Trend unavailable while the API is down.';
                      document.querySelector('#trend-accuracy').textContent = 'Trend unavailable while the API is down.';
                      document.querySelector('#observation-summary').textContent = 'No saved observation metrics are available.';
                      document.querySelector('#observation-list').innerHTML = '<p>Connect the Kaviyo API and database to load saved answers.</p>';
                    }
                  }
                  const localTime = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16);
                  document.querySelector('#observed-at').value = localTime;
                  missionSelect.addEventListener('change', () => refresh());
                  document.querySelector('#visibility-target').addEventListener('change', () => refresh());
                  document.querySelector('#load-more-observations').addEventListener('click', (event) => {
                    event.currentTarget.disabled = true;
                    refresh(true);
                  });
                  document.querySelector('#observation-form').addEventListener('submit', async (event) => {
                    event.preventDefault(); status.textContent = 'Saving manual observation…';
                    const claimText = document.querySelector('#claim-text').value.trim();
                    const reference = document.querySelector('#evidence-reference').value.trim();
                    const positionValue = document.querySelector('#position').value;
                    const productRef = document.querySelector('#product-ref').value.trim();
                    const productName = document.querySelector('#product-name').value.trim();
                    if (productName && !productRef) { status.textContent = 'Add the catalog product reference for this mention.'; return; }
                    const observedAt = new Date(document.querySelector('#observed-at').value);
                    if (Number.isNaN(observedAt.getTime())) { status.textContent = 'Enter when you observed the answer.'; return; }
                    const body = {
                      provider: document.querySelector('#provider').value.trim(),
                      prompt: document.querySelector('#prompt').value.trim(),
                      answer: document.querySelector('#answer').value.trim(),
                      sourceUrl: document.querySelector('#source-url').value.trim() || null,
                      observedAt: observedAt.toISOString(),
                      mentions: productRef ? [{ productReference: productRef, productName: productName || null,
                        brandName: null, context: null, position: positionValue ? Number(positionValue) : null }] : [],
                      claims: claimText ? [{ claimText, productReference: productRef || null, verdict: document.querySelector('#claim-verdict').value,
                        evidenceReference: reference || null, evidenceValue: document.querySelector('#evidence-value').value.trim() || null }] : []
                    };
                    try {
                      await request(`/missions/${encodeURIComponent(missionSelect.value)}/observations`, { method: 'POST', body: JSON.stringify(body) });
                      status.textContent = 'Saved as a manually recorded observation. Kaviyo did not query the assistant.';
                      document.querySelector('#answer').value = '';
                      await refresh();
                    } catch (error) { status.textContent = error.message; }
                  });
                  const extractRun = document.querySelector('#extract-run');
                  if (extractRun) {
                    const catalogApi = `http://${location.hostname}:8083/api/v1/catalog`;
                    let catalogCache = null;
                    async function catalogProducts() {
                      if (!catalogCache) {
                        const response = await fetch(`${catalogApi}/products?limit=100`);
                        if (!response.ok) throw new Error('Catalog lookup failed.');
                        catalogCache = (await response.json()).products || [];
                      }
                      return catalogCache;
                    }
                    const matchProduct = (products, name) => {
                      const needle = String(name || '').toLowerCase().trim();
                      if (!needle) return null;
                      return products.find((p) => String(p.title || '').toLowerCase().includes(needle))
                        || products.find((p) => needle.includes(String(p.title || '').toLowerCase())) || null;
                    };
                    const useButton = (label, onUse) => {
                      const button = document.createElement('button');
                      button.type = 'button';
                      button.className = 'action-link action-link--secondary';
                      button.textContent = label;
                      button.addEventListener('click', onUse);
                      return button;
                    };
                    extractRun.addEventListener('click', async () => {
                      const provider = (document.querySelector('#extract-provider').value || '').trim();
                      const prompt = (document.querySelector('#extract-prompt').value || '').trim();
                      const answer = (document.querySelector('#extract-answer').value || '').trim();
                      const extractStatus = document.querySelector('#extract-status');
                      const box = document.querySelector('#extract-results');
                      if (!answer) { extractStatus.textContent = 'Paste the answer you saw first.'; return; }
                      extractRun.disabled = true;
                      extractStatus.textContent = 'Reading the answer — this can take a few seconds…';
                      box.replaceChildren();
                      try {
                        const suggestion = (await request('/observations/extract',
                          { method: 'POST', body: JSON.stringify({ prompt: prompt || null, answer }) })).suggestion;
                        document.querySelector('#provider').value = provider;
                        document.querySelector('#prompt').value = prompt;
                        document.querySelector('#answer').value = answer;
                        const head = document.createElement('p');
                        head.textContent = `Found ${suggestion.mentions.length} mention${suggestion.mentions.length === 1 ? '' : 's'} and ${suggestion.claims.length} claim${suggestion.claims.length === 1 ? '' : 's'}. Use what looks right; every value stays editable below.`;
                        box.append(head);
                        for (const mention of suggestion.mentions) {
                          const card = document.createElement('div');
                          card.className = 'extract-suggestion';
                          const title = document.createElement('p');
                          title.textContent = `Mention: ${mention.productName || mention.brandName || '(unnamed)'}${mention.position ? ` · #${mention.position} in answer` : ''}`;
                          card.append(title);
                          if (mention.quote) { const quote = document.createElement('p'); quote.textContent = `“${mention.quote}”`; card.append(quote); }
                          const matchNote = document.createElement('p');
                          card.append(matchNote, useButton('Use this mention', async () => {
                            document.querySelector('#product-name').value = mention.productName || mention.brandName || '';
                            document.querySelector('#position').value = mention.position || '';
                            try {
                              const matched = matchProduct(await catalogProducts(), mention.productName || mention.brandName);
                              document.querySelector('#product-ref').value = matched ? matched.productId : '';
                              matchNote.textContent = matched ? `Matched to ${matched.title} (${matched.productId}).` : 'No catalog match — enter the reference yourself.';
                            } catch { matchNote.textContent = 'Catalog lookup failed — enter the reference yourself.'; }
                          }));
                          box.append(card);
                        }
                        for (const claim of suggestion.claims) {
                          const card = document.createElement('div');
                          card.className = 'extract-suggestion';
                          const title = document.createElement('p');
                          title.textContent = `Claim: ${claim.claimText} → ${claim.suggestedVerdict}`;
                          card.append(title);
                          if (claim.rationale) { const why = document.createElement('p'); why.textContent = claim.rationale; card.append(why); }
                          card.append(useButton('Use this claim', () => {
                            document.querySelector('#claim-text').value = claim.claimText;
                            document.querySelector('#claim-verdict').value = claim.suggestedVerdict;
                            document.querySelector('.claim-details').open = true;
                          }));
                          box.append(card);
                        }
                        extractStatus.textContent = suggestion.catalogTruncated
                          ? 'Suggestions ready — catalog facts were truncated, so confirm verdicts carefully.'
                          : 'Suggestions ready — review each one, then save below.';
                      } catch (error) { extractStatus.textContent = error.message; }
                      finally { extractRun.disabled = false; }
                    });
                  }
                  refresh();
                })();
                """;
    }

    public String agentSimulationScript() {
        return """
                (() => {
                  const workspace = document.querySelector('#simulation-workspace');
                  const button = document.querySelector('#run-simulation');
                  const result = document.querySelector('#simulation-result');
                  if (!workspace || !button || !result) return;
                  const showError = (text, back) => {
                    const copy = document.createElement('p');
                    copy.textContent = text;
                    result.replaceChildren(copy);
                    reveal(result);
                    if (back) {
                      const nav = document.createElement('p');
                      const link = document.createElement('a');
                      link.href = '/discovery';
                      link.textContent = 'Back to your business setup';
                      nav.append(link);
                      result.append(nav);
                    }
                  };
                  const previewResult = result;
                  const reveal = (node) => {
                    node.hidden = false;
                    try { node.scrollIntoView({ block: 'nearest' }); node.focus({ preventScroll: true }); } catch {}
                  };
                  const plainError = (message) => {
                    const text = message || '';
                    if (/RESPONSE_TOO_LARGE/.test(text)) return 'We could not read that page because it is too large — this often happens with media-style homepages. Paste a link to one specific product page instead, or add a seller listing link above to compare prices.';
                    if (/DESTINATION_BLOCKED|blocked/i.test(text)) return 'That site blocked automated reading. Paste a link to one specific product page instead, or add a seller listing link above to compare prices.';
                    if (/redirected off merchant domain/.test(text)) return 'That address redirected away from your store domain. Check the link and try a product page on your own site.';
                    if (/PAGE_SCAN_INCOMPLETE|fetch failed|crawler HTTP/.test(text)) return 'We could not read that page right now. Check the link, then try again with one specific product page.';
                    if (/No usable public sources|Merchant page unavailable/.test(text)) return 'There is nothing to compare yet: your page could not be read and no listing link was added. Paste one product page link or one seller listing link and run it again.';
                    return text || 'The comparison could not finish. Try again.';
                  };
                  const reviewButton = document.querySelector('#review-site');
                  const marketResult = document.querySelector('#market-report');
                  const selector = document.querySelector('#comparison-product');
                  selector.onchange = () => { previewResult.replaceChildren(); previewResult.hidden=true; marketResult.replaceChildren(); };
                  let canSearch = false;
                  fetch('http://127.0.0.1:8090/simulate/capabilities').then(r => r.json()).then(data => {
                    canSearch = data.automaticSearch === true;
                    if (canSearch) document.querySelector('#listing-guidance').textContent = 'Kaviyo will look for comparable listings. You can also add a specific seller’s link.';
                  }).catch(() => {});
                  async function run(stage) {
                    const reviewing = stage === 'site';
                    const result = reviewing ? previewResult : marketResult;
                    const activeButton = reviewing ? reviewButton : button;
                    const website = document.querySelector('#merchant-website');
                    if (reviewing && (!website.value.trim() || !website.reportValidity())) {
                      website.focus(); result.textContent='Enter a valid website to review.'; reveal(result); return;
                    }
                    if (!selector.value) { result.textContent='Choose a catalog product first.'; reveal(result); selector.focus(); return; }
                    const listings = document.querySelector('#listing-urls');
                    if (!reviewing && !canSearch && !listings.value.trim()) {
                      result.textContent='Add another seller’s product link above, or choose Review website below to continue without a price comparison.';
                      reveal(result); listings.focus(); return;
                    }
                    const label = activeButton.textContent;
                    const mission = (workspace.dataset.mission || '').trim();
                    let products = [];
                    try { products = [JSON.parse(workspace.dataset.products)[Number(document.querySelector("#comparison-product").value)]]; } catch { products = []; }
                    if (!mission || !Array.isArray(products) || products.length < 1 || products.length > 10) {
                      showError('This run needs a shopper question and 1–10 products with facts. Update your business setup, then run it again.', true);
                      button.disabled = false;
                      button.textContent = label;
                      return;
                    }
                    button.disabled = true;
                    reviewButton.disabled = true; selector.disabled = true; activeButton.textContent = reviewing ? 'Reading website…' : 'Comparing prices…';
                    result.textContent = reviewing ? 'Reading the merchant page and preparing suggested edits…' : 'Checking public listings against the selected catalog product…';
                    reveal(result);
                    const body = {
                      business: workspace.dataset.business,
                      mission: workspace.dataset.mission,
                      website: reviewing ? website.value.trim() : undefined,
                      listingUrls: document.querySelector('#listing-urls').value.split(String.fromCharCode(10)).map(s => s.trim()).filter(Boolean),
                      products: products, marketComparison: true
                    };
                    try {
                      const response = await fetch('http://127.0.0.1:8090/simulate', {
                        signal: AbortSignal.timeout(125_000),
                        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
                      });
                      const data = await response.json();
                      if (!response.ok) throw new Error(data.message || `Simulation failed (${response.status})`);
                      const title = document.createElement('h3'); title.textContent = reviewing ? 'Website findings' : 'Market findings';
                      const copy = document.createElement('div');
                      for (const line of (data.answer || data.message || 'No result returned.').split(String.fromCharCode(10)).filter(text => text.trim())) {
                        const paragraph = document.createElement('p'); paragraph.textContent = line.trim(); copy.append(paragraph);
                      }
                      const note = document.createElement('small'); note.textContent = data.limitation || 'This result is a simulation and is not an observed assistant answer.';
                      result.replaceChildren(title, copy, note);
                      for (const source of data.sources || []) {
                        const row = document.createElement('p');
                        const link = document.createElement('a'); link.href = source.url; link.textContent = source.id + " · " + source.title;
                        row.append(link, ' · retrieved ' + source.retrievedAt); result.append(row);
                      }
                      const merchant = (data.sources || []).find(s => s.id === 'M1');
                      if (body.marketComparison && merchant) {
                        const excerpt = JSON.parse(merchant.excerpt).text || '';
                        const section = document.createElement('section');
                        const element = (tag, text) => { const node = document.createElement(tag); node.textContent = text; return node; };
                        section.append(element('h2','Product section preview'), element('p','Compare the words shoppers see today with the proposed wording. This preview shows the text only.'));
                        const panels = document.createElement('div'); panels.className = 'setup-profile';
                        const current = document.createElement('article'), proposed = document.createElement('article');
                        current.append(element('h3','Current page'), element('p',excerpt || 'No visible copy captured.'));
                        panels.append(current); section.append(panels); result.append(section);
                        if (data.proposal) {
                          const draft = element('textarea',''); draft.value = data.proposal.copy; draft.rows = 4; draft.maxLength = 600;
                          const label = element('label','Suggested addition · edit before approving'); label.append(draft);
                          proposed.append(element('h3','Proposed page'),element('p',excerpt),label,element('small','Supporting catalog fact: '+data.proposal.fact)); panels.append(proposed);
                          const message = element('p',''); message.setAttribute('role','status');
                          const id = crypto.randomUUID();
                          const buttons = ['Approve draft','Keep original'].map((text,index) => {
                            const control = element('button',text); control.type='button'; control.className='action-link';
                            control.onclick = async () => {
                              if (!draft.value.trim()) { message.textContent='Enter proposed copy first.'; return; }
                              buttons.forEach(b => b.disabled=true); draft.disabled=true;
                              try {
                                const saved = await fetch('http://127.0.0.1:8083/api/v1/discovery/page-reviews', {
                                  method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({id,product:products[0].name,url:merchant.url,
                                    retrievedAt:merchant.retrievedAt,currentCopy:excerpt,catalogFacts:products[0].facts,supportingFact:data.proposal.fact,proposedCopy:draft.value.trim(),
                                    sources:data.sources.map(s=>s.url),choice:index===0?'APPROVE':'KEEP_ORIGINAL'})
                                });
                                if (!saved.ok) throw new Error('Save failed: '+saved.status);
                                message.textContent=index===0?'Approved draft saved. Your live page is unchanged.':'Decision saved. Keep the original page.';
                              } catch (error) { message.textContent=error.message; buttons.forEach(b=>b.disabled=false); draft.disabled=false; }
                            }; return control;
                          });
                          section.append(...buttons,message);
                        } else section.append(element('p','No edit backed by a quoted catalog fact was returned for this page.'));
                      }
                      result.dataset.provider = data.provider || 'offline';
                      reveal(result);
                    } catch (error) {
                      result.textContent = plainError(error.message);
                      reveal(result);
                    } finally {
                      button.disabled=false; reviewButton.disabled=false; selector.disabled=false; activeButton.textContent=label;
                    }
                  }
                  button.addEventListener('click', () => run('market'));
                  reviewButton.addEventListener('click', () => run('site'));
                })();
                """;
    }

    public String draftReviewScript() {
        return """
                (() => {
                  const root = document.querySelector('#draft-workspace');
                  if (!root) return;
                  const api = `http://${location.hostname}:8083`;
                  const scanId = root.dataset.scan || '';
                  const productId = root.dataset.product || '';
                  const findingCode = root.dataset.finding || '';
                  // Mirror of MetaEvidenceDraftService.DRAFTABLE_FINDINGS: only these can be drafted.
                  const DRAFTABLE = new Set(['DESCRIPTION_MISSING', 'DESCRIPTION_THIN']);
                  const findingBody = document.querySelector('#draft-finding-body');
                  const existingBody = document.querySelector('#draft-existing-body');
                  const missionInput = document.querySelector('#draft-mission');
                  const submitButton = document.querySelector('#draft-submit');
                  const createNote = document.querySelector('#draft-create-note');
                  const detail = document.querySelector('#draft-detail');
                  const networkError = 'Couldn’t reach the Kaviyo API. Check your connection and try again.';

                  const friendlyError = ({ status, code, message }) => {
                    if (code === 'MODEL_OUTPUT_REJECTED') return 'The model’s draft didn’t pass Kaviyo’s evidence checks, so nothing was saved. Try again — a fresh attempt often passes.';
                    if (code === 'MODEL_OUTPUT_TRUNCATED') return 'The model ran out of room mid-answer. Try again in a moment.';
                    if (code === 'MODEL_REQUEST_REJECTED' || code === 'MODEL_RESPONSE_SHAPE_INVALID'
                        || code === 'MODEL_OUTPUT_INVALID_JSON' || code === 'MODEL_OUTPUT_EMPTY') return 'The model returned an unusable answer. Try again in a moment.';
                    if (code === 'PROVIDER_NOT_CONFIGURED') return 'Model credentials aren’t configured on this server, so drafts can’t run right now.';
                    if (code === 'FINDING_NOT_DRAFTABLE') return 'Only missing or thin descriptions can be drafted right now.';
                    if (code === 'FINDING_NOT_FOUND' || code === 'PRODUCT_NOT_IN_SCAN' || code === 'SCAN_NOT_FOUND'
                        || code === 'DRAFT_NOT_FOUND') return 'That scan, product, or finding no longer exists. Run a fresh scan and try again.';
                    if (code === 'DRAFT_ALREADY_DECIDED') return 'This draft already has a decision — decisions are final.';
                    if (code === 'DRAFT_NOT_APPROVED') return 'Approve the draft before exporting it.';
                    if (code === 'REVISION_NOTE_REQUIRED') return 'Add a note explaining what should change.';
                    if (status === 409) return message || 'That action conflicts with the draft’s current state.';
                    if (status >= 500) return 'Kaviyo hit an error processing that. Try again in a moment.';
                    return message || 'That didn’t work. Try again.';
                  };

                  async function call(path, options) {
                    let response;
                    try {
                      response = await fetch(`${api}${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
                    } catch { throw new Error(networkError); }
                    let data = {};
                    try { data = await response.json(); } catch { /* non-JSON body */ }
                    if (!response.ok) throw new Error(friendlyError({ status: response.status, code: data.code, message: data.message }));
                    return data;
                  }

                  const el = (tag, className, text) => {
                    const node = document.createElement(tag);
                    if (className) node.className = className;
                    if (text != null) node.textContent = text;
                    return node;
                  };
                  const statusChip = (status) => {
                    const tone = status === 'NEEDS_REVIEW' ? 'review' : status === 'REJECTED' ? 'rejected' : 'decided';
                    const label = { NEEDS_REVIEW: 'Needs review', APPROVED: 'Approved', REJECTED: 'Rejected', EXPORTED: 'Exported' }[status] || status;
                    return el('span', `draft-status draft-status--${tone}`, label);
                  };
                  const when = (iso) => { try { return new Date(iso).toLocaleString(); } catch { return ''; } };

                  async function loadFinding() {
                    try {
                      const detail = (await call(`/api/v1/catalog/scans/${encodeURIComponent(scanId)}`)).scan;
                      const item = (detail.products || []).find((entry) => entry.productId === productId);
                      const issues = (item && item.report && item.report.issues) || [];
                      const issue = issues.find((entry) => entry.code === findingCode) || issues.find((entry) => DRAFTABLE.has(entry.code));
                      findingBody.replaceChildren();
                      if (!item) {
                        findingBody.append(el('p', 'run-empty', 'That product isn’t part of this scan. Run a fresh scan and try again.'));
                        return null;
                      }
                      if (!issue) {
                        findingBody.append(el('p', 'run-empty', 'No draftable finding here — only missing or thin descriptions can be drafted right now.'));
                        return null;
                      }
                      const title = el('strong', null, `${issue.title || issue.code}`);
                      const meta = el('small', null, `${item.productTitle || productId} · ${issue.code} · ${(issue.severity || '').toLowerCase()}`);
                      const observation = el('p', null, issue.observation || '');
                      findingBody.append(title, meta, observation);
                      if (!DRAFTABLE.has(issue.code)) {
                        findingBody.append(el('p', 'run-empty', 'This finding type can’t be drafted — only missing or thin descriptions.'));
                        return null;
                      }
                      return issue;
                    } catch (error) {
                      findingBody.replaceChildren(el('p', 'run-empty', error.message));
                      return null;
                    }
                  }

                  async function loadDrafts() {
                    try {
                      const drafts = ((await call(`/api/v1/catalog/drafts?scanRunId=${encodeURIComponent(scanId)}`)).drafts || [])
                        .filter((draft) => draft.productId === productId);
                      existingBody.replaceChildren();
                      if (!drafts.length) {
                        existingBody.append(el('p', 'run-empty', 'No drafts yet for this product. Draft the first fix below.'));
                        return;
                      }
                      const list = el('ul', 'catalog-review-list');
                      for (const draft of drafts) {
                        const row = el('li');
                        const head = el('div');
                        head.append(statusChip(draft.status), el('strong', null, ` Revision ${draft.currentRevision} · ${when(draft.updatedAt)}`));
                        const open = el('button', 'action-link action-link--secondary', 'Open');
                        open.type = 'button';
                        open.addEventListener('click', () => viewDraft(draft.id));
                        row.append(head, open);
                        list.append(row);
                      }
                      existingBody.append(list);
                    } catch (error) {
                      existingBody.replaceChildren(el('p', 'run-empty', error.message));
                    }
                  }

                  function citations(list, references) {
                    const items = Array.isArray(references) ? references : [];
                    if (!items.length) {
                      list.append(el('li', null, 'No citations recorded.'));
                      return;
                    }
                    for (const citation of items) {
                      const fragment = citation.copy_fragment || '';
                      const source = citation.source_value || '';
                      const shown = source.length > 120 ? `${source.slice(0, 120)}…` : source;
                      const row = el('li');
                      row.append(el('strong', null, `“${fragment}”`), el('span', null, `from: ${shown}`));
                      list.append(row);
                    }
                  }

                  async function viewDraft(draftId) {
                    detail.hidden = false;
                    detail.replaceChildren(el('p', 'run-empty', 'Loading the draft…'));
                    try {
                      const draft = (await call(`/api/v1/catalog/drafts/${encodeURIComponent(draftId)}`)).draft;
                      const preview = await call(`/api/v1/catalog/drafts/${encodeURIComponent(draftId)}/preview`);
                      detail.replaceChildren();
                      const head = el('div');
                      head.append(el('h2', null, `Draft for ${draft.productTitle || productId} `), statusChip(draft.status));
                      const meta = el('small', null, `${draft.revision.authorLabel || 'Muse Spark'} · revision ${draft.currentRevision} · ${when(draft.updatedAt)}`);
                      const diff = el('div', 'draft-diff');
                      const current = el('blockquote', 'review-mission', preview.currentDescription || '(no current description)');
                      const proposed = el('blockquote', 'review-mission', preview.proposedDescription || '(empty proposal)');
                      diff.append(el('div', null), el('div', null));
                      diff.children[0].append(el('h3', null, 'Current'), current);
                      diff.children[1].append(el('h3', null, 'Proposed'), proposed);
                      const rationale = el('p', null, draft.revision.rationale || '');
                      const citeHead = el('h3', null, 'Cited evidence');
                      const citeList = el('ul', 'catalog-review-list');
                      citations(citeList, draft.revision.evidenceReferences);
                      detail.append(head, meta, diff, rationale, citeHead, citeList);
                      const structured = el('details');
                      structured.append(el('summary', null, 'Suggested JSON-LD'));
                      const pre = el('pre', 'draft-pre', JSON.stringify(preview.productStructuredDataPreview || {}, null, 2));
                      structured.append(pre);
                      detail.append(structured);
                      if (draft.status === 'NEEDS_REVIEW') {
                        const label = el('label', null, 'Decision note (required to request changes)');
                        const note = el('textarea', null);
                        note.rows = 2;
                        note.maxLength = 1000;
                        note.placeholder = 'Why approve, reject, or change this?';
                        label.append(note);
                        const actions = el('div', 'draft-actions');
                        const approve = el('button', 'action-link', 'Approve');
                        const reject = el('button', 'action-link action-link--secondary', 'Reject');
                        const revise = el('button', 'action-link action-link--secondary', 'Request changes');
                        for (const button of [approve, reject, revise]) button.type = 'button';
                        approve.addEventListener('click', () => decide(draft.id, 'APPROVE', note.value));
                        reject.addEventListener('click', () => decide(draft.id, 'REJECT', note.value));
                        revise.addEventListener('click', () => decide(draft.id, 'REQUEST_REVISION', note.value));
                        actions.append(approve, reject, revise);
                        detail.append(label, actions);
                      } else if (draft.status === 'APPROVED') {
                        const actions = el('div', 'draft-actions');
                        const exporter = el('button', 'action-link', 'Export change packet');
                        exporter.type = 'button';
                        exporter.addEventListener('click', () => runExport(draft.id, exporter));
                        actions.append(exporter);
                        detail.append(actions);
                      } else {
                        detail.append(el('p', 'run-empty', draft.status === 'EXPORTED'
                          ? 'Change packet exported. Decisions on this draft are complete.'
                          : 'Decisions on this draft are complete.'));
                      }
                      detail.scrollIntoView({ block: 'start' });
                    } catch (error) {
                      detail.replaceChildren(el('p', 'run-empty', error.message));
                    }
                  }

                  async function decide(draftId, decision, note) {
                    try {
                      await call(`/api/v1/catalog/drafts/${encodeURIComponent(draftId)}/decision`,
                        { method: 'POST', body: JSON.stringify({ decision, note: note || null }) });
                      await viewDraft(draftId);
                      await loadDrafts();
                    } catch (error) {
                      createNote.textContent = error.message;
                    }
                  }

                  async function runExport(draftId, button) {
                    button.disabled = true;
                    try {
                      const packet = (await call(`/api/v1/catalog/drafts/${encodeURIComponent(draftId)}/export`, { method: 'POST' })).packet;
                      const panel = el('div');
                      panel.append(el('h3', null, 'Change packet'),
                        el('small', null, `Export ${packet.exportId} · ${when(packet.generatedAt)}`),
                        el('pre', 'draft-pre', JSON.stringify(packet.payload || {}, null, 2)));
                      detail.append(panel);
                      await loadDrafts();
                    } catch (error) {
                      createNote.textContent = error.message;
                    } finally {
                      button.disabled = false;
                    }
                  }

                  submitButton.addEventListener('click', async () => {
                    const mission = (missionInput.value || '').trim();
                    if (!mission) {
                      createNote.textContent = 'Describe the shopper need first — the draft is written for that buyer.';
                      return;
                    }
                    if (mission.length > 600) {
                      createNote.textContent = 'Shopper need must be 600 characters or fewer.';
                      return;
                    }
                    submitButton.disabled = true;
                    createNote.textContent = 'Asking Muse for a cited draft — this can take up to a minute…';
                    try {
                      const created = await call(`/api/v1/catalog/scans/${encodeURIComponent(scanId)}/products/${encodeURIComponent(productId)}/drafts`,
                        { method: 'POST', body: JSON.stringify({ findingCode, shopperQuestion: mission }) });
                      createNote.textContent = '';
                      await viewDraft(created.draft.id);
                      await loadDrafts();
                    } catch (error) {
                      createNote.textContent = error.message;
                    } finally {
                      submitButton.disabled = false;
                    }
                  });

                  (async () => {
                    const issue = await loadFinding();
                    await loadDrafts();
                    if (!issue) {
                      missionInput.disabled = true;
                      submitButton.disabled = true;
                    }
                  })();
                })();
                """;
    }

    public String priceCompareScript() {
        return """
                (() => {
                  const root = document.querySelector('#compare-workspace');
                  if (!root) return;
                  const api = `http://${location.hostname}:8083`;
                  const productId = root.dataset.product || '';
                  const baseline = document.querySelector('#compare-baseline');
                  const run = document.querySelector('#compare-run');
                  const status = document.querySelector('#compare-status');
                  const results = document.querySelector('#compare-results');
                  const inputs = [1, 2, 3].map((index) => document.querySelector(`#compare-url-${index}`));
                  const networkError = 'Couldn’t reach the Kaviyo API. Check your connection and try again.';

                  const friendlyError = ({ status, code, message }) => {
                    if (code === 'COMPARISON_URL_INVALID') return 'Each entry must be an http(s) page URL.';
                    if (code === 'COMPARISON_REQUEST_INVALID') return 'Paste between one and three page URLs.';
                    if (code === 'PRODUCT_NOT_FOUND') return 'That product isn’t in this workspace anymore. Confirm the catalog again.';
                    if (status >= 500) return 'Kaviyo hit an error processing that. Try again in a moment.';
                    return message || 'That didn’t work. Try again.';
                  };

                  async function call(path, options) {
                    let response;
                    try {
                      response = await fetch(`${api}${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
                    } catch { throw new Error(networkError); }
                    let data = {};
                    try { data = await response.json(); } catch { /* non-JSON body */ }
                    if (!response.ok) throw new Error(friendlyError({ status: response.status, code: data.code, message: data.message }));
                    return data;
                  }

                  const el = (tag, className, text) => {
                    const node = document.createElement(tag);
                    if (className) node.className = className;
                    if (text != null) node.textContent = text;
                    return node;
                  };
                  const chip = (match) => {
                    const tone = match === 'MATCH' ? 'decided' : match === 'DIFFER' ? 'rejected' : 'review';
                    const label = match === 'MATCH' ? 'Match' : match === 'DIFFER' ? 'Differs' : 'Unknown';
                    return el('span', `draft-status draft-status--${tone}`, label);
                  };
                  const money = (amount, currency) => amount == null
                    ? 'no readable price' : `${amount}${currency ? ` ${currency}` : ''}`;
                  const when = (iso) => { try { return new Date(iso).toLocaleString(); } catch { return iso || ''; } };

                  function render(report) {
                    results.replaceChildren();
                    for (const item of report.comparisons || []) {
                      const card = el('article', 'run-finding');
                      const link = el('a', 'template-link', item.sourceUrl || 'source');
                      link.href = item.sourceUrl || '#';
                      link.target = '_blank';
                      link.rel = 'noopener';
                      const meta = el('small', null,
                        `fetched ${when(item.fetchedAt)} · ${String(item.fetchStatus || '').toLowerCase().replaceAll('_', ' ')}${item.httpStatus ? ` · HTTP ${item.httpStatus}` : ''}`);
                      card.append(link, meta);
                      const priceRow = el('p', null, `Price: ${money(item.price, item.currency)} `);
                      priceRow.append(chip(item.priceMatch));
                      const availRow = el('p', null, `Availability: ${item.availability || 'no readable availability'} `);
                      availRow.append(chip(item.availabilityMatch));
                      card.append(priceRow, availRow);
                      for (const note of item.notes || []) card.append(el('p', null, note));
                      results.append(card);
                    }
                    if (!results.children.length) results.append(el('p', 'run-empty', 'No comparisons came back. Try again.'));
                  }

                  async function loadBaseline() {
                    try {
                      const products = (await call('/api/v1/catalog/products?limit=100')).products || [];
                      const product = products.find((entry) => entry.productId === productId);
                      if (!product) {
                        baseline.textContent = 'That product isn’t in this workspace. Confirm the catalog again.';
                        run.disabled = true;
                        return;
                      }
                      baseline.textContent = `${product.title || productId}: ${money(product.price, product.currency)} · ${product.availability || 'unknown availability'}`;
                    } catch (error) {
                      baseline.textContent = error.message;
                      run.disabled = true;
                    }
                  }

                  run.addEventListener('click', async () => {
                    const urls = inputs.map((input) => (input.value || '').trim()).filter(Boolean);
                    if (!urls.length) {
                      status.textContent = 'Paste at least one page URL to compare.';
                      return;
                    }
                    run.disabled = true;
                    status.textContent = 'Fetching each page once — this can take a few seconds…';
                    try {
                      const report = (await call(`/api/v1/catalog/products/${encodeURIComponent(productId)}/price-comparison`,
                        { method: 'POST', body: JSON.stringify({ sourceUrls: urls }) })).report;
                      render(report);
                      status.textContent = '';
                    } catch (error) {
                      status.textContent = error.message;
                    } finally {
                      run.disabled = false;
                    }
                  });

                  loadBaseline();
                })();
                """;
    }

    public String governanceScript() {
        return """
                (() => {
                  const alertsBox = document.querySelector('#governance-alerts');
                  const accuracyBox = document.querySelector('#governance-accuracy');
                  if (!alertsBox && !accuracyBox) return;
                  const api = `http://${location.hostname}:8083/api/v1/discovery`;

                  async function request(path) {
                    const response = await fetch(`${api}${path}`, { headers: { 'Content-Type': 'application/json' } });
                    const data = await response.json().catch(() => ({}));
                    if (!response.ok) throw new Error(data.message || `Request failed (${response.status})`);
                    return data;
                  }

                  const el = (tag, className, text) => {
                    const node = document.createElement(tag);
                    if (className) node.className = className;
                    if (text != null) node.textContent = text;
                    return node;
                  };
                  const when = (iso) => { try { return new Date(iso).toLocaleString(); } catch { return iso || ''; } };

                  async function loadAlerts() {
                    try {
                      const items = (await request('/contradictions?limit=20')).contradictions || [];
                      alertsBox.replaceChildren();
                      if (!items.length) {
                        alertsBox.append(el('p', 'run-empty', 'No contradictions recorded. When a saved answer contradicts your catalog, it appears here.'));
                        return;
                      }
                      const list = el('ul', 'catalog-review-list');
                      for (const item of items) {
                        const row = el('li');
                        row.append(el('strong', null, item.claimText || '(empty claim)'));
                        const context = [`seen ${when(item.observedAt)}`, item.provider || 'unknown source']
                          .concat((item.products || []).map((ref) => `ref ${ref}`));
                        row.append(el('span', null, context.join(' · ')));
                        if (item.evidenceReference || item.evidenceValue) {
                          row.append(el('span', null, `catalog evidence: ${item.evidenceReference || '—'}${item.evidenceValue ? ` = ${item.evidenceValue}` : ''}`));
                        }
                        list.append(row);
                      }
                      alertsBox.append(list);
                    } catch (error) {
                      alertsBox.replaceChildren(el('p', 'run-empty', `Contradiction alerts unavailable: ${error.message}`));
                    }
                  }

                  async function loadAccuracy() {
                    try {
                      const buckets = (await request('/trends?points=12')).trend || [];
                      const assessed = buckets.filter((bucket) => bucket.accuracyRate != null);
                      accuracyBox.replaceChildren();
                      if (!assessed.length) {
                        accuracyBox.append(el('p', 'run-empty', 'No assessed claims yet. Accuracy appears here once recorded answers carry checked claims.'));
                        return;
                      }
                      const table = el('table', 'policy-table');
                      const head = el('thead');
                      const headRow = el('tr');
                      headRow.append(el('th', null, 'Week'), el('th', null, 'Assessed claims'), el('th', null, 'Accurate'));
                      head.append(headRow);
                      const body = el('tbody');
                      for (const bucket of assessed) {
                        const row = el('tr');
                        row.append(el('td', null, bucket.weekStart || ''),
                          el('td', null, String(bucket.assessedClaims ?? 0)),
                          el('td', null, `${Math.round(bucket.accuracyRate * 100)}%`));
                        body.append(row);
                      }
                      table.append(head, body);
                      accuracyBox.append(table);
                    } catch (error) {
                      accuracyBox.replaceChildren(el('p', 'run-empty', `Weekly accuracy unavailable: ${error.message}`));
                    }
                  }

                  loadAlerts();
                  loadAccuracy();
                })();
                """;
    }

    public String scanRunScript() {
        return """
                (() => {
                  const root = document.querySelector('#scan-run-workspace');
                  if (!root) return;
                  const api = `http://${location.hostname}:8083`;
                  const pill = document.querySelector('#run-pill');
                  const count = document.querySelector('#run-count');
                  const fill = document.querySelector('#run-bar-fill');
                  const note = document.querySelector('#run-note');
                  const events = document.querySelector('#run-events');
                  const findingList = document.querySelector('#run-finding-list');
                  const summary = document.querySelector('#run-summary');
                  const startSection = document.querySelector('#run-start');
                  const startButton = document.querySelector('#start-scan');
                  const startNote = document.querySelector('#run-start-note');
                  const seen = new Set();
                  let timer = null;

                  const clock = () => new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
                  const fetchStatusLabel = (status) => ({
                    FETCHED: 'page fetched', DESTINATION_BLOCKED: 'destination blocked', FETCH_FAILED: 'fetch failed',
                    ROBOTS_BLOCKED: 'blocked by robots rules', UNSUPPORTED_URL: 'unsupported URL'
                  }[status] || String(status || 'unknown').toLowerCase().replaceAll('_', ' '));
                  const hostname = (url) => { try { return new URL(url).hostname; } catch { return 'unparseable URL'; } };

                  async function catalogSnapshot() {
                    const first = await (async () => {
                      const response = await fetch(`${api}/api/v1/catalog/products?limit=100&offset=0`);
                      if (!response.ok) throw new Error(`Catalog lookup failed (${response.status})`);
                      return response.json();
                    })();
                    const products = first.products || [];
                    let total = products.length;
                    let more = first.hasMore;
                    let offset = products.length;
                    while (more && offset < 5000) {
                      const response = await fetch(`${api}/api/v1/catalog/products?limit=100&offset=${offset}`);
                      if (!response.ok) throw new Error(`Catalog lookup failed (${response.status})`);
                      const page = await response.json();
                      const items = page.products || [];
                      if (!items.length) break;
                      total += items.length;
                      more = page.hasMore;
                      offset += items.length;
                    }
                    return { products, total };
                  }

                  async function preloadTargets() {
                    const list = document.querySelector('#run-target-list');
                    if (!list || root.dataset.scan || !startButton) return;
                    list.replaceChildren();
                    const loading = document.createElement('li');
                    loading.textContent = 'Loading confirmed catalog…';
                    list.append(loading);
                    try {
                      const { products, total } = await catalogSnapshot();
                      list.replaceChildren();
                      if (!products.length) {
                        startNote.textContent = 'No confirmed products yet. Confirm a catalog first, then return here to watch the scan.';
                        startButton.disabled = true;
                        return;
                      }
                      note.textContent = total > products.length
                        ? `Ready to scan ${products.length} of ${total} confirmed products (100 per run).`
                        : `Ready to scan ${products.length} confirmed product${products.length === 1 ? '' : 's'} listed below.`;
                      for (const item of products.slice(0, 10)) {
                        const li = document.createElement('li');
                        const title = document.createElement('strong');
                        title.textContent = item.title || item.productId;
                        const url = document.createElement('span');
                        url.textContent = item.productUrl || 'no product URL saved';
                        li.append(title, url);
                        if (item.productId) {
                          const compare = document.createElement('a');
                          compare.className = 'template-link';
                          compare.href = `/discovery/compare?product=${encodeURIComponent(item.productId)}`;
                          compare.textContent = 'Compare outside prices →';
                          li.append(compare);
                        }
                        list.append(li);
                      }
                      if (total > 10) {
                        const more = document.createElement('li');
                        more.textContent = total > products.length
                          ? `…and ${total - 10} more confirmed products (${products.length} in this run).`
                          : `…and ${total - 10} more confirmed products.`;
                        list.append(more);
                      }
                    } catch (error) {
                      list.replaceChildren();
                      startNote.textContent = `${error.message}. Check your connection and try again.`;
                    }
                  }

                  function addEvent(icon, title, message, tone) {
                    const item = document.createElement('li');
                    item.className = `run-event${tone ? ` run-event--${tone}` : ''}`;
                    const badge = document.createElement('span');
                    badge.className = 'run-event-icon';
                    badge.textContent = icon;
                    badge.setAttribute('aria-hidden', 'true');
                    const body = document.createElement('div');
                    const head = document.createElement('div');
                    head.className = 'run-event-head';
                    const heading = document.createElement('strong');
                    heading.textContent = title;
                    const time = document.createElement('span');
                    time.textContent = clock();
                    head.append(heading, time);
                    const copy = document.createElement('p');
                    copy.textContent = message;
                    body.append(head, copy);
                    item.append(badge, body);
                    events.prepend(item);
                    while (events.children.length > 30) events.lastChild.remove();
                  }

                  function addFinding(productTitle, issue, scanId, productId) {
                    const empty = findingList.querySelector('.run-empty');
                    if (empty) empty.remove();
                    const card = document.createElement('article');
                    card.className = 'run-finding';
                    const chip = document.createElement('span');
                    const severity = String(issue.severity || 'LOW').toUpperCase();
                    chip.className = `run-severity run-severity--${severity.toLowerCase()}`;
                    chip.textContent = severity;
                    const title = document.createElement('strong');
                    title.textContent = issue.title || issue.code || 'Finding';
                    const meta = document.createElement('small');
                    meta.textContent = `${productTitle} · ${issue.code || ''}`.trim();
                    const detail = document.createElement('p');
                    detail.textContent = issue.observation || '';
                    card.append(chip, title, meta, detail);
                    if (['DESCRIPTION_MISSING', 'DESCRIPTION_THIN'].includes(issue.code)) {
                      const fix = document.createElement('a');
                      fix.className = 'back-link';
                      fix.href = `/discovery/drafts?scan=${encodeURIComponent(scanId)}&product=${encodeURIComponent(productId)}&finding=${encodeURIComponent(issue.code)}`;
                      fix.textContent = 'Draft a fix →';
                      card.append(fix);
                    }
                    findingList.append(card);
                  }

                  function setPill(status) {
                    pill.textContent = { QUEUED: 'Queued', RUNNING: 'Scanning', COMPLETED: 'Complete', PARTIAL: 'Partial', FAILED: 'Failed' }[status] || status;
                    pill.dataset.tone = status === 'COMPLETED' ? 'good' : status === 'FAILED' ? 'bad' : status === 'PARTIAL' ? 'warn' : 'busy';
                  }

                  async function poll(scanId) {
                    let detail;
                    try {
                      const response = await fetch(`${api}/api/v1/catalog/scans/${encodeURIComponent(scanId)}`);
                      if (!response.ok) throw new Error(`Scan lookup failed (${response.status})`);
                      detail = (await response.json()).scan;
                    } catch (error) {
                      note.textContent = `${error.message}. Check your connection and try again.`;
                      timer = setTimeout(() => poll(scanId), 3000);
                      return;
                    }
                    const status = detail.status || 'UNKNOWN';
                    setPill(status);
                    const total = Number(detail.requestedProducts || (detail.products || []).length || 0);
                    const done = Number(detail.completedProducts || 0);
                    count.textContent = total ? `${Math.min(done, total)} of ${total} products` : `${done} products`;
                    fill.style.width = total ? `${Math.round((100 * Math.min(done, total)) / total)}%` : '0%';
                    if (status === 'QUEUED') note.textContent = 'Queued — waiting for a scan worker to pick up this run…';
                    else if (status === 'RUNNING') note.textContent = total && done < total
                      ? `Fetching and checking product ${Math.min(done, total) + 1} of ${total}…`
                      : 'Finishing the last checks…';
                    for (const item of detail.products || []) {
                      if (seen.has(item.productId)) continue;
                      seen.add(item.productId);
                      const title = item.productTitle || item.productId;
                      const issues = (item.report && item.report.issues) || [];
                      const fetchLabel = fetchStatusLabel(item.page && item.page.status);
                      const pageUrl = item.page && (item.page.requestedUrl || item.page.finalUrl);
                      addEvent(title.charAt(0).toUpperCase(), `Checked ${title}`,
                        `${issues.length} finding${issues.length === 1 ? '' : 's'} · ${fetchLabel}${pageUrl ? ` · ${pageUrl}` : ''}.`, issues.length ? 'warn' : 'good');
                      for (const issue of issues) addFinding(title, issue, scanId, item.productId);
                    }
                    if (status === 'COMPLETED' || status === 'PARTIAL' || status === 'FAILED') {
                      finish(scanId, detail, status);
                      return;
                    }
                    timer = setTimeout(() => poll(scanId), 800);
                  }

                  function finish(scanId, detail, status) {
                    if (timer) clearTimeout(timer);
                    const issues = (detail.report && detail.report.issues) || [];
                    const highs = issues.filter((issue) => issue.severity === 'HIGH').length;
                    addEvent(status === 'FAILED' ? '!' : '✓',
                      status === 'FAILED' ? 'Scan failed' : status === 'PARTIAL' ? 'Scan partially complete' : 'Scan complete',
                      status === 'FAILED' ? 'The run could not finish. Check the API logs and retry.'
                        : `${issues.length} finding${issues.length === 1 ? '' : 's'} across ${seen.size} products · ${highs} high severity.`,
                      status === 'FAILED' ? 'bad' : 'good');
                    note.textContent = `Run ${scanId} finished with status ${status}.`;
                    const heading = document.createElement('h2');
                    heading.textContent = 'What changed on this run';
                    const copy = document.createElement('p');
                    copy.textContent = status === 'FAILED'
                      ? 'No findings were saved. Retry the scan once the API is healthy.'
                      : 'Review each finding with its evidence, then record how assistants describe these products to track discovery over time.';
                    const pages = detail.products || [];
                    const unfetched = pages.filter((item) => !item.page || item.page.status !== 'FETCHED').length;
                    if (status !== 'FAILED' && pages.length && unfetched) {
                      copy.textContent += ` Kaviyo could not fetch ${unfetched} of ${pages.length} pages (blocked or unreachable URLs) — price, availability, and structured-data checks need a fetchable product page.`;
                    }
                    const actions = document.createElement('div');
                    actions.className = 'run-summary-actions';
                    const observe = document.createElement('a');
                    observe.className = 'action-link';
                    observe.href = '/discovery/observe';
                    observe.textContent = 'Record an assistant answer';
                    const again = document.createElement('a');
                    again.className = 'back-link';
                    again.href = '/discovery/run';
                    again.textContent = 'Run another scan →';
                    actions.append(observe, again);
                    summary.replaceChildren(heading, copy, actions);
                    summary.hidden = false;
                  }

                  async function start() {
                    startButton.disabled = true;
                    startButton.textContent = 'Starting…';
                    setPill('QUEUED');
                    try {
                      const { products: targets, total } = await catalogSnapshot();
                      const ids = targets.map((item) => item.productId).filter(Boolean);
                      if (!ids.length) {
                        startNote.textContent = 'No confirmed products yet. Confirm a catalog first, then return here to watch the scan.';
                        startButton.disabled = false;
                        startButton.textContent = 'Scan catalog now';
                        return;
                      }
                      const sites = [...new Set(targets.map((item) => hostname(item.productUrl)))];
                      const scope = total > ids.length ? `${ids.length} of ${total}` : `${ids.length}`;
                      addEvent('◷', 'Scan queued', `Checking ${scope} product${ids.length === 1 ? '' : 's'} against confirmed catalog facts · sites: ${sites.slice(0, 5).join(', ')}${sites.length > 5 ? ` (+${sites.length - 5} more)` : ''}${total > ids.length ? ' (100 per run)' : ''}.`, 'busy');
                      const queued = await fetch(`${api}/api/v1/catalog/scans`, {
                        method: 'POST', headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ productIds: ids })
                      });
                      if (queued.status === 503) throw new Error('Page scanning is disabled on this API deployment');
                      if (!queued.ok) throw new Error(`Scan request failed (${queued.status})`);
                      const created = await queued.json();
                      startSection.hidden = true;
                      note.textContent = `Run ${created.scanRunId} queued. Watching every product as it finishes.`;
                      history.replaceState(null, '', `/discovery/run?scan=${encodeURIComponent(created.scanRunId)}`);
                      poll(created.scanRunId);
                    } catch (error) {
                      note.textContent = `${error.message}. Check your connection and try again.`;
                      startButton.disabled = false;
                      startButton.textContent = 'Scan catalog now';
                    }
                  }

                  if (root.dataset.scan) {
                    addEvent('◷', 'Resuming live view', 'Replaying saved progress, then following the run to completion.', 'busy');
                    poll(root.dataset.scan);
                  } else if (startButton) {
                    startButton.addEventListener('click', start);
                    preloadTargets();
                  }
                })();
                """;
    }

    public String onboardingScript() {
        return """
                (() => {
                  const form = document.querySelector('.discovery-setup');
                  const status = document.querySelector('#onboarding-status');
                  if (!form) return;
                  const fileInput = document.querySelector('#catalog-file');
                  const csvField = document.querySelector('#catalog-csv');
                  const filename = document.querySelector('#file-name');
                  const hasProducts = !!(fileInput && csvField && form.elements.product);
                  const key = 'kaviyo.setup.' + (form.elements.accountEmail?.value || 'local').toLowerCase();
                  const fields = ['business','website','businessType','background','policies','mission1','mission2','mission3','truthSource','product','productUrl','facts','titleColumn','csv'];
                  let fileName = '';
                  const goalList = form.querySelector('#goal-list');
                  const goalCount = form.querySelector('#goal-count');
                  const goalAdd = form.querySelector('#goal-add');
                  const hasGoals = !!(goalList && goalCount && goalAdd);
                  const MAX_GOALS = 5;
                  const goalRows = () => hasGoals ? [...goalList.querySelectorAll('.goal-row')] : [];
                  function syncGoalRows() {
                    if (!hasGoals) return;
                    const rows = goalRows();
                    rows.forEach((row, index) => {
                      const selects = row.querySelectorAll('select');
                      selects[0].name = `goalMetric${index}`;
                      row.querySelector('input').name = `goalTarget${index}`;
                      selects[1].name = `goalPeriod${index}`;
                      row.querySelector('.goal-remove').disabled = rows.length < 2;
                    });
                    goalCount.value = String(rows.length);
                    goalAdd.disabled = rows.length >= MAX_GOALS;
                  }
                  if (hasGoals) {
                    goalAdd.addEventListener('click', () => {
                      if (goalRows().length >= MAX_GOALS) return;
                      const fresh = goalList.firstElementChild.cloneNode(true);
                      fresh.querySelector('input').value = '';
                      goalList.append(fresh);
                      syncGoalRows();
                    });
                    goalList.addEventListener('click', (event) => {
                      if (!event.target.classList.contains('goal-remove') || goalRows().length < 2) return;
                      event.target.closest('.goal-row').remove();
                      syncGoalRows();
                    });
                    syncGoalRows();
                  }
                  function saveDraft() {
                    const draft = {};
                    for (const name of fields) { const field = form.elements[name]; if (field) draft[name] = field.value; }
                    if (hasGoals) {
                      draft.goals = goalRows().map(row => {
                        const selects = row.querySelectorAll('select');
                        return [selects[0].value, row.querySelector('input').value, selects[1].value];
                      });
                    }
                    try { sessionStorage.setItem(key, JSON.stringify({...draft, fileName})); }
                    catch { status.textContent = 'Your browser could not save this draft. Keep this tab open.'; }
                  }
                  try {
                    const draft = JSON.parse(sessionStorage.getItem(key) || 'null');
                    if (draft) {
                      for (const name of fields) { const field = form.elements[name]; if (field && typeof draft[name] === 'string') field.value = draft[name]; }
                      if (hasGoals && Array.isArray(draft.goals)) {
                        while (goalRows().length < Math.min(Math.max(draft.goals.length, 1), MAX_GOALS)) goalAdd.click();
                        goalRows().forEach((row, index) => {
                          const saved = draft.goals[index] || [];
                          const selects = row.querySelectorAll('select');
                          if (saved[0]) selects[0].value = saved[0];
                          row.querySelector('input').value = saved[1] || '';
                          if (saved[2]) selects[1].value = saved[2];
                        });
                        syncGoalRows();
                      }
                      fileName = draft.fileName || '';
                      if (csvField && csvField.value && filename) filename.textContent = fileName + ' · restored';
                      const manualEntry = form.querySelector('.manual-entry');
                      if (manualEntry && form.elements.product && form.elements.product.value) manualEntry.open = true;
                    }
                  } catch {}
                  form.addEventListener('input', saveDraft);
                  if (fileInput && csvField && filename) {
                    const clear = document.createElement('button'); clear.type = 'button'; clear.textContent = 'Remove CSV';
                    filename.after(clear);
                    clear.onclick = () => { fileInput.value = ''; csvField.value = ''; fileName = ''; filename.textContent = 'No file selected'; saveDraft(); };
                    fileInput.addEventListener('change', async () => {
                      const file = fileInput.files?.[0]; if (!file) return;
                      csvField.value = ''; fileName = ''; saveDraft();
                      if (file.size > 1_048_576) { clear.click(); status.textContent = 'Choose a CSV smaller than 1 MB.'; return; }
                      try { const contents = await file.text(); if (file !== fileInput.files?.[0]) return;
                        csvField.value = contents; fileName = file.name; filename.textContent = fileName; saveDraft();
                      } catch { clear.click(); status.textContent = 'The file could not be read.'; }
                    });
                  }
                  form.querySelectorAll('.expand-panel').forEach((panel) => {
                    const toggle = panel.querySelector('.expand-panel__button');
                    const body = panel.querySelector('.expand-panel__body');
                    if (!toggle || !body) return;
                    toggle.addEventListener('click', () => {
                      const open = toggle.getAttribute('aria-expanded') !== 'true';
                      toggle.setAttribute('aria-expanded', String(open));
                      body.hidden = !open;
                      if (open && panel.classList.contains('store-connect-panel')) {
                        const storeUrlField = form.querySelector('#connect-store-url');
                        const website = form.elements.website ? form.elements.website.value.trim() : '';
                        if (storeUrlField && !storeUrlField.value.trim() && website) {
                          storeUrlField.value = website;
                          saveDraft();
                        }
                      }
                    });
                  });
                  const provider = form.querySelector('#connect-provider');
                  const connectStatus = form.querySelector('#connect-status');
                  const connectFetch = form.querySelector('#connect-fetch');
                  if (provider && connectFetch) {
                    const toggleProvider = () => form.querySelectorAll('[data-connect]').forEach(field => {
                      field.hidden = field.dataset.connect !== provider.value;
                    });
                    provider.addEventListener('change', toggleProvider);
                    toggleProvider();
                    const cell = (value) => {
                      const text = String(value ?? '');
                      return /[",\\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
                    };
                    connectFetch.addEventListener('click', async () => {
                      const payload = { provider: provider.value };
                      if (provider.value === 'WOOCOMMERCE') {
                        payload.storeUrl = form.querySelector('#connect-store-url').value.trim();
                        payload.consumerKey = form.querySelector('#connect-key').value.trim();
                        payload.consumerSecret = form.querySelector('#connect-secret').value;
                      } else if (provider.value === 'SHOPIFY') {
                        payload.storeUrl = form.querySelector('#connect-shopify-domain').value.trim();
                        payload.apiKey = form.querySelector('#connect-shopify-token').value.trim();
                      } else {
                        payload.apiKey = form.querySelector('#connect-apikey').value.trim();
                        payload.siteId = form.querySelector('#connect-site').value.trim();
                      }
                      connectFetch.disabled = true;
                      connectStatus.textContent = 'Fetching products from your store…';
                      try {
                        const response = await fetch('http://127.0.0.1:8083/api/v1/catalog/connect', {
                          method: 'POST', headers: { 'Content-Type': 'application/json' },
                          body: JSON.stringify(payload), signal: AbortSignal.timeout(30000)
                        });
                        const data = await response.json();
                        if (!response.ok) throw new Error(data.message || `Fetch failed (${response.status})`);
                        const rows = (data.products || []).map(p =>
                          [p.name, p.sku, p.price, p.currency, p.availability, p.url, p.description].map(cell).join(','));
                        csvField.value = ['name,sku,price,currency,availability,url,description', ...rows].join('\\n');
                        fileName = `${data.productCount} products from ${data.store || provider.value}`;
                        filename.textContent = fileName;
                        connectStatus.textContent = `${data.productCount} products ready — continue below to review them.`;
                        saveDraft();
                      } catch (error) {
                        connectStatus.textContent = error.message || 'The store could not be read.';
                      } finally {
                        form.querySelector('#connect-secret').value = '';
                        form.querySelector('#connect-apikey').value = '';
                        form.querySelector('#connect-shopify-token').value = '';
                        connectFetch.disabled = false;
                      }
                    });
                  }
                  form.addEventListener('submit', async (event) => {
                    if (!hasProducts) { saveDraft(); return; }
                    event.preventDefault();
                    const file = fileInput.files?.[0];
                    if (!file && !csvField.value && !form.elements.product.value.trim()) { status.textContent = 'Choose your catalog or enter a product below.'; return; }
                    try {
                      if (file) { if (file.size > 1_048_576) return; csvField.value = await file.text(); fileName = file.name; }
                      saveDraft();
                      HTMLFormElement.prototype.submit.call(form);
                    } catch { status.textContent = 'The CSV could not be read. Try exporting it as UTF-8 CSV.'; }
                  });
                })();
                """;
    }

    public String themeScript() {
        return """
                (() => {
                  const body = document.body;
                  const button = document.querySelector('[data-theme-toggle]');
                  const saved = localStorage.getItem('kaviyo-theme-v2');
                  const flow = body.classList.contains('flow-page');
                  body.dataset.theme = flow ? 'light' : (saved || 'light');
                  const update = () => {
                    const dark = body.dataset.theme === 'dark';
                    if (!button) return;
                    button.setAttribute('aria-pressed', String(dark));
                    button.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
                    button.title = dark ? 'Switch to light mode' : 'Switch to dark mode';
                    button.textContent = dark ? '☼' : '◐';
                  };
                  update();
                  button?.addEventListener('click', () => {
                    if (flow) return;
                    body.dataset.theme = body.dataset.theme === 'dark' ? 'light' : 'dark';
                    localStorage.setItem('kaviyo-theme-v2', body.dataset.theme);
                    update();
                  });
                })();
                """;
    }

    private static String productsJson(List<CatalogItem> products) {
        return "[" + products.stream().map(item -> "{\"name\":" + jsonString(item.name())
                + ",\"facts\":" + jsonString(item.facts()) + ",\"url\":" + jsonString(orEmpty(item.url())) + "}").collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : orEmpty(value).toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }

    private static String discoveryProgress(String active) {
        return """
                <ol class="discovery-progress" aria-label="Discovery workflow">
                  <li %s><span>1</span> Business + catalog</li>
                  <li %s><span>2</span> Review your products</li>
                  <li %s><span>3</span> Check a product page</li>
                </ol>
                """.formatted(
                active.equals("mission") ? "aria-current=\"step\"" : active.equals("fit") || active.equals("simulate") ? "class=\"is-complete\"" : "",
                active.equals("fit") ? "aria-current=\"step\"" : active.equals("simulate") ? "class=\"is-complete\"" : "",
                active.equals("simulate") ? "aria-current=\"step\"" : "");
    }

    private static String progress(String active) {
        return """
                <nav class="flow-progress" aria-label="Sample audit steps">
                  <span class="%s" %s>Choose data</span>
                  <span class="%s" %s>Review scope</span>
                  <span>See findings</span>
                </nav>
                """.formatted(
                active.equals("Choose data") ? "active" : "",
                active.equals("Choose data") ? "aria-current=\"step\"" : "",
                active.equals("Review scope") ? "active" : "",
                active.equals("Review scope") ? "aria-current=\"step\"" : "");
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    private static String query(String value) {
        return java.net.URLEncoder.encode(orEmpty(value), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
