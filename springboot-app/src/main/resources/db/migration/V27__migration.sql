CREATE TABLE IF NOT EXISTS remote_config
(
    remote_config_id    BIGSERIAL PRIMARY KEY,
    app_id              VARCHAR(100) NOT NULL,
    platform            VARCHAR(50)  NOT NULL,
    min_supported_build BIGINT       NOT NULL,
    force_update        BOOLEAN      NOT NULL DEFAULT FALSE,
    update_type         VARCHAR(30)  NOT NULL DEFAULT 'FLEXIBLE',
    features_json       TEXT         NOT NULL DEFAULT '{}',
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS remote_config_action
(
    remote_config_action_id BIGSERIAL PRIMARY KEY,
    config_id               BIGINT       NOT NULL,
    action_key              VARCHAR(100) NOT NULL,
    action_type             VARCHAR(50)  NOT NULL,
    target                  VARCHAR(255) NOT NULL,
    min_app_build           BIGINT,
    conditions_json         TEXT,
    payload_json            TEXT,
    sort_order              INTEGER      NOT NULL DEFAULT 0,
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_remote_config_action_config
        FOREIGN KEY (config_id)
            REFERENCES remote_config (remote_config_id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_remote_config_active
    ON remote_config (app_id, platform)
    WHERE active = TRUE;

