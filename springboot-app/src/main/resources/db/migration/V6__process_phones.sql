DO
$$
    DECLARE
        r RECORD;
    BEGIN
        FOR r IN
            SELECT *
            FROM (SELECT tu.id_user, concat(tu.name, ' ', tu.last_name) as name
                  FROM tb_users tu
                           INNER JOIN tb_users_roles ur ON ur.id_user = tu.id_user
                  WHERE ur.id_role IN (6, 3)
                    AND tu.status = true
                  ORDER BY tu.name) AS ordered_users
            LOOP
                IF r.name = 'Christiano Assis' THEN
                    UPDATE tb_users SET phone_number = '37999529434' WHERE id_user = r.id_user;
                ELSIF r.name = 'Cleyton Garcia' THEN
                    UPDATE tb_users SET phone_number = '35997131850' WHERE id_user = r.id_user;
                ELSIF r.name = 'Edgar Silva' THEN
                    UPDATE tb_users SET phone_number = '31999062813' WHERE id_user = r.id_user;
                ELSIF r.name = 'Felipe Oliveira' THEN
                    UPDATE tb_users SET phone_number = '37991325535' WHERE id_user = r.id_user;
                ELSIF r.name = 'Felipe Henrique' THEN
                    UPDATE tb_users SET phone_number = '37991913931' WHERE id_user = r.id_user;
                ELSIF r.name = 'Francisco Flavio' THEN
                    UPDATE tb_users
                    SET phone_number = '37999173366',
                        last_name    = 'Gato'
                    WHERE id_user = r.id_user;
                ELSIF r.name = 'Giliard Araujo' THEN
                    UPDATE tb_users SET phone_number = '33999389448' WHERE id_user = r.id_user;
                ELSIF r.name = 'Gleydstom Moura' THEN
                    UPDATE tb_users SET phone_number = '31997341300' WHERE id_user = r.id_user;
                ELSIF r.name = 'Hilario Aparecido' THEN
                    UPDATE tb_users SET phone_number = '33998245289' WHERE id_user = r.id_user;
                ELSIF r.name = 'Isael Lima' THEN
                    UPDATE tb_users SET phone_number = '37999089325' WHERE id_user = r.id_user;
                ELSIF r.name = 'Joaquim Oliveira' THEN
                    UPDATE tb_users SET phone_number = '35997003931' WHERE id_user = r.id_user;
                ELSIF r.name = 'Juvenal Silva' THEN
                    UPDATE tb_users SET phone_number = '38998030226' WHERE id_user = r.id_user;
                ELSIF r.name = 'Luis Silva' THEN
                    UPDATE tb_users SET phone_number = '35999840748' WHERE id_user = r.id_user;
                ELSIF r.name = 'Marcelo Araujo' THEN
                    UPDATE tb_users SET phone_number = '33999894443' WHERE id_user = r.id_user;
                ELSIF r.name = 'Marcelo Rodrigues' THEN
                    UPDATE tb_users SET phone_number = '33998428081' WHERE id_user = r.id_user;
                ELSIF r.name = 'Marcos Henrique' THEN
                    UPDATE tb_users SET phone_number = '31991237746' WHERE id_user = r.id_user;
                ELSIF r.name = 'Marcos Vinicius' THEN
                    UPDATE tb_users SET phone_number = '33998366450' WHERE id_user = r.id_user;
                ELSIF r.name = 'Mauro Vinicius' THEN
                    UPDATE tb_users SET phone_number = '33988436950' WHERE id_user = r.id_user;
                ELSIF r.name = 'Paulo Adriano' THEN
                    UPDATE tb_users SET phone_number = '35984347465' WHERE id_user = r.id_user;
                ELSIF r.name = 'Renato Rocha' THEN
                    UPDATE tb_users SET phone_number = '37999057895' WHERE id_user = r.id_user;
                ELSIF r.name = 'Vando Alves' THEN
                    UPDATE tb_users SET phone_number = '31996832727' WHERE id_user = r.id_user;
                ELSIF r.name = 'Welliton Ferreira' THEN
                    UPDATE tb_users SET phone_number = '37991328994' WHERE id_user = r.id_user;
                END IF;
            END LOOP;
    END
$$;
