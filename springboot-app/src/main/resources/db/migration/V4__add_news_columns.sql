-- 1. new table tb_reservation_managements
ALTER TABLE tb_pre_measurements_streets
    ADD COLUMN IF NOT EXISTS prioritized BOOLEAN      NULL,
    ADD COLUMN IF NOT EXISTS comment     VARCHAR(150) NULL;

ALTER TABLE tb_reservation_managements
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NULL;

ALTER TABLE tb_material_reservation
    DROP COLUMN IF EXISTS material_truck_stock_id,
    DROP COLUMN IF EXISTS material_first_deposit_stock_id,
    DROP COLUMN IF EXISTS material_second_deposit_stock_id,
    ADD COLUMN IF NOT EXISTS material_stock_id BIGINT NULL
        CONSTRAINT fk_material_stock
            REFERENCES tb_material_stock (material_id_stock);

ALTER TABLE tb_companies
    RENAME COLUMN company_name TO social_reason;

ALTER TABLE tb_companies
    ADD COLUMN IF NOT EXISTS fantasy_name    VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS company_cnpj    VARCHAR(14)  NULL,
    ADD COLUMN IF NOT EXISTS company_contact VARCHAR(80)  NULL,
    ADD COLUMN IF NOT EXISTS company_phone   VARCHAR(15)  NULL,
    ADD COLUMN IF NOT EXISTS company_email   VARCHAR(50)  NULL,
    ADD COLUMN IF NOT EXISTS company_address VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS company_logo    VARCHAR(100) NULL,
    ALTER COLUMN social_reason TYPE varchar(100);


UPDATE tb_companies
SET fantasy_name    = 'SOLUTIONS ENGENHARIA',
    company_cnpj    = '26777222000109',
    company_contact = 'Daniela Lamounier',
    company_phone   = '31985112231',
    company_email   = 'atendimento@solutionscl.com.br',
    company_address = 'Av. Raja Gabáglia, 4859 - Santa Lúcia, Belo Horizonte - MG',
    company_logo    = 'https://minio.thryon.com.br/scl-construtora/logo_solutions.png';