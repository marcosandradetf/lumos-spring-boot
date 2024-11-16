-- Script para inserir groups
INSERT INTO tb_groups(id_group, group_name)
VALUES
    (1, 'Equipamentos Elétricos'),
    (2, 'Ferramentas e Instrumentos'),
    (3, 'Materiais de Construção'),
    (4, 'Componentes Eletrônicos'),
    (5, 'Equipamentos de Proteção Individual (EPI)'),
    (6, 'Equipamentos de Medição e Teste'),
    (7, 'Peças de Reposição'),
    (8, 'Materiais de Limpeza e Manutenção')
ON CONFLICT (group_name) DO NOTHING;

-- Script para inserir types
INSERT INTO tb_types(id_type, type_name, id_group)
VALUES
    -- Equipamentos Elétricos (ID 1)
    (1, 'Transformadores', 1),
    (2, 'Disjuntores', 1),
    (3, 'Motores Elétricos', 1),
    (4, 'Condutores Elétricos', 1),
    (5, 'Sensores de Temperatura', 1),
    (6, 'Relés', 1),
    (7, 'Fusíveis', 1),
    (8, 'Leds', 1),

    -- Ferramentas e Instrumentos (ID 2)
    (9, 'Ferramentas Manuais', 2),
    (10, 'Ferramentas Elétricas', 2),

    -- Materiais de Construção (ID 3)
    (11, 'Tubulações e Conexões', 3),

    -- Componentes Eletrônicos (ID 4)
    (12, 'Componentes Pneumáticos', 4),

    -- Equipamentos de Proteção Individual (EPI) (ID 5)
    (13, 'EPI (Equipamentos de Proteção Individual)', 5),

    -- Equipamentos de Medição e Teste (ID 6)
    (14, 'Equipamentos de Medição', 6),

    -- Peças de Reposição (ID 7)
    (15, 'Baterias e Acumuladores', 7),

    -- Materiais de Limpeza e Manutenção (ID 8)
    (16, 'Lubrificantes e Óleos', 8),
    (17, 'Materiais de Solda', 8)
ON CONFLICT (type_name) DO NOTHING;

-- script company
insert into tb_companies (company_name)
values('SCL Solutions')
    ON CONFLICT (company_name) DO NOTHING;

-- script deposit
insert into tb_deposits (deposit_name)
values('Galpão BH')
    ON CONFLICT (deposit_name) DO NOTHING;

-- script roles
INSERT INTO tb_roles (id_role, nome_role)
VALUES
    (1, 'ADMIN'),
    (2, 'MANAGER'),
    (3, 'BASIC')
ON CONFLICT (nome_role) DO NOTHING;


--

