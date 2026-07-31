-- Byte-for-byte the same SHAPE as the real V3__force_rls_and_app_role.sql in ilr-identity-service and
-- ilr-eligibility-service, and the shape the remaining 14 services will adopt. It is copied rather
-- than paraphrased on purpose: the harness's job is to prove that THIS construct is actually
-- exercised under test, and the construct includes the conditional guard.
--
-- The IF EXISTS guard is the trap. `ilr_app` genuinely does not exist in some environments (a bare
-- local Postgres started for `mvn spring-boot:run -Dspring-boot.run.profiles=local`), so the GRANT is
-- guarded to keep the migration a no-op there rather than failing service boot. The cost is that in
-- ANY environment lacking the role — including a naively configured Testcontainers database — this
-- migration reports SUCCESS having applied nothing at all beyond the FORCE statements. A
-- "migrations apply cleanly" test would be green while the grants production depends on went
-- unexercised.
--
-- IlrDatabaseRoles creates `ilr_app` before Flyway runs precisely so this branch is taken, and
-- FlywayMigrationIT asserts the grants landed rather than assuming they did.

ALTER TABLE harness_cases FORCE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ilr_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON harness_cases TO ilr_app;
        -- harness_reference_data carries no tenant_id and no RLS policy (the control table in V1),
        -- but the application still needs ordinary reads on it.
        GRANT SELECT ON harness_reference_data TO ilr_app;
    END IF;
END
$$;
