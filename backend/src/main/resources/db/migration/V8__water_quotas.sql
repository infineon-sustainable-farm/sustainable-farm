CREATE TABLE IF NOT EXISTS water_quotas (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id UUID NOT NULL,
    quota_month DATE NOT NULL,
    quota_liters DOUBLE PRECISION NOT NULL,
    label VARCHAR(120),
    CONSTRAINT chk_water_quotas_target_type CHECK (target_type IN ('farm', 'zone')),
    CONSTRAINT chk_water_quotas_positive CHECK (quota_liters > 0),
    CONSTRAINT uk_water_quotas_target_month UNIQUE (target_type, target_id, quota_month)
);

CREATE INDEX IF NOT EXISTS idx_water_quotas_target ON water_quotas (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_water_quotas_month ON water_quotas (quota_month);
