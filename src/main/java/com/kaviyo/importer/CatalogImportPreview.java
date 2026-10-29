package com.kaviyo.importer;

import java.util.List;

public record CatalogImportPreview(
        String dataClassification,
        String previewHash,
        int totalRows,
        int readyRows,
        int rowsNeedingReview,
        List<CatalogImportRowPreview> rows) {

    public CatalogImportPreview {
        rows = List.copyOf(rows);
    }
}
