package com.kaviyo.backend.workspace;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.List;
import java.util.UUID;

/** Resolves the current workspace from verified identity and its database memberships. */
@RequestScoped
public class WorkspaceContext {
    public static final UUID LOCAL_DEMO_WORKSPACE_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String WORKSPACE_HEADER = "X-Kaviyo-Workspace-ID";

    @Inject SecurityIdentity identity;
    @Inject WorkspaceRepository repository;
    @Context HttpHeaders headers;

    public UUID workspaceId() {
        if (isLocalDemo()) return LOCAL_DEMO_WORKSPACE_ID;
        if (identity.isAnonymous()) throw new ForbiddenException("Authentication is required.");

        String subject = identity.getPrincipal().getName();
        List<WorkspaceRepository.Workspace> memberships = repository.listForSubject(subject);
        String selectedId = headers.getHeaderString(WORKSPACE_HEADER);
        if (selectedId == null || selectedId.isBlank()) {
            if (memberships.size() == 1) return memberships.get(0).id();
            if (memberships.isEmpty()) {
                throw new ForbiddenException("Create or join a workspace before using this endpoint.");
            }
            throw new BadRequestException("Select a workspace with the X-Kaviyo-Workspace-ID header.");
        }

        UUID id;
        try {
            id = UUID.fromString(selectedId);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("X-Kaviyo-Workspace-ID must be a UUID.");
        }
        if (!repository.isMember(subject, id)) throw new NotFoundException("Workspace not found.");
        return id;
    }

    public String subject() {
        return isLocalDemo() ? "local-demo-user" : identity.getPrincipal().getName();
    }

    public boolean isLocalDemo() {
        return LaunchMode.current().isDevOrTest();
    }
}
