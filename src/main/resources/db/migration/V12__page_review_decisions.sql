CREATE TABLE page_review_decisions (
  workspace_id UUID NOT NULL REFERENCES workspaces(id),
  id UUID NOT NULL,
  reviewer TEXT NOT NULL,
  payload JSONB NOT NULL,
  recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY(workspace_id,id)
);
