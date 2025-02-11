-- Extension
CREATE
EXTENSION IF NOT EXISTS unaccent;

-- Script para inserir groups
INSERT INTO tb_groups (group_name)
SELECT group_name
FROM (VALUES ('Materiais Elétricos'),
             ('Ferramentas e Instrumentos'),
             ('Materiais de Construção'),
             ('Componentes Eletrônicos'),
             ('Equipamentos de Proteção Individual (EPI)'),
             ('Equipamentos de Medição e Teste'),
             ('Peças de Reposição'),
             ('Materiais de Limpeza e Manutenção')) AS groups(group_name)
WHERE NOT EXISTS (SELECT 1
                  FROM tb_groups);

-- Script para inserir types
INSERT INTO tb_types (type_name, id_group)
SELECT type_name, id_group
FROM (VALUES
          -- Equipamentos Elétricos (ID 1)
          ('Disjuntores', 1),
          ('Motores Elétricos', 1),
          ('Cabos', 1),
          ('Relés', 1),
          ('Fusíveis', 1),
          ('Leds', 1),

          -- Ferramentas e Instrumentos (ID 2)
          ('Ferramentas Manuais', 2),
          ('Ferramentas Elétricas', 2),

          -- Materiais de Construção (ID 3)
          ('Tubulações e Conexões', 3),

          -- Equipamentos de Proteção Individual (EPI) (ID 5)
          ('EPI (Equipamentos de Proteção Individual)', 5),

          -- Equipamentos de Medição e Teste (ID 6)
          ('Equipamentos de Medição', 6)) AS data(type_name, id_group)
WHERE NOT EXISTS (SELECT 1
                  FROM tb_types);
-- script roles
INSERT INTO tb_roles (role_name)
SELECT role_name
FROM (VALUES ('ADMIN'),
             ('ANALISTA'),
             ('RESPONSAVEL_TECNICO'),
             ('ELETRICISTA'),
             ('MOTORISTA'),
             ('ESTOQUISTA'),
             ('ESTOQUISTA_CHEFE')) AS roles(role_name)
WHERE NOT EXISTS (SELECT 1
                  FROM tb_roles);

-- scripts
-- 01.00
BEGIN;

UPDATE tb_roles
SET role_name = 'ELETRICISTA'
WHERE role_id = (
    SELECT role_id
    FROM tb_roles
    WHERE role_name = 'TECNICO'  -- Substitua por qualquer outro critério
    LIMIT 1  -- Garante que só um resultado será retornado
    )
  AND NOT EXISTS (
SELECT 1
FROM tb_roles
WHERE role_name = 'ELETRICISTA'
    );

UPDATE tb_roles
SET role_name = 'MOTORISTA'
WHERE role_id = (
    SELECT role_id
    FROM tb_roles
    WHERE role_name = 'OPERADOR'  -- Substitua por qualquer outro critério
    LIMIT 1  -- Garante que só um resultado será retornado
    )
  AND NOT EXISTS (
SELECT 1
FROM tb_roles
WHERE role_name = 'MOTORISTA'
    );

INSERT INTO tb_roles (role_name)
SELECT role_name
FROM (VALUES ('RESPONSAVEL_TECNICO')) AS roles(role_name)
WHERE NOT EXISTS (SELECT 1
                  FROM tb_roles);

-- Criação da tabela
CREATE TABLE IF NOT EXISTS tb_version_control
(
    version_script
    varchar
(
    10
) NOT NULL,
    script_executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    version_script
)
    );

-- Registro da versão
INSERT INTO tb_version_control (version_script)
SELECT '01.00' WHERE NOT EXISTS (SELECT 1 FROM tb_version_control WHERE version_script = '01.00');

-- Alteração na tabela
ALTER TABLE tb_teams DROP COLUMN IF EXISTS user_id;

COMMIT;


