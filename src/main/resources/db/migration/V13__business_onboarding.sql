CREATE TABLE merchant_setups (
 workspace_id UUID NOT NULL REFERENCES workspaces(id), id UUID NOT NULL,
 business TEXT NOT NULL, website TEXT, background TEXT, policies TEXT,
 goal TEXT NOT NULL, cadence TEXT NOT NULL CHECK (cadence IN ('daily','weekly','monthly')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(workspace_id,id)
);
