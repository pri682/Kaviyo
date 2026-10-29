package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportConfirmation;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.importer.CatalogCsvPreviewService;
import com.kaviyo.importer.CatalogImportMapper;
import com.kaviyo.importer.CatalogImportPreview;
import com.kaviyo.importer.CsvPreviewException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Path("/api/v1/catalog/imports/confirm")
@Consumes("text/csv")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogImportConfirmResource {
    private final CatalogCsvPreviewService previewService = new CatalogCsvPreviewService();
    private final CatalogImportMapper importMapper = new CatalogImportMapper();

    @Inject
    CatalogImportRepository importRepository;

    @POST
    public Response confirm(String csv, @HeaderParam("X-Kaviyo-Confirm-Hash") String confirmationHash) {
        if (confirmationHash == null || confirmationHash.isBlank()) {
            return Response.status(428)
                    .entity(new ApiError("IMPORT_CONFIRMATION_REQUIRED",
                            "Preview the CSV, review its rows, then send its previewHash in X-Kaviyo-Confirm-Hash."))
                    .build();
        }
        String normalizedHash = confirmationHash.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalizedHash.matches("[0-9a-f]{64}")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("IMPORT_CONFIRMATION_INVALID",
                            "X-Kaviyo-Confirm-Hash must be the 64-character previewHash returned by preview."))
                    .build();
        }

        try {
            CatalogImportPreview preview = previewService.preview(csv);
            if (!constantTimeEquals(preview.previewHash(), normalizedHash)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ApiError("IMPORT_PREVIEW_CHANGED",
                                "CSV content differs from the reviewed preview. Preview it again before confirming."))
                        .build();
            }
            long invalidRows = preview.rows().stream().filter(row -> !row.readyToImport()).count();
            if (invalidRows > 0) {
                return Response.status(422)
                        .entity(new ApiError("IMPORT_HAS_INVALID_ROWS",
                                invalidRows + " row(s) need correction or removal before confirmation."))
                        .build();
            }
            CatalogImportConfirmation confirmation = importRepository.confirm(
                    importMapper.confirmed(preview, Instant.now()));
            return Response.status(Response.Status.CREATED).entity(confirmation).build();
        } catch (CsvPreviewException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("CSV_PREVIEW_INVALID", exception.getMessage()))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.serverError()
                    .entity(new ApiError("CATALOG_IMPORT_SAVE_FAILED", exception.getMessage()))
                    .build();
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    public record ApiError(String code, String message) {
    }
}
