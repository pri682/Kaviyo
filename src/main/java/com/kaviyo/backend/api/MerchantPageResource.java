package com.kaviyo.backend.api;

import com.kaviyo.backend.workspace.WorkspaceContext;
import com.kaviyo.crawler.SafePageFetcher;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;

@Path("/api/v1/discovery/merchant-page")
@Produces(MediaType.APPLICATION_JSON)
public class MerchantPageResource {
    @Inject SafePageFetcher fetcher;
    @Inject WorkspaceContext workspace;
    @ConfigProperty(name="kaviyo.crawler.enabled", defaultValue="false") boolean enabled;

    @POST @Consumes(MediaType.APPLICATION_JSON)
    public Response inspect(Request request) {
        workspace.workspaceId();
        if (!enabled) return Response.status(503).build();
        if (request == null || request.url() == null || request.url().length() > 2048)
            return Response.status(400).build();
        URI url;
        try { url = URI.create(request.url()); }
        catch (IllegalArgumentException invalid) { return Response.status(400).build(); }
        var page = fetcher.scan("merchant-page", url);
        return Response.ok(page).build();
    }
    public record Request(String url) {}
}
