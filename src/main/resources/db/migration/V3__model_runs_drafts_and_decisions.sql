CREATE TABLE model_runs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    scan_run_id UUID,
    product_id UUID,
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('META_MODEL_API', 'BASETEN')),
    model_id VARCHAR(160) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    prompt_version VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL
        CHECK (status IN ('COMPLETED', 'REJECTED', 'FAILED', 'UNAVAILABLE')),
    input_evidence_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    tool_names JSONB NOT NULL DEFAULT '[]'::jsonb,
    output_json JSONB,
    validation_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    UNIQUE (workspace_id, id),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id),
    FOREIGN KEY (workspace_id, product_id)
        REFERENCES catalog_products (workspace_id, id),
    CHECK (completed_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX model_runs_workspace_time_idx
    ON model_runs (workspace_id, started_at DESC);

CREATE TABLE catalog_drafts (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    product_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    source_finding_id UUID,
    current_revision INTEGER NOT NULL DEFAULT 1 CHECK (current_revision > 0),
    status VARCHAR(24) NOT NULL DEFAULT 'NEEDS_REVIEW'
        CHECK (status IN ('NEEDS_REVIEW', 'APPROVED', 'REJECTED', 'EXPORTED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    decision_note VARCHAR(1000),
    UNIQUE (workspace_id, id),
    FOREIGN KEY (workspace_id, product_id)
        REFERENCES catalog_products (workspace_id, id),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id),
    FOREIGN KEY (workspace_id, source_finding_id)
        REFERENCES catalog_scan_findings (workspace_id, id)
);

CREATE INDEX catalog_drafts_workspace_time_idx
    ON catalog_drafts (workspace_id, updated_at DESC);

CREATE TABLE catalog_draft_revisions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    draft_id UUID NOT NULL,
    revision_number INTEGER NOT NULL CHECK (revision_number > 0),
    field_key VARCHAR(120) NOT NULL CHECK (field_key IN ('description')),
    original_value TEXT,
    proposed_value TEXT NOT NULL CHECK (length(proposed_value) BETWEEN 1 AND 4000),
    rationale TEXT NOT NULL,
    source_model_run_id UUID NOT NULL,
    evidence_references JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, draft_id, revision_number),
    UNIQUE (workspace_id, id),
    FOREIGN KEY (workspace_id, draft_id)
        REFERENCES catalog_drafts (workspace_id, id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id, source_model_run_id)
        REFERENCES model_runs (workspace_id, id)
);

CREATE TABLE draft_decisions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    draft_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    decision VARCHAR(24) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    actor_label VARCHAR(120) NOT NULL,
    note VARCHAR(1000),
    decided_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (workspace_id, draft_id, revision_number)
        REFERENCES catalog_draft_revisions (workspace_id, draft_id, revision_number),
    FOREIGN KEY (workspace_id, draft_id)
        REFERENCES catalog_drafts (workspace_id, id)
);

CREATE TABLE draft_export_packets (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    draft_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    format VARCHAR(24) NOT NULL CHECK (format IN ('JSON_CHANGE_PACKET')),
    payload JSONB NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (workspace_id, draft_id, revision_number)
        REFERENCES catalog_draft_revisions (workspace_id, draft_id, revision_number),
    FOREIGN KEY (workspace_id, draft_id)
        REFERENCES catalog_drafts (workspace_id, id)
);
