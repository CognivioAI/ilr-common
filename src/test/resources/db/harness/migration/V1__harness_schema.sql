-- KAN-222 harness self-test schema. Deliberately mirrors the SHAPE of every service's
-- V1__create_*_tables.sql, including the pre-KAN-189 weakness: ENABLE ROW LEVEL SECURITY only,
-- with no FORCE. V3 adds FORCE, exactly as the real migrations do, so the harness proves the
-- estate's actual migration sequence rather than an idealised one.

CREATE TABLE harness_cases (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    name          TEXT NOT NULL,
    signed_off_by UUID
);

ALTER TABLE harness_cases ENABLE ROW LEVEL SECURITY;

-- The estate-standard policy. current_setting(..., true) returns NULL rather than erroring when the
-- setting is absent, so an unbound session matches no rows: fail closed. RlsTenantBindingAspect is
-- what sets it, per transaction.
CREATE POLICY tenant_isolation ON harness_cases
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);

-- A table with NO tenant_id, so FlywayMigrationIT's generic "every tenant-bearing table has RLS
-- forced" assertion is proved to be discriminating rather than trivially true. If the assertion were
-- written to require RLS on every table it would fail here — and if it were written to require it on
-- no table it would pass vacuously. This table is the control.
CREATE TABLE harness_reference_data (
    code  TEXT PRIMARY KEY,
    label TEXT NOT NULL
);
