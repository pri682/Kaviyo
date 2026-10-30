DROP INDEX discovery_observations_mission_time_idx;

CREATE INDEX discovery_observations_workspace_page_idx
    ON discovery_observations (workspace_id, created_at DESC, id DESC);

CREATE INDEX discovery_observations_mission_page_idx
    ON discovery_observations (workspace_id, mission_id, created_at DESC, id DESC);
