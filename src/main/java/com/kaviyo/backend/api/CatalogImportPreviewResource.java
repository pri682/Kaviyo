package com.kaviyo.backend.api;

import com.kaviyo.importer.CatalogCsvPreviewService;
import com.kaviyo.importer.CatalogImportPreview;
import com.kaviyo.importer.CsvPreviewException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/catalog/imports/preview")
@Consumes("text/csv")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogImportPreviewResource {
    private final CatalogCsvPreviewService previewService = new CatalogCsvPreviewService();

    @POST
    public Response preview(String csv) {
        try {
            CatalogImportPreview preview = previewService.preview(csv);
            return Response.ok(preview).build();
        } catch (CsvPreviewException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("CSV_PREVIEW_INVALID", exception.getMessage()))
                    .build();
        }
    }

    public record ApiError(String code, String message) {
    }
}
