alter table direct_execution
    ADD COLUMN IF NOT EXISTS signature_uri TEXT,
    ADD COLUMN IF NOT EXISTS responsible TEXT,
    ADD COLUMN IF NOT EXISTS sign_date timestamp;