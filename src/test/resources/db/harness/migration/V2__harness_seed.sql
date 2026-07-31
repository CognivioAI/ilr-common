-- Seeded as the owner role, BEFORE V3 applies FORCE — matching how the real services seed reference
-- and demo data in an early migration and force RLS in a later one. After V3 the owner can no longer
-- read these rows back, which is itself asserted in FlywayMigrationIT.
--
-- Tenant ids match IlrBearerTokens.DEFAULT_TENANT_ID / OTHER_TENANT_ID so a spec's token and the
-- seeded data agree without either restating the other.

INSERT INTO harness_cases (id, tenant_id, name) VALUES
    ('aaaa0001-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'tenant-A first case'),
    ('aaaa0002-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'tenant-A second case'),
    ('bbbb0001-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'tenant-B only case');

INSERT INTO harness_reference_data (code, label) VALUES
    ('ILR', 'Indefinite Leave to Remain'),
    ('LR', 'Long Residence');
