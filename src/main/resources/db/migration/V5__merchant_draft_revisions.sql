ALTER TABLE catalog_draft_revisions
    ALTER COLUMN source_model_run_id DROP NOT NULL;

ALTER TABLE catalog_draft_revisions
    ADD COLUMN author_type VARCHAR(24) NOT NULL DEFAULT 'META_MODEL'
        CHECK (author_type IN ('META_MODEL', 'MERCHANT')),
    ADD COLUMN author_label VARCHAR(120) NOT NULL DEFAULT 'META_MODEL';

ALTER TABLE catalog_drafts
    DROP CONSTRAINT catalog_drafts_status_check;

ALTER TABLE catalog_drafts
    ADD CONSTRAINT catalog_drafts_status_check
        CHECK (status IN ('NEEDS_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED', 'EXPORTED'));

ALTER TABLE draft_decisions
    DROP CONSTRAINT draft_decisions_decision_check;

ALTER TABLE draft_decisions
    ADD CONSTRAINT draft_decisions_decision_check
        CHECK (decision IN ('APPROVED', 'REJECTED', 'REQUEST_REVISION'));
