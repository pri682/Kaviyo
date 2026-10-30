package com.kaviyo.backend.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import javax.sql.DataSource;
import java.util.UUID;
import java.util.List;

@Path("/api/v1/discovery/page-reviews")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PageReviewResource {
    @Inject WorkspaceContext workspace;
    @Inject DataSource database;
    @Inject ObjectMapper mapper;

    public record Review(UUID id, String product, String url, String retrievedAt, String currentCopy,
                         String catalogFacts, String supportingFact, String proposedCopy, String choice, List<String> sources) {}
    @POST
    public Response record(Review review) throws Exception {
        if (review == null || review.id()==null || review.product()==null || review.product().isBlank()
                || review.url()==null || review.retrievedAt()==null || review.catalogFacts()==null
                || review.currentCopy()==null || review.proposedCopy()==null || review.proposedCopy().isBlank()
                || !("APPROVE".equals(review.choice()) || "KEEP_ORIGINAL".equals(review.choice())))
            return Response.status(400).build();
        if ("APPROVE".equals(review.choice()) && (review.supportingFact()==null || review.supportingFact().isBlank()
                || !review.catalogFacts().contains(review.supportingFact()) || review.sources()==null
                || !review.sources().contains(review.url())))
            return Response.status(400).entity(java.util.Map.of("message","Approval requires a quoted catalog fact and the reviewed page source.")).build();
        String payload = mapper.writeValueAsString(review);
        if (payload.length()>32000) return Response.status(413).build();
        try (var c=database.getConnection(); var s=c.prepareStatement("""
                INSERT INTO page_review_decisions(workspace_id,id,reviewer,payload) VALUES (?,?,?,?::jsonb)
                ON CONFLICT (workspace_id,id) DO UPDATE SET payload=page_review_decisions.payload
                WHERE page_review_decisions.payload=EXCLUDED.payload AND page_review_decisions.reviewer=EXCLUDED.reviewer
                """)) {
            s.setObject(1,workspace.workspaceId()); s.setObject(2,review.id()); s.setString(3,workspace.subject()); s.setString(4,payload);
            return s.executeUpdate()==1 ? Response.ok(review).build() : Response.status(409).build();
        }
    }
}
