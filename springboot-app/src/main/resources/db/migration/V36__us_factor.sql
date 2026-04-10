ALTER TABLE contract_item
    ADD COLUMN if not exists factor numeric default 1 not null;

ALTER TABLE contract
    RENAME COLUMN unify_services TO uses_su_factor;
