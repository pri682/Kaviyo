package com.kaviyo.backend.catalog;

import com.kaviyo.catalog.CatalogFactObservation;
import com.kaviyo.catalog.CatalogProductRecord;
import com.kaviyo.catalog.ConfirmedCatalogImport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CatalogImportRepository {
    @Inject
    DataSource dataSource;

    public CatalogImportConfirmation confirm(ConfirmedCatalogImport batch) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertImport(connection, batch);
                for (CatalogProductRecord product : batch.products()) {
                    UUID productId = upsertProduct(connection, batch, product);
                    replaceCurrentAttributes(connection, productId, product.attributes(), batch.confirmedAt());
                    insertFactObservations(connection, batch, productId, product.observations());
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException(
                    "Catalog import could not be saved. No products were confirmed.", exception);
        }

        return new CatalogImportConfirmation(
                "SYNTHETIC_DEMO", batch.workspaceId(), batch.importId(), batch.products().size(),
                batch.confirmedAt(),
                "Catalog facts were saved to the local demo workspace. Product URLs were not fetched.");
    }

    public List<CatalogProductSnapshot> listCurrentProducts(UUID workspaceId) {
        String sql = """
                SELECT p.id, p.external_product_id, p.title, p.sku, p.brand, p.category,
                       p.description, p.price, p.currency, p.availability, p.product_url,
                       p.last_import_id, p.updated_at, a.attribute_key, a.attribute_value
                FROM catalog_products p
                LEFT JOIN catalog_product_attributes a ON a.product_id = p.id
                WHERE p.workspace_id = ?
                ORDER BY p.external_product_id, a.attribute_key
                """;
        Map<UUID, ProductBuilder> products = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    UUID productId = rows.getObject("id", UUID.class);
                    ProductBuilder product = products.get(productId);
                    if (product == null) {
                        product = new ProductBuilder(
                                productId,
                                rows.getString("external_product_id"),
                                rows.getString("title"),
                                rows.getString("sku"),
                                rows.getString("brand"),
                                rows.getString("category"),
                                rows.getString("description"),
                                rows.getBigDecimal("price"),
                                rows.getString("currency"),
                                rows.getString("availability"),
                                rows.getString("product_url"),
                                rows.getObject("last_import_id", UUID.class),
                                rows.getTimestamp("updated_at").toInstant());
                        products.put(productId, product);
                    }
                    String key = rows.getString("attribute_key");
                    if (key != null) {
                        product.attributes.put(key, rows.getString("attribute_value"));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Catalog products could not be loaded.", exception);
        }
        return products.values().stream().map(ProductBuilder::toSnapshot).toList();
    }

    public CatalogProductPage listCurrentProducts(UUID workspaceId, int limit, int offset) {
        String sql = """
                WITH product_page AS (
                    SELECT p.id, p.external_product_id, p.title, p.sku, p.brand, p.category,
                           p.description, p.price, p.currency, p.availability, p.product_url,
                           p.last_import_id, p.updated_at
                    FROM catalog_products p
                    WHERE p.workspace_id = ?
                    ORDER BY p.external_product_id
                    LIMIT ? OFFSET ?
                )
                SELECT p.id, p.external_product_id, p.title, p.sku, p.brand, p.category,
                       p.description, p.price, p.currency, p.availability, p.product_url,
                       p.last_import_id, p.updated_at, a.attribute_key, a.attribute_value
                FROM product_page p
                LEFT JOIN catalog_product_attributes a ON a.product_id = p.id
                ORDER BY p.external_product_id, a.attribute_key
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            statement.setInt(2, limit + 1);
            statement.setInt(3, offset);
            List<CatalogProductSnapshot> products = readProductRows(statement);
            boolean hasMore = products.size() > limit;
            return new CatalogProductPage(hasMore ? products.subList(0, limit) : products, hasMore);
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Catalog products could not be loaded.", exception);
        }
    }

    public List<CatalogProductSnapshot> findCurrentProducts(UUID workspaceId, List<String> externalProductIds) {
        if (externalProductIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(externalProductIds.size(), "?"));
        String sql = """
                SELECT p.id, p.external_product_id, p.title, p.sku, p.brand, p.category,
                       p.description, p.price, p.currency, p.availability, p.product_url,
                       p.last_import_id, p.updated_at, a.attribute_key, a.attribute_value
                FROM catalog_products p
                LEFT JOIN catalog_product_attributes a ON a.product_id = p.id
                WHERE p.workspace_id = ? AND p.external_product_id IN (%s)
                ORDER BY p.external_product_id, a.attribute_key
                """.formatted(placeholders);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, workspaceId);
            for (int index = 0; index < externalProductIds.size(); index++) {
                statement.setString(index + 2, externalProductIds.get(index));
            }
            return readProductRows(statement);
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Selected catalog products could not be loaded.", exception);
        }
    }

    private static List<CatalogProductSnapshot> readProductRows(PreparedStatement statement) throws SQLException {
        Map<UUID, ProductBuilder> products = new LinkedHashMap<>();
        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                UUID productId = rows.getObject("id", UUID.class);
                ProductBuilder product = products.get(productId);
                if (product == null) {
                    product = new ProductBuilder(productId, rows.getString("external_product_id"),
                            rows.getString("title"), rows.getString("sku"), rows.getString("brand"),
                            rows.getString("category"), rows.getString("description"), rows.getBigDecimal("price"),
                            rows.getString("currency"), rows.getString("availability"),
                            rows.getString("product_url"), rows.getObject("last_import_id", UUID.class),
                            rows.getTimestamp("updated_at").toInstant());
                    products.put(productId, product);
                }
                String key = rows.getString("attribute_key");
                if (key != null) product.attributes.put(key, rows.getString("attribute_value"));
            }
        }
        return products.values().stream().map(ProductBuilder::toSnapshot).toList();
    }

    private static void insertImport(Connection connection, ConfirmedCatalogImport batch) throws SQLException {
        String sql = """
                INSERT INTO catalog_imports (
                    id, workspace_id, source_type, content_sha256, status,
                    product_count, created_at, confirmed_at
                ) VALUES (?, ?, 'CSV', ?, 'CONFIRMED', ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, batch.importId());
            statement.setObject(2, batch.workspaceId());
            statement.setString(3, batch.contentSha256());
            statement.setInt(4, batch.products().size());
            statement.setTimestamp(5, Timestamp.from(batch.confirmedAt()));
            statement.setTimestamp(6, Timestamp.from(batch.confirmedAt()));
            statement.executeUpdate();
        }
    }

    private static UUID upsertProduct(
            Connection connection,
            ConfirmedCatalogImport batch,
            CatalogProductRecord product) throws SQLException {
        String sql = """
                INSERT INTO catalog_products (
                    id, workspace_id, external_product_id, title, sku, brand, category,
                    description, price, currency, availability, product_url, last_import_id,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (workspace_id, external_product_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    sku = EXCLUDED.sku,
                    brand = EXCLUDED.brand,
                    category = EXCLUDED.category,
                    description = EXCLUDED.description,
                    price = EXCLUDED.price,
                    currency = EXCLUDED.currency,
                    availability = EXCLUDED.availability,
                    product_url = EXCLUDED.product_url,
                    last_import_id = EXCLUDED.last_import_id,
                    updated_at = EXCLUDED.updated_at
                RETURNING id
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, batch.workspaceId());
            statement.setString(3, product.externalProductId());
            statement.setString(4, product.title());
            statement.setString(5, product.sku());
            statement.setString(6, product.brand());
            statement.setString(7, product.category());
            statement.setString(8, product.description());
            if (product.price() == null) {
                statement.setNull(9, Types.NUMERIC);
            } else {
                statement.setBigDecimal(9, product.price());
            }
            statement.setString(10, product.currency());
            statement.setString(11, product.availability());
            statement.setString(12, product.productUrl() == null ? null : product.productUrl().toString());
            statement.setObject(13, batch.importId());
            statement.setTimestamp(14, Timestamp.from(batch.confirmedAt()));
            statement.setTimestamp(15, Timestamp.from(batch.confirmedAt()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Database did not return the product identifier.");
                }
                return resultSet.getObject(1, UUID.class);
            }
        }
    }

    private static void replaceCurrentAttributes(
            Connection connection,
            UUID productId,
            Map<String, String> attributes,
            Instant confirmedAt) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM catalog_product_attributes WHERE product_id = ?")) {
            delete.setObject(1, productId);
            delete.executeUpdate();
        }
        if (attributes.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO catalog_product_attributes (product_id, attribute_key, attribute_value, updated_at)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                insert.setObject(1, productId);
                insert.setString(2, attribute.getKey());
                insert.setString(3, attribute.getValue());
                insert.setTimestamp(4, Timestamp.from(confirmedAt));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static void insertFactObservations(
            Connection connection,
            ConfirmedCatalogImport batch,
            UUID productId,
            List<CatalogFactObservation> facts) throws SQLException {
        String sql = """
                INSERT INTO catalog_fact_observations (
                    id, workspace_id, product_id, import_id, field_key, value_text,
                    source_type, source_reference, observed_at, confirmed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (CatalogFactObservation fact : facts) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, batch.workspaceId());
                statement.setObject(3, productId);
                statement.setObject(4, batch.importId());
                statement.setString(5, fact.fieldKey());
                statement.setString(6, fact.value());
                statement.setString(7, fact.sourceType());
                statement.setString(8, fact.sourceReference());
                statement.setTimestamp(9, Timestamp.from(fact.observedAt()));
                statement.setTimestamp(10, Timestamp.from(fact.confirmedAt()));
                statement.addBatch();
            }
            if (!facts.isEmpty()) {
                statement.executeBatch();
            }
        }
    }

    public record CatalogProductSnapshot(
            UUID id,
            String productId,
            String title,
            String sku,
            String brand,
            String category,
            String description,
            java.math.BigDecimal price,
            String currency,
            String availability,
            String productUrl,
            UUID lastImportId,
            Instant updatedAt,
            Map<String, String> attributes) {
    }

    public record CatalogProductPage(List<CatalogProductSnapshot> products, boolean hasMore) {
        public CatalogProductPage { products = List.copyOf(products); }
    }

    private static final class ProductBuilder {
        private final UUID id;
        private final String productId;
        private final String title;
        private final String sku;
        private final String brand;
        private final String category;
        private final String description;
        private final java.math.BigDecimal price;
        private final String currency;
        private final String availability;
        private final String productUrl;
        private final UUID lastImportId;
        private final Instant updatedAt;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        private ProductBuilder(
                UUID id, String productId, String title, String sku, String brand, String category,
                String description, java.math.BigDecimal price, String currency, String availability,
                String productUrl, UUID lastImportId, Instant updatedAt) {
            this.id = id;
            this.productId = productId;
            this.title = title;
            this.sku = sku;
            this.brand = brand;
            this.category = category;
            this.description = description;
            this.price = price;
            this.currency = currency;
            this.availability = availability;
            this.productUrl = productUrl;
            this.lastImportId = lastImportId;
            this.updatedAt = updatedAt;
        }

        private CatalogProductSnapshot toSnapshot() {
            return new CatalogProductSnapshot(
                    id, productId, title, sku, brand, category, description, price,
                    currency, availability, productUrl, lastImportId, updatedAt, Map.copyOf(attributes));
        }
    }
}
