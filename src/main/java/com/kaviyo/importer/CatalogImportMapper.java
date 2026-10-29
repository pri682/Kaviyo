package com.kaviyo.importer;

import com.kaviyo.catalog.CatalogFactObservation;
import com.kaviyo.catalog.CatalogProductRecord;
import com.kaviyo.catalog.ConfirmedCatalogImport;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Converts the validated import preview into domain records after the user confirms it. */
public final class CatalogImportMapper {
    public static final UUID LOCAL_DEMO_WORKSPACE_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000001");

    public ConfirmedCatalogImport confirmed(CatalogImportPreview preview, Instant confirmedAt) {
        List<CatalogProductRecord> products = preview.rows().stream()
                .map(row -> product(row, confirmedAt))
                .toList();
        return new ConfirmedCatalogImport(
                UUID.randomUUID(), LOCAL_DEMO_WORKSPACE_ID, preview.previewHash(), confirmedAt, products);
    }

    private static CatalogProductRecord product(CatalogImportRowPreview row, Instant confirmedAt) {
        List<CatalogFactObservation> observations = new ArrayList<>();
        add(observations, "product_id", row.productId(), row.rowNumber(), confirmedAt);
        add(observations, "title", row.title(), row.rowNumber(), confirmedAt);
        add(observations, "sku", row.sku(), row.rowNumber(), confirmedAt);
        add(observations, "brand", row.brand(), row.rowNumber(), confirmedAt);
        add(observations, "category", row.category(), row.rowNumber(), confirmedAt);
        add(observations, "description", row.description(), row.rowNumber(), confirmedAt);
        add(observations, "price", row.price() == null ? null : row.price().toPlainString(), row.rowNumber(), confirmedAt);
        add(observations, "currency", row.currency(), row.rowNumber(), confirmedAt);
        add(observations, "availability", row.availability(), row.rowNumber(), confirmedAt);
        add(observations, "url", row.productUrl().toString(), row.rowNumber(), confirmedAt);
        row.attributes().forEach((key, value) ->
                add(observations, "attribute." + key, value, row.rowNumber(), confirmedAt));

        return new CatalogProductRecord(
                row.productId(), row.title(), row.sku(), row.brand(), row.category(), row.description(),
                row.price(), row.currency(), row.availability(), row.productUrl(),
                Map.copyOf(row.attributes()), observations);
    }

    private static void add(
            List<CatalogFactObservation> observations,
            String field,
            String value,
            int rowNumber,
            Instant observedAt) {
        if (value != null && !value.isBlank()) {
            observations.add(new CatalogFactObservation(
                    field, value, "MERCHANT_CSV",
                    "csv:row:" + rowNumber + ":column:" + field,
                    observedAt, observedAt));
        }
    }
}
