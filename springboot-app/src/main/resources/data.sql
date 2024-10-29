-- Script para inserir grupos
INSERT INTO tb_grupos (id_grupo, nome_grupo)
VALUES
    (1, 'Equipamentos Elétricos'),
    (2, 'Ferramentas e Instrumentos'),
    (3, 'Materiais de Construção'),
    (4, 'Componentes Eletrônicos'),
    (5, 'Equipamentos de Proteção Individual (EPI)'),
    (6, 'Equipamentos de Medição e Teste'),
    (7, 'Peças de Reposição'),
    (8, 'Materiais de Limpeza e Manutenção')
ON CONFLICT (nome_grupo) DO NOTHING;

-- Script para inserir tipos
INSERT INTO tb_tipos (id_tipo, nome_tipo, id_grupo)
VALUES
    -- Equipamentos Elétricos (ID 1)
    (1, 'Transformadores', 1),
    (2, 'Disjuntores', 1),
    (3, 'Motores Elétricos', 1),
    (4, 'Condutores Elétricos', 1),
    (5, 'Sensores de Temperatura', 1),
    (6, 'Relés', 1),
    (7, 'Fusíveis', 1),

    -- Ferramentas e Instrumentos (ID 2)
    (8, 'Ferramentas Manuais', 2),
    (9, 'Ferramentas Elétricas', 2),

    -- Materiais de Construção (ID 3)
    (10, 'Tubulações e Conexões', 3),

    -- Componentes Eletrônicos (ID 4)
    (11, 'Componentes Pneumáticos', 4),

    -- Equipamentos de Proteção Individual (EPI) (ID 5)
    (12, 'EPI (Equipamentos de Proteção Individual)', 5),

    -- Equipamentos de Medição e Teste (ID 6)
    (13, 'Equipamentos de Medição', 6),

    -- Peças de Reposição (ID 7)
    (14, 'Baterias e Acumuladores', 7),

    -- Materiais de Limpeza e Manutenção (ID 8)
    (15, 'Lubrificantes e Óleos', 8),
    (16, 'Materiais de Solda', 8)
ON CONFLICT (nome_tipo) DO NOTHING;

-- script empresa
insert into tb_empresas (nome_empresa)
values('SCL Solutions')
    ON CONFLICT (nome_empresa) DO NOTHING;

-- script almoxarifado
insert into tb_almoxarifados (nome_almoxarifado)
values('Galpão BH')
    ON CONFLICT (nome_almoxarifado) DO NOTHING;

-- script roles
INSERT INTO tb_roles (id_role, nome_role)
VALUES
    (1, 'ADMIN'),
    (2, 'MANAGER'),
    (3, 'BASIC')
ON CONFLICT (nome_role) DO NOTHING;