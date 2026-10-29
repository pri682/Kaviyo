CREATE TABLE discovery_simulations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    scan_run_id UUID NOT NULL,
    target_product_id UUID NOT NULL,
    draft_id UUID NOT NULL,
    shopper_question VARCHAR(1000) NOT NULL,
    simulation_label VARCHAR(48) NOT NULL DEFAULT 'CONTROLLED_MODEL_SIMULATION'
        CHECK (simulation_label = 'CONTROLLED_MODEL_SIMULATION'),
    baseline_model_run_id UUID NOT NULL,
    draft_model_run_id UUID NOT NULL,
    baseline_selected_product_id VARCHAR(180) NOT NULL,
    draft_selected_product_id VARCHAR(180) NOT NULL,
    outcome VARCHAR(48) NOT NULL CHECK (outcome IN (
        'TARGET_SELECTED_BOTH',
        'TARGET_SELECTED_AFTER_DRAFT',
        'TARGET_SELECTED_BEFORE_ONLY',
        'TARGET_NOT_SELECTED_EITHER'
    )),
    comparison JSONB NOT NULL,
    limitation TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (workspace_id, id),
    FOREIGN KEY (workspace_id, scan_run_id)
        REFERENCES catalog_scan_runs (workspace_id, id),
    FOREIGN KEY (workspace_id, target_product_id)
        REFERENCES catalog_products (workspace_id, id),
    FOREIGN KEY (workspace_id, draft_id)
        REFERENCES catalog_drafts (workspace_id, id),
    FOREIGN KEY (workspace_id, baseline_model_run_id)
        REFERENCES model_runs (workspace_id, id),
    FOREIGN KEY (workspace_id, draft_model_run_id)
        REFERENCES model_runs (workspace_id, id)
);

CREATE INDEX discovery_simulations_workspace_time_idx
    ON discovery_simulations (workspace_id, created_at DESC);
