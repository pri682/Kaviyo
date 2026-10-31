package com.kaviyo.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationExtractionServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void acceptsWellFormedSuggestions() {
        ObjectNode parsed = mapper.createObjectNode();
        ArrayNode mentions = parsed.putArray("mentions");
        ObjectNode mention = mentions.addObject();
        mention.put("brandName", "Cedar Works");
        mention.put("productName", "Cedar Air 13");
        mention.put("position", 1);
        mention.put("quote", "Cedar Air 13");
        ArrayNode claims = parsed.putArray("claims");
        ObjectNode claim = claims.addObject();
        claim.put("claimText", "16 GB RAM");
        claim.put("suggestedVerdict", "SUPPORTED");
        claim.put("rationale", "Catalog lists 16 GB RAM.");
        assertEquals(1, ObservationExtractionService.mentions(parsed).size());
        assertEquals(1, ObservationExtractionService.claims(parsed).size());
    }

    @Test
    void rejectsUnknownVerdict() {
        ObjectNode parsed = mapper.createObjectNode();
        parsed.putArray("mentions");
        ArrayNode claims = parsed.putArray("claims");
        ObjectNode claim = claims.addObject();
        claim.put("claimText", "16 GB RAM");
        claim.put("suggestedVerdict", "MAYBE");
        ModelProviderException error = assertThrows(ModelProviderException.class,
                () -> ObservationExtractionService.claims(parsed));
        assertEquals("EXTRACTION_REJECTED", error.code());
    }

    @Test
    void rejectsNamelessMentionAndOversizeOutput() {
        ObjectNode nameless = mapper.createObjectNode();
        nameless.putArray("mentions").addObject().put("quote", "it");
        nameless.putArray("claims");
        assertThrows(ModelProviderException.class,
                () -> ObservationExtractionService.mentions(nameless));
        ObjectNode overflow = mapper.createObjectNode();
        ArrayNode mentions = overflow.putArray("mentions");
        for (int index = 0; index <= ObservationExtractionService.MAX_MENTIONS; index++) {
            mentions.addObject().put("productName", "Product " + index);
        }
        overflow.putArray("claims");
        assertThrows(ModelProviderException.class,
                () -> ObservationExtractionService.mentions(overflow));
    }

    @Test
    void rejectsBlankClaimText() {
        ObjectNode parsed = mapper.createObjectNode();
        parsed.putArray("mentions");
        ArrayNode claims = parsed.putArray("claims");
        ObjectNode claim = claims.addObject();
        claim.put("claimText", "  ");
        claim.put("suggestedVerdict", "UNKNOWN");
        assertThrows(ModelProviderException.class,
                () -> ObservationExtractionService.claims(parsed));
        assertTrue(ObservationExtractionService.claims(emptyParsed()).isEmpty());
    }

    @Test
    void ranksMentionedProductsFirst() {
        var unrelated = snapshot("daypack-1", "Trail Daypack", "Cedar Works");
        var mentioned = snapshot("laptop-1", "Cedar Air 13", "Cedar Works");
        var ordered = ObservationExtractionService.ranked(
                java.util.List.of(unrelated, mentioned), "The Cedar Air 13 is a great laptop");
        assertEquals("laptop-1", ordered.get(0).productId());
        var tied = ObservationExtractionService.ranked(
                java.util.List.of(unrelated, mentioned), "something with no overlap xyzzy");
        assertEquals("Cedar Air 13", tied.get(0).title());
    }

    private static com.kaviyo.backend.catalog.CatalogImportRepository.CatalogProductSnapshot snapshot(
            String productId, String title, String brand) {
        return new com.kaviyo.backend.catalog.CatalogImportRepository.CatalogProductSnapshot(
                java.util.UUID.randomUUID(), productId, title, "SKU", brand, "Category",
                "Description", null, null, null, null, null, null, java.util.Map.of());
    }

    private ObjectNode emptyParsed() {
        ObjectNode parsed = mapper.createObjectNode();
        parsed.putArray("mentions");
        parsed.putArray("claims");
        return parsed;
    }
}
