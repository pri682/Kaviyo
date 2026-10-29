package com.kaviyo.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ModelRunRepository {
    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public ModelRunRecord save(ModelRunRecord run) {
        String sql = """
                INSERT INTO model_runs (
                    id, workspace_id, scan_run_id, product_id, provider, model_id,
                    operation, prompt_version, status, input_evidence_ids, tool_names,
                    output_json, validation_json, prompt_tokens, completion_tokens,
                    started_at, completed_at, error_code
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb),
                          CAST(? AS jsonb), CAST(? AS jsonb), ?, ?, ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, run.id());
            statement.setObject(2, run.workspaceId());
            statement.setObject(3, run.scanRunId());
            statement.setObject(4, run.productId());
            statement.setString(5, run.provider());
            statement.setString(6, run.modelId());
            statement.setString(7, run.operation());
            statement.setString(8, run.promptVersion());
            statement.setString(9, run.status());
            statement.setString(10, json(run.inputEvidenceIds()));
            statement.setString(11, json(run.toolNames()));
            statement.setString(12, run.outputJson() == null ? null : json(run.outputJson()));
            statement.setString(13, json(run.validation()));
            if (run.promptTokens() == null) statement.setNull(14, java.sql.Types.INTEGER);
            else statement.setInt(14, run.promptTokens());
            if (run.completionTokens() == null) statement.setNull(15, java.sql.Types.INTEGER);
            else statement.setInt(15, run.completionTokens());
            statement.setTimestamp(16, Timestamp.from(run.startedAt()));
            if (run.completedAt() == null) statement.setNull(17, java.sql.Types.TIMESTAMP_WITH_TIMEZONE);
            else statement.setTimestamp(17, Timestamp.from(run.completedAt()));
            statement.setString(18, run.errorCode());
            statement.executeUpdate();
            return run;
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("The model run could not be saved.", exception);
        }
    }

    public Optional<ModelRunRecord> get(UUID workspaceId, UUID modelRunId) {
        String sql = """
                SELECT id, workspace_id, scan_run_id, product_id, provider, model_id,
                       operation, prompt_version, status, input_evidence_ids, tool_names,
                       output_json, validation_json, prompt_tokens, completion_tokens,
                       started_at, completed_at, error_code
                FROM model_runs WHERE workspace_id = ? AND id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, modelRunId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(new ModelRunRecord(
                        rows.getObject("id", UUID.class),
                        rows.getObject("workspace_id", UUID.class),
                        rows.getObject("scan_run_id", UUID.class),
                        rows.getObject("product_id", UUID.class),
                        rows.getString("provider"), rows.getString("model_id"),
                        rows.getString("operation"), rows.getString("prompt_version"),
                        rows.getString("status"),
                        readJson(rows.getString("input_evidence_ids")),
                        readJson(rows.getString("tool_names")),
                        rows.getString("output_json") == null ? null : readJson(rows.getString("output_json")),
                        readJson(rows.getString("validation_json")),
                        (Integer) rows.getObject("prompt_tokens"),
                        (Integer) rows.getObject("completion_tokens"),
                        rows.getTimestamp("started_at").toInstant(),
                        rows.getTimestamp("completed_at") == null ? null
                                : rows.getTimestamp("completed_at").toInstant(),
                        rows.getString("error_code")));
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("The model run could not be loaded.", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CatalogImportPersistenceException("Model run metadata could not be encoded.", exception);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new CatalogImportPersistenceException("Saved model run metadata could not be decoded.", exception);
        }
    }

    public record ModelRunRecord(
            UUID id,
            UUID workspaceId,
            UUID scanRunId,
            UUID productId,
            String provider,
            String modelId,
            String operation,
            String promptVersion,
            String status,
            JsonNode inputEvidenceIds,
            JsonNode toolNames,
            JsonNode outputJson,
            JsonNode validation,
            Integer promptTokens,
            Integer completionTokens,
            Instant startedAt,
            Instant completedAt,
            String errorCode) {

        public static ModelRunRecord create(
                UUID workspaceId,
                UUID scanRunId,
                UUID productId,
                String provider,
                String modelId,
                String operation,
                String promptVersion,
                String status,
                List<String> inputEvidenceIds,
                List<String> toolNames,
                JsonNode output,
                JsonNode validation,
                Integer promptTokens,
                Integer completionTokens,
                Instant startedAt,
                Instant completedAt,
                String errorCode) {
            return new ModelRunRecord(UUID.randomUUID(), workspaceId, scanRunId, productId,
                    provider, modelId, operation, promptVersion, status,
                    stringArray(inputEvidenceIds),
                    stringArray(toolNames),
                    output, validation == null ? JsonNodeFactory.instance.objectNode() : validation,
                    promptTokens, completionTokens, startedAt, completedAt, errorCode);
        }

        private static ArrayNode stringArray(List<String> values) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            values.forEach(result::add);
            return result;
        }
    }
}
