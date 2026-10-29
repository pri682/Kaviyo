package com.kaviyo.app;

import com.kaviyo.catalog.Product;

import java.util.List;

/** Renders the guided, synthetic-data walkthrough shown after leaving the home page. */
public final class AuditFlowRenderer {
    public String renderHowItWorks() {
        return page("How Kaviyo works", "How it works", """
                <main class="flow-main overview-main">
                  <p class="eyebrow">A practical product-quality workflow</p>
                  <h1>From product information to a reviewable next step.</h1>
                  <p class="flow-intro">Kaviyo is designed to help small businesses understand where product details need attention. The sample walkthrough demonstrates the audit stage with synthetic data.</p>
                  <div class="overview-steps">
                    <article><span>Bring product data</span><h2>Start with the catalog.</h2><p>Product facts come from the merchant's catalog or another source the business trusts.</p></article>
                    <article><span>Compare the sources</span><h2>Find gaps and conflicts.</h2><p>Kaviyo checks product details against page evidence and shows exactly what it observed.</p></article>
                    <article><span>Review before changes</span><h2>Decide what to update.</h2><p>A person confirms the right value before any change reaches a live listing.</p></article>
                  </div>
                  <p class="demo-disclosure">This local demo only includes seeded product records and page snapshots. Store connections, AI drafting, and publishing are not connected.</p>
                  <a class="action-link" href="/audit/start">Walk through a sample audit <span aria-hidden="true">↗</span></a>
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
            @media (max-width: 760px) {
              .flow-page .site-header, .flow-main--sample, .flow-page .flow-footer { width: calc(100% - 36px); }
              .sample-workspace { grid-template-columns: 1fr; gap: 28px; margin-top: 34px; }
              .flow-next-step { padding: 18px 0 0 19px; }
              .flow-heading { padding-top: 36px; }
              .flow-heading h1 { max-width: 640px; font-size: clamp(46px, 10vw, 64px); }
              .flow-page .flow-intro { font-size: 14px; }
            }
            @media (max-width: 480px) {
              .flow-page .site-header nav { gap: 8px; }
              .flow-page .site-header nav a { padding: 8px 5px; font-size: 10px; }
              .flow-page .nav-action { padding: 8px 9px; font-size: 10px; }
              .flow-page .flow-progress { gap: 9px; font-size: 10px; }
              .flow-page .catalog-caption { align-items: flex-start; flex-direction: column; gap: 5px; }
              .sample-product-table tbody th { min-width: 160px; }
            }
            """;

    private static String page(String title, String current, String content) {
        StringBuilder html = new StringBuilder(12_000);
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light">
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
                    </nav>
                    <a class="nav-action" href="/audit/start">%s</a>
                  </header>
                """.formatted(escape(current)));
        html.append(content);
        html.append("""
                  <footer class="footer flow-footer"><span>Kaviyo</span><span>Sample walkthrough · synthetic data</span></footer>
                </body>
                </html>
                """);
        return html.toString();
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
}
