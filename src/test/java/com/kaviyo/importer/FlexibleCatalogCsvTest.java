package com.kaviyo.importer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlexibleCatalogCsvTest {
    @Test void acceptsExportIndexAndCustomColumnsWithoutProductUrl() {
        var csv = "index;Listing headline;Material / finish\n1;\"Lamp; tall\";Brass\n";
        var service = new CatalogCsvPreviewService();
        var preview = service.preview(CatalogCsvPreviewService.mapTitleColumn(csv, "Listing headline"));
        var row = preview.rows().get(0);
        assertTrue(row.readyToImport());
        assertEquals("Lamp; tall", row.title());
        assertEquals("1", row.attributes().get("index"));
        assertEquals("Brass", row.attributes().get("material / finish"));
        assertNull(row.productUrl());
    }

    @Test void recognizesTabSeparatedProductNameAndSku() {
        var row = new CatalogCsvPreviewService().preview("Product Name\tVariant SKU\tColour\nCoat\tJK-1\tBlue").rows().get(0);
        assertEquals("JK-1", row.productId());
        assertTrue(row.readyToImport());
    }
}
