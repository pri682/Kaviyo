CREATE TABLE workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO workspaces (id, name)
VALUES ('00000000-0000-4000-8000-000000000001', 'Kaviyo Local Demo')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE catalog_imports (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    source_type VARCHAR(32) NOT NULL CHECK (source_type = 'CSV'),
    content_sha256 CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('CONFIRMED')),
    product_count INTEGER NOT NULL CHECK (product_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, id)
);

CREATE INDEX catalog_imports_workspace_time_idx ON catalog_imports (workspace_id, confirmed_at DESC);

CREATE TABLE catalog_products (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    external_product_id VARCHAR(180) NOT NULL,
    title VARCHAR(500) NOT NULL,
    sku VARCHAR(180),
    brand VARCHAR(180),
    category VARCHAR(180),
    description TEXT,
    price NUMERIC(15, 4),
    currency CHAR(3),
    availability VARCHAR(24) CHECK (
        availability IS NULL OR availability IN ('IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN')
    ),
    product_url TEXT NOT NULL,
    last_import_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, external_product_id),
    UNIQUE (workspace_id, id),
    FOREIGN KEY (workspace_id, last_import_id) REFERENCES catalog_imports (workspace_id, id),
    CHECK (price IS NULL OR price >= 0)
);

CREATE INDEX catalog_products_workspace_idx ON catalog_products (workspace_id);

CREATE TABLE catalog_product_attributes (
    product_id UUID NOT NULL REFERENCES catalog_products (id) ON DELETE CASCADE,
    attribute_key VARCHAR(160) NOT NULL,
    attribute_value TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (product_id, attribute_key)
);

CREATE TABLE catalog_fact_observations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    product_id UUID NOT NULL,
    import_id UUID NOT NULL,
    field_key VARCHAR(180) NOT NULL,
    value_text TEXT NOT NULL,
    source_type VARCHAR(32) NOT NULL CHECK (source_type = 'MERCHANT_CSV'),
    source_reference VARCHAR(500) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (workspace_id, product_id) REFERENCES catalog_products (workspace_id, id),
    FOREIGN KEY (workspace_id, import_id) REFERENCES catalog_imports (workspace_id, id)
);

CREATE INDEX catalog_fact_observations_product_time_idx
    ON catalog_fact_observations (workspace_id, product_id, observed_at DESC);

CREATE INDEX catalog_fact_observations_import_idx
    ON catalog_fact_observations (import_id);
