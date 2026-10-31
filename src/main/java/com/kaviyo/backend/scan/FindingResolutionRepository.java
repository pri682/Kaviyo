package com.kaviyo.backend.scan;

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
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FindingResolutionRepository {
    @Inject
    DataSource dataSource;

    public record ResolutionRecord(UUID id, UUID workspaceId, UUID scanRunId, UUID productId,
            String findingCode, String resolution, String note, Instant decidedAt) {
    }

    public ResolutionRecord save(UUID workspaceId, UUID scanRunId, UUID productId, UUID sourceFindingId,
            String findingCode, String resolution, String note, Instant decidedAt) {
        String sql = """
                INSERT INTO finding_resolutions (
                    id, workspace_id, scan_run_id, product_id, source_finding_id,
                    finding_code, resolution, note, decided_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (workspace_id, scan_run_id, product_id, finding_code)
                DO UPDATE SET resolution = EXCLUDED.resolution, note = EXCLUDED.note,
                    decided_at = EXCLUDED.decided_at
                RETURNING id, decided_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, workspaceId);
            statement.setObject(3, scanRunId);
            statement.setObject(4, productId);
            if (sourceFindingId == null) statement.setNull(5, java.sql.Types.OTHER);
            else statement.setObject(5, sourceFindingId);
            statement.setString(6, findingCode);
            statement.setString(7, resolution);
            statement.setString(8, note);
            statement.setTimestamp(9, Timestamp.from(decidedAt));
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return new ResolutionRecord((UUID) rows.getObject("id"), workspaceId, scanRunId, productId,
                        findingCode, resolution, note, rows.getTimestamp("decided_at").toInstant());
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Finding resolution could not be saved.", exception);
        }
    }

    public Optional<ResolutionRecord> find(UUID workspaceId, UUID scanRunId, UUID productId, String findingCode) {
        String sql = """
                SELECT id, resolution, note, decided_at FROM finding_resolutions
                WHERE workspace_id = ? AND scan_run_id = ? AND product_id = ? AND finding_code = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, scanRunId);
            statement.setObject(3, productId);
            statement.setString(4, findingCode);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(new ResolutionRecord((UUID) rows.getObject("id"), workspaceId, scanRunId,
                        productId, findingCode, rows.getString("resolution"), rows.getString("note"),
                        rows.getTimestamp("decided_at").toInstant()));
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Finding resolution could not be loaded.", exception);
        }
    }
}
