package com.kaviyo.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DraftRepository {
    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public DraftRecord create(
            UUID workspaceId,
            UUID scanRunId,
            CatalogImportRepository.CatalogProductSnapshot product,
            UUID sourceFindingId,
            String proposedDescription,
            String rationale,
            UUID modelRunId,
            JsonNode evidenceReferences,
            Instant createdAt) {
        UUID draftId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement draft = connection.prepareStatement("""
                    INSERT INTO catalog_drafts (
                        id, workspace_id, product_id, scan_run_id, source_finding_id,
                        current_revision, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, 1, 'NEEDS_REVIEW', ?, ?)
                    """);
                 PreparedStatement revision = connection.prepareStatement("""
                    INSERT INTO catalog_draft_revisions (
                        id, workspace_id, draft_id, revision_number, field_key,
                        original_value, proposed_value, rationale, source_model_run_id,
                        evidence_references, created_at
                    ) VALUES (?, ?, ?, 1, 'description', ?, ?, ?, ?, CAST(? AS jsonb), ?)
                    """)) {
                draft.setObject(1, draftId);
                draft.setObject(2, workspaceId);
                draft.setObject(3, product.id());
                draft.setObject(4, scanRunId);
                draft.setObject(5, sourceFindingId);
                draft.setTimestamp(6, Timestamp.from(createdAt));
                draft.setTimestamp(7, Timestamp.from(createdAt));
                draft.executeUpdate();

                revision.setObject(1, revisionId);
                revision.setObject(2, workspaceId);
                revision.setObject(3, draftId);
                revision.setString(4, product.description());
                revision.setString(5, proposedDescription);
                revision.setString(6, rationale);
                revision.setObject(7, modelRunId);
                revision.setString(8, json(evidenceReferences));
                revision.setTimestamp(9, Timestamp.from(createdAt));
                revision.executeUpdate();
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceError("The proposed draft could not be saved.", exception);
        }
        return get(workspaceId, draftId)
                .orElseThrow(() -> persistenceError("The saved draft could not be loaded.", null));
    }

    public List<DraftRecord> list(UUID workspaceId, UUID scanRunId) {
        String sql = baseSelect() + " WHERE d.workspace_id = ? AND (?::uuid IS NULL OR d.scan_run_id = ?)"
                + " ORDER BY d.updated_at DESC, d.id";
        List<DraftRecord> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, scanRunId);
            statement.setObject(3, scanRunId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(readDraft(rows));
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw persistenceError("Drafts could not be loaded.", exception);
        }
    }

    public Optional<DraftRecord> get(UUID workspaceId, UUID draftId) {
        String sql = baseSelect() + " WHERE d.workspace_id = ? AND d.id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, draftId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(readDraft(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw persistenceError("The requested draft could not be loaded.", exception);
        }
    }

    public Optional<DraftRecord> createMerchantRevision(
            UUID workspaceId, UUID draftId, String proposedDescription,
            String rationale, JsonNode evidenceReferences, Instant createdAt) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement lock = connection.prepareStatement("""
                    SELECT d.current_revision, d.status, r.proposed_value
                    FROM catalog_drafts d
                    JOIN catalog_draft_revisions r ON r.workspace_id = d.workspace_id
                        AND r.draft_id = d.id AND r.revision_number = d.current_revision
                    WHERE d.workspace_id = ? AND d.id = ? FOR UPDATE OF d
                    """)) {
                lock.setObject(1, workspaceId);
                lock.setObject(2, draftId);
                try (ResultSet row = lock.executeQuery()) {
                    if (!row.next()) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    String status = row.getString("status");
                    if (!"NEEDS_REVIEW".equals(status) && !"REVISION_REQUESTED".equals(status)) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    int nextRevision = row.getInt("current_revision") + 1;
                    String previousValue = row.getString("proposed_value");
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO catalog_draft_revisions (
                                id, workspace_id, draft_id, revision_number, field_key,
                                original_value, proposed_value, rationale, source_model_run_id,
                                evidence_references, created_at, author_type, author_label
                            ) VALUES (?, ?, ?, ?, 'description', ?, ?, ?, NULL, CAST(? AS jsonb), ?,
                                      'MERCHANT', 'LOCAL_DEMO_MERCHANT')
                            """)) {
                        insert.setObject(1, UUID.randomUUID());
                        insert.setObject(2, workspaceId);
                        insert.setObject(3, draftId);
                        insert.setInt(4, nextRevision);
                        insert.setString(5, previousValue);
                        insert.setString(6, proposedDescription);
                        insert.setString(7, rationale);
                        insert.setString(8, json(evidenceReferences));
                        insert.setTimestamp(9, Timestamp.from(createdAt));
                        insert.executeUpdate();
                    }
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE catalog_drafts
                            SET current_revision = ?, status = 'NEEDS_REVIEW', updated_at = ?,
                                decided_at = NULL, decision_note = NULL
                            WHERE workspace_id = ? AND id = ?
                            """)) {
                        update.setInt(1, nextRevision);
                        update.setTimestamp(2, Timestamp.from(createdAt));
                        update.setObject(3, workspaceId);
                        update.setObject(4, draftId);
                        update.executeUpdate();
                    }
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            throw persistenceError("The merchant-authored draft revision could not be saved.", exception);
        }
        return get(workspaceId, draftId);
    }

    public Optional<DraftRecord> decide(
            UUID workspaceId, UUID draftId, String requestedDecision, String note, Instant decidedAt) {
        String decision = switch (requestedDecision) {
            case "APPROVE" -> "APPROVED";
            case "REJECT" -> "REJECTED";
            case "REQUEST_REVISION" -> "REQUEST_REVISION";
            default -> throw new IllegalArgumentException("Unsupported draft decision.");
        };
        String nextStatus = "REQUEST_REVISION".equals(decision) ? "REVISION_REQUESTED" : decision;
        String select = """
                SELECT current_revision, status FROM catalog_drafts
                WHERE workspace_id = ? AND id = ? FOR UPDATE
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setObject(1, workspaceId);
                statement.setObject(2, draftId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    int revisionNumber = rows.getInt("current_revision");
                    if (!"NEEDS_REVIEW".equals(rows.getString("status"))) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO draft_decisions (
                                id, workspace_id, draft_id, revision_number, decision,
                                actor_label, note, decided_at
                            ) VALUES (?, ?, ?, ?, ?, 'LOCAL_DEMO_MERCHANT', ?, ?)
                            """)) {
                        insert.setObject(1, UUID.randomUUID());
                        insert.setObject(2, workspaceId);
                        insert.setObject(3, draftId);
                        insert.setInt(4, revisionNumber);
                        insert.setString(5, decision);
                        insert.setString(6, note);
                        insert.setTimestamp(7, Timestamp.from(decidedAt));
                        insert.executeUpdate();
                    }
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE catalog_drafts
                            SET status = ?, updated_at = ?, decided_at = ?, decision_note = ?
                            WHERE workspace_id = ? AND id = ?
                            """)) {
                    update.setString(1, nextStatus);
                        update.setTimestamp(2, Timestamp.from(decidedAt));
                        update.setTimestamp(3, Timestamp.from(decidedAt));
                        update.setString(4, note);
                        update.setObject(5, workspaceId);
                        update.setObject(6, draftId);
                        update.executeUpdate();
                    }
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            throw persistenceError("The draft decision could not be saved.", exception);
        }
        return get(workspaceId, draftId);
    }

    public Optional<ChangePacket> exportApproved(UUID workspaceId, UUID draftId, Instant generatedAt) {
        String sql = """
                SELECT d.status, d.scan_run_id, d.current_revision,
                       p.external_product_id, p.title, p.product_url,
                       r.field_key, r.original_value, r.proposed_value, r.rationale,
                       r.evidence_references
                FROM catalog_drafts d
                JOIN catalog_products p ON p.id = d.product_id AND p.workspace_id = d.workspace_id
                JOIN catalog_draft_revisions r ON r.workspace_id = d.workspace_id
                    AND r.draft_id = d.id AND r.revision_number = d.current_revision
                WHERE d.workspace_id = ? AND d.id = ? FOR UPDATE OF d
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement query = connection.prepareStatement(sql)) {
                query.setObject(1, workspaceId);
                query.setObject(2, draftId);
                try (ResultSet row = query.executeQuery()) {
                    if (!row.next()) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    String status = row.getString("status");
                    if (!"APPROVED".equals(status) && !"EXPORTED".equals(status)) {
                        connection.rollback();
                        return Optional.empty();
                    }
                    int revision = row.getInt("current_revision");
                    ObjectNode payload = objectMapper.createObjectNode();
                    payload.put("dataClassification", "SYNTHETIC_DEMO");
                    payload.put("publishStatus", "NOT_PUBLISHED");
                    payload.put("draftId", draftId.toString());
                    payload.put("scanRunId", row.getObject("scan_run_id", UUID.class).toString());
                    payload.put("productId", row.getString("external_product_id"));
                    payload.put("productTitle", row.getString("title"));
                    payload.put("productUrl", row.getString("product_url"));
                    payload.put("revision", revision);
                    ObjectNode change = payload.putObject("change");
                    change.put("field", row.getString("field_key"));
                    change.put("currentValue", row.getString("original_value"));
                    change.put("proposedValue", row.getString("proposed_value"));
                    payload.put("rationale", row.getString("rationale"));
                    try {
                        payload.set("evidenceReferences", objectMapper.readTree(row.getString("evidence_references")));
                    } catch (JsonProcessingException exception) {
                        throw persistenceError("The approved draft evidence could not be decoded.", exception);
                    }
                    payload.put("generatedAt", generatedAt.toString());
                    UUID exportId = UUID.randomUUID();
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO draft_export_packets (
                                id, workspace_id, draft_id, revision_number, format, payload, generated_at
                            ) VALUES (?, ?, ?, ?, 'JSON_CHANGE_PACKET', CAST(? AS jsonb), ?)
                            """)) {
                        insert.setObject(1, exportId);
                        insert.setObject(2, workspaceId);
                        insert.setObject(3, draftId);
                        insert.setInt(4, revision);
                        insert.setString(5, json(payload));
                        insert.setTimestamp(6, Timestamp.from(generatedAt));
                        insert.executeUpdate();
                    }
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE catalog_drafts SET status = 'EXPORTED', updated_at = ?
                            WHERE workspace_id = ? AND id = ?
                            """)) {
                        update.setTimestamp(1, Timestamp.from(generatedAt));
                        update.setObject(2, workspaceId);
                        update.setObject(3, draftId);
                        update.executeUpdate();
                    }
                    connection.commit();
                    return Optional.of(new ChangePacket(exportId, payload, generatedAt));
                }
            }
        } catch (SQLException exception) {
            throw persistenceError("The approved change packet could not be exported.", exception);
        }
    }

    private static String baseSelect() {
        return """
                SELECT d.id, d.scan_run_id, d.source_finding_id, d.current_revision, d.status,
                       d.created_at, d.updated_at, d.decided_at, d.decision_note,
                       p.external_product_id, p.title, p.product_url,
                       r.id AS revision_id, r.field_key, r.original_value, r.proposed_value,
                       r.rationale, r.source_model_run_id, r.evidence_references,
                       r.created_at AS revision_created_at, r.author_type, r.author_label
                FROM catalog_drafts d
                JOIN catalog_products p ON p.id = d.product_id AND p.workspace_id = d.workspace_id
                JOIN catalog_draft_revisions r ON r.workspace_id = d.workspace_id
                    AND r.draft_id = d.id AND r.revision_number = d.current_revision
                """;
    }

    private DraftRecord readDraft(ResultSet row) throws SQLException {
        Timestamp decidedAt = row.getTimestamp("decided_at");
        JsonNode evidence = readJson(row.getString("evidence_references"));
        DraftRevision revision = new DraftRevision(
                row.getObject("revision_id", UUID.class), row.getInt("current_revision"),
                row.getString("field_key"), row.getString("original_value"),
                row.getString("proposed_value"), row.getString("rationale"),
                row.getObject("source_model_run_id", UUID.class), evidence,
                row.getTimestamp("revision_created_at").toInstant(),
                row.getString("author_type"), row.getString("author_label"));
        return new DraftRecord(
                row.getObject("id", UUID.class), row.getObject("scan_run_id", UUID.class),
                row.getObject("source_finding_id", UUID.class), row.getString("external_product_id"),
                row.getString("title"), row.getString("product_url"),
                row.getInt("current_revision"), row.getString("status"),
                row.getTimestamp("created_at").toInstant(), row.getTimestamp("updated_at").toInstant(),
                decidedAt == null ? null : decidedAt.toInstant(), row.getString("decision_note"), revision);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw persistenceError("Draft data could not be encoded.", exception);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw persistenceError("Saved draft data could not be decoded.", exception);
        }
    }

    private static CatalogImportPersistenceException persistenceError(String message, Throwable cause) {
        return new CatalogImportPersistenceException(message, cause);
    }

    public record DraftRevision(
            UUID revisionId,
            int revision,
            String fieldKey,
            String currentValue,
            String proposedValue,
            String rationale,
            UUID sourceModelRunId,
            JsonNode evidenceReferences,
            Instant createdAt,
            String authorType,
            String authorLabel) {
    }

    public record DraftRecord(
            UUID id,
            UUID scanRunId,
            UUID sourceFindingId,
            String productId,
            String productTitle,
            String productUrl,
            int currentRevision,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant decidedAt,
            String decisionNote,
            DraftRevision revision) {
    }

    public record ChangePacket(UUID exportId, JsonNode payload, Instant generatedAt) {
    }
}
