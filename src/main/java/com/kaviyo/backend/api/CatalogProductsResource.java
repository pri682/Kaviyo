package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.backend.catalog.PriceComparisonService;
import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Path("/api/v1/catalog/products")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogProductsResource {
    @Inject
    CatalogImportRepository importRepository;
    @Inject
    WorkspaceContext workspaceContext;
    @Inject
    PriceComparisonService priceComparisonService;

    @GET
    public Response list(@DefaultValue("50") @QueryParam("limit") int limit,
                         @DefaultValue("0") @QueryParam("offset") int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 1_000_000) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("CATALOG_PAGE_INVALID", "limit must be 1–100 and offset 0–1000000."))
                    .build();
        }
        CatalogImportRepository.CatalogProductPage page = importRepository.listCurrentProducts(
                workspaceContext.workspaceId(), limit, offset);
        return Response.ok(new CatalogProductsResponse(
                "MERCHANT_SUPPLIED", workspaceContext.workspaceId(), limit, offset,
                page.hasMore(), page.products())).build();
    }

    @POST
    @Path("/{productId}/price-comparison")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response comparePrices(@PathParam("productId") String productId, ComparisonRequest request) {
        if (productId == null || productId.isBlank() || productId.length() > 180) {
            return error(Response.Status.BAD_REQUEST, "PRODUCT_ID_INVALID",
                    "productId must be a nonblank catalog identifier.");
        }
        if (request == null || request.sourceUrls() == null || request.sourceUrls().isEmpty()
                || request.sourceUrls().size() > PriceComparisonService.MAX_SOURCES) {
            return error(Response.Status.BAD_REQUEST, "COMPARISON_REQUEST_INVALID",
                    "sourceUrls must list 1 to " + PriceComparisonService.MAX_SOURCES + " outside product page URLs.");
        }
        List<URI> sources = new ArrayList<>();
        for (String raw : request.sourceUrls()) {
            URI source = parseSourceUrl(raw);
            if (source == null) {
                return error(Response.Status.BAD_REQUEST, "COMPARISON_URL_INVALID",
                        "Each source URL must be an http(s) URL up to 2000 characters.");
            }
            sources.add(source);
        }
        try {
            return priceComparisonService.compare(workspaceContext.workspaceId(), productId.trim(), sources)
                    .<Response>map(report -> Response.ok(new ComparisonResponse(
                            "MERCHANT_SUPPLIED", report,
                            "Each pasted URL was fetched once on demand with KaviyoProductAuditBot "
                                    + "(robots-aware, no page scripts run). Price and availability come from "
                                    + "Product structured data or price meta tags; missing values are reported "
                                    + "as unknown, never guessed. Merchant catalog values are the baseline.",
                            "Outside pages change at any time; this comparison reflects one fetch per URL "
                                    + "and is not stored."))
                            .build())
                    .orElseGet(() -> error(Response.Status.NOT_FOUND, "PRODUCT_NOT_FOUND",
                            "No confirmed product exists with that ID."));
        } catch (CatalogImportPersistenceException exception) {
            return error(Response.Status.SERVICE_UNAVAILABLE, "COMPARISON_UNAVAILABLE",
                    "Saved catalog facts could not be loaded.");
        }
    }

    private static URI parseSourceUrl(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 2000) return null;
        try {
            URI uri = new URI(raw.trim());
            String scheme = uri.getScheme();
            if ((!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null) return null;
            return uri;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static Response error(Response.Status status, String code, String message) {
        return Response.status(status).entity(new ApiError(code, message)).build();
    }

    public record ComparisonRequest(List<String> sourceUrls) {
        public ComparisonRequest {
            sourceUrls = sourceUrls == null ? List.of() : List.copyOf(sourceUrls);
        }
    }

    public record ComparisonResponse(
            String dataClassification,
            PriceComparisonService.ComparisonReport report,
            String methodology,
            String limitation) {
    }

    public record CatalogProductsResponse(
            String dataClassification,
            java.util.UUID workspaceId,
            int limit,
            int offset,
            boolean hasMore,
            List<CatalogImportRepository.CatalogProductSnapshot> products) {
        public CatalogProductsResponse {
            products = List.copyOf(products);
        }
    }

    public record ApiError(String code, String message) { }
}
