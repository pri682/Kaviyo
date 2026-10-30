package com.kaviyo.backend.api;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.scan.NotificationRecord;
import com.kaviyo.backend.scan.ScanRepository;
import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/notifications")
@Produces(MediaType.APPLICATION_JSON)
public final class NotificationResource {
    @Inject
    ScanRepository scanRepository;
    @Inject
    WorkspaceContext workspaceContext;

    @GET
    public Response list(@QueryParam("unreadOnly") boolean unreadOnly) {
        try {
            return Response.ok(new NotificationList("SYNTHETIC_DEMO",
                    scanRepository.listNotifications(workspaceContext.workspaceId(), unreadOnly)))
                    .build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("NOTIFICATIONS_UNAVAILABLE", "Saved notifications could not be loaded."))
                    .build();
        }
    }

    @POST
    @Path("/{notificationId}/read")
    public Response markRead(@PathParam("notificationId") String notificationId) {
        UUID id;
        try {
            id = UUID.fromString(notificationId);
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("NOTIFICATION_ID_INVALID", "notificationId must be a UUID."))
                    .build();
        }
        try {
            if (!scanRepository.markNotificationRead(workspaceContext.workspaceId(),
                    id, Instant.now())) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ApiError("NOTIFICATION_NOT_FOUND", "No notification exists with that ID."))
                        .build();
            }
            return Response.noContent().build();
        } catch (CatalogImportPersistenceException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("NOTIFICATIONS_UNAVAILABLE", "Notification state could not be saved."))
                    .build();
        }
    }

    public record NotificationList(String dataClassification, List<NotificationRecord> notifications) {
        public NotificationList {
            notifications = List.copyOf(notifications);
        }
    }

    public record ApiError(String code, String message) {
    }
}
