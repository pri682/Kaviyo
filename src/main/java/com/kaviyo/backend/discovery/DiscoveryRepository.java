package com.kaviyo.backend.discovery;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DiscoveryRepository {
    @Inject DataSource dataSource;

    public List<Mission> listMissions(UUID workspaceId) {
        String sql = "SELECT id, mission_text, source_type, created_at FROM discovery_missions WHERE workspace_id = ? ORDER BY created_at DESC";
        List<Mission> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setObject(1, workspaceId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) result.add(new Mission(r.getObject("id", UUID.class), workspaceId,
                        r.getString("mission_text"), r.getString("source_type"),
                        r.getTimestamp("created_at").toInstant()));
            }
        } catch (SQLException e) {
            throw new CatalogImportPersistenceException("Discovery missions could not be loaded.", e);
        }
        return List.copyOf(result);
    }

    public Mission createMission(UUID workspaceId, String missionText, String sourceType) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        try (Connection c = dataSource.getConnection(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO discovery_missions(id, workspace_id, mission_text, source_type, created_at) VALUES (?, ?, ?, ?, ?)")) {
            s.setObject(1, id); s.setObject(2, workspaceId); s.setString(3, missionText);
            s.setString(4, sourceType); s.setTimestamp(5, Timestamp.from(now)); s.executeUpdate();
        } catch (SQLException e) {
            throw new CatalogImportPersistenceException("Discovery mission could not be saved.", e);
        }
        return new Mission(id, workspaceId, missionText, sourceType, now);
    }

    public List<Observation> listObservations(UUID workspaceId, UUID missionId) {
        return listObservationPage(workspaceId, missionId, 200, null, null).observations();
    }

    public ObservationPage listObservationPage(UUID workspaceId, UUID missionId, int limit,
                                                Instant beforeCreatedAt, UUID beforeId) {
        if ((beforeCreatedAt == null) != (beforeId == null)) {
            throw new IllegalArgumentException("Both observation cursor values must be provided together.");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT id, mission_id, provider, collection_method, status, prompt_text, answer_text,
                       source_url, observed_at, created_at
                FROM discovery_observations WHERE workspace_id = ?
                """);
        if (missionId != null) sql.append(" AND mission_id = ?");
        if (beforeCreatedAt != null) sql.append(" AND (created_at < ? OR (created_at = ? AND id < ?))");
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            statement.setObject(parameter++, workspaceId);
            if (missionId != null) statement.setObject(parameter++, missionId);
            if (beforeCreatedAt != null) {
                statement.setTimestamp(parameter++, Timestamp.from(beforeCreatedAt));
                statement.setTimestamp(parameter++, Timestamp.from(beforeCreatedAt));
                statement.setObject(parameter++, beforeId);
            }
            statement.setInt(parameter, limit + 1);

            List<ObservationRow> rows = new ArrayList<>(limit + 1);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp observedAt = result.getTimestamp("observed_at");
                    rows.add(new ObservationRow(result.getObject("id", UUID.class),
                            result.getObject("mission_id", UUID.class), result.getString("provider"),
                            result.getString("collection_method"), result.getString("status"),
                            result.getString("prompt_text"), result.getString("answer_text"),
                            result.getString("source_url"), observedAt == null ? null : observedAt.toInstant(),
                            result.getTimestamp("created_at").toInstant()));
                }
            }

            boolean hasMore = rows.size() > limit;
            if (hasMore) rows.remove(rows.size() - 1);
            List<UUID> observationIds = rows.stream().map(ObservationRow::id).toList();
            Map<UUID, List<Mention>> mentionsByObservation = new HashMap<>();
            Map<UUID, List<Claim>> claimsByObservation = new HashMap<>();
            observationIds.forEach(id -> {
                mentionsByObservation.put(id, new ArrayList<>());
                claimsByObservation.put(id, new ArrayList<>());
            });
            if (!observationIds.isEmpty()) {
                loadMentions(connection, workspaceId, observationIds, mentionsByObservation);
                loadClaims(connection, workspaceId, observationIds, claimsByObservation);
            }
            List<Observation> observations = rows.stream().map(row -> new Observation(row.id(), row.missionId(),
                    row.provider(), row.collectionMethod(), row.status(), row.prompt(), row.answer(), row.sourceUrl(),
                    row.observedAt(), row.createdAt(), mentionsByObservation.get(row.id()),
                    claimsByObservation.get(row.id()))).toList();
            ObservationRow last = rows.isEmpty() ? null : rows.get(rows.size() - 1);
            return new ObservationPage(observations, hasMore,
                    hasMore && last != null ? last.createdAt() : null,
                    hasMore && last != null ? last.id() : null);
        } catch (SQLException e) {
            throw new CatalogImportPersistenceException("Discovery observations could not be loaded.", e);
        }
    }

    public Observation createManualObservation(UUID workspaceId, UUID missionId, String provider,
            String prompt, String answer, String sourceUrl, Instant observedAt,
            List<MentionInput> mentions, List<ClaimInput> claims) {
        UUID id = UUID.randomUUID();
        Instant created = Instant.now();
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement s = c.prepareStatement("""
                        INSERT INTO discovery_observations(id, workspace_id, mission_id, provider,
                            collection_method, status, prompt_text, answer_text, source_url, observed_at, created_at)
                        VALUES (?, ?, ?, ?, 'MANUAL', 'OBSERVED', ?, ?, ?, ?, ?)
                        """)) {
                    s.setObject(1, id); s.setObject(2, workspaceId); s.setObject(3, missionId);
                    s.setString(4, provider); s.setString(5, prompt); s.setString(6, answer);
                    s.setString(7, sourceUrl); s.setTimestamp(8, Timestamp.from(observedAt));
                    s.setTimestamp(9, Timestamp.from(created)); s.executeUpdate();
                }
                for (MentionInput mention : mentions) try (PreparedStatement s = c.prepareStatement("""
                        INSERT INTO discovery_mentions(id, workspace_id, observation_id, product_reference,
                            brand_name, product_name, mention_context, position) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    s.setObject(1, UUID.randomUUID()); s.setObject(2, workspaceId); s.setObject(3, id);
                    s.setString(4, mention.productReference()); s.setString(5, mention.brandName());
                    s.setString(6, mention.productName()); s.setString(7, mention.context());
                    if (mention.position() == null) s.setObject(8, null); else s.setInt(8, mention.position());
                    s.executeUpdate();
                }
                for (ClaimInput claim : claims) try (PreparedStatement s = c.prepareStatement("""
                        INSERT INTO discovery_claims(id, workspace_id, observation_id, claim_text, verdict,
                            evidence_reference, evidence_value, product_reference) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    s.setObject(1, UUID.randomUUID()); s.setObject(2, workspaceId); s.setObject(3, id);
                    s.setString(4, claim.claimText()); s.setString(5, claim.verdict());
                    s.setString(6, claim.evidenceReference()); s.setString(7, claim.evidenceValue()); s.setString(8, claim.productReference());
                    s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback(); throw e;
            }
        } catch (SQLException e) {
            throw new CatalogImportPersistenceException("Discovery observation could not be saved.", e);
        }
        return new Observation(id, missionId, provider, "MANUAL", "OBSERVED", prompt, answer,
                sourceUrl, observedAt, created, mentions.stream().map(m -> new Mention(m.productReference(),
                m.brandName(), m.productName(), m.context(), m.position())).toList(), claims.stream()
                .map(c -> new Claim(c.claimText(), c.verdict(), c.evidenceReference(), c.evidenceValue(), c.productReference())).toList());
    }

    private static void loadMentions(Connection connection, UUID workspaceId, List<UUID> observationIds,
                                     Map<UUID, List<Mention>> byObservation) throws SQLException {
        String sql = "SELECT observation_id, product_reference, brand_name, product_name, mention_context, position "
                + "FROM discovery_mentions WHERE workspace_id = ? AND observation_id IN ("
                + placeholders(observationIds.size()) + ") ORDER BY observation_id, position NULLS LAST, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            for (int i = 0; i < observationIds.size(); i++) statement.setObject(i + 2, observationIds.get(i));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID observationId = result.getObject("observation_id", UUID.class);
                    byObservation.get(observationId).add(new Mention(result.getString("product_reference"),
                            result.getString("brand_name"), result.getString("product_name"),
                            result.getString("mention_context"), (Integer) result.getObject("position")));
                }
            }
        }
    }

    private static void loadClaims(Connection connection, UUID workspaceId, List<UUID> observationIds,
                                   Map<UUID, List<Claim>> byObservation) throws SQLException {
        String sql = "SELECT observation_id, claim_text, verdict, evidence_reference, evidence_value, product_reference "
                + "FROM discovery_claims WHERE workspace_id = ? AND observation_id IN ("
                + placeholders(observationIds.size()) + ") ORDER BY observation_id, claim_text, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            for (int i = 0; i < observationIds.size(); i++) statement.setObject(i + 2, observationIds.get(i));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UUID observationId = result.getObject("observation_id", UUID.class);
                    byObservation.get(observationId).add(new Claim(result.getString("claim_text"),
                            result.getString("verdict"), result.getString("evidence_reference"),
                            result.getString("evidence_value"), result.getString("product_reference")));
                }
            }
        }
    }

    private static String placeholders(int count) { return String.join(",", java.util.Collections.nCopies(count, "?")); }

    public record Mission(UUID id, UUID workspaceId, String missionText, String sourceType, Instant createdAt) { }
    public record ObservationPage(List<Observation> observations, boolean hasMore,
                                  Instant nextCursorCreatedAt, UUID nextCursorId) {
        public ObservationPage { observations = List.copyOf(observations); }
    }
    private record ObservationRow(UUID id, UUID missionId, String provider, String collectionMethod,
                                  String status, String prompt, String answer, String sourceUrl,
                                  Instant observedAt, Instant createdAt) { }
    public record MentionInput(String productReference, String brandName, String productName, String context, Integer position) { }
    public record ClaimInput(String claimText, String verdict, String evidenceReference, String evidenceValue, String productReference) { }
    public record Mention(String productReference, String brandName, String productName, String context, Integer position) { }
    public record Claim(String claimText, String verdict, String evidenceReference, String evidenceValue, String productReference) { }
    public record Observation(UUID id, UUID missionId, String provider, String collectionMethod, String status,
            String prompt, String answer, String sourceUrl, Instant observedAt, Instant createdAt,
            List<Mention> mentions, List<Claim> claims) {
        public Observation { mentions = List.copyOf(mentions); claims = List.copyOf(claims); }
    }
}
