package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.workspace.WorkspaceContext;
import com.kaviyo.backend.workspace.WorkspaceRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/v1/workspaces")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public final class WorkspaceResource {
    @Inject WorkspaceContext workspaceContext;
    @Inject WorkspaceRepository workspaceRepository;

    @GET
    public Response list() {
        try {
            return Response.ok(new WorkspaceList(workspaceRepository.listForSubject(workspaceContext.subject()))).build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("WORKSPACES_UNAVAILABLE", "Workspaces could not be loaded.")).build();
        }
    }

    @POST
    public Response create(CreateWorkspaceRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() || request.name().trim().length() > 160) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("WORKSPACE_NAME_INVALID", "name must contain 1–160 characters.")).build();
        }
        try {
            WorkspaceRepository.Workspace workspace = workspaceRepository.createForSubject(
                    workspaceContext.subject(), request.name().trim());
            return Response.status(Response.Status.CREATED).entity(workspace).build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("WORKSPACE_CREATE_UNAVAILABLE", "The workspace could not be created.")).build();
        }
    }

    public record CreateWorkspaceRequest(String name) { }
    public record WorkspaceList(List<WorkspaceRepository.Workspace> workspaces) {
        public WorkspaceList { workspaces = List.copyOf(workspaces); }
    }
    public record ApiError(String code, String message) { }
}
