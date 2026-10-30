ALTER TABLE catalog_scan_runs
    DROP CONSTRAINT catalog_scan_runs_status_check,
    DROP CONSTRAINT catalog_scan_runs_requested_products_check;

ALTER TABLE catalog_scan_runs
    ADD CONSTRAINT catalog_scan_runs_status_check
        CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    ADD CONSTRAINT catalog_scan_runs_requested_products_check
        CHECK (requested_products BETWEEN 1 AND 100);

CREATE TABLE catalog_scan_jobs (
    scan_run_id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    available_at TIMESTAMPTZ NOT NULL,
    claimed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, scan_run_id),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id) ON DELETE CASCADE
);

CREATE TABLE catalog_scan_job_products (
    workspace_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    ordinal INTEGER NOT NULL CHECK (ordinal >= 0),
    product_snapshot JSONB NOT NULL,
    PRIMARY KEY (workspace_id, scan_run_id, ordinal),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_jobs (workspace_id, scan_run_id) ON DELETE CASCADE
);

CREATE INDEX catalog_scan_jobs_claim_idx
    ON catalog_scan_jobs (available_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX catalog_scan_jobs_stale_claim_idx
    ON catalog_scan_jobs (claimed_at)
    WHERE status = 'RUNNING';
