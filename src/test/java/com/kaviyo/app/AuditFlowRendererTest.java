package com.kaviyo.app;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditFlowRendererTest {
    private final AuditFlowRenderer renderer = new AuditFlowRenderer();

    private static final List<String> SAMPLE_STRINGS = List.of(
            "Sample Trail Co.", "Sample Kitchen Co.", "Sample Home Co.",
            "Demo Ridge", "Demo Pour-Over", "Demo Stackable",
            "mission-pack", "mission-coffee", "mission-storage");

    @Test
    void cookerQuestionShowsCookerBeforeCollapsedInventory() {
        var catalog = List.of(new AuditFlowRenderer.CatalogItem("Printer",""), new AuditFlowRenderer.CatalogItem("Smart Blender Cooker",""));
        String html = renderer.renderCatalogReview("session","Merchant","","DTC",List.of("cooker"),catalog,2,0,"CSV","a@b.co","STARTER",List.of());
        String visible = html.substring(0, html.indexOf("<summary>Inspect uploaded catalog rows"));
        assertTrue(visible.contains("Smart Blender Cooker"));
        assertFalse(visible.contains("Printer"));
        assertTrue(renderer.renderSimulation("Merchant","cooker",catalog).contains("value='1' selected>Smart Blender Cooker"));
    }

    @Test
    void shopperWordingDoesNotExcludeMerchantProducts() {
        String html = renderer.renderSimulation("Pet store", "cats",
                List.of(new AuditFlowRenderer.CatalogItem("Trail Daypack", "Padded straps")));
        assertTrue(html.contains("Trail Daypack"));
        assertTrue(html.contains("id=\"run-simulation\""));
    }

    @Test
    void simulationWithMerchantDataShowsThatDataOnly() {
        String html = renderer.renderSimulation("Trail Co.", "a daypack need",
                List.of(new AuditFlowRenderer.CatalogItem("Trail Pack", "20L canvas")));
        assertTrue(html.contains("Trail Co."));
        assertTrue(html.contains("Trail Pack"));
        for (String sample : SAMPLE_STRINGS) {
            assertFalse(html.contains(sample), "leaked sample content: " + sample);
        }
    }

    @Test
    void catalogReviewListsEveryShopperNeed() {
        String html = renderer.renderCatalogReview("session-1", "Trail Co.", "https://trail.example",
                "DTC", List.of("a daypack need", "a tent need"),
                List.of(new AuditFlowRenderer.CatalogItem("Trail Pack", "20L canvas")),
                1, 0, "CSV", "owner@trail.example", "STARTER", List.of("Mention rate ≥ 60% · weekly"), false, null);
        assertTrue(html.contains("Mention rate ≥ 60% · weekly"));
        assertTrue(html.contains("a daypack need"));
        assertTrue(html.contains("a tent need"));
        assertTrue(html.contains("Need 1"));
        assertTrue(html.contains("Need 2"));
    }

    @Test
    void savedReviewShowsPipelineStrip() {
        String html = renderer.renderCatalogReview("session-1", "Trail Co.", "https://trail.example",
                "DTC", List.of("a daypack need", "a tent need"),
                List.of(new AuditFlowRenderer.CatalogItem("Trail Pack", "20L canvas")),
                1, 0, "CSV", "owner@trail.example", "STARTER", List.of(), true, "Catalog saved.");
        assertTrue(html.contains("Your pipeline"), "saved review should show the pipeline");
        assertTrue(html.contains("Catalog confirmed"), "first pipeline step should be done");
        assertTrue(html.contains("/discovery/simulation?session=session-1"), "strip should link the simulation");
        assertTrue(html.contains("/discovery/observe\">Track your goals"), "strip should link measurement");
    }

    @Test
    void onboardingFormEditsStructuredGoals() {
        String html = renderer.renderDiscoveryWorkspace(null, "owner@trail.example", "STARTER");
        assertTrue(html.contains("id=\"goal-list\""));
        assertTrue(html.contains("name=\"goalMetric0\""));
        assertTrue(html.contains("name=\"goalTarget0\""));
        assertTrue(html.contains("name=\"goalPeriod0\""));
        assertTrue(html.contains("id=\"goal-add\""));
        assertFalse(html.contains("name=\"goal\""), "free-text goal should be replaced by the editor");
        assertTrue(renderer.onboardingScript().contains("syncGoalRows"));
    }

    @Test
    void observeMissionPickerExplainsItself() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("Any assistant works"), "provider field must explain any-model support");
        assertTrue(html.contains("it never queries assistants"), "no-live-querying must be disclosed");
        assertTrue(html.contains("#mission-id { padding:"), "mission picker must read as a button");
        assertTrue(renderer.discoveryWorkspaceScript().contains("No missions yet"),
                "empty missions need guidance, not a stuck loader");
        assertTrue(renderer.discoveryWorkspaceScript().contains("Finish onboarding to create your first shopper mission."),
                "empty guidance must point at onboarding");
        assertTrue(renderer.discoveryWorkspaceScript().contains("Missions unavailable"),
                "failed missions load must surface, not hang");
    }

    @Test
    void observationPageOffersExtractionAssist() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("id=\"extract-answer\""));
        assertTrue(html.contains("id=\"extract-run\""));
        assertTrue(html.contains("id=\"extract-results\""));
        assertTrue(renderer.discoveryWorkspaceScript().contains("/observations/extract"));
    }

    @Test
    void observationPageReservesWeeklyTrendCharts() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("id=\"trend-mentions\""));
        assertTrue(html.contains("id=\"trend-accuracy\""));
        assertTrue(renderer.discoveryWorkspaceScript().contains("/trends?missionId="));
    }

    @Test
    void comparePageNeedsAProduct() {
        assertTrue(renderer.renderPriceCompare(null).contains("No product selected"));
        assertTrue(renderer.renderPriceCompare("  ").contains("/discovery/run"));
    }

    @Test
    void comparePageWiresInputsAndScript() {
        String html = renderer.renderPriceCompare("pack-1");
        assertTrue(html.contains("data-product=\"pack-1\""));
        assertTrue(html.contains("id=\"compare-url-1\""));
        assertTrue(html.contains("id=\"compare-url-3\""));
        assertTrue(html.contains("/assets/price-compare.js"));
        assertTrue(renderer.priceCompareScript().contains("/price-comparison"));
        assertTrue(renderer.scanRunScript().contains("/discovery/compare?product="));
    }

    @Test
    void onboardingLinksSampleCatalog() {
        String html = renderer.renderCatalogReview("session-9", "Trail Co.", null, "DTC",
                List.of("a need"), List.of(), 0, 0, "CSV", "owner@trail.example", "STARTER",
                List.of(), false, null);
        assertTrue(html.contains("/assets/sample-catalog.csv"));
        assertFalse(html.contains("catalog-template.csv"), "duplicate template link should be gone");
    }

    @Test
    void flowLinksToInlineEvidencePolicy() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("href=\"/discovery/observe#review-policy\""));
        assertFalse(html.contains("href=\"/discovery/governance\""));
    }

    @Test
    void flowHeaderKeepsEntryPointsOnly() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("href=\"/discovery\">Onboard"));
        assertTrue(html.contains("href=\"/discovery/observe\">Measure"));
        assertFalse(html.contains(">Pipeline</a>"), "mid-flow destinations must leave the header");
        assertFalse(html.contains(">Simulations</a>"), "mid-flow destinations must leave the header");
        assertFalse(html.contains(">Rules</a>"), "mid-flow destinations must leave the header");
    }

    @Test
    void largeReviewCapsRenderedRows() {
        java.util.List<AuditFlowRenderer.CatalogItem> products = new java.util.ArrayList<>();
        for (int index = 0; index < 150; index++) {
            products.add(new AuditFlowRenderer.CatalogItem("Product " + index, "facts"));
        }
        String html = renderer.renderCatalogReview("session-1", "Trail Co.", "https://trail.example",
                "DTC", List.of("a need"), products, 150, 0, "CSV", "owner@trail.example", "STARTER",
                List.of(), false, null);
        assertTrue(html.contains("Product 99"));
        assertTrue(html.contains("…and 50 more ready products"));
    }

    @Test
    void observationPagePreservesAlertsAndPolicy() {
        String html = renderer.renderObservationCapture(null);
        assertTrue(html.contains("Evidence and approval rules"));
        assertTrue(html.contains("A reviewer decides"));
        assertTrue(html.contains("id=\"governance-alerts\""));
        assertTrue(html.contains("id=\"governance-accuracy\""));
        assertTrue(html.contains("/assets/governance.js"));
        assertFalse(html.contains("href=\"/discovery/governance\""));
        assertTrue(renderer.governanceScript().contains("/contradictions?limit="));
        assertTrue(renderer.governanceScript().contains("/trends?points="));
    }

    @Test
    void simulationWithoutDataGuidesToOnboarding() {
        String html = renderer.renderSimulation(null, null, List.of());
        assertTrue(html.contains("/discovery"), "expected an onboarding link");
        for (String sample : SAMPLE_STRINGS) {
            assertFalse(html.contains(sample), "leaked sample content: " + sample);
        }
    }

    @Test
    void flowPagesDefaultToLightEditorialTheme() {
        assertTrue(renderer.themeScript().contains("kaviyo-theme-v2"), "theme preference key should be versioned");
        assertTrue(renderer.themeScript().contains("flow ? 'light'"), "flow pages should always render light");
        assertTrue(renderer.themeScript().contains("if (flow) return"), "flow theme toggle should be inert");
        String html = renderer.renderSimulation("Merchant", "https://shop.example", "a need",
                List.of(new AuditFlowRenderer.CatalogItem("Pack", "Price: 10 USD")));
        assertTrue(html.contains("border: 1px solid #41a1cf"), "primary action should be an outlined CTA");
        assertTrue(html.contains("border-bottom: 1px solid #444141"), "inputs should use the paper-form underline");
    }

    @Test
    void onboardingUsesEditorialChapters() {
        String html = renderer.renderDiscoveryWorkspace(null, "owner@trail.example", "STARTER");
        assertEquals(4, html.split("class=\"form-chapter\"", -1).length - 1, "expected four business chapters");
        for (String heading : List.of("Your business", "Business profile", "Your goals", "Customer questions")) {
            assertTrue(html.contains(heading), "missing chapter: " + heading);
        }
        assertFalse(html.contains("Source of truth"), "truth source lives on the products step");
        assertFalse(html.contains("Your products"), "products live on the products step");
        assertTrue(html.contains("name=\"stage\" value=\"business\""), "business stage marker missing");
        assertTrue(html.contains("Continue to products"), "business step should lead to products");
        assertTrue(html.contains("form-submit"), "submit row should be grouped");
        assertTrue(html.contains("name=\"goalMetric0\""), "goal editor hooks must survive the restructure");
    }

    @Test
    void reviewWithoutProductsEmbedsAddProductsForm() {
        String html = renderer.renderCatalogReview("session-9", "Trail & Co.", "https://trail.example", "DTC",
                List.of("a need"), List.of(), 0, 0, "CSV", "owner@trail.example", "STARTER",
                List.of("Mention rate at least 50% weekly"), false, null);
        assertTrue(html.contains("Add products to finish setup."), "review should ask for products first");
        assertTrue(html.contains("Setup review · Trail &amp; Co."), "setup summary should name the business");
        assertTrue(html.contains("trail.example"), "setup summary should show the website");
        assertTrue(html.contains("Mention rate at least 50% weekly"), "setup summary should show goals");
        assertTrue(html.contains("name=\"stage\" value=\"products\""), "products stage marker missing");
        assertTrue(html.contains("name=\"session\" value=\"session-9\""), "products form must reference the session");
        assertTrue(html.contains("Source of truth"), "missing truth-source chapter");
        assertTrue(html.contains("Your products"), "missing products chapter");
        assertTrue(html.contains("id=\"catalog-csv\""), "catalog hooks must survive the restructure");
        assertTrue(html.contains("Continue to product review"), "products form should lead to review");
        assertFalse(html.contains("Finish setup and start investigation"), "confirm must wait for products");
        String errorHtml = renderer.renderCatalogReview("session-9", "Trail & Co.", null, "DTC",
                List.of("a need"), List.of(), 0, 0, "CSV", "owner@trail.example", "STARTER",
                List.of(), false, "Choose a CSV file or add a product manually.");
        assertTrue(errorHtml.contains("Choose a CSV file or add a product manually."), "products error must render");
    }

    @Test
    void draftsPageGuidesMismatchResolution() {
        String html = renderer.renderDraftReview("scan-1", "prod-1", "PRICE_MISMATCH");
        assertTrue(html.contains("Resolve the mismatch"), "resolution mode should reframe the page");
        assertTrue(html.contains("id=\"resolve-submit\""), "resolution submit missing");
        assertTrue(html.contains("name=\"resolve-choice\""), "resolution choices missing");
        assertTrue(html.contains("value=\"ACCEPT_CATALOG\""), "catalog-accept choice missing");
        assertTrue(html.contains("value=\"ACCEPT_PAGE\""), "page-accept choice missing");
        assertTrue(html.contains("id=\"resolve-evidence\""), "evidence comparison missing");
        assertFalse(html.contains("id=\"draft-submit\""), "AI copy drafting must hide in resolution mode");
        String draftHtml = renderer.renderDraftReview("scan-1", "prod-1", "DESCRIPTION_THIN");
        assertTrue(draftHtml.contains("id=\"draft-submit\""), "draft mode must keep AI drafting");
        assertFalse(draftHtml.contains("id=\"resolve-submit\""), "resolution UI must hide in draft mode");
    }

    @Test
    void catalogReviewConfirmsWhenProductsExist() {
        var products = List.of(new AuditFlowRenderer.CatalogItem("Pack", "facts"));
        String html = renderer.renderCatalogReview("session-1", "Trail Co.", "https://trail.example", "DTC",
                List.of("a need"), products, 1, 0, "CSV", "owner@trail.example", "STARTER",
                List.of(), false, null);
        assertTrue(html.contains("Finish setup and start investigation"), "confirm must show with products");
        assertFalse(html.contains("Add products to finish setup."), "products prompt must hide with products");
    }

    @Test
    void accountStepKeepsItsRhythm() {
        String html = renderer.renderAccountStep();
        assertTrue(html.contains("field-note"), "helper text should be present");
        assertTrue(html.contains("Continue to business setup"), "CTA should be present");
        assertTrue(html.contains(".discovery-setup { max-width: 960px; gap: 18px;"),
                "form rhythm must not collapse where chapters are absent");
    }

    @Test
    void onboardingConnectsRealStores() {
        String html = renderer.renderCatalogReview("session-9", "Trail Co.", null, "DTC",
                List.of("a need"), List.of(), 0, 0, "CSV", "owner@trail.example", "STARTER",
                List.of(), false, null);
        assertTrue(html.contains("id=\"connect-provider\""), "provider picker missing");
        assertTrue(html.contains("id=\"connect-fetch\""), "fetch button missing");
        assertTrue(html.contains("id=\"connect-store-url\""), "store address field missing");
        assertTrue(html.contains("id=\"connect-apikey\""), "Wix key field missing");
        assertTrue(html.contains("id=\"connect-shopify-domain\""), "Shopify domain field missing");
        assertTrue(html.contains("Connect your store (optional)"), "connection must be framed as optional");
        assertTrue(html.contains("REST API"), "key instructions must say where to find keys");
        assertTrue(html.contains("Your website"), "truth source should address the reader");
        assertTrue(html.contains("appearance: none"), "selects should match the paper style");
        assertTrue(html.contains("never stored"), "key handling must be disclosed");
        assertFalse(html.contains("name=\"connect-secret\""), "secrets must not submit with the form");
        assertTrue(html.contains("id=\"store-connect-toggle\""), "store connect toggle missing");
        assertTrue(html.contains("aria-controls=\"store-connect-body\""), "store connect must control its body");
        assertTrue(html.contains("id=\"store-connect-body\" role=\"region\" aria-labelledby=\"store-connect-toggle\" hidden"),
                "store connect must stay collapsed until clicked");
        assertTrue(html.contains("id=\"spreadsheet-toggle\""), "spreadsheet toggle missing");
        assertTrue(html.contains("id=\"spreadsheet-body\" role=\"region\" aria-labelledby=\"spreadsheet-toggle\" hidden"),
                "spreadsheet upload must stay collapsed until clicked");
        assertTrue(html.contains("expand-panel__button"), "disclosures must be real buttons");
        assertFalse(html.contains("expand-panel > summary"), "flex must never sit directly on a summary element");
        assertTrue(renderer.onboardingScript().contains("/api/v1/catalog/connect"), "fetch must hit the connect API");
        assertTrue(renderer.onboardingScript().contains("aria-expanded"),
                "toggles must expose their expanded state");
        assertTrue(renderer.onboardingScript().contains("store-connect-panel"),
                "store address should prefill from the website already given");
        assertTrue(renderer.onboardingScript().contains("hasProducts"),
                "script must tolerate the business-only step");
        assertTrue(renderer.onboardingScript().contains("hasGoals"),
                "script must tolerate the products-only step");
        assertTrue(renderer.onboardingScript().contains("join('\\n')"), "CSV newline must survive the text block");
    }

    @Test
    void howItWorksFollowsTheVisionJourney() {
        String html = renderer.renderHowItWorks();
        for (String step : List.of("01 · Onboard", "02 · Pipeline", "03 · Simulations", "04 · Measure")) {
            assertTrue(html.contains(step), "missing journey step: " + step);
        }
        assertTrue(html.contains("source of truth"), "catalog role should be stated");
        assertFalse(html.contains("Controlled preview"), "old framing should be gone");
    }

    @Test
    void simulationFeedbackIsAlwaysVisibleAndPlain() {
        String html = renderer.renderSimulation("Merchant", "https://shop.example", "a need",
                List.of(new AuditFlowRenderer.CatalogItem("Pack", "Price: 10 USD")));
        assertTrue(html.contains("tabindex=\"-1\""), "result section should take focus when revealed");
        String script = renderer.agentSimulationScript();
        assertTrue(script.contains("plainError("), "crawler jargon should be translated");
        assertTrue(script.contains("scrollIntoView"), "feedback should scroll into view");
        assertTrue(script.contains("specific product page"), "dead end should offer a next step");
    }
}
