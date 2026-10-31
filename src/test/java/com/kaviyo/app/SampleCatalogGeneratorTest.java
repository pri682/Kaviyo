package com.kaviyo.app;

import com.kaviyo.importer.CatalogCsvPreviewService;
import com.kaviyo.importer.CsvPreviewException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleCatalogGeneratorTest {
    @Test
    void generatesTwoThousandImportableRows() throws CsvPreviewException {
        String csv = SampleCatalogGenerator.generate();
        assertTrue(csv.getBytes(StandardCharsets.UTF_8).length < 1_048_576,
                "sample must fit the 1 MiB upload cap");
        var preview = new CatalogCsvPreviewService().preview(csv);
        assertEquals(SampleCatalogGenerator.ROW_COUNT, preview.totalRows());
        assertEquals(0, preview.rowsNeedingReview());
    }

    @Test
    void outputIsDeterministicAndRealistic() {
        assertEquals(SampleCatalogGenerator.generate(10), SampleCatalogGenerator.generate(10));
        String first = SampleCatalogGenerator.generate(1);
        assertTrue(first.startsWith("product_id,title,url,sku,brand,category,description,price,currency,availability,"));
        assertTrue(first.contains("cedarworks-daypacks-1"));
        assertTrue(first.contains("Cedar Works"));
        String fifty = SampleCatalogGenerator.generate(50);
        assertTrue(fifty.contains("Lightweight and durable."),
                "expected one intentionally thin description for the draft-flow demo");
    }
}
