CREATE TABLE workspace_memberships (
    workspace_id UUID NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    subject VARCHAR(255) NOT NULL,
    role VARCHAR(24) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (workspace_id, subject)
);

CREATE INDEX workspace_memberships_subject_idx ON workspace_memberships (subject, workspace_id);

INSERT INTO workspace_memberships (workspace_id, subject, role)
VALUES ('00000000-0000-4000-8000-000000000001', 'local-demo-user', 'OWNER')
ON CONFLICT (workspace_id, subject) DO NOTHING;
