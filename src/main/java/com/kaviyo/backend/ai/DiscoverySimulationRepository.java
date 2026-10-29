package com.kaviyo.backend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DiscoverySimulationRepository {
    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public SimulationRecord save(SimulationRecord simulation) {
        String sql = """
                INSERT INTO discovery_simulations (
                    id, workspace_id, scan_run_id, target_product_id, draft_id,
                    shopper_question, simulation_label, baseline_model_run_id, draft_model_run_id,
                    baseline_selected_product_id, draft_selected_product_id, outcome,
                    comparison, limitation, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'CONTROLLED_MODEL_SIMULATION', ?, ?, ?, ?, ?,
                          CAST(? AS jsonb), ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, simulation.id());
            statement.setObject(2, simulation.workspaceId());
            statement.setObject(3, simulation.scanRunId());
            statement.setObject(4, simulation.targetProductId());
            statement.setObject(5, simulation.draftId());
            statement.setString(6, simulation.shopperQuestion());
            statement.setObject(7, simulation.baselineModelRunId());
            statement.setObject(8, simulation.draftModelRunId());
            statement.setString(9, simulation.baselineSelectedProductId());
            statement.setString(10, simulation.draftSelectedProductId());
            statement.setString(11, simulation.outcome());
            statement.setString(12, json(simulation.comparison()));
            statement.setString(13, simulation.limitation());
            statement.setTimestamp(14, Timestamp.from(simulation.createdAt()));
            statement.executeUpdate();
            return simulation;
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("The controlled discovery simulation could not be saved.", exception);
        }
    }

    private String json(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new CatalogImportPersistenceException("The simulation comparison could not be encoded.", exception);
        }
    }

    public record SimulationRecord(
            UUID id,
            UUID workspaceId,
            UUID scanRunId,
            UUID targetProductId,
            UUID draftId,
            String shopperQuestion,
            UUID baselineModelRunId,
            UUID draftModelRunId,
            String baselineSelectedProductId,
            String draftSelectedProductId,
            String outcome,
            JsonNode comparison,
            String limitation,
            Instant createdAt) {
    }
}
