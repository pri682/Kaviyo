CREATE TABLE catalog_scan_runs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    status VARCHAR(24) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    observation_type VARCHAR(32) NOT NULL CHECK (observation_type = 'KAVIYO_FETCH'),
    limitation TEXT NOT NULL,
    requested_products INTEGER NOT NULL CHECK (requested_products BETWEEN 1 AND 3),
    completed_products INTEGER NOT NULL DEFAULT 0 CHECK (completed_products >= 0),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    UNIQUE (workspace_id, id),
    CHECK (completed_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX catalog_scan_runs_workspace_time_idx
    ON catalog_scan_runs (workspace_id, started_at DESC);

CREATE TABLE catalog_scan_products (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    product_id UUID NOT NULL,
    external_product_id VARCHAR(180) NOT NULL,
    product_title VARCHAR(500) NOT NULL,
    page_status VARCHAR(40) NOT NULL,
    catalog_snapshot JSONB NOT NULL,
    page_snapshot JSONB NOT NULL,
    audit_report JSONB NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, id),
    UNIQUE (workspace_id, scan_run_id, product_id),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, product_id)
        REFERENCES catalog_products (workspace_id, id)
);

CREATE INDEX catalog_scan_products_run_idx
    ON catalog_scan_products (workspace_id, scan_run_id, external_product_id);

CREATE TABLE catalog_scan_findings (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    scan_product_id UUID NOT NULL,
    product_id UUID NOT NULL,
    rule_code VARCHAR(100) NOT NULL,
    severity VARCHAR(16) NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
    confidence VARCHAR(16) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    title VARCHAR(500) NOT NULL,
    observation TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    evidence JSONB NOT NULL,
    workflow_status VARCHAR(24) NOT NULL DEFAULT 'OPEN'
        CHECK (workflow_status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, id),
    UNIQUE (workspace_id, scan_run_id, product_id, rule_code),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, scan_product_id)
        REFERENCES catalog_scan_products (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, product_id)
        REFERENCES catalog_products (workspace_id, id)
);

CREATE INDEX catalog_scan_findings_run_idx
    ON catalog_scan_findings (workspace_id, scan_run_id, severity, created_at DESC);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    scan_run_id UUID NOT NULL,
    finding_id UUID,
    notification_type VARCHAR(32) NOT NULL
        CHECK (notification_type IN ('AUDIT_COMPLETED', 'HIGH_PRIORITY_FINDING')),
    dedupe_key VARCHAR(300) NOT NULL,
    title VARCHAR(240) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    UNIQUE (workspace_id, dedupe_key),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, finding_id)
        REFERENCES catalog_scan_findings (workspace_id, id)
);

CREATE INDEX notifications_workspace_unread_idx
    ON notifications (workspace_id, created_at DESC)
    WHERE read_at IS NULL;
