ALTER TABLE contract_reference_item
ADD COLUMN IF NOT EXISTS status text NOT NULL DEFAULT 'ACTIVE';

UPDATE contract_reference_item cri
SET status = 'PENDING_VALIDATION'
WHERE (
    UPPER(COALESCE(cri.type, '')) IN ('SERVIÇO', 'SERVICO', 'PROJETO')
    AND NOT EXISTS (
        SELECT 1
        FROM contract_item_dependency cid
        WHERE cid.contract_item_reference_id = cri.contract_reference_item_id
    )
) OR (
    UPPER(COALESCE(cri.type, '')) NOT IN ('SERVIÇO', 'SERVICO', 'PROJETO')
    AND NOT EXISTS (
        SELECT 1
        FROM material_contract_reference_item mcri
        WHERE mcri.contract_reference_item_id = cri.contract_reference_item_id
    )
);

ALTER TABLE contract_reference_item
    DROP CONSTRAINT IF EXISTS contract_reference_item_status_check;
