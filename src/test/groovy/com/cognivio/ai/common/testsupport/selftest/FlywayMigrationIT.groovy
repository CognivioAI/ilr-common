package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.tenancy.TenantSessionInitializer
import com.cognivio.ai.common.testsupport.integration.IlrDatabaseRoles
import com.cognivio.ai.common.testsupport.integration.IlrPostgresContainer
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationState
import org.springframework.beans.factory.annotation.Autowired

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * Tier B — the migrations, the RLS state they leave behind, and whether the conditional GRANTs
 * actually took effect.
 *
 * <p>Written as service-agnostic assertions on purpose: apart from the two table names in the
 * "grants took effect" feature, every check here is a query over the catalogue and would be
 * copy-pasteable into all 16 services unchanged. The generic RLS assertion is the estate-wide
 * regression guard for KAN-189 — {@code RlsForceEnforcementIT} proves the <em>property</em> on a
 * synthetic table, whereas this proves a service's <em>actual</em> schema has it.
 */
class FlywayMigrationIT extends AbstractHarnessSelfTestIT {

    private static final TenantSessionInitializer BINDER = new TenantSessionInitializer()

    @Autowired
    Flyway flyway

    def "every migration applied cleanly, with none pending or out of order"() {
        when:
        def applied = flyway.info().applied()
        def pending = flyway.info().pending()

        then: "V1, V2 and V3 all present and successful"
        applied.size() == 3
        applied.every { it.state == MigrationState.SUCCESS }
        applied*.version*.version == ['1', '2', '3']

        and:
        pending.length == 0
    }

    def "every tenant-scoped table has row security both ENABLED and FORCED, with a policy"() {
        given: "the catalogue, for every public table carrying a tenant_id column"
        def tenantTables = IlrPostgresContainer.withOwnerRole { Connection conn -> query(conn, '''
            SELECT c.relname                AS table_name,
                   c.relrowsecurity         AS enabled,
                   c.relforcerowsecurity    AS forced,
                   (SELECT count(*) FROM pg_policies p
                     WHERE p.schemaname = 'public' AND p.tablename = c.relname) AS policy_count
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = 'public'
              AND c.relkind = 'r'
              AND EXISTS (SELECT 1 FROM information_schema.columns col
                           WHERE col.table_schema = 'public'
                             AND col.table_name = c.relname
                             AND col.column_name = 'tenant_id')
        ''') { ResultSet rs ->
            [table: rs.getString('table_name'), enabled: rs.getBoolean('enabled'),
             forced: rs.getBoolean('forced'), policies: rs.getInt('policy_count')]
        } }

        expect: "the assertion has something to assert on — a vacuous pass here would be the bug"
        !tenantTables.isEmpty()

        and: "ENABLE alone is the KAN-189 defect; FORCE is what removes the owner's exemption"
        tenantTables.every { it.enabled }
        tenantTables.every { it.forced }
        tenantTables.every { it.policies >= 1 }
    }

    def "the assertion above is discriminating: a table without tenant_id is correctly not required to have RLS"() {
        expect: "harness_reference_data is the control — RLS off, and that is correct, not a failure"
        !rowSecurityEnabled('harness_reference_data')
    }

    def "the conditional GRANT branch in V3 was taken — the app role can actually reach the tables"() {
        when: "reading as ilr_app, which owns nothing and inherits no privilege from the owner"
        def count = IlrPostgresContainer.withAppRole { Connection c -> scalarInt(c, 'SELECT count(*) FROM harness_cases') }

        then: "no 'permission denied for table harness_cases' — so the IF EXISTS branch ran"
        noExceptionThrown()
        count != null

        and: "stated directly as well, so the reason for a failure is unambiguous"
        hasPrivilege(IlrDatabaseRoles.APP_ROLE, 'harness_cases', 'SELECT')
        hasPrivilege(IlrDatabaseRoles.APP_ROLE, 'harness_cases', 'INSERT')
        hasPrivilege(IlrDatabaseRoles.APP_ROLE, 'harness_reference_data', 'SELECT')
    }

    def "the app role owns nothing, so RLS applies to it unconditionally"() {
        expect:
        tableOwner('harness_cases') == IlrDatabaseRoles.OWNER_ROLE

        and:
        IlrPostgresContainer.withAppRole { Connection c ->
            IlrDatabaseRoles.currentUserIsSuperuser(c) == false
        }
    }

    def "with no tenant bound, the app role sees zero rows — the application-layer bug fails CLOSED"() {
        expect: "this is what a missing RlsTenantBindingAspect looks like: no data, not other tenants' data"
        IlrPostgresContainer.withAppRole { Connection c ->
            scalarInt(c, 'SELECT count(*) FROM harness_cases') == 0
        }
    }

    def "with a tenant bound, the app role sees exactly that tenant's rows"() {
        expect:
        IlrPostgresContainer.withAppRole { Connection c ->
            c.autoCommit = false
            try {
                BINDER.apply(c, tenantId)
                return names(c) == expectedNames
            } finally {
                c.rollback()
            }
        }

        where:
        tenantId || expectedNames
        TENANT_A || ['tenant-A first case', 'tenant-A second case']
        TENANT_B || ['tenant-B only case']
    }

    def "FORCE also filters the schema-owning migration role"() {
        expect: "post-V3 the owner can no longer read back the rows it seeded in V2"
        IlrPostgresContainer.withOwnerRole { Connection c ->
            scalarInt(c, 'SELECT count(*) FROM harness_cases') == 0
        }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean rowSecurityEnabled(String table) {
        IlrPostgresContainer.withOwnerRole { Connection c ->
            scalarBoolean(c, "SELECT relrowsecurity FROM pg_class WHERE relname = '${table}'")
        }
    }

    private static String tableOwner(String table) {
        IlrPostgresContainer.withOwnerRole { Connection c ->
            scalarString(c, "SELECT tableowner FROM pg_tables WHERE schemaname = 'public' AND tablename = '${table}'")
        }
    }

    private static boolean hasPrivilege(String role, String table, String privilege) {
        IlrPostgresContainer.withOwnerRole { Connection c ->
            scalarBoolean(c, "SELECT has_table_privilege('${role}', '${table}', '${privilege}')")
        }
    }

    private static List<String> names(Connection connection) {
        query(connection, 'SELECT name FROM harness_cases ORDER BY name') { ResultSet rs -> rs.getString('name') }
    }

    private static <T> List<T> query(Connection connection, String sql, Closure<T> mapper) {
        List<T> rows = []
        connection.createStatement().withCloseable { Statement statement ->
            ResultSet rs = statement.executeQuery(sql)
            while (rs.next()) {
                rows << mapper.call(rs)
            }
        }
        return rows
    }

    private static Integer scalarInt(Connection connection, String sql) throws SQLException {
        scalar(connection, sql) { ResultSet rs -> rs.getInt(1) }
    }

    private static Boolean scalarBoolean(Connection connection, String sql) {
        scalar(connection, sql) { ResultSet rs -> rs.getBoolean(1) }
    }

    private static String scalarString(Connection connection, String sql) {
        scalar(connection, sql) { ResultSet rs -> rs.getString(1) }
    }

    private static <T> T scalar(Connection connection, String sql, Closure<T> mapper) {
        connection.createStatement().withCloseable { Statement statement ->
            ResultSet rs = statement.executeQuery(sql)
            return rs.next() ? mapper.call(rs) : null
        }
    }
}
