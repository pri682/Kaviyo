package com.kaviyo.backend.api;

import com.kaviyo.backend.workspace.WorkspaceContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/merchant-setups")
public class MerchantSetupResource {
    private static final List<String> CADENCES = List.of("daily", "weekly", "monthly");
    private static final List<String> GOAL_METRICS =
            List.of("MENTION_RATE", "ACCURACY_RATE", "FINDINGS_FIXED", "OBSERVATIONS_RECORDED");
    static final int MAX_GOALS = 5;

    @Inject DataSource database;
    @Inject WorkspaceContext workspace;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response latest() {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT id, business, business_type, website, background, policies, goal, cadence, updated_at
                     FROM merchant_setups WHERE workspace_id = ?
                     ORDER BY updated_at DESC NULLS LAST, created_at DESC LIMIT 1
                     """)) {
            statement.setObject(1, workspace.workspaceId());
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(new ApiError("SETUP_NOT_FOUND", "No business profile was saved yet.")).build();
                }
                return Response.ok(new SetupResponse(
                        rows.getObject("id", UUID.class), rows.getString("business"),
                        rows.getString("business_type"), rows.getString("website"),
                        rows.getString("background"), rows.getString("policies"),
                        rows.getString("goal"), rows.getString("cadence"),
                        rows.getTimestamp("updated_at") == null ? null
                                : rows.getTimestamp("updated_at").toInstant())).build();
            }
        } catch (SQLException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SETUP_UNAVAILABLE", "The business profile could not be loaded.")).build();
        }
    }

    @PUT @Path("/{id}") @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@PathParam("id") UUID id, @FormParam("business") String business,
                         @FormParam("businessType") String businessType,
                         @FormParam("website") String website, @FormParam("background") String background,
                         @FormParam("policies") String policies, @FormParam("goal") String goal,
                         @FormParam("cadence") String cadence) {
        String invalid = validationError(business, businessType, website, background, policies, goal, cadence);
        if (invalid != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("SETUP_REQUEST_INVALID", invalid)).build();
        }
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO merchant_setups(workspace_id, id, business, business_type, website, background,
                         policies, goal, cadence, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now())
                     ON CONFLICT (workspace_id, id) DO UPDATE SET
                         business = EXCLUDED.business, business_type = EXCLUDED.business_type,
                         website = EXCLUDED.website, background = EXCLUDED.background,
                         policies = EXCLUDED.policies, goal = EXCLUDED.goal, cadence = EXCLUDED.cadence,
                         updated_at = now()
                     """)) {
            statement.setObject(1, workspace.workspaceId());
            statement.setObject(2, id);
            statement.setString(3, business);
            statement.setString(4, emptyToNull(businessType));
            statement.setString(5, emptyToNull(website));
            statement.setString(6, emptyToNull(background));
            statement.setString(7, emptyToNull(policies));
            statement.setString(8, goal);
            statement.setString(9, cadence);
            statement.executeUpdate();
        } catch (SQLException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("SETUP_UNAVAILABLE", "The business profile could not be saved.")).build();
        }
        return Response.noContent().build();
    }

    static String validationError(String business, String businessType, String website,
                                  String background, String policies, String goal, String cadence) {
        if (business == null || business.isBlank()) return "Business name is required.";
        if (business.length() > 100) return "Business name must fit 100 characters.";
        if (businessType != null && businessType.length() > 40) return "Business type must fit 40 characters.";
        if (website != null && website.length() > 2000) return "Website must fit 2000 characters.";
        if (background != null && background.length() > 3000) return "Background must fit 3000 characters.";
        if (policies != null && policies.length() > 3000) return "Policies must fit 3000 characters.";
        if (goal != null && goal.length() > 600) return "Goal must fit 600 characters.";
        if (cadence != null && !CADENCES.contains(cadence)) return "Cadence must be daily, weekly, or monthly.";
        return null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record SetupResponse(UUID id, String business, String businessType, String website,
                                String background, String policies, String goal, String cadence,
                                Instant updatedAt) {
    }

    @GET
    @Path("/goals")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGoals() {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT id, metric, target, period, created_at FROM merchant_goals
                     WHERE workspace_id = ? ORDER BY created_at, id
                     """)) {
            statement.setObject(1, workspace.workspaceId());
            List<GoalRow> goals = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    goals.add(new GoalRow(rows.getObject("id", UUID.class), rows.getString("metric"),
                            rows.getBigDecimal("target"), rows.getString("period"),
                            rows.getTimestamp("created_at").toInstant()));
                }
            }
            return Response.ok(new GoalListResponse("MERCHANT_SUPPLIED", goals)).build();
        } catch (SQLException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("GOALS_UNAVAILABLE", "Goals could not be loaded.")).build();
        }
    }

    @PUT
    @Path("/goals")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response replaceGoals(GoalListRequest request) {
        String invalid = validateGoals(request == null ? null : request.goals());
        if (invalid != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("GOALS_REQUEST_INVALID", invalid)).build();
        }
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (var delete = connection.prepareStatement(
                    "DELETE FROM merchant_goals WHERE workspace_id = ?")) {
                delete.setObject(1, workspace.workspaceId());
                delete.executeUpdate();
            }
            try (var insert = connection.prepareStatement(
                    "INSERT INTO merchant_goals(workspace_id, id, metric, target, period) VALUES (?, ?, ?, ?, ?)")) {
                for (GoalInput goal : request.goals()) {
                    insert.setObject(1, workspace.workspaceId());
                    insert.setObject(2, UUID.randomUUID());
                    insert.setString(3, goal.metric());
                    insert.setBigDecimal(4, goal.target());
                    insert.setString(5, goal.period());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
        } catch (SQLException exception) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ApiError("GOALS_UNAVAILABLE", "Goals could not be saved.")).build();
        }
        return Response.noContent().build();
    }

    static String validateGoals(List<GoalInput> goals) {
        if (goals == null || goals.isEmpty()) return "Set at least one goal.";
        if (goals.size() > MAX_GOALS) return "Set at most " + MAX_GOALS + " goals.";
        boolean allKeyed = goals.stream().allMatch(g -> g != null && g.metric() != null && g.period() != null);
        if (allKeyed) {
            long distinctPairs = goals.stream().map(g -> g.metric() + "|" + g.period()).distinct().count();
            if (distinctPairs != goals.size()) return "Set each metric once per period.";
        }
        for (int index = 0; index < goals.size(); index++) {
            GoalInput goal = goals.get(index);
            String where = "Goal " + (index + 1);
            if (goal == null || goal.metric() == null || !GOAL_METRICS.contains(goal.metric())) {
                return where + " needs a known metric.";
            }
            if (goal.period() == null || !CADENCES.contains(goal.period())) {
                return where + " needs a daily, weekly, or monthly period.";
            }
            if (goal.target() == null) return where + " needs a target number.";
            if (isRate(goal.metric())) {
                if (goal.target().compareTo(java.math.BigDecimal.ONE) < 0
                        || goal.target().compareTo(new java.math.BigDecimal("100")) > 0) {
                    return where + " target must be a percent between 1 and 100.";
                }
            } else if (goal.target().compareTo(java.math.BigDecimal.ONE) < 0
                    || goal.target().stripTrailingZeros().scale() > 0) {
                return where + " target must be a whole number of 1 or more.";
            }
        }
        return null;
    }

    static boolean isRate(String metric) {
        return "MENTION_RATE".equals(metric) || "ACCURACY_RATE".equals(metric);
    }

    public record GoalInput(String metric, java.math.BigDecimal target, String period) {
    }

    public record GoalListRequest(List<GoalInput> goals) {
        public GoalListRequest {
            goals = goals == null ? List.of() : List.copyOf(goals);
        }
    }

    public record GoalRow(UUID id, String metric, java.math.BigDecimal target, String period, Instant createdAt) {
    }

    public record GoalListResponse(String dataClassification, List<GoalRow> goals) {
        public GoalListResponse {
            goals = List.copyOf(goals);
        }
    }

    public record ApiError(String code, String message) {
    }
}
