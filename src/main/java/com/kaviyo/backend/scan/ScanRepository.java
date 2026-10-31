package com.kaviyo.backend.scan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaviyo.audit.AuditIssue;
import com.kaviyo.audit.AuditReport;
import com.kaviyo.audit.Evidence;
import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import com.kaviyo.backend.catalog.CatalogImportRepository;
import com.kaviyo.crawler.PageScanSnapshot;
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

@ApplicationScoped
public class ScanRepository {
    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public StartedScan activateQueued(UUID workspaceId, UUID scanRunId, int attempt) {
        String sql = """
                UPDATE catalog_scan_runs r SET status = 'RUNNING'
                FROM catalog_scan_jobs j
                WHERE r.workspace_id = ? AND r.id = ? AND r.status IN ('QUEUED', 'RUNNING')
                  AND j.workspace_id = r.workspace_id AND j.scan_run_id = r.id
                  AND j.status = 'RUNNING' AND j.attempts = ?
                RETURNING r.requested_products, r.started_at
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, scanRunId);
            statement.setInt(3, attempt);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) {
                    throw persistenceError("The queued scan run is missing or already complete.", null);
                }
                return new StartedScan(scanRunId, workspaceId, row.getInt("requested_products"),
                        row.getTimestamp("started_at").toInstant(), attempt);
            }
        } catch (SQLException exception) {
            throw persistenceError("The queued scan could not be started.", exception);
        }
    }

    public StartedScan begin(UUID workspaceId, int requestedProducts, Instant startedAt) {
        UUID id = UUID.randomUUID();
        String sql = """
                INSERT INTO catalog_scan_runs (
                    id, workspace_id, status, observation_type, limitation,
                    requested_products, started_at
                ) VALUES (?, ?, 'RUNNING', 'KAVIYO_FETCH', ?, ?, ?)
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setObject(2, workspaceId);
            statement.setString(3, ScanRunDetailMessages.LIMITATION);
            statement.setInt(4, requestedProducts);
            statement.setTimestamp(5, Timestamp.from(startedAt));
            statement.executeUpdate();
            return new StartedScan(id, workspaceId, requestedProducts, startedAt, 0);
        } catch (SQLException exception) {
            throw persistenceError("A scan run could not be created.", exception);
        }
    }

    public void recordProduct(
            StartedScan scan,
            CatalogImportRepository.CatalogProductSnapshot product,
            PageScanSnapshot page,
            AuditReport report) {
        UUID scanProductId = UUID.randomUUID();
        String productSql = """
                INSERT INTO catalog_scan_products (
                    id, workspace_id, scan_run_id, product_id, external_product_id,
                    product_title, page_status, catalog_snapshot, page_snapshot, audit_report, observed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?)
                """;
        String findingSql = """
                INSERT INTO catalog_scan_findings (
                    id, workspace_id, scan_run_id, scan_product_id, product_id,
                    rule_code, severity, confidence, title, observation, recommended_action,
                    evidence, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement productStatement = connection.prepareStatement(productSql);
                 PreparedStatement findingStatement = connection.prepareStatement(findingSql)) {
                assertCurrentAttempt(connection, scan);
                productStatement.setObject(1, scanProductId);
                productStatement.setObject(2, scan.workspaceId());
                productStatement.setObject(3, scan.id());
                productStatement.setObject(4, product.id());
                productStatement.setString(5, product.productId());
                productStatement.setString(6, product.title());
                productStatement.setString(7, page.status().name());
                productStatement.setString(8, json(product));
                productStatement.setString(9, json(page));
                productStatement.setString(10, json(report));
                productStatement.setTimestamp(11, Timestamp.from(page.observedAt()));
                productStatement.executeUpdate();

                for (AuditIssue issue : report.issues()) {
                    findingStatement.setObject(1, UUID.randomUUID());
                    findingStatement.setObject(2, scan.workspaceId());
                    findingStatement.setObject(3, scan.id());
                    findingStatement.setObject(4, scanProductId);
                    findingStatement.setObject(5, product.id());
                    findingStatement.setString(6, issue.code());
                    findingStatement.setString(7, issue.severity().name());
                    findingStatement.setString(8, issue.confidence().name());
                    findingStatement.setString(9, issue.title());
                    findingStatement.setString(10, issue.observation());
                    findingStatement.setString(11, issue.recommendedAction());
                    findingStatement.setString(12, json(issue.evidence()));
                    findingStatement.setTimestamp(13, Timestamp.from(page.observedAt()));
                    findingStatement.addBatch();
                }
                findingStatement.executeBatch();
                try (PreparedStatement progress = connection.prepareStatement("""
                        UPDATE catalog_scan_runs
                        SET completed_products = (
                            SELECT COUNT(*) FROM catalog_scan_products
                            WHERE workspace_id = ? AND scan_run_id = ?
                        )
                        WHERE workspace_id = ? AND id = ? AND status = 'RUNNING'
                        """)) {
                    progress.setObject(1, scan.workspaceId());
                    progress.setObject(2, scan.id());
                    progress.setObject(3, scan.workspaceId());
                    progress.setObject(4, scan.id());
                    progress.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw persistenceError("A scan result could not be saved.", exception);
        }
    }

    public ScanRunDetail finish(StartedScan scan, ScanRunStatus status, Instant completedAt) {
        if (status == ScanRunStatus.RUNNING) {
            throw new IllegalArgumentException("A completed scan cannot remain RUNNING.");
        }
        String update = """
                UPDATE catalog_scan_runs
                SET status = ?,
                    completed_products = (
                        SELECT COUNT(*) FROM catalog_scan_products
                        WHERE workspace_id = ? AND scan_run_id = ?
                    ),
                    completed_at = ?
                WHERE workspace_id = ? AND id = ? AND status = 'RUNNING'
                """;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                assertCurrentAttempt(connection, scan);
                statement.setString(1, status.name());
                statement.setObject(2, scan.workspaceId());
                statement.setObject(3, scan.id());
                statement.setTimestamp(4, Timestamp.from(completedAt));
                statement.setObject(5, scan.workspaceId());
                statement.setObject(6, scan.id());
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("The scan run is missing or has already completed.");
                }
            }

            String completionMessage = status == ScanRunStatus.COMPLETED
                    ? "The product page check finished. Review its saved evidence and findings."
                    : "The product page check finished with items that need another look.";
            insertNotification(connection, scan, null, "AUDIT_COMPLETED",
                    "Product check complete", completionMessage, completedAt,
                    "scan:" + scan.id() + ":completed");
            insertHighPriorityNotifications(connection, scan, completedAt);
            connection.commit();
        } catch (SQLException exception) {
            throw persistenceError("The scan run could not be completed.", exception);
        }
        return get(scan.workspaceId(), scan.id())
                .orElseThrow(() -> persistenceError("The completed scan could not be reloaded.", null));
    }

    public void fail(StartedScan scan, Instant completedAt) {
        String sql = """
                UPDATE catalog_scan_runs
                SET status = 'FAILED', completed_at = ?,
                    completed_products = (
                        SELECT COUNT(*) FROM catalog_scan_products
                        WHERE workspace_id = ? AND scan_run_id = ?
                    )
                WHERE workspace_id = ? AND id = ? AND status = 'RUNNING'
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(completedAt));
            statement.setObject(2, scan.workspaceId());
            statement.setObject(3, scan.id());
            statement.setObject(4, scan.workspaceId());
            statement.setObject(5, scan.id());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceError("The failed scan status could not be saved.", exception);
        }
    }

    public Optional<ScanRunDetail> get(UUID workspaceId, UUID scanRunId) {
        String runSql = """
                SELECT status, observation_type, limitation, requested_products,
                       completed_products, started_at, completed_at
                FROM catalog_scan_runs WHERE workspace_id = ? AND id = ?
                """;
        String productSql = """
                SELECT external_product_id, product_title, page_snapshot, audit_report
                FROM catalog_scan_products
                WHERE workspace_id = ? AND scan_run_id = ?
                ORDER BY external_product_id
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement runStatement = connection.prepareStatement(runSql)) {
            runStatement.setObject(1, workspaceId);
            runStatement.setObject(2, scanRunId);
            try (ResultSet run = runStatement.executeQuery()) {
                if (!run.next()) {
                    return Optional.empty();
                }
                String status = run.getString("status");
                String observationType = run.getString("observation_type");
                String limitation = run.getString("limitation");
                int requested = run.getInt("requested_products");
                int completed = run.getInt("completed_products");
                Instant startedAt = run.getTimestamp("started_at").toInstant();
                Timestamp completedTimestamp = run.getTimestamp("completed_at");
                Instant completedAt = completedTimestamp == null ? null : completedTimestamp.toInstant();

                List<ScanRunDetail.ProductObservation> products = new ArrayList<>();
                try (PreparedStatement productsStatement = connection.prepareStatement(productSql)) {
                    productsStatement.setObject(1, workspaceId);
                    productsStatement.setObject(2, scanRunId);
                    try (ResultSet rows = productsStatement.executeQuery()) {
                        while (rows.next()) {
                            PageScanSnapshot page = fromJson(rows.getString("page_snapshot"), PageScanSnapshot.class);
                            AuditReport report = fromJson(rows.getString("audit_report"), AuditReport.class);
                            products.add(new ScanRunDetail.ProductObservation(
                                    rows.getString("external_product_id"),
                                    rows.getString("product_title"), page, report));
                        }
                    }
                }
                List<AuditIssue> issues = products.stream()
                        .flatMap(product -> product.report().issues().stream())
                        .toList();
                Instant generatedAt = completedAt == null ? startedAt : completedAt;
                AuditReport report = new AuditReport(generatedAt, products.size(), issues);
                return Optional.of(new ScanRunDetail(scanRunId, workspaceId, status,
                        observationType, limitation, requested, completed, startedAt, completedAt,
                        products, report));
            }
        } catch (SQLException exception) {
            throw persistenceError("The scan report could not be loaded.", exception);
        }
    }

    private static void assertCurrentAttempt(Connection connection, StartedScan scan) throws SQLException {
        if (scan.attempt() == 0) return;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT attempts FROM catalog_scan_jobs
                WHERE workspace_id = ? AND scan_run_id = ? AND status = 'RUNNING'
                FOR UPDATE
                """)) {
            statement.setObject(1, scan.workspaceId());
            statement.setObject(2, scan.id());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || row.getInt("attempts") != scan.attempt()) {
                    throw persistenceError("This worker no longer owns the scan lease.", null);
                }
            }
        }
    }

    public Optional<CatalogImportRepository.CatalogProductSnapshot> capturedProduct(
            UUID workspaceId, UUID scanRunId, String externalProductId) {
        String sql = """
                SELECT catalog_snapshot FROM catalog_scan_products
                WHERE workspace_id = ? AND scan_run_id = ? AND external_product_id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, scanRunId);
            statement.setString(3, externalProductId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                return Optional.of(fromJson(rows.getString("catalog_snapshot"),
                        CatalogImportRepository.CatalogProductSnapshot.class));
            }
        } catch (SQLException exception) {
            throw persistenceError("The captured catalog facts could not be loaded.", exception);
        }
    }

    public Optional<UUID> findingId(UUID workspaceId, UUID scanRunId, UUID internalProductId, String ruleCode) {
        String sql = """
                SELECT id FROM catalog_scan_findings
                WHERE workspace_id = ? AND scan_run_id = ? AND product_id = ? AND rule_code = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setObject(2, scanRunId);
            statement.setObject(3, internalProductId);
            statement.setString(4, ruleCode);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getObject("id", UUID.class)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw persistenceError("The source finding could not be loaded.", exception);
        }
    }

    public List<NotificationRecord> listNotifications(UUID workspaceId, boolean unreadOnly) {
        String sql = """
                SELECT id, scan_run_id, finding_id, notification_type, title, message, created_at, read_at
                FROM notifications
                WHERE workspace_id = ? AND (? = false OR read_at IS NULL)
                ORDER BY created_at DESC, id
                LIMIT 100
                """;
        List<NotificationRecord> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setBoolean(2, unreadOnly);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Timestamp readAt = rows.getTimestamp("read_at");
                    result.add(new NotificationRecord(
                            rows.getObject("id", UUID.class),
                            rows.getObject("scan_run_id", UUID.class),
                            rows.getObject("finding_id", UUID.class),
                            rows.getString("notification_type"),
                            rows.getString("title"),
                            rows.getString("message"),
                            rows.getTimestamp("created_at").toInstant(),
                            readAt == null ? null : readAt.toInstant()));
                }
            }
            return List.copyOf(result);
        } catch (SQLException exception) {
            throw persistenceError("Notifications could not be loaded.", exception);
        }
    }

    public boolean markNotificationRead(UUID workspaceId, UUID notificationId, Instant readAt) {
        String sql = """
                UPDATE notifications SET read_at = COALESCE(read_at, ?)
                WHERE workspace_id = ? AND id = ?
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(readAt));
            statement.setObject(2, workspaceId);
            statement.setObject(3, notificationId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw persistenceError("The notification state could not be updated.", exception);
        }
    }

    private static void insertHighPriorityNotifications(
            Connection connection, StartedScan scan, Instant createdAt) throws SQLException {
        String select = """
                SELECT id, title, observation FROM catalog_scan_findings
                WHERE workspace_id = ? AND scan_run_id = ? AND severity = 'HIGH'
                ORDER BY created_at, id
                """;
        try (PreparedStatement query = connection.prepareStatement(select)) {
            query.setObject(1, scan.workspaceId());
            query.setObject(2, scan.id());
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    UUID findingId = rows.getObject("id", UUID.class);
                    insertNotification(connection, scan, findingId, "HIGH_PRIORITY_FINDING",
                            rows.getString("title"), rows.getString("observation"), createdAt,
                            "scan:" + scan.id() + ":finding:" + findingId);
                }
            }
        }
    }

    private static void insertNotification(
            Connection connection,
            StartedScan scan,
            UUID findingId,
            String type,
            String title,
            String message,
            Instant createdAt,
            String dedupeKey) throws SQLException {
        String sql = """
                INSERT INTO notifications (
                    id, workspace_id, scan_run_id, finding_id, notification_type,
                    dedupe_key, title, message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (workspace_id, dedupe_key) DO NOTHING
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, scan.workspaceId());
            statement.setObject(3, scan.id());
            statement.setObject(4, findingId);
            statement.setString(5, type);
            statement.setString(6, dedupeKey);
            statement.setString(7, title);
            statement.setString(8, message);
            statement.setTimestamp(9, Timestamp.from(createdAt));
            statement.executeUpdate();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw persistenceError("An evidence record could not be encoded.", exception);
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (IOException exception) {
            throw persistenceError("A saved evidence record could not be decoded.", exception);
        }
    }

    private static CatalogImportPersistenceException persistenceError(String message, Throwable cause) {
        return new CatalogImportPersistenceException(message, cause);
    }

    public record StartedScan(UUID id, UUID workspaceId, int requestedProducts, Instant startedAt, int attempt) {
    }

    private static final class ScanRunDetailMessages {
        private static final String LIMITATION = "Kaviyo fetched selected product URLs at this time. This does not prove Google or any assistant crawled, indexed, ranked, or recommended a product.";
    }
}
