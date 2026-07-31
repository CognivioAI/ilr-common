package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.testsupport.integration.IlrDatabaseRoles
import com.cognivio.ai.common.testsupport.integration.IlrPostgresContainer

import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Proves the harness's guard <b>fires</b>, rather than merely existing.
 *
 * <p>An unfired guard is worth nothing, and a guard that is only asserted to be present is the same
 * class of test KAN-190 was raised about — it cannot fail for the defect it is aimed at. So this spec
 * does two things:
 *
 * <ol>
 *   <li>hands {@link IlrDatabaseRoles#assertRuntimeRoleIsNotSuperuser} each of the three roles in
 *       turn and asserts it rejects the two that would make the suite vacuous and accepts the one
 *       that would not; and</li>
 *   <li>demonstrates the vacuity directly — the same queries the rest of the suite relies on,
 *       run over a superuser connection and over a conditional GRANT for an absent role, both
 *       succeeding while proving nothing.</li>
 * </ol>
 *
 * <p>Read the second group as the counterfactual: <em>this</em> is the result set a naively
 * configured Testcontainers integration test would have been asserting against.
 */
class HarnessFalseAssuranceIT extends AbstractHarnessSelfTestIT {

    def "the guard rejects the Testcontainers bootstrap superuser"() {
        when:
        IlrDatabaseRoles.assertRuntimeRoleIsNotSuperuser(IlrPostgresContainer.superuserDataSource())

        then:
        IllegalStateException ex = thrown()
        ex.message.contains('SUPERUSER')
        ex.message.contains(IlrDatabaseRoles.APP_ROLE)
    }

    def "the guard rejects the migration owner role"() {
        when: "ilr_owner is not a superuser, so the first check passes — but it owns the tables"
        IlrDatabaseRoles.assertRuntimeRoleIsNotSuperuser(IlrPostgresContainer.ownerDataSource())

        then: "an owner is exempt from RLS on any table missing FORCE, i.e. the KAN-189 defect"
        IllegalStateException ex = thrown()
        ex.message.contains(IlrDatabaseRoles.OWNER_ROLE)
    }

    def "the guard accepts the application role"() {
        when:
        IlrDatabaseRoles.assertRuntimeRoleIsNotSuperuser(IlrPostgresContainer.appDataSource())

        then:
        noExceptionThrown()
    }

    def "counterfactual 1: a superuser connection reads every tenant's rows, despite ENABLE and FORCE"() {
        when: "no tenant bound at all — the exact condition under which the app role sees nothing"
        int superuserVisible = IlrPostgresContainer.withRole(
                IlrPostgresContainer.instance().username,
                IlrPostgresContainer.instance().password) { Connection c ->
            scalarInt(c, 'SELECT count(*) FROM harness_cases')
        }
        int appVisible = IlrPostgresContainer.withAppRole { Connection c ->
            scalarInt(c, 'SELECT count(*) FROM harness_cases')
        }

        then: "the superuser sees all three seeded rows across both tenants; the app role sees none"
        superuserVisible == 3
        appVisible == 0

        and: """this is the whole point. A cross-tenant isolation test written over the bootstrap
                connection would have been asserting against an unfiltered table, and would have
                reported green whether the RLS policies were correct, wrong, or entirely absent."""
        superuserVisible != appVisible
    }

    def "counterfactual 2: V3's IF EXISTS role guard silently grants nothing when the role is absent"() {
        when: "the same conditional-grant construct every V3 migration uses, for a role that does not exist"
        IlrPostgresContainer.withOwnerRole { Connection c ->
            execute(c,
                    'DROP TABLE IF EXISTS guard_demo',
                    'CREATE TABLE guard_demo (id INT PRIMARY KEY)',
                    '''DO $$
                       BEGIN
                           IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'role_that_does_not_exist') THEN
                               GRANT SELECT ON guard_demo TO role_that_does_not_exist;
                           END IF;
                       END
                       $$;''')
        }

        then: "the statement succeeded — Flyway would record the migration as SUCCESS"
        noExceptionThrown()

        and: "having granted nothing whatsoever"
        !hasPrivilege(IlrDatabaseRoles.APP_ROLE, 'guard_demo', 'SELECT')

        and: """whereas the real V3, run against a container where IlrDatabaseRoles created ilr_app
                first, did take the branch. That difference is invisible in Flyway's output, which is
                why the harness owns role creation rather than leaving it to each service's spec."""
        hasPrivilege(IlrDatabaseRoles.APP_ROLE, 'harness_cases', 'SELECT')

        cleanup:
        IlrPostgresContainer.withOwnerRole { Connection c -> execute(c, 'DROP TABLE IF EXISTS guard_demo') }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean hasPrivilege(String role, String table, String privilege) {
        IlrPostgresContainer.withOwnerRole { Connection c ->
            scalarBoolean(c, "SELECT has_table_privilege('${role}', '${table}', '${privilege}')")
        }
    }

    private static void execute(Connection connection, String... statements) {
        connection.autoCommit = true
        connection.createStatement().withCloseable { Statement statement ->
            statements.each { statement.execute(it) }
        }
    }

    private static Integer scalarInt(Connection connection, String sql) {
        scalar(connection, sql) { ResultSet rs -> rs.getInt(1) }
    }

    private static Boolean scalarBoolean(Connection connection, String sql) {
        scalar(connection, sql) { ResultSet rs -> rs.getBoolean(1) }
    }

    private static <T> T scalar(Connection connection, String sql, Closure<T> mapper) {
        connection.createStatement().withCloseable { Statement statement ->
            ResultSet rs = statement.executeQuery(sql)
            return rs.next() ? mapper.call(rs) : null
        }
    }
}
