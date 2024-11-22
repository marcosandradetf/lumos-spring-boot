-- Extension
CREATE EXTENSION IF NOT EXISTS unaccent;

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

insert into tb_materials (material_name, material_power, material_brand, buy_unit, request_unit, id_material_type, stock_quantity, stock_available, inactive, id_company, id_deposit)
values
    ('Relé', 1000, 'Genérica', 'CX', 'UN', 1, 100, 100, false, 1, 1),
    ('Fio Elétrico', 200, 'ElétricaCo', 'Rolo', 'M', 2, 500, 500, false, 1, 1),
    ('Interruptor', 500, 'Artemis', 'UN', 'UN', 3, 150, 140, false, 1, 1),
    ('Disjuntor', 1500, 'PowerPro', 'CX', 'UN', 4, 80, 75, false, 1, 1),
    ('Lâmpada LED', 200, 'LumeTech', 'CX', 'UN', 5, 200, 180, false, 1, 1),
    ('Tomada', 300, 'ElétricaCo', 'UN', 'UN', 6, 350, 340, false, 1, 1),
    ('Fita isolante', 50, 'FitaFlex', 'Rolo', 'M', 7, 600, 580, false, 1, 1),
    ('Cabo de Aço', 1000, 'AçoMaster', 'Rolo', 'M', 8, 400, 380, false, 1, 1),
    ('Chave de Fenda', 50, 'ToolKing', 'UN', 'UN', 9, 100, 95, false, 1, 1),
    ('Parafuso', 200, 'FixMaster', 'CX', 'UN', 10, 1200, 1150, false, 1, 1),
    ('Bateria', 1200, 'MaxPower', 'UN', 'UN', 1, 150, 140, false, 1, 1),
    ('Placa Solar', 2500, 'SolarTech', 'CX', 'UN', 2, 50, 48, false, 1, 1),
    ('Gerador', 5000, 'PowerMax', 'UN', 'UN', 3, 20, 18, false, 1, 1),
    ('Ar condicionado', 2000, 'CoolTech', 'UN', 'UN', 4, 30, 28, false, 1, 1),
    ('Cadeira de escritório', 0, 'OfficeKing', 'UN', 'UN', 5, 500, 450, false, 1, 1),
    ('Mesa de escritório', 0, 'OfficeKing', 'UN', 'UN', 6, 200, 180, false, 1, 1),
    ('Alicate', 300, 'ToolMaster', 'UN', 'UN', 7, 600, 590, false, 1, 1),
    ('Lima', 100, 'LimaFlex', 'UN', 'UN', 8, 300, 280, false, 1, 1),
    ('Esmerilhadeira', 1500, 'GrindMaster', 'UN', 'UN', 9, 50, 45, false, 1, 1),
    ('Furadeira', 1200, 'DrillPro', 'UN', 'UN', 10, 100, 90, false, 1, 1),
    ('Serra', 2000, 'CutTech', 'UN', 'UN', 1, 150, 140, false, 1, 1),
    ('Ventilador', 200, 'AirCool', 'UN', 'UN', 2, 400, 380, false, 1, 1),
    ('Cinta de amarração', 100, 'SecureCo', 'Rolo', 'M', 3, 700, 650, false, 1, 1),
    ('Papel A4', 0, 'PaperPro', 'PCT', 'UN', 4, 1000, 980, false, 1, 1),
    ('Caneta', 0, 'WriteTech', 'UN', 'UN', 5, 500, 490, false, 1, 1),
    ('Lápis', 0, 'PencilMaster', 'UN', 'UN', 6, 1000, 950, false, 1, 1),
    ('Marcador', 0, 'MarkIt', 'UN', 'UN', 7, 300, 290, false, 1, 1),
    ('Post-it', 0, 'StickyPro', 'PCT', 'UN', 8, 500, 480, false, 1, 1),
    ('Computador', 5000, 'TechCore', 'UN', 'UN', 9, 150, 140, false, 1, 1),
    ('Monitor', 2000, 'ScreenMaster', 'UN', 'UN', 10, 200, 190, false, 1, 1),
    ('Teclado', 100, 'KeyPro', 'UN', 'UN', 1, 300, 290, false, 1, 1),
    ('Mouse', 50, 'ClickTech', 'UN', 'UN', 2, 400, 390, false, 1, 1),
    ('Cabeamento', 1000, 'CableTech', 'Rolo', 'M', 3, 600, 590, false, 1, 1),
    ('Switch', 1500, 'NetGear', 'UN', 'UN', 4, 80, 75, false, 1, 1),
    ('Roteador', 2000, 'SpeedTech', 'UN', 'UN', 5, 150, 145, false, 1, 1),
    ('HD Externo', 500, 'DataSafe', 'UN', 'UN', 6, 100, 90, false, 1, 1),
    ('Pen Drive', 100, 'FlashDrive', 'UN', 'UN', 7, 200, 190, false, 1, 1),
    ('CD-R', 0, 'DiscMaster', 'PCT', 'UN', 8, 1000, 980, false, 1, 1),
    ('DVD-R', 0, 'DiscPro', 'PCT', 'UN', 9, 1000, 970, false, 1, 1),
    ('Câmera de segurança', 2000, 'SecureCam', 'UN', 'UN', 10, 50, 45, false, 1, 1),
    ('Alarme', 1000, 'AlertTech', 'UN', 'UN', 1, 80, 75, false, 1, 1),
    ('Sensor de movimento', 500, 'MoveSense', 'UN', 'UN', 2, 120, 110, false, 1, 1),
    ('Controle remoto', 50, 'RemotePro', 'UN', 'UN', 3, 200, 190, false, 1, 1),
    ('Cabo HDMI', 0, 'HDMIConnect', 'UN', 'UN', 4, 300, 280, false, 1, 1),
    ('Transformador', 5000, 'PowerTrans', 'UN', 'UN', 5, 100, 90, false, 1, 1);