CREATE TABLE discovery_missions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces (id),
    mission_text VARCHAR(1000) NOT NULL,
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('MERCHANT_AUTHORED', 'SYNTHETIC')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, id)
);

CREATE INDEX discovery_missions_workspace_idx ON discovery_missions (workspace_id, created_at DESC);

CREATE TABLE discovery_observations (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    mission_id UUID NOT NULL,
    provider VARCHAR(80) NOT NULL,
    collection_method VARCHAR(32) NOT NULL CHECK (
        collection_method IN ('MANUAL', 'MODEL_SIMULATION', 'LIVE_PROVIDER')
    ),
    status VARCHAR(24) NOT NULL CHECK (status IN ('OBSERVED', 'SIMULATED', 'UNAVAILABLE')),
    prompt_text VARCHAR(2000) NOT NULL,
    answer_text TEXT,
    source_url TEXT,
    observed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (workspace_id, mission_id) REFERENCES discovery_missions (workspace_id, id),
    CHECK (
      (collection_method = 'MANUAL' AND status = 'OBSERVED' AND answer_text IS NOT NULL AND observed_at IS NOT NULL)
      OR (collection_method = 'LIVE_PROVIDER' AND status = 'OBSERVED' AND answer_text IS NOT NULL AND observed_at IS NOT NULL)
      OR (collection_method = 'MODEL_SIMULATION' AND status = 'SIMULATED')
      OR status = 'UNAVAILABLE'
    ),
    UNIQUE (workspace_id, id)
);

CREATE INDEX discovery_observations_mission_time_idx
    ON discovery_observations (workspace_id, mission_id, created_at DESC);

CREATE TABLE discovery_mentions (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    observation_id UUID NOT NULL,
    product_reference VARCHAR(240) NOT NULL,
    brand_name VARCHAR(240),
    product_name VARCHAR(500),
    mention_context TEXT,
    position INTEGER CHECK (position IS NULL OR position > 0),
    FOREIGN KEY (workspace_id, observation_id)
        REFERENCES discovery_observations (workspace_id, id) ON DELETE CASCADE
);

CREATE TABLE discovery_claims (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    observation_id UUID NOT NULL,
    claim_text VARCHAR(1200) NOT NULL,
    verdict VARCHAR(24) NOT NULL CHECK (verdict IN ('SUPPORTED', 'CONTRADICTED', 'UNKNOWN', 'STALE')),
    evidence_reference VARCHAR(1000),
    evidence_value VARCHAR(1200),
    FOREIGN KEY (workspace_id, observation_id)
        REFERENCES discovery_observations (workspace_id, id) ON DELETE CASCADE
);

INSERT INTO discovery_missions (id, workspace_id, mission_text, source_type)
VALUES
 ('00000000-0000-4000-8000-000000000101', '00000000-0000-4000-8000-000000000001',
  'Find a lightweight daypack that fits a 15-inch laptop.', 'SYNTHETIC'),
 ('00000000-0000-4000-8000-000000000102', '00000000-0000-4000-8000-000000000001',
  'Brew one or two cups without disposable paper filters.', 'SYNTHETIC'),
 ('00000000-0000-4000-8000-000000000103', '00000000-0000-4000-8000-000000000001',
  'Organize a small kitchen with stackable ceramic containers.', 'SYNTHETIC');
