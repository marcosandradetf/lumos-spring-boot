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
             ('OPERADOR'),
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
WHERE role_id = (SELECT role_id
                 FROM tb_roles
                WHERE role_name = 'TECNICO' -- Substitua por qualquer outro critério
    LIMIT 1                                  -- Garante que só um resultado será retornado
    )
  AND NOT EXISTS (
SELECT 1
FROM tb_roles
WHERE role_name = 'ELETRICISTA'
    );

UPDATE tb_roles
SET role_name = 'MOTORISTA'
WHERE role_id = (SELECT role_id
                 FROM tb_roles
                WHERE role_name = 'OPERADOR' -- Substitua por qualquer outro critério
    LIMIT 1                                   -- Garante que só um resultado será retornado
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

INSERT INTO tb_contract_reference_items (complete_description, description, type, linking, item_dependency)
SELECT complete_description, description, type, linking, item_dependency
FROM (VALUES ('SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED', 'SERVIÇO DE INSTALAÇÃO DE LUMINÁRIA EM LED', 'SERVIÇO', NULL,
              'LED'),
             ('SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS', 'SERVIÇO DE RECOLOCAÇÃO DE BRAÇOS', 'SERVIÇO', NULL, 'BRAÇO'),
             ('RELÉ FOTOELETRONICO1000W 105-305V', 'RELÉ FOTOELETRONICO1000W 105-305V', 'RELÉ', NULL, 'LED'),
             ('CONECTOR PERFURANTE DERIVAÇÃO 10-35MM² TIPO  CDP70;',
              'CONECTOR PERFURANTE DERIVAÇÃO 10-35MM² TIPO  CDP70;', 'CONECTOR', NULL, NULL),
             ('CABO FLEXÍVEL 1,5MM', 'CABO FLEXÍVEL 1,5MM', 'CABO', '1,5MM', 'BRAÇO'),
             ('CABO FIO PP 2,5MM', 'CABO FIO PP 2,5MM', 'CABO', '2,5MM', 'BRAÇO'),
             ('PARAFUSOS DE FIXAÇÃO DAS CINTAS E BRAÇOS', 'PARAFUSOS DE FIXAÇÃO DAS CINTAS E BRAÇOS', 'PARAFUSO', NULL,
              NULL),
             ('CINTAS PARA FIXAÇÃO DE  BRAÇOS', 'CINTAS PARA FIXAÇÃO DE  BRAÇOS', 'CINTA', NULL, NULL),
             ('BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 1,5M', 'BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 1,5M', 'BRAÇO', '1,5',
              NULL),
             ('BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 2,5M', 'BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 2,5M', 'BRAÇO', '2,5',
              NULL),
             ('BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 3,6M', 'BRAÇO GALVANIZADO PADRÃO CEMIG ATÉ 3,6M', 'BRAÇO', '3,6',
              NULL),
             ('SERVIÇO DE EXECUÇÃO DE PROJETO POR IP', 'SERVIÇO DE EXECUÇÃO DE PROJETO POR IP', 'SERVIÇO', NULL, 'LED'),
             ('SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LED',
              'SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LED', 'MANUTENÇÃO', 'LED', NULL),
             ('SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LUMINÁRIA CONVENCIONAL',
              'SERVIÇO DE MANUTENÇÃO MENSAL POR PONTO DE ILUMINAÇÃO PÚBLICA - LUMINÁRIA CONVENCIONAL', 'MANUTENÇÃO',
              NULL, 'CONVENCIONAL'),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 50W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 50W', 'LED', '50W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 60W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 60W', 'LED', '60W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 70W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 70W', 'LED', '70W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 80W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 80W', 'LED', '80W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 100W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 100W', 'LED', '100W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 110W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 110W', 'LED', '110W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 120W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 120W', 'LED', '120W', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 150W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 150W', 'LED', '150w', NULL),
             ('LUMINÁRIA PARA ILUMINACÃO PÚBLICA LED, 200W LUMINĂRIA PARA ILUMINAÇÃO PÚBLICA LED, TENSÃO DE ENTRADA COM 200/240V - 50 A 60HZ, POTÊNCIA NOMINAL MÁXIMA 70W FLUXO LUMINOSO MÍNIMO DE 135 LUMENS/WT, FATOR DE POTÊNCIA MÍNIMO 0,95, IRC MÍNIMO 70. LENTES/REFRATOR EM VIDRO, DISTRIBUIÇÃO DA INTENSIDADE LUMINOSA DE CLASSIFICACÃO TIPO II MÉDIA OU CURTA, CORPO EM ALUMÍNIO INJETADO COM PESCOCO ARTICULADO COM NO MINIMO +/- 10 GRAUS. RESISTÊNCIA A IMPACTOS MECÂNICOS COM GRAU MÍNIMO DE PROTECÃO IK 08. GRAU DE PROTEÇÃO CONTRA SÓLIDOS E LÍQUIDOS DE MÍNIMO IP 66, TEMPERATURA DE COR: 5.000K ± 10%. VIDA ÚTIL DO LED MÍNIMA DE 50.000 HORAS. CHIP LED HIGH POWER TIPO SMD, DISPOSITIVO PARA PROTEÇÃO CONTRA SURTO DPS DE TENSÃO MÍNIMO 12KA/10KV, DISTORÇÃO HARMÔNICA TOTAL (THD) MENOR QUE 10%, COM SUPORTE DE FIXAÇÃO EM BRAÇOS DE 33 À 60, PREPARADAS COM TOMADAS EXTERNAS PARA UTILIZACAО DE RELE FOTOELETRICO. AS LUMINÁRIAS DEVEM POSSUIR 14 REGISTRO ATIVO NO INMETRO E CERTIFICADO PROCEL PARA ATENDIMENTO AS EXIGÊNCIAS DA ANEEL. GARANTIA MÍNIMA DE 05 ANOS.',
              'LUMINÁRIA PARA ILUMINAÇÃO PÚBLICA LED 200W', 'LED', '200W', NULL),
             ('REFLETOR DE LED 150W', 'REFLETOR DE LED 150W', 'REFLETOR', '150W', NULL),
             ('REFLETOR DE LED 180W', 'REFLETOR DE LED 180W', 'REFLETOR', '180W', NULL),
             ('REFLETOR DE LED 200W', 'REFLETOR DE LED 200W', 'REFLETOR', '200W', NULL),
             ('REFLETOR DE LED 240W', 'REFLETOR DE LED 240W', 'REFLETOR', '240W', NULL),
             ('REFLETOR DE LED 270W', 'REFLETOR DE LED 270W', 'REFLETOR', '270W', NULL),
             ('POSTE DE CIMENTO 7M', 'POSTE DE CIMENTO 7M', 'POSTE CIMENTO', '7M', NULL),
             ('POSTE DE CIMENTO 10M', 'POSTE DE CIMENTO 10M', 'POSTE CIMENTO', '10M', NULL),
             ('POSTE DE CIMENTO 12M', 'POSTE DE CIMENTO 12M', 'POSTE CIMENTO', '12M', NULL),
             ('POSTE ORNAMENTAL GALVANIZADO 7M', 'POSTE ORNAMENTAL GALVANIZADO 7M', 'POSTE GALVANIZADO',
              '7M', NULL),
             ('POSTE ORNAMENTAL GALVANIZADO 8M', 'POSTE ORNAMENTAL GALVANIZADO 8M', 'POSTE GALVANIZADO',
              '8M', NULL),
             ('POSTE ORNAMENTAL GALVANIZADO 10M', 'POSTE ORNAMENTAL GALVANIZADO 10M', 'POSTE GALVANIZADO',
              '10M', NULL),
             ('POSTE ORNAMENTAL FORNECIMENTO E INSTALAÇÃO DE POSTE ORNAMENTAL GALVANIZADO 12M LIVRE, 4 PARA 3 1/2 COM SUPORTE PARA (03) TRÊS LUMINÁRIAS PUBLICA DE LED 100W, CERTIFICADA INMETRO – PORTARIA 20, INCLUINDO, MÃO DE OBRA ESPECIALIZADA, COM EQUIPAMENTOS DE SEGURANÇA INDIVIDUAL (EPI),  EQUIPAMENTOS DE SEGURANÇA COLETIVA (EPC) E TODO MATERIAL NECESSARIO PARA REALIZAÇÃO DO SERVIÇO, ASSENTAMENTO E CAIXA DE PASSAGEM.',
              'POSTE ORNAMENTAL GALVANIZADO 12M', 'POSTE GALVANIZADO', '12M',NULL)) AS items(complete_description, description, type, linking, item_dependency)
WHERE NOT EXISTS (SELECT 1
                  FROM tb_contract_reference_items);



