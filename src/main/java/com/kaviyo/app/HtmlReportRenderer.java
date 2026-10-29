package com.kaviyo.app;

import com.kaviyo.audit.AuditIssue;
import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.Evidence;
import com.kaviyo.audit.Severity;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders the local demo report as a self-contained merchant-facing HTML page. */
public final class HtmlReportRenderer {
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a 'UTC'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    public String render(AuditReport report) {
        long high = report.count(Severity.HIGH);
        long medium = report.count(Severity.MEDIUM);
        long low = report.count(Severity.LOW);
        Map<String, List<AuditIssue>> issuesByProduct = new LinkedHashMap<>();
        for (AuditIssue issue : report.issues()) {
            issuesByProduct.computeIfAbsent(issue.productId(), ignored -> new ArrayList<>()).add(issue);
        }

        StringBuilder html = new StringBuilder(22_000);
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light">
                  <title>Kaviyo | Sample audit results</title>
                  <style>
                """);
        html.append(STYLES);
        html.append("""
                  </style>
                </head>
                <body>
                  <header class="site-header">
                    <a class="brand" href="/" aria-label="Kaviyo home"><span class="brand-mark" aria-hidden="true">k</span><span>kaviyo</span></a>
                    <nav aria-label="Page navigation">
                      <a href="/">Home</a>
                      <a class="current" href="#findings" aria-current="page">Sample results</a>
                    </nav>
                    <span class="run-label"><i aria-hidden="true"></i>Sample data</span>
                  </header>
                  <main id="top" class="main">
                    <section class="report-hero" aria-labelledby="page-title">
                      <div class="hero-copy">
                        <p class="eyebrow">Sample audit <span>/</span> Results</p>
                        <h1 id="page-title">Review the product details we found.</h1>
                        <p class="intro-copy">This report compares synthetic catalog records with synthetic page snapshots. Open a finding to inspect its source values.</p>
                      </div>
                      <p class="report-time">Generated <time datetime="%s">%s</time></p>
                    </section>
                    <section class="run-summary" aria-label="Audit summary">
                      <span><strong>%d</strong> products</span>
                      <span><strong>%d</strong> findings</span>
                      <span><strong>%d</strong> high priority</span>
                      <span><strong>%d</strong> medium priority</span>
                      <span><strong>%d</strong> low priority</span>
                    </section>
                    <div class="content-grid">
                      <section id="findings" class="queue" aria-labelledby="findings-title">
                        <div class="section-heading">
                          <div><p class="eyebrow">Product by product</p><h2 id="findings-title">Findings</h2></div>
                        </div>
                """.formatted(escape(report.generatedAt().toString()), escape(TIMESTAMP.format(report.generatedAt())),
                report.productsChecked(), report.issues().size(), high, medium, low));

        for (List<AuditIssue> productIssues : issuesByProduct.values()) {
            appendProductGroup(html, productIssues);
        }

        html.append("""
                      </section>
                      <aside class="guide" aria-label="How to use this report">
                        <section class="guide-section">
                          <p class="eyebrow">What to do next</p>
                          <h2>Confirm each source before changing a listing.</h2>
                          <p>A conflict shows that two observed values differ. Kaviyo cannot tell which one is current, so check with the merchant's source of truth first.</p>
                          <dl class="severity-key">
                            <div><dt class="high">High priority</dt><dd>Two observed values conflict</dd></div>
                            <div><dt class="medium">Medium priority</dt><dd>A useful detail or page signal is missing</dd></div>
                            <div><dt class="low">Low priority</dt><dd>More confirmed detail may help</dd></div>
                          </dl>
                        </section>
                        <p class="grounding-note"><strong>Demo only.</strong> This report uses synthetic records and does not scan a live store or measure assistant rankings.</p>
                        <a class="quiet-link back-home" href="/">Back to Kaviyo home</a>
                      </aside>
                    </div>
                    <footer class="footer"><span>Kaviyo</span><span>Synthetic data · no live store scan · no ranking measurement</span></footer>
                  </main>
                </body>
                </html>
                """);
        return html.toString();
    }

    private static void appendProductGroup(StringBuilder html, List<AuditIssue> issues) {
        AuditIssue first = issues.get(0);
        html.append("<section class=\"product-group\" aria-labelledby=\"product-")
                .append(escape(first.productId())).append("\"><header class=\"product-heading\"><div class=\"product-name\"><h3 id=\"product-")
                .append(escape(first.productId())).append("\">").append(escape(first.productTitle())).append("</h3><span>")
                .append(escape(first.productId())).append("</span></div><span class=\"product-total\">")
                .append(issues.size()).append(issues.size() == 1 ? " item" : " items").append("</span></header>");

        for (AuditIssue issue : issues) {
            appendIssue(html, issue);
        }
        html.append("</section>");
    }

    private static void appendIssue(StringBuilder html, AuditIssue issue) {
        String severity = issue.severity().name().toLowerCase(Locale.ROOT);
        html.append("<article class=\"finding ").append(severity).append("\"><div class=\"finding-side\"><span class=\"severity ")
                .append(severity).append("\">").append(escape(capitalize(severity)))
                .append(" priority</span></div><div class=\"finding-body\"><div class=\"finding-title-row\"><h4>")
                .append(escape(issue.title())).append("</h4><span class=\"confidence\">")
                .append(escape(capitalize(issue.confidence().name().toLowerCase(Locale.ROOT))))
                .append(" confidence</span></div><p class=\"observation\">").append(escape(issue.observation()))
                .append("</p><div class=\"next-step\"><span>Next step</span><p>")
                .append(escape(issue.recommendedAction())).append("</p></div><details><summary>Evidence and source details <span aria-hidden=\"true\">+</span></summary><div class=\"evidence-list\">");

        for (Evidence evidence : issue.evidence()) {
            html.append("<div class=\"evidence-item\"><strong>").append(escape(evidence.source())).append("</strong><dl>")
                    .append("<div><dt>Field</dt><dd>").append(escape(evidence.field())).append("</dd></div>")
                    .append("<div><dt>Observed value</dt><dd>").append(escape(evidence.value())).append("</dd></div>")
                    .append("<div><dt>Source URL</dt><dd><code>").append(escape(evidence.sourceUri().toString())).append("</code></dd></div>")
                    .append("<div><dt>Observed</dt><dd><time datetime=\"").append(escape(evidence.observedAt().toString()))
                    .append("\">").append(escape(TIMESTAMP.format(evidence.observedAt()))).append("</time></dd></div>")
                    .append("</dl></div>");
        }
        html.append("</div></details></div></article>");
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }

    static final String STYLES = """
            :root {
              color-scheme: light;
              --color-fresh-grass: #8ed462;
              --color-cream-paper: #f5f1e4;
              --color-ink-black: #2c2e2a;
              --color-pure-white: #ffffff;
              --color-sandstone: #e0dbce;
              --color-stone-gray: #80827f;
              --color-hairline-mist: #d5d5d4;
              --color-pure-ink: #000000;
              --color-sky-pop: #2ba0ff;
              --color-coral-pop: #ff705d;
              --color-sunshine-pop: #f5e211;
              --font-inter: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
              --ink: var(--color-ink-black);
              --muted: #62665f;
              --quiet: var(--color-stone-gray);
              --line: var(--color-hairline-mist);
              --canvas: var(--color-cream-paper);
              --paper: var(--color-pure-white);
              --sandstone: var(--color-sandstone);
              --grass: var(--color-fresh-grass);
              --grass-ink: #355b2a;
              --high: #963d30;
              --high-soft: #f6e8e2;
              --medium: #76550d;
              --medium-soft: #f4edda;
              --low: #416a48;
              --low-soft: #e8efe3;
            }
            * { box-sizing: border-box; }
            html { scroll-behavior: smooth; }
            body { margin: 0; background: var(--canvas); color: var(--ink); font: 15px/1.55 var(--font-inter); }
            a { color: inherit; }
            a:focus-visible, summary:focus-visible { outline: 3px solid var(--grass-ink); outline-offset: 3px; }
            .site-header { width: min(1240px, calc(100% - 64px)); min-height: 66px; margin: 20px auto 0; padding: 8px 12px 8px 16px; display: flex; align-items: center; gap: 24px; border: 1px solid rgba(44,46,42,.08); border-radius: 999px; background: var(--paper); }
            .brand { display: flex; align-items: center; gap: 9px; color: var(--ink); font-size: 19px; font-weight: 600; letter-spacing: -.055em; text-decoration: none; }
            .brand-mark { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 12px; background: var(--grass); color: var(--ink); font-size: 20px; font-weight: 700; }
            .site-header nav { display: flex; align-items: center; gap: 5px; }
            .site-header nav a { display: inline-flex; align-items: center; padding: 10px 14px; border-radius: 999px; color: var(--muted); font-size: 13px; text-decoration: none; }
            .site-header nav a.current { background: #eff6e9; color: var(--ink); font-weight: 600; }
            .nav-action { display: inline-flex; align-items: center; justify-content: center; padding: 10px 17px; border: 1px solid rgba(44,46,42,.18); border-radius: 999px; color: var(--ink); font-size: 13px; font-weight: 550; text-decoration: none; }
            .nav-action:hover { background: var(--grass); }
            .run-label { display: inline-flex; align-items: center; gap: 8px; margin-left: auto; padding: 9px 13px; border: 1px solid var(--line); border-radius: 999px; color: var(--muted); font-size: 11px; font-weight: 600; white-space: nowrap; }
            .run-label i { width: 8px; height: 8px; border: 2px solid var(--grass-ink); border-radius: 50%; background: var(--grass); }
            .main { width: min(1240px, calc(100% - 64px)); margin: 0 auto; }
            .home-main, .flow-main { width: min(1160px, calc(100% - 64px)); margin: 0 auto; }
            .home-journey { display: grid; grid-template-columns: minmax(0, 1.04fr) minmax(320px, .96fr); align-items: start; gap: clamp(35px, 6vw, 88px); padding: 58px 0 28px; }
            .home-copy { grid-column: 1; grid-row: 1; max-width: 640px; }
            .home-copy .eyebrow { margin-bottom: 20px; font-size: 15px; }
            .home-copy h1 { max-width: 640px; margin-bottom: 20px; font-size: clamp(52px, 7.2vw, 88px); font-weight: 500; letter-spacing: -.07em; line-height: .97; }
            .home-description { max-width: 560px; margin: 0; color: var(--muted); font-size: 18px; line-height: 1.55; }
            .home-actions, .flow-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 18px 24px; margin-top: 29px; }
            .action-link { min-height: 48px; display: inline-flex; align-items: center; justify-content: center; gap: 13px; padding: 12px 17px; border: 1px solid var(--ink); border-radius: 12px; background: var(--color-fresh-grass); color: var(--ink); font-size: 14px; font-weight: 600; text-decoration: none; transition: background-color .16s ease, transform .16s ease; }
            .action-link:hover { background: #a1e37b; transform: translateY(-2px); }
            .action-link:focus-visible, .nav-action:focus-visible, .quiet-link:focus-visible { outline: 3px solid var(--grass-ink); outline-offset: 3px; }
            .action-link span { font-size: 17px; line-height: 1; }
            .quiet-link { color: var(--ink); font-size: 14px; text-decoration-color: var(--quiet); text-underline-offset: 4px; }
            .quiet-link:hover { text-decoration-thickness: 2px; }
            .home-note { display: flex; align-items: center; gap: 9px; margin: 26px 0 0; color: var(--muted); font-size: 12px; }
            .home-note span { width: 9px; height: 9px; border: 2px solid var(--grass-ink); border-radius: 50%; background: var(--grass); }
            .home-art { position: sticky; z-index: 2; top: 96px; grid-column: 2; grid-row: 1 / span 2; align-self: start; min-width: 0; min-height: 0; aspect-ratio: 760 / 520; overflow: hidden; border: 0; border-radius: 52px; background: var(--grass); }
            .discovery-scene { display: block; width: 100%; height: 100%; font-family: var(--font-inter); }
            .scene-background { fill: var(--grass); }
            .scene-sun { fill: var(--color-sunshine-pop); }
            .scene-path { fill: none; stroke: var(--ink); stroke-width: 4; stroke-dasharray: 8 13; stroke-linecap: round; opacity: .42; animation: route-flow 14s linear infinite; }
            .scene-spark { fill: var(--color-sky-pop); transform-box: fill-box; transform-origin: center; animation: scene-twinkle 3.2s ease-in-out infinite alternate; }
            .scene-star { fill: var(--color-coral-pop); animation-delay: .7s; }
            .scene-assistant, .scene-product-card, .scene-merchant, .scene-shopper { transition: translate .72s cubic-bezier(.2,.7,.2,1); }
            .scene-assistant { transform-box: fill-box; transform-origin: center; animation: assistant-float 6s ease-in-out infinite; }
            .scene-product-card { transform-box: fill-box; transform-origin: center; animation: product-float 5s ease-in-out infinite; }
            .home-art[data-active-step="2"] .scene-assistant { translate: -10px 22px; }
            .home-art[data-active-step="2"] .scene-product-card { translate: -8px 6px; }
            .home-art[data-active-step="2"] .scene-shopper { translate: -7px 0; }
            .home-art[data-active-step="3"] .scene-assistant { translate: 10px 4px; }
            .home-art[data-active-step="3"] .scene-product-card { translate: 12px -8px; }
            .home-art[data-active-step="3"] .scene-merchant { translate: 8px 0; }
            .home-art[data-active-step="3"] .scene-shopper { translate: -15px 0; }
            .scene-merchant-thinking, .scene-shopper-thinking { transform-box: fill-box; transform-origin: center; animation: thinking-tilt 6s ease-in-out infinite; }
            .scene-shopper-thinking { animation-delay: 1.2s; animation-direction: reverse; }
            .scene-label { fill: var(--grass-ink); font-size: 13px; font-weight: 700; letter-spacing: .06em; }
            .scene-question { fill: var(--ink); font-size: 20px; font-weight: 550; }
            .scene-kaviyo { fill: var(--grass-ink); font-size: 14px; font-weight: 650; }
            .scene-product-name { fill: var(--ink); font-size: 20px; font-weight: 650; }
            .scene-product-detail { fill: var(--muted); font-size: 15px; font-weight: 500; }
            .scene-mismatch-question, .scene-review-question { opacity: 0; transition: opacity .35s ease; }
            .home-art[data-active-step="2"] .scene-intent-question, .home-art[data-active-step="3"] .scene-intent-question,
            .home-art[data-active-step="1"] .scene-mismatch-question, .home-art[data-active-step="3"] .scene-mismatch-question,
            .home-art[data-active-step="1"] .scene-review-question, .home-art[data-active-step="2"] .scene-review-question { opacity: 0; }
            .home-art[data-active-step="2"] .scene-mismatch-question,
            .home-art[data-active-step="3"] .scene-review-question,
            .home-art[data-active-step="1"] .scene-intent-question { opacity: 1; }
            .scene-mismatch-detail, .scene-review-detail, .scene-alert-status, .scene-review-status { opacity: 0; transition: opacity .35s ease; }
            .home-art[data-active-step="2"] .scene-default-detail, .home-art[data-active-step="3"] .scene-default-detail,
            .home-art[data-active-step="1"] .scene-mismatch-detail, .home-art[data-active-step="3"] .scene-mismatch-detail,
            .home-art[data-active-step="1"] .scene-review-detail, .home-art[data-active-step="2"] .scene-review-detail,
            .home-art[data-active-step="2"] .scene-check-status, .home-art[data-active-step="3"] .scene-check-status,
            .home-art[data-active-step="1"] .scene-alert-status, .home-art[data-active-step="3"] .scene-alert-status,
            .home-art[data-active-step="1"] .scene-review-status, .home-art[data-active-step="2"] .scene-review-status { opacity: 0; }
            .home-art[data-active-step="2"] .scene-mismatch-detail, .home-art[data-active-step="2"] .scene-alert-status,
            .home-art[data-active-step="3"] .scene-review-detail, .home-art[data-active-step="3"] .scene-review-status,
            .home-art[data-active-step="1"] .scene-default-detail, .home-art[data-active-step="1"] .scene-check-status { opacity: 1; }
            .scene-caption { fill: var(--ink); font-size: 12px; font-weight: 500; opacity: .78; }
            @keyframes route-flow { to { stroke-dashoffset: -84; } }
            @keyframes scene-twinkle { from { opacity: .55; transform: scale(.84); } to { opacity: 1; transform: scale(1.08); } }
            @keyframes assistant-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }
            @keyframes product-float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(4px); } }
            @keyframes thinking-tilt { 0%, 100% { transform: translateY(0) rotate(0); } 50% { transform: translateY(-2px) rotate(-2deg); } }
            .home-footnote { max-width: 1160px; padding: 16px 0; border-top: 1px solid var(--line); }
            .home-footnote p { max-width: 760px; margin: 0; color: var(--muted); font-size: 12px; }
            .home-footer { margin-top: 0; }
            .home-footer a { color: var(--grass-ink); }
            .home-journey { padding-top: 64px; }
            .home-copy h1 { max-width: 680px; font-size: clamp(60px, 7.4vw, 96px); letter-spacing: -.075em; }
            .home-description { max-width: 590px; font-size: 19px; }
            .home-actions { gap: 20px 25px; margin-top: 32px; }
            .home-actions .home-primary, .home-close .home-primary { min-height: 50px; gap: 11px; padding: 11px 20px; border: 1px solid rgba(44,46,42,.14); border-radius: 999px; background: var(--paper); font-size: 14px; font-weight: 550; }
            .home-actions .home-primary:hover, .home-close .home-primary:hover { background: var(--grass); }
            .action-dot { width: 9px; height: 9px; flex: 0 0 9px; border-radius: 50%; background: var(--color-sky-pop); }
            .home-note { max-width: 480px; display: block; margin-top: 24px; font-size: 13px; line-height: 1.55; }
            .home-note span { display: none; }
            .home-service, .home-story, .home-small-business, .home-close { scroll-margin-top: 32px; }
            .home-service { grid-column: 1; grid-row: 2; padding: 88px 0 52px; border-top: 1px solid var(--line); }
            .service-heading, .service-intro { display: block; }
            .service-heading { margin-bottom: 25px; }
            .service-heading .eyebrow { margin: 0 0 15px; }
            .service-heading h2 { max-width: 610px; margin: 0; font-size: clamp(43px, 5.3vw, 68px); font-weight: 500; letter-spacing: -.068em; line-height: 1; }
            .service-intro { margin-left: 0; }
            .service-intro p { max-width: 590px; margin: 0; color: var(--muted); font-size: 16px; line-height: 1.65; }
            .service-intro p + p { margin-top: 13px; }
            .service-steps { display: grid; grid-template-columns: 1fr; margin-top: 24px; border-top: 1px solid var(--line); }
            .service-steps article { min-height: 56vh; display: flex; flex-direction: column; justify-content: center; padding: 36px 0; border-bottom: 1px solid var(--line); }
            .service-steps article:last-child { border-bottom: 0; }
            .service-mark { width: 14px; height: 14px; display: block; margin-bottom: 24px; border-radius: 50%; transition: transform .35s ease; }
            .service-steps article h3 { transition: color .35s ease; }
            .service-steps article.is-active .service-mark { transform: scale(1.3); }
            .service-steps article.is-active h3 { color: var(--grass-ink); }
            .mark-green { background: var(--grass-ink); }
            .mark-blue { background: var(--color-sky-pop); }
            .mark-coral { background: var(--color-coral-pop); }
            .service-steps h3 { max-width: 270px; margin: 0 0 8px; font-size: 23px; font-weight: 550; letter-spacing: -.045em; line-height: 1.16; }
            .service-steps p { max-width: 310px; margin: 0; color: var(--muted); font-size: 14px; line-height: 1.6; }
            .home-story { margin: 0 -25px; padding: 62px 64px 54px; border-radius: 64px; background: var(--paper); }
            .story-heading { display: grid; grid-template-columns: 1.1fr .75fr; gap: clamp(30px, 7vw, 100px); align-items: end; }
            .story-heading h2 { max-width: 650px; margin: 0; font-size: clamp(42px, 5.1vw, 64px); font-weight: 500; letter-spacing: -.068em; line-height: 1; }
            .story-heading > p { max-width: 400px; margin: 0 0 3px; color: var(--muted); font-size: 15px; line-height: 1.6; }
            .story-explorer { position: relative; margin: 38px 0 0; padding: 0; border: 0; }
            .story-explorer legend { margin-bottom: 12px; color: var(--quiet); font-size: 12px; }
            .story-explorer input[type="radio"] { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; clip-path: inset(50%); }
            .story-explorer input[type="radio"] + label { display: inline-flex; align-items: center; min-height: 40px; margin-right: 20px; padding: 4px 0 7px; border-bottom: 2px solid transparent; color: var(--muted); cursor: pointer; font-size: 14px; }
            .story-explorer input[type="radio"]:checked + label { border-color: var(--grass-ink); color: var(--ink); font-weight: 600; }
            .story-explorer input[type="radio"]:focus-visible + label { outline: 3px solid var(--grass-ink); outline-offset: 4px; }
            .story-panels { margin-top: 13px; border-top: 1px solid var(--line); }
            .story-panel { display: none; grid-template-columns: minmax(0, 1fr) minmax(300px, .8fr); gap: clamp(28px, 7vw, 90px); align-items: center; min-height: 260px; padding: 30px 0 13px; }
            #story-compare:checked ~ .story-panels #story-panel-compare,
            #story-evidence:checked ~ .story-panels #story-panel-evidence,
            #story-next-step:checked ~ .story-panels #story-panel-next-step { display: grid; }
            .story-kicker { margin: 0 0 9px; color: var(--grass-ink); font-size: 13px; font-weight: 600; }
            .story-panel-copy h3 { max-width: 510px; margin: 0 0 11px; font-size: clamp(30px, 3.6vw, 43px); font-weight: 500; letter-spacing: -.06em; line-height: 1.06; }
            .story-panel-copy > p:last-child { max-width: 490px; margin: 0; color: var(--muted); font-size: 15px; line-height: 1.65; }
            .comparison-sheet, .evidence-sheet, .review-sheet { align-self: center; padding: 23px 25px; border-radius: 30px; background: var(--canvas); }
            .comparison-heading, .comparison-sheet > div { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 16px; padding: 13px 0; border-bottom: 1px solid var(--line); }
            .comparison-heading { padding-top: 0; color: var(--quiet); font-size: 11px; }
            .comparison-sheet > div > span { color: var(--muted); font-size: 13px; }
            .comparison-sheet > div > strong { font-size: 17px; font-weight: 550; }
            .comparison-sheet > p { display: flex; align-items: center; gap: 8px; margin: 16px 0 0; color: var(--high); font-size: 12px; font-weight: 550; }
            .evidence-sheet { display: grid; gap: 0; }
            .evidence-sheet > div { display: grid; grid-template-columns: 34px 1fr; align-items: start; gap: 13px; padding: 12px 0; border-bottom: 1px solid var(--line); }
            .evidence-sheet > div:first-child { padding-top: 0; }
            .evidence-sheet > div:last-child { padding-bottom: 0; border-bottom: 0; }
            .evidence-symbol { width: 31px; height: 34px; border: 1px solid var(--grass-ink); border-radius: 9px; background: #e8efe3; }
            .page-symbol { border-color: #28668c; background: #e4f0f5; }
            .evidence-sheet p, .review-sheet p { display: grid; gap: 3px; margin: 0; }
            .evidence-sheet strong, .review-sheet strong { font-size: 13px; font-weight: 600; }
            .evidence-sheet p span, .review-sheet p span { color: var(--muted); font-size: 13px; }
            .evidence-sheet small { color: var(--quiet); font-size: 11px; }
            .review-sheet { background: #eaf3e3; }
            .review-check { width: 27px; height: 27px; display: grid; place-items: center; margin-bottom: 19px; border-radius: 50%; background: var(--grass); font-size: 15px; }
            .review-sheet small { display: block; margin-top: 18px; color: var(--muted); font-size: 11px; }
            .story-disclosure { margin: 20px 0 0; padding-top: 14px; border-top: 1px solid var(--line); color: var(--quiet); font-size: 11px; line-height: 1.5; }
            .home-small-business { display: grid; grid-template-columns: 1.1fr .75fr; gap: clamp(30px, 7vw, 100px); align-items: start; padding: 88px 0; border-bottom: 1px solid var(--line); }
            .home-small-business h2 { max-width: 650px; margin: 0; font-size: clamp(43px, 5.2vw, 66px); font-weight: 500; letter-spacing: -.068em; line-height: 1; }
            .home-small-business > div:last-child { padding-top: 24px; }
            .home-small-business > div:last-child > p { max-width: 440px; margin: 0 0 16px; color: var(--muted); font-size: 15px; line-height: 1.65; }
            .home-small-business .small-business-note { padding-left: 14px; border-left: 2px solid var(--grass); font-size: 12px; }
            .home-small-business .quiet-link { display: inline-block; margin-top: 7px; font-size: 14px; }
            .home-close { margin: 0 -25px 35px; padding: 54px 64px 58px; border-radius: 64px; background: var(--color-sunshine-pop); }
            .home-close .eyebrow { color: var(--ink); }
            .home-close h2 { max-width: 740px; margin: 0 0 12px; font-size: clamp(45px, 6vw, 76px); font-weight: 500; letter-spacing: -.07em; line-height: .98; }
            .home-close > p:not(.eyebrow) { max-width: 560px; margin: 0; color: #4b4c42; font-size: 16px; line-height: 1.55; }
            .home-close .home-primary { margin-top: 25px; }
            .home-footer { width: min(1160px, calc(100% - 64px)); align-items: center; margin: 0 auto; padding: 18px 0 27px; }
            .home-footer > span:nth-child(2) { margin-right: auto; margin-left: 15px; }
            .home-footer a { font-size: 12px; text-decoration-color: var(--quiet); text-underline-offset: 3px; }
            .report-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 52px 0 22px; }
            .report-hero .hero-copy { max-width: 760px; }
            .report-hero h1 { max-width: 760px; margin-bottom: 12px; font-size: clamp(38px, 5vw, 60px); font-weight: 500; letter-spacing: -.065em; line-height: 1; }
            .report-hero .intro-copy { font-size: 15px; }
            .report-time { flex: 0 0 auto; margin: 0 0 2px; color: var(--muted); font-size: 12px; }
            .report-time time { color: var(--ink); }
            .flow-main { max-width: 850px; margin-top: 50px; margin-bottom: 90px; }
            .flow-progress { display: flex; flex-wrap: wrap; gap: 9px 25px; margin-bottom: 24px; color: var(--quiet); font-size: 13px; }
            .flow-progress span { position: relative; padding-bottom: 7px; }
            .flow-progress span:not(:last-child)::after { position: absolute; top: 9px; left: calc(100% + 7px); width: 11px; height: 1px; background: var(--line); content: ""; }
            .flow-progress .active { border-bottom: 2px solid var(--grass-ink); color: var(--ink); font-weight: 600; }
            .flow-card { padding: clamp(22px, 4vw, 44px); border: 1px solid rgba(44,46,42,.08); border-radius: 28px; background: var(--paper); }
            .flow-card h1, .overview-main h1 { max-width: 700px; margin: 0 0 13px; font-size: clamp(42px, 6vw, 66px); font-weight: 500; letter-spacing: -.065em; line-height: 1; }
            .step-label { margin: 0 0 11px; color: var(--grass-ink); font-size: 14px; font-weight: 600; }
            .flow-intro { max-width: 650px; margin: 0; color: var(--muted); font-size: 16px; line-height: 1.6; }
            .source-choice { display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 13px; margin-top: 28px; padding: 17px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
            .choice-mark { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; background: var(--grass); font-size: 21px; font-weight: 600; }
            .source-choice strong { display: block; font-size: 15px; font-weight: 600; }
            .source-choice p { margin: 3px 0 0; color: var(--muted); font-size: 12px; }
            .choice-state { color: var(--grass-ink); font-size: 12px; font-weight: 600; }
            .flow-card .flow-actions { margin-top: 24px; }
            .demo-disclosure { max-width: 660px; margin: 20px 0 0; color: var(--muted); font-size: 12px; }
            .scope-summary { display: flex; gap: 38px; margin: 25px 0 5px; padding: 16px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
            .scope-summary div { display: grid; gap: 3px; }
            .scope-summary strong { font-size: 16px; font-weight: 550; }
            .scope-summary span { color: var(--muted); font-size: 12px; }
            .sample-products { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
            .sample-products li { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: 5px 15px; padding: 14px 0; border-bottom: 1px solid #eeeae0; }
            .sample-products strong { font-size: 14px; font-weight: 550; }
            .sample-products span { color: var(--muted); font-size: 12px; }
            .overview-main { max-width: 1040px; }
            .overview-main > .eyebrow { margin-bottom: 16px; }
            .overview-main > h1 { margin-bottom: 17px; }
            .overview-steps { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0; margin: 35px 0 22px; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
            .overview-steps article { padding: 20px 22px 23px 0; }
            .overview-steps article + article { padding-left: 22px; border-left: 1px solid var(--line); }
            .overview-steps article > span { color: var(--grass-ink); font-size: 12px; font-weight: 600; }
            .overview-steps h2 { margin: 10px 0 7px; font-size: 21px; font-weight: 550; letter-spacing: -.04em; }
            .overview-steps p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.55; }
            .overview-main > .demo-disclosure { max-width: 740px; margin: 0 0 21px; padding: 13px 15px; border-left: 3px solid var(--grass); background: #f8f5ec; }
            .flow-footer { width: min(1160px, calc(100% - 64px)); margin-right: auto; margin-left: auto; }
            .hero-copy { max-width: 690px; }
            .eyebrow { margin: 0 0 12px; color: var(--grass-ink); font-size: 12px; font-weight: 600; }
            .eyebrow > span { padding: 0 5px; color: var(--quiet); }
            h1, h2, h3, h4, p { margin-top: 0; }
            h1 { max-width: 690px; margin-bottom: 18px; font-size: clamp(42px, 5.8vw, 68px); font-weight: 550; letter-spacing: -.065em; line-height: .99; }
            .intro-copy { max-width: 570px; margin: 0; color: var(--muted); font-size: 16px; line-height: 1.6; }
            .hero-visual { padding: 22px; border: 1px solid rgba(44,46,42,.08); border-radius: 30px; background: var(--paper); }
            .run-summary { display: flex; flex-wrap: wrap; gap: 7px 20px; margin-bottom: 36px; padding: 0 0 14px; border-bottom: 1px solid var(--line); }
            .run-summary span { color: var(--muted); font-size: 13px; }
            .run-summary strong { color: var(--ink); font-size: 14px; font-weight: 550; }
            .content-grid { display: grid; grid-template-columns: minmax(0, 1.8fr) minmax(265px, .76fr); align-items: start; gap: clamp(24px, 3.2vw, 44px); }
            .section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; margin: 0 0 17px; }
            .section-heading .eyebrow { margin-bottom: 4px; }
            .section-heading h2 { margin: 0; font-size: 32px; font-weight: 550; letter-spacing: -.05em; }
            .product-group { margin-bottom: 20px; padding: 12px 22px 10px; border: 1px solid rgba(44,46,42,.08); border-radius: 25px; background: var(--paper); }
            .product-heading { display: flex; align-items: center; gap: 12px; padding: 5px 0 14px; border-bottom: 1px solid var(--line); }
            .product-name { min-width: 0; display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px; }
            .product-name h3 { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: -.02em; }
            .product-name > span { color: var(--quiet); font-size: 11px; }
            .product-total { margin-left: auto; color: var(--muted); font-size: 12px; white-space: nowrap; }
            .finding { display: grid; grid-template-columns: 104px minmax(0, 1fr); gap: 13px; padding: 17px 0 18px; border-bottom: 1px solid #eeeae0; }
            .finding:last-child { border-bottom: 0; }
            .finding-side { display: flex; align-items: center; gap: 8px; padding-top: 1px; }
            .severity { display: inline-flex; align-items: center; gap: 7px; color: var(--muted); font-size: 12px; font-weight: 550; }
            .severity.high, .severity-key dt.high { color: var(--high); }
            .severity.medium, .severity-key dt.medium { color: var(--medium); }
            .severity.low, .severity-key dt.low { color: var(--low); }
            .finding-body { min-width: 0; }
            .finding-title-row { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
            .finding h4 { margin: 0; font-size: 15px; font-weight: 600; letter-spacing: -.025em; line-height: 1.35; }
            .confidence { color: var(--quiet); font-size: 10px; white-space: nowrap; }
            .observation { margin: 7px 0 12px; color: var(--muted); font-size: 13px; line-height: 1.55; }
            .next-step { display: grid; grid-template-columns: 68px minmax(0, 1fr); gap: 8px; padding: 10px 0 0; border-top: 1px solid #eeeae0; }
            .next-step > span { padding-top: 1px; color: var(--grass-ink); font-size: 11px; font-weight: 600; }
            .next-step p { margin: 0; color: var(--ink); font-size: 12px; line-height: 1.55; }
            details { margin-top: 11px; }
            summary { display: inline-flex; align-items: center; gap: 7px; color: var(--grass-ink); cursor: pointer; font-size: 11px; font-weight: 600; list-style: none; }
            summary::-webkit-details-marker { display: none; }
            summary span { font-size: 15px; line-height: 1; }
            details[open] summary span { transform: rotate(45deg); }
            .evidence-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 10px; padding-top: 13px; }
            .evidence-item { min-width: 0; padding: 13px; border: 1px solid var(--line); border-left: 3px solid var(--grass); border-radius: 15px; background: #f8f5ec; }
            .evidence-item > strong { display: block; margin-bottom: 9px; color: var(--ink); font-size: 12px; }
            .evidence-item dl { display: grid; grid-template-columns: 1fr 1.15fr; gap: 9px 11px; margin: 0; }
            .evidence-item dl div { min-width: 0; }
            .evidence-item dt { color: var(--quiet); font-size: 10px; }
            .evidence-item dd { overflow-wrap: anywhere; margin: 2px 0 0; color: var(--ink); font-size: 11px; }
            .evidence-item code { color: var(--muted); font: inherit; font-size: 11px; }
            .guide { display: grid; gap: 15px; }
            .guide-section { padding: 21px; border: 1px solid rgba(44,46,42,.08); border-radius: 24px; background: var(--paper); }
            .guide-section + .guide-section { padding-top: 21px; }
            .guide-section h2 { max-width: 300px; margin: 0 0 9px; font-size: 22px; font-weight: 550; letter-spacing: -.045em; line-height: 1.15; }
            .guide-section > p:not(.eyebrow) { margin-bottom: 16px; color: var(--muted); font-size: 13px; line-height: 1.6; }
            .severity-key { display: grid; gap: 10px; margin: 0; padding-top: 14px; border-top: 1px solid var(--line); }
            .severity-key > div { display: grid; grid-template-columns: 105px 1fr; gap: 8px; }
            .severity-key dt { display: flex; align-items: center; gap: 7px; color: var(--ink); font-size: 11px; font-weight: 600; }
            .severity-key dd { margin: 0; color: var(--muted); font-size: 11px; line-height: 1.45; }
            .grounding-note { margin: 0; padding: 14px 15px; border-radius: 15px; background: #eff6e9; color: var(--muted); font-size: 12px; line-height: 1.55; }
            .grounding-note strong { color: var(--ink); }
            .footer { display: flex; justify-content: space-between; gap: 14px; margin-top: 35px; padding: 16px 0 24px; border-top: 1px solid var(--line); color: var(--quiet); font-size: 11px; }
            .footer > span:first-child { color: var(--muted); font-weight: 600; }
            .footer > span:first-child span { padding: 0 3px; color: var(--grass-ink); }
            @media (max-width: 920px) {
              .site-header, .main, .home-main, .flow-main { width: min(100% - 44px, 780px); }
              .home-journey { grid-template-columns: minmax(0, 1fr) minmax(270px, .8fr); gap: 28px; }
              .home-copy h1 { font-size: clamp(50px, 7vw, 68px); }
              .home-story, .home-close { margin-right: 0; margin-left: 0; padding-right: 38px; padding-left: 38px; border-radius: 48px; }
              .story-panel { grid-template-columns: minmax(0, 1fr) minmax(270px, .85fr); gap: 30px; }
              .home-footer { width: min(100% - 44px, 780px); }
              .report-hero h1 { font-size: clamp(42px, 6.5vw, 58px); }
              .content-grid { grid-template-columns: 1fr; gap: 18px; }
              .guide { grid-template-columns: repeat(2, minmax(0, 1fr)); }
              .grounding-note { grid-column: 1 / -1; }
              .flow-footer { width: min(100% - 44px, 780px); }
            }
            @media (max-width: 760px) {
              .home-journey { --scroll-scene-rail: clamp(116px, 28vw, 176px); }
              .home-journey { grid-template-columns: 1fr; gap: 0; }
              .home-copy { grid-column: 1; grid-row: 1; }
              .home-art { grid-column: 1; grid-row: 2; position: sticky; top: 78px; width: var(--scroll-scene-rail); justify-self: end; margin: 24px 0 30px; border-radius: 25px; }
              .home-service { grid-column: 1; grid-row: 3; padding-right: calc(var(--scroll-scene-rail) + 18px); }
            }
            @media (max-width: 640px) {
              .site-header, .main, .home-main, .flow-main { width: calc(100% - 32px); }
              .site-header { min-height: 58px; margin-top: 12px; gap: 9px; padding: 6px 8px 6px 10px; }
              .brand { gap: 7px; font-size: 17px; }
              .brand-mark { width: 31px; height: 31px; border-radius: 11px; }
              .site-header nav { margin-left: auto; }
              .site-header nav a { padding: 9px 10px; font-size: 11px; }
              .site-header nav a:not(.current) { display: none; }
              .run-label { gap: 6px; margin-left: 0; padding: 8px 9px; font-size: 9px; }
              .nav-action { padding: 8px 10px; font-size: 11px; }
              .home-journey { padding: 43px 0 26px; }
              .home-copy { grid-column: 1; grid-row: 1; }
              .home-copy .eyebrow { margin-bottom: 14px; font-size: 13px; }
              .home-copy h1 { max-width: 530px; font-size: clamp(44px, 12vw, 62px); }
              .home-description { font-size: 16px; }
              .home-actions, .flow-actions { align-items: flex-start; gap: 17px; }
              .home-art { border-radius: 22px; }
              .home-service { padding: 58px calc(var(--scroll-scene-rail) + 18px) 58px 0; }
              .service-heading, .service-intro, .story-heading, .home-small-business { gap: 18px; }
              .service-heading { margin-bottom: 15px; }
              .service-heading h2 { font-size: clamp(32px, 8vw, 48px); }
              .service-steps { margin-top: 32px; }
              .service-steps article { min-height: 44vh; padding: 22px 0; }
              .service-mark { margin-bottom: 16px; }
              .home-story { margin: 0; padding: 33px 23px 28px; border-radius: 32px; }
              .story-heading h2 { font-size: clamp(40px, 10vw, 56px); }
              .story-explorer { margin-top: 25px; }
              .story-explorer input[type="radio"] + label { margin-right: 14px; font-size: 12px; }
              .story-panel { grid-template-columns: 1fr; gap: 20px; min-height: 0; padding-top: 23px; }
              .story-panel-copy h3 { font-size: 34px; }
              .comparison-sheet, .evidence-sheet, .review-sheet { padding: 19px; border-radius: 23px; }
              .home-small-business { gap: 4px; padding: 58px 0; }
              .home-small-business h2 { font-size: clamp(42px, 10vw, 58px); }
              .home-small-business > div:last-child { padding-top: 10px; }
              .home-close { margin: 0 0 24px; padding: 35px 23px 38px; border-radius: 32px; }
              .home-close h2 { font-size: clamp(43px, 11vw, 62px); }
              .home-footer { width: calc(100% - 32px); }
              .report-hero { align-items: flex-start; flex-direction: column; gap: 12px; padding: 37px 0 20px; }
              .report-hero h1, .flow-card h1, .overview-main h1 { font-size: clamp(40px, 11vw, 56px); }
              .report-hero .intro-copy { font-size: 14px; }
              .report-time { font-size: 11px; }
              .run-summary { gap: 7px 14px; margin-bottom: 27px; }
              .section-heading h2 { font-size: 29px; }
              .product-group { padding: 10px 15px 7px; border-radius: 20px; }
              .product-heading { gap: 9px; }
              .product-name { gap: 3px 7px; }
              .product-name h3 { font-size: 14px; }
              .product-name > span { font-size: 11px; }
              .product-total { font-size: 11px; }
              .finding { grid-template-columns: 1fr; gap: 6px; padding: 15px 0 16px; }
              .finding-side { min-height: 22px; }
              .finding-title-row { align-items: flex-start; flex-direction: column; gap: 4px; }
              .finding h4 { font-size: 14px; }
              .observation { font-size: 12px; }
              .next-step { grid-template-columns: 1fr; gap: 3px; }
              .evidence-list { grid-template-columns: 1fr; }
              .guide { grid-template-columns: 1fr; }
              .guide-section { padding: 18px; border-radius: 20px; }
              .grounding-note { grid-column: auto; }
              .flow-main { margin-top: 32px; margin-bottom: 56px; }
              .flow-progress { gap: 8px 16px; font-size: 11px; }
              .flow-progress span:not(:last-child)::after { left: calc(100% + 4px); width: 8px; }
              .flow-card { padding: 21px; border-radius: 22px; }
              .source-choice { grid-template-columns: 38px minmax(0, 1fr); gap: 10px; }
              .choice-mark { width: 36px; height: 36px; }
              .choice-state { grid-column: 2; }
              .scope-summary { gap: 22px; }
              .sample-products li { align-items: flex-start; flex-direction: column; }
              .overview-steps { grid-template-columns: 1fr; margin: 24px 0 18px; }
              .overview-steps article, .overview-steps article + article { padding: 16px 0; border-left: 0; border-bottom: 1px solid var(--line); }
              .overview-steps article:last-child { border-bottom: 0; }
              .flow-footer { width: calc(100% - 32px); }
              .footer { flex-direction: column; gap: 5px; margin-top: 24px; }
            }
            @media (max-width: 380px) {
              .site-header, .main, .home-main, .flow-main { width: calc(100% - 24px); }
              .brand { font-size: 16px; }
              .run-label { font-size: 8px; }
              .home-art { border-radius: 24px; }
              .flow-footer { width: calc(100% - 24px); }
              .home-footer { width: calc(100% - 24px); }
            }
            @media (prefers-reduced-motion: reduce) {
              html { scroll-behavior: auto; }
              .action-link { transition: none; }
              .scene-assistant, .scene-product-card, .scene-merchant, .scene-shopper, .scene-mismatch-question, .scene-review-question, .scene-mismatch-detail, .scene-review-detail, .scene-alert-status, .scene-review-status,
              .service-mark, .service-steps article h3 { transition: none; }
              .scene-path, .scene-spark, .scene-assistant, .scene-product-card, .scene-merchant-thinking, .scene-shopper-thinking { animation: none; }
            }
            """;
}
