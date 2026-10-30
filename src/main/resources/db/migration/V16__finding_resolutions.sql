CREATE TABLE finding_resolutions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    product_id UUID NOT NULL,
    source_finding_id UUID,
    finding_code VARCHAR(80) NOT NULL,
    resolution VARCHAR(24) NOT NULL
        CHECK (resolution IN ('ACCEPT_CATALOG', 'ACCEPT_PAGE', 'DISMISSED')),
    note VARCHAR(2000),
    decided_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, id),
    UNIQUE (workspace_id, scan_run_id, product_id, finding_code),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, product_id)
        REFERENCES catalog_products (workspace_id, id),
    FOREIGN KEY (workspace_id, source_finding_id)
        REFERENCES catalog_scan_findings (workspace_id, id)
);

CREATE INDEX finding_resolutions_workspace_time_idx
    ON finding_resolutions (workspace_id, decided_at DESC);
