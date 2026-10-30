CREATE TABLE merchant_goals (
  workspace_id UUID NOT NULL REFERENCES workspaces(id),
  id UUID NOT NULL,
  metric VARCHAR(32) NOT NULL
    CHECK (metric IN ('MENTION_RATE', 'ACCURACY_RATE', 'FINDINGS_FIXED', 'OBSERVATIONS_RECORDED')),
  target NUMERIC NOT NULL CHECK (target > 0),
  period VARCHAR(16) NOT NULL CHECK (period IN ('daily', 'weekly', 'monthly')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, id)
);
CREATE INDEX merchant_goals_workspace_idx ON merchant_goals (workspace_id, created_at);

ALTER TABLE merchant_setups ALTER COLUMN goal DROP NOT NULL;
ALTER TABLE merchant_setups ALTER COLUMN cadence DROP NOT NULL;
