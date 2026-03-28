-- Planos pagos (não são “trial”: o trial é o [SubscriptionStatus], não o nome do plano).
INSERT INTO plan (plan_name, description, price_per_user_monthly, price_per_user_yearly, is_active)
VALUES ('Essencial',
        'Funcionalidades core para equipes pequenas',
        49.00,
        490.00,
        TRUE)
ON CONFLICT (plan_name) DO UPDATE
SET description       = EXCLUDED.description,
    price_per_user_monthly = EXCLUDED.price_per_user_monthly,
    price_per_user_yearly = EXCLUDED.price_per_user_yearly,
    is_active           = EXCLUDED.is_active;

INSERT INTO plan (plan_name, description, price_per_user_monthly, price_per_user_yearly, is_active)
VALUES ('Profissional',
        'Plano completo com trial de 14 dias no self-service',
        99.00,
        990.00,
        TRUE)
ON CONFLICT (plan_name) DO UPDATE
SET description       = EXCLUDED.description,
    price_per_user_monthly = EXCLUDED.price_per_user_monthly,
    price_per_user_yearly = EXCLUDED.price_per_user_yearly,
    is_active           = EXCLUDED.is_active;

INSERT INTO plan (plan_name, description, price_per_user_monthly, price_per_user_yearly, is_active)
VALUES ('Enterprise',
        'Escala, integrações e suporte prioritário',
        149.00,
        1490.00,
        TRUE)
ON CONFLICT (plan_name) DO UPDATE
SET description       = EXCLUDED.description,
    price_per_user_monthly = EXCLUDED.price_per_user_monthly,
    price_per_user_yearly = EXCLUDED.price_per_user_yearly,
    is_active           = EXCLUDED.is_active;
