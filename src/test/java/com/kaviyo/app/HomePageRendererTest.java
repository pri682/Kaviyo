package com.kaviyo.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HomePageRendererTest {
    private final HomePageRenderer renderer = new HomePageRenderer();

    @Test
    void darkButtonsStayReadable() {
        String html = renderer.render();
        assertTrue(html.contains(".home-page[data-theme=\"dark\"] .home-nav .nav-action"),
                "nav CTA needs a dark-mode text color");
        assertTrue(html.contains(".home-page[data-theme=\"dark\"] .home-close .home-primary"),
                "hero CTA needs a dark-mode text color");
    }
}
