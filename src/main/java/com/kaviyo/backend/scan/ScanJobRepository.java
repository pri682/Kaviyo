package com.kaviyo.backend.scan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.io.IOException;
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

/** PostgreSQL-backed scan queue. SKIP LOCKED lets multiple app instances share the work safely. */
@ApplicationScoped
public class ScanJobRepository {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_ACTIVE_JOBS_PER_WORKSPACE = 20;

    @Inject DataSource dataSource;
    @Inject ObjectMapper objectMapper;

    public QueuedScan enqueue(UUID workspaceId,
                              List<CatalogImportRepository.CatalogProductSnapshot> products,
                              Instant queuedAt, UUID setupId) {
        UUID runId = setupId == null ? UUID.randomUUID() : UUID.nameUUIDFromBytes((workspaceId+"/"+setupId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String runSql = """
                INSERT INTO catalog_scan_runs (
                    id, workspace_id, status, observation_type, limitation,
                    requested_products, started_at
                ) VALUES (?, ?, 'QUEUED', 'KAVIYO_FETCH', ?, ?, ?)
                """;
        String jobSql = """
                INSERT INTO catalog_scan_jobs (
                    scan_run_id, workspace_id, status, attempts, available_at, created_at, updated_at
                ) VALUES (?, ?, 'PENDING', 0, ?, ?, ?)
                """;
        String productSql = """
                INSERT INTO catalog_scan_job_products (workspace_id, scan_run_id, ordinal, product_snapshot)
                VALUES (?, ?, ?, CAST(? AS jsonb))
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Serialize enqueue checks per workspace so concurrent requests cannot exceed the cap.
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id FROM workspaces WHERE id = ? FOR UPDATE")) {
                    lock.setObject(1, workspaceId);
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) throw new SQLException("Workspace does not exist.");
                    }
                }
                if (setupId != null) {
                    try (var existing = connection.prepareStatement("SELECT requested_products,started_at FROM catalog_scan_runs WHERE workspace_id=? AND id=?")) {
                        existing.setObject(1,workspaceId); existing.setObject(2,runId);
                        try (var row=existing.executeQuery()) {
                            if (row.next()) return new QueuedScan(runId,workspaceId,row.getInt(1),row.getTimestamp(2).toInstant());
                        }
                    }
                }
                try (PreparedStatement count = connection.prepareStatement("""
                        SELECT count(*) FROM catalog_scan_jobs
                        WHERE workspace_id = ? AND status IN ('PENDING', 'RUNNING')
                        """)) {
                    count.setObject(1, workspaceId);
                    try (ResultSet row = count.executeQuery()) {
                        row.next();
                        if (row.getInt(1) >= MAX_ACTIVE_JOBS_PER_WORKSPACE) {
                            throw new QueueCapacityException(MAX_ACTIVE_JOBS_PER_WORKSPACE);
                        }
                    }
                }
                try (PreparedStatement run = connection.prepareStatement(runSql);
                 PreparedStatement job = connection.prepareStatement(jobSql);
                 PreparedStatement item = connection.prepareStatement(productSql)) {
                run.setObject(1, runId);
                run.setObject(2, workspaceId);
                run.setString(3, "Kaviyo fetches the selected product pages. This does not prove any assistant crawled, indexed, ranked, or recommended them.");
                run.setInt(4, products.size());
                run.setTimestamp(5, Timestamp.from(queuedAt));
                run.executeUpdate();

                job.setObject(1, runId);
                job.setObject(2, workspaceId);
                job.setTimestamp(3, Timestamp.from(queuedAt));
                job.setTimestamp(4, Timestamp.from(queuedAt));
                job.setTimestamp(5, Timestamp.from(queuedAt));
                job.executeUpdate();

                for (int i = 0; i < products.size(); i++) {
                    item.setObject(1, workspaceId);
                    item.setObject(2, runId);
                    item.setInt(3, i);
                    item.setString(4, json(products.get(i)));
                    item.addBatch();
                }
                item.executeBatch();
                connection.commit();
                return new QueuedScan(runId, workspaceId, products.size(), queuedAt);
                }
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceError("The scan could not be queued.", exception);
        }
    }

    /** Claims one ready job atomically. Stale claims become eligible after a worker crash. */
    public Optional<ClaimedScan> claimNext(Instant now) {
        String claimSql = """
                WITH candidate AS (
                    SELECT scan_run_id
                    FROM catalog_scan_jobs
                    WHERE (status = 'PENDING' AND available_at <= ?)
                       OR (status = 'RUNNING' AND claimed_at < ?)
                    ORDER BY created_at, scan_run_id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                UPDATE catalog_scan_jobs j
                SET status = 'RUNNING', attempts = j.attempts + 1,
                    claimed_at = ?, updated_at = ?
                FROM candidate c
                WHERE j.scan_run_id = c.scan_run_id
                RETURNING j.scan_run_id, j.workspace_id, j.attempts
                """;
        String itemSql = """
                SELECT product_snapshot FROM catalog_scan_job_products
                WHERE workspace_id = ? AND scan_run_id = ? ORDER BY ordinal
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement claim = connection.prepareStatement(claimSql)) {
                claim.setTimestamp(1, Timestamp.from(now));
                claim.setTimestamp(2, Timestamp.from(now.minusSeconds(180)));
                claim.setTimestamp(3, Timestamp.from(now));
                claim.setTimestamp(4, Timestamp.from(now));
                UUID runId;
                UUID workspaceId;
                int attempt;
                try (ResultSet row = claim.executeQuery()) {
                    if (!row.next()) {
                        connection.commit();
                        return Optional.empty();
                    }
                    runId = row.getObject("scan_run_id", UUID.class);
                    workspaceId = row.getObject("workspace_id", UUID.class);
                    attempt = row.getInt("attempts");
                }
                List<CatalogImportRepository.CatalogProductSnapshot> products = new ArrayList<>();
                try (PreparedStatement items = connection.prepareStatement(itemSql)) {
                    items.setObject(1, workspaceId);
                    items.setObject(2, runId);
                    try (ResultSet rows = items.executeQuery()) {
                        while (rows.next()) {
                            products.add(fromJson(rows.getString(1)));
                        }
                    }
                }
                connection.commit();
                return Optional.of(new ClaimedScan(runId, workspaceId, attempt, List.copyOf(products), now));
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceError("A queued scan could not be claimed.", exception);
        }
    }

    public void complete(ClaimedScan scan, Instant completedAt) {
        updateStatus(scan, "COMPLETED", completedAt);
    }

    public void heartbeat(ClaimedScan scan, Instant at) {
        String sql = """
                UPDATE catalog_scan_jobs SET claimed_at = ?, updated_at = ?
                WHERE workspace_id = ? AND scan_run_id = ? AND status = 'RUNNING' AND attempts = ?
                """;
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(at));
            statement.setTimestamp(2, Timestamp.from(at));
            statement.setObject(3, scan.workspaceId());
            statement.setObject(4, scan.runId());
            statement.setInt(5, scan.attempt());
            if (statement.executeUpdate() != 1) {
                throw persistenceError("The scan worker no longer owns this job.", null);
            }
        } catch (SQLException exception) {
            throw persistenceError("The scan worker lease could not be renewed.", exception);
        }
    }

    /** Requeues transient failures with backoff; the last failed attempt marks the run terminal. */
    public void retryOrFail(ClaimedScan scan, Instant now) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement current = connection.prepareStatement("""
                        SELECT attempts FROM catalog_scan_jobs
                        WHERE workspace_id = ? AND scan_run_id = ? AND status = 'RUNNING'
                        FOR UPDATE
                        """)) {
                    current.setObject(1, scan.workspaceId());
                    current.setObject(2, scan.runId());
                    try (ResultSet row = current.executeQuery()) {
                        if (!row.next() || row.getInt("attempts") != scan.attempt()) {
                            connection.commit();
                            return;
                        }
                    }
                }
                if (scan.attempt() < MAX_ATTEMPTS) {
                    try (PreparedStatement delete = connection.prepareStatement("""
                            DELETE FROM catalog_scan_products
                            WHERE workspace_id = ? AND scan_run_id = ?
                            """)) {
                        delete.setObject(1, scan.workspaceId());
                        delete.setObject(2, scan.runId());
                        delete.executeUpdate();
                    }
                    try (PreparedStatement resetRun = connection.prepareStatement("""
                            UPDATE catalog_scan_runs SET status = 'QUEUED', completed_products = 0, completed_at = NULL
                            WHERE workspace_id = ? AND id = ?
                            """)) {
                        resetRun.setObject(1, scan.workspaceId());
                        resetRun.setObject(2, scan.runId());
                        resetRun.executeUpdate();
                    }
                    int delaySeconds = Math.min(60, 2 << scan.attempt());
                    try (PreparedStatement retry = connection.prepareStatement("""
                            UPDATE catalog_scan_jobs
                            SET status = 'PENDING', claimed_at = NULL, available_at = ?, updated_at = ?
                            WHERE workspace_id = ? AND scan_run_id = ?
                            """)) {
                        retry.setTimestamp(1, Timestamp.from(now.plusSeconds(delaySeconds)));
                        retry.setTimestamp(2, Timestamp.from(now));
                        retry.setObject(3, scan.workspaceId());
                        retry.setObject(4, scan.runId());
                        retry.executeUpdate();
                    }
                } else {
                    try (PreparedStatement failRun = connection.prepareStatement("""
                            UPDATE catalog_scan_runs SET status = 'FAILED', completed_at = ?
                            WHERE workspace_id = ? AND id = ? AND status IN ('QUEUED', 'RUNNING')
                            """)) {
                        failRun.setTimestamp(1, Timestamp.from(now));
                        failRun.setObject(2, scan.workspaceId());
                        failRun.setObject(3, scan.runId());
                        failRun.executeUpdate();
                    }
                    try (PreparedStatement failJob = connection.prepareStatement("""
                            UPDATE catalog_scan_jobs SET status = 'FAILED', claimed_at = NULL, updated_at = ?
                            WHERE workspace_id = ? AND scan_run_id = ?
                            """)) {
                        failJob.setTimestamp(1, Timestamp.from(now));
                        failJob.setObject(2, scan.workspaceId());
                        failJob.setObject(3, scan.runId());
                        failJob.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceError("The scan retry state could not be saved.", exception);
        }
    }

    private void updateStatus(ClaimedScan scan, String status, Instant at) {
        String sql = """
                UPDATE catalog_scan_jobs SET status = ?, claimed_at = NULL, updated_at = ?
                WHERE workspace_id = ? AND scan_run_id = ? AND status = 'RUNNING' AND attempts = ?
                """;
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setTimestamp(2, Timestamp.from(at));
            statement.setObject(3, scan.workspaceId());
            statement.setObject(4, scan.runId());
            statement.setInt(5, scan.attempt());
            if (statement.executeUpdate() != 1) {
                throw persistenceError("The claimed scan job is no longer active.", null);
            }
        } catch (SQLException exception) {
            throw persistenceError("The scan job status could not be saved.", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw persistenceError("A scan input could not be encoded.", exception);
        }
    }

    private CatalogImportRepository.CatalogProductSnapshot fromJson(String value) {
        try {
            return objectMapper.readValue(value, CatalogImportRepository.CatalogProductSnapshot.class);
        } catch (IOException exception) {
            throw persistenceError("A queued scan input could not be decoded.", exception);
        }
    }

    private static CatalogImportPersistenceException persistenceError(String message, Throwable cause) {
        return new CatalogImportPersistenceException(message, cause);
    }

    public static final class QueueCapacityException extends RuntimeException {
        public QueueCapacityException(int maximum) {
            super("This workspace already has " + maximum + " active scans. Wait for a scan to finish before starting another.");
        }
    }

    public record QueuedScan(UUID runId, UUID workspaceId, int productCount, Instant queuedAt) { }
    public record ClaimedScan(UUID runId, UUID workspaceId, int attempt,
                              List<CatalogImportRepository.CatalogProductSnapshot> products,
                              Instant claimedAt) { }
}
