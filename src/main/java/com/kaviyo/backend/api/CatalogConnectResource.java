package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.StoreConnectionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** One-shot product fetch from a merchant store. Credentials pass through and are never stored or logged. */
@Path("/api/v1/catalog/connect")
public class CatalogConnectResource {
    @Inject StoreConnectionService connections;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(ConnectRequest request) {
        String provider = request == null ? null : request.provider();
        String invalid = StoreConnectionService.validate(provider,
                request == null ? null : request.storeUrl(),
                request == null ? null : request.consumerKey(),
                request == null ? null : request.consumerSecret(),
                request == null ? null : request.apiKey(),
                request == null ? null : request.siteId());
        if (invalid != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("CONNECT_INVALID", invalid)).build();
        }
        try {
            var result = connections.fetch(provider, request.storeUrl(), request.consumerKey(),
                    request.consumerSecret(), request.apiKey(), request.siteId());
            return Response.ok(new ConnectResponse(result.provider(), result.store(), result.productCount(),
                    result.fetchedAt().toString(), result.products())).build();
        } catch (StoreConnectionService.ConnectionFailed failed) {
            return Response.status(502).entity(new ApiError("CONNECT_FAILED", failed.getMessage())).build();
        } catch (RuntimeException unexpected) {
            return Response.status(502).entity(new ApiError("CONNECT_FAILED", "The store could not be read right now.")).build();
        }
    }

    public record ConnectRequest(String provider, String storeUrl, String consumerKey, String consumerSecret,
                                 String apiKey, String siteId) {
    }

    public record ConnectResponse(String provider, String store, int productCount, String fetchedAt,
                                  java.util.List<StoreConnectionService.ConnectedProduct> products) {
    }

    public record ApiError(String code, String message) {
    }
}
