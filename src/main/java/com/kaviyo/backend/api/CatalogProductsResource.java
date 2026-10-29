package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.importer.CatalogImportMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/catalog/products")
@Produces(MediaType.APPLICATION_JSON)
public final class CatalogProductsResource {
    @Inject
    CatalogImportRepository importRepository;

    @GET
    public CatalogProductsResponse list() {
        return new CatalogProductsResponse(
                "SYNTHETIC_DEMO",
                "This local endpoint reads the confirmed demo workspace. Workspace authentication is not implemented.",
                CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID,
                importRepository.listCurrentProducts(CatalogImportMapper.LOCAL_DEMO_WORKSPACE_ID));
    }

    public record CatalogProductsResponse(
            String dataClassification,
            String limitation,
            java.util.UUID workspaceId,
            List<CatalogImportRepository.CatalogProductSnapshot> products) {
        public CatalogProductsResponse {
            products = List.copyOf(products);
        }
    }
}
