-- Billing: catálogo de planos (se ainda não existir), subscription por tenant, invoice mensal, auditoria em app_user.
-- Decisões: 1 subscription por tenant (UNIQUE tenant_id); preços congelados na subscription (snapshot);
-- fatura idempotente por (subscription_id, period_start); seat = usuário operacional faturável (ver regras na doc / código).

CREATE TABLE IF NOT EXISTS plan
(
    plan_name              TEXT PRIMARY KEY,
    description            TEXT,
    price_per_user_monthly NUMERIC(10, 2),
    price_per_user_yearly  NUMERIC(10, 2),
    is_active              BOOLEAN                  DEFAULT TRUE NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_plan_active ON plan (is_active);

CREATE TABLE IF NOT EXISTS module
(
    module_code TEXT PRIMARY KEY,
    name        TEXT                                   NOT NULL,
    description TEXT,
    is_active   BOOLEAN                  DEFAULT TRUE  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_module_active ON module (is_active);

CREATE TABLE IF NOT EXISTS plan_module
(
    plan_name   TEXT    NOT NULL REFERENCES plan (plan_name) ON DELETE CASCADE,
    module_code TEXT    NOT NULL REFERENCES module (module_code) ON DELETE CASCADE,
    enabled     BOOLEAN DEFAULT TRUE NOT NULL,
    PRIMARY KEY (plan_name, module_code)
);

CREATE INDEX IF NOT EXISTS idx_plan_module_plan ON plan_module (plan_name);
CREATE INDEX IF NOT EXISTS idx_plan_module_module ON plan_module (module_code);

INSERT INTO plan (plan_name, description, price_per_user_monthly, price_per_user_yearly, is_active)
VALUES ('Profissional', 'Plano Profissional — trial 14 dias (ajuste valores conforme comercial)', 99.00, 990.00, TRUE)
ON CONFLICT (plan_name) DO NOTHING;

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS deactivated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_app_user_tenant_billing
    ON app_user (tenant_id)
    WHERE deactivated_at IS NULL
      AND status IS TRUE
      AND support IS FALSE;

CREATE TABLE IF NOT EXISTS subscription
(
    subscription_id                 UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    tenant_id                       UUID                     NOT NULL UNIQUE REFERENCES tenant (tenant_id) ON DELETE CASCADE,
    plan_name                       TEXT                     NOT NULL REFERENCES plan (plan_name),
    status                          TEXT                     NOT NULL,
    billing_cycle                   TEXT                     NOT NULL,
    trial_ends_at                   TIMESTAMPTZ,
    current_period_start            DATE                     NOT NULL,
    current_period_end              DATE                     NOT NULL,
    price_per_user_monthly_snapshot NUMERIC(10, 2)         NOT NULL,
    price_per_user_yearly_snapshot  NUMERIC(10, 2),
    currency                        CHAR(3)                NOT NULL DEFAULT 'BRL',
    payment_provider_customer_id    TEXT,
    payment_provider                TEXT,
    cancel_at_period_end            BOOLEAN                NOT NULL DEFAULT FALSE,
    canceled_at                     TIMESTAMPTZ,
    created_at                      TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ            NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscription_status CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED')),
    CONSTRAINT chk_subscription_cycle CHECK (billing_cycle IN ('MONTHLY', 'YEARLY'))
);

CREATE INDEX IF NOT EXISTS idx_subscription_status ON subscription (status);
CREATE INDEX IF NOT EXISTS idx_subscription_trial ON subscription (trial_ends_at) WHERE status = 'TRIAL';

CREATE TABLE IF NOT EXISTS invoice
(
    invoice_id           UUID PRIMARY KEY                  DEFAULT gen_random_uuid(),
    subscription_id      UUID                     NOT NULL REFERENCES subscription (subscription_id) ON DELETE CASCADE,
    tenant_id            UUID                     NOT NULL REFERENCES tenant (tenant_id) ON DELETE CASCADE,
    period_start         DATE                     NOT NULL,
    period_end           DATE                     NOT NULL,
    billing_cycle        TEXT                     NOT NULL,
    seat_count           INTEGER                  NOT NULL,
    unit_price_snapshot  NUMERIC(10, 2)           NOT NULL,
    amount_total         NUMERIC(12, 2)           NOT NULL,
    currency             CHAR(3)                  NOT NULL DEFAULT 'BRL',
    status               TEXT                     NOT NULL,
    issued_at            TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    due_at               TIMESTAMPTZ,
    paid_at              TIMESTAMPTZ,
    external_payment_id  TEXT,
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE')),
    CONSTRAINT chk_invoice_seats CHECK (seat_count >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_subscription_period ON invoice (subscription_id, period_start);

CREATE INDEX IF NOT EXISTS idx_invoice_tenant_period ON invoice (tenant_id, period_start DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_subscription ON invoice (subscription_id);
