-- Site Security joins the integrated platform (PR #93).
--
-- 1. The access register (site_security_users) and the access log (site_security_logs) reference
--    the shared people directory (person, ADR 0004) instead of copying names, initials and
--    emails. A row written before this migration keeps its person when its name matches exactly
--    one person (ignoring case); otherwise a credential is removed (it cannot exist without a
--    holder) and a log entry becomes an unidentified passage. The copied columns are dropped.
-- 2. New Site Security tables: the operator assignments (module role, permissions and zones of
--    a person; no account, credential or session) and the audit trail of administrative changes.
--
-- Site Security tables only; the baseline (V1) is not changed. Column order: id first, then the
-- other columns by name, as in V1. Constraint names follow ReadableConstraintNamingStrategy.

-- ============================================================================
-- Access register: the holder is a person
-- ============================================================================

ALTER TABLE site_security_users ADD COLUMN person_id bigint;
ALTER TABLE site_security_users ADD COLUMN notification_status character varying(16);

UPDATE site_security_users u
SET person_id = p.id
FROM person p
WHERE lower(trim(p.full_name)) = lower(trim(u.name))
  AND (SELECT count(*) FROM person q WHERE lower(trim(q.full_name)) = lower(trim(u.name))) = 1;

-- One credential per person: of several rows linked to the same person, the first id is kept.
DELETE FROM site_security_users u
WHERE u.person_id IS NULL
   OR EXISTS (SELECT 1 FROM site_security_users v WHERE v.person_id = u.person_id AND v.id < u.id);

ALTER TABLE site_security_users ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE site_security_users DROP COLUMN initials;
ALTER TABLE site_security_users DROP COLUMN name;
ALTER TABLE site_security_users
    ADD CONSTRAINT site_security_users_notification_status_check
        CHECK (notification_status IN ('PENDING', 'SENT', 'FAILED'));
ALTER TABLE site_security_users ADD CONSTRAINT uk_site_security_users_person_id UNIQUE (person_id);
ALTER TABLE site_security_users
    ADD CONSTRAINT fk_site_security_users_person FOREIGN KEY (person_id) REFERENCES person(id);

-- ============================================================================
-- Access log: the person who passed, empty for an unidentified subject
-- ============================================================================

ALTER TABLE site_security_logs ADD COLUMN person_id bigint;

UPDATE site_security_logs l
SET person_id = p.id
FROM person p
WHERE lower(trim(p.full_name)) = lower(trim(l.user_name))
  AND (SELECT count(*) FROM person q WHERE lower(trim(q.full_name)) = lower(trim(l.user_name))) = 1;

ALTER TABLE site_security_logs DROP COLUMN initials;
ALTER TABLE site_security_logs DROP COLUMN user_name;
ALTER TABLE site_security_logs
    ADD CONSTRAINT fk_site_security_logs_person FOREIGN KEY (person_id) REFERENCES person(id);
CREATE INDEX idx_site_security_logs_person_id ON site_security_logs (person_id);

-- ============================================================================
-- Operator assignments: Site Security role, permissions and zones of a person
-- ============================================================================

CREATE TABLE site_security_operators (
    id character varying(36) NOT NULL,
    person_id bigint NOT NULL,
    role character varying(16) NOT NULL,
    CONSTRAINT site_security_operators_role_check CHECK (role IN ('READ', 'ADMIN', 'GATE', 'USER'))
);

CREATE TABLE site_security_operator_permissions (
    operator_id character varying(36) NOT NULL,
    permission character varying(32)
);

CREATE TABLE site_security_operator_zones (
    operator_id character varying(36) NOT NULL,
    zone_id character varying(32)
);

ALTER TABLE site_security_operators ADD CONSTRAINT site_security_operators_pkey PRIMARY KEY (id);
ALTER TABLE site_security_operators ADD CONSTRAINT uk_site_security_operators_person_id UNIQUE (person_id);
ALTER TABLE site_security_operators
    ADD CONSTRAINT fk_site_security_operators_person FOREIGN KEY (person_id) REFERENCES person(id);

ALTER TABLE site_security_operator_permissions
    ADD CONSTRAINT fk_site_security_operator_permissions_operator
        FOREIGN KEY (operator_id) REFERENCES site_security_operators(id);
CREATE INDEX idx_site_security_operator_permissions_operator_id
    ON site_security_operator_permissions (operator_id);

ALTER TABLE site_security_operator_zones
    ADD CONSTRAINT fk_site_security_operator_zones_operator
        FOREIGN KEY (operator_id) REFERENCES site_security_operators(id);
CREATE INDEX idx_site_security_operator_zones_operator_id ON site_security_operator_zones (operator_id);

-- ============================================================================
-- Audit trail of administrative changes (append only, no personal data)
-- ============================================================================

CREATE TABLE site_security_audit_events (
    id character varying(36) NOT NULL,
    action character varying(32) NOT NULL,
    actor_kind character varying(16),
    actor_operator_id character varying(36) NOT NULL,
    detail character varying(64),
    occurred_at timestamp(6) with time zone NOT NULL,
    target_id character varying(36) NOT NULL,
    target_type character varying(16) NOT NULL
);

ALTER TABLE site_security_audit_events ADD CONSTRAINT site_security_audit_events_pkey PRIMARY KEY (id);
CREATE INDEX idx_site_security_audit_events_target ON site_security_audit_events (target_type, target_id);
CREATE INDEX idx_site_security_audit_events_occurred ON site_security_audit_events (occurred_at);
