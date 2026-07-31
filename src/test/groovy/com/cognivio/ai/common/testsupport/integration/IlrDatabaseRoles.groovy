package com.cognivio.ai.common.testsupport.integration

import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.JdbcDatabaseContainer

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Creates, inside the harness container, the two-role topology every ILR service's
 * {@code V3__force_rls_and_app_role.sql} migration is written against, and then
 * <b>asserts that tests actually connect as the right one</b>.
 *
 * <h2>Why a role split is not optional</h2>
 * A stock {@code PostgreSQLContainer} hands out one login: the bootstrap user, which is a true
 * Postgres <b>superuser</b>. An integration test that simply points Spring at
 * {@code container.getJdbcUrl()}/{@code getUsername()} — the obvious thing to write, and what
 * almost every Testcontainers example shows — is broken in two ways at once, both silent:
 *
 * <ol>
 *   <li><b>The GRANT branch never executes.</b> Every service's V3 migration wraps its grants in
 *       {@code IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ilr_app')}, because {@code ilr_app}
 *       genuinely does not exist in some environments. With no such role in the container the branch
 *       is skipped, Flyway reports the migration as {@code SUCCESS}, and a "migrations apply cleanly"
 *       test passes having verified nothing about the grants that production depends on.</li>
 *   <li><b>RLS is bypassed entirely.</b> A superuser bypasses row security <em>unconditionally</em> —
 *       {@code FORCE ROW LEVEL SECURITY} does not apply to it, because FORCE only removes the
 *       <em>owner's</em> exemption, not the superuser's. So a cross-tenant isolation assertion run on
 *       the bootstrap connection is testing a database with RLS effectively switched off. It will
 *       report green whether the policies are correct, wrong, or absent.</li>
 * </ol>
 *
 * That combination is precisely the false-assurance failure mode KAN-190 was raised about, so the
 * harness must not merely avoid it — it must make it impossible to fall into accidentally. Hence
 * {@link #assertRuntimeRoleIsNotSuperuser}, which {@code AbstractIlrIntegrationSpec} runs before
 * every feature, on the real application {@link DataSource}. A harness that has been misconfigured
 * fails loudly on its first test rather than quietly passing all of them.
 *
 * <h2>The topology</h2>
 * <ul>
 *   <li>{@link #OWNER_ROLE} — {@code NOSUPERUSER CREATEDB}. Owns the schema objects; what Flyway
 *       runs as. Deliberately not a superuser, so it mirrors a real RDS master user (which is
 *       <em>not</em> a true Postgres superuser) and remains subject to {@code FORCE}.</li>
 *   <li>{@link #APP_ROLE} — {@code NOSUPERUSER}, owns nothing. What the application connects as, and
 *       therefore subject to RLS unconditionally. The name must match the V3 migrations
 *       character-for-character or the {@code IF EXISTS} branch no-ops again.</li>
 * </ul>
 *
 * @see IlrPostgresContainer
 * @see AbstractIlrIntegrationSpec
 */
final class IlrDatabaseRoles {

    /**
     * The migration/schema-owning role. Flyway runs as this.
     *
     * <p>{@code CREATEDB} matches the production owner's grant set; it is not used by the harness
     * itself but keeps the role shaped like the real one.
     */
    static final String OWNER_ROLE = 'ilr_owner'

    /** Password for {@link #OWNER_ROLE}. Test-only credentials for a throwaway container. */
    static final String OWNER_PASSWORD = 'ilr_owner_test_pw'

    /**
     * The application runtime role.
     *
     * <p><b>This literal is load-bearing.</b> It must equal the {@code rolname} tested by every
     * service's {@code V3__force_rls_and_app_role.sql}. If the estate ever renames the runtime role,
     * this constant and those migrations must change together — otherwise the grants silently stop
     * being applied under test and the migration tests go back to proving nothing.
     */
    static final String APP_ROLE = 'ilr_app'

    /** Password for {@link #APP_ROLE}. Test-only credentials for a throwaway container. */
    static final String APP_PASSWORD = 'ilr_app_test_pw'

    private IlrDatabaseRoles() {
        throw new AssertionError('static helper; not instantiable')
    }

    /**
     * Creates {@link #OWNER_ROLE} and {@link #APP_ROLE} in {@code container}, connecting as the
     * bootstrap superuser. This is the only place the harness uses the superuser connection, and it
     * runs no assertions — it exists purely to create the roles the assertions then run as.
     *
     * <p>Postgres 15+ revoked {@code CREATE} on schema {@code public} from {@code PUBLIC}, so the
     * owner is granted it explicitly or Flyway cannot create anything. {@link #APP_ROLE} gets
     * {@code USAGE} only: it must never be able to create objects, because an object it created it
     * would own, and an owner is exempt from RLS unless FORCE is set — which would reintroduce the
     * bypass by the back door.
     */
    static void createIn(JdbcDatabaseContainer<?> container) {
        withConnection(container, container.username, container.password) { Connection root ->
            execute(root,
                    "DROP ROLE IF EXISTS ${APP_ROLE}",
                    "DROP ROLE IF EXISTS ${OWNER_ROLE}",
                    "CREATE ROLE ${OWNER_ROLE} LOGIN PASSWORD '${OWNER_PASSWORD}' NOSUPERUSER CREATEDB",
                    "CREATE ROLE ${APP_ROLE} LOGIN PASSWORD '${APP_PASSWORD}' NOSUPERUSER",
                    "GRANT CREATE, USAGE ON SCHEMA public TO ${OWNER_ROLE}",
                    "GRANT USAGE ON SCHEMA public TO ${APP_ROLE}")
        }
    }

    /**
     * Fails loudly unless the connection this {@link DataSource} hands out is a non-superuser role
     * that is not the migration owner — i.e. unless RLS can actually engage for it.
     *
     * <p>Called by {@link AbstractIlrIntegrationSpec#setup} on every feature, against the real
     * application datasource, so it covers a subclass that overrides
     * {@code spring.datasource.username} as well as a mistake in the harness itself.
     *
     * @throws IllegalStateException with a diagnosis, never a bare assertion failure — the whole
     *         point is that whoever hits this understands why their green test was meaningless
     */
    static void assertRuntimeRoleIsNotSuperuser(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalStateException(
                    'ilr-common integration harness: no DataSource was injected. The Spring context did ' +
                            'not start — check that the spec extends AbstractIlrIntegrationSpec and that its ' +
                            '@ContextConfiguration is intact (see the comment on that class).')
        }
        dataSource.connection.withCloseable { Connection connection ->
            assertConnectionRoleIsNotSuperuser(connection)
        }
    }

    /**
     * Connection-level form of {@link #assertRuntimeRoleIsNotSuperuser(DataSource)}.
     *
     * <p>Deliberately a different method name rather than an overload: Groovy resolves overloads
     * dynamically by runtime argument type, so a {@code null} argument to
     * {@code assertRuntimeRoleIsNotSuperuser(...)} would fail as an ambiguous-dispatch error rather
     * than as the diagnosis above — turning a clear "your context did not start" message into a
     * confusing one, on the exact code path whose job is to be clear.
     */
    static void assertConnectionRoleIsNotSuperuser(Connection connection) {
        String role = currentUser(connection)
        Boolean superuser = currentUserIsSuperuser(connection)

        if (superuser == null) {
            throw new IllegalStateException(
                    "ilr-common integration harness: could not determine whether the connected role " +
                            "'${role}' is a superuser (no pg_user row). Refusing to run integration tests " +
                            "whose RLS assertions would be unverifiable.")
        }
        if (superuser) {
            throw new IllegalStateException(
                    "ilr-common integration harness: tests are connected as '${role}', which is a Postgres " +
                            "SUPERUSER. A superuser bypasses row-level security unconditionally (FORCE ROW " +
                            "LEVEL SECURITY removes the owner's exemption, not the superuser's), and the " +
                            "IF EXISTS(...'${APP_ROLE}') branch in every V3__force_rls_and_app_role.sql is " +
                            "skipped when that role does not exist. Every tenant-isolation and GRANT " +
                            "assertion in this suite would therefore pass regardless of whether the policies " +
                            "are correct. Connect as '${APP_ROLE}' (see IlrDatabaseRoles) — do NOT point " +
                            "spring.datasource.username at the Testcontainers bootstrap user.")
        }
        if (role == OWNER_ROLE) {
            throw new IllegalStateException(
                    "ilr-common integration harness: tests are connected as the migration/owner role " +
                            "'${OWNER_ROLE}'. The schema owner is exempt from RLS on any table that has not " +
                            "had FORCE ROW LEVEL SECURITY applied, so a table missing FORCE — the exact " +
                            "KAN-189 defect — would not be detected. Connect as '${APP_ROLE}'.")
        }
    }

    /** The role the connection is authenticated as. */
    static String currentUser(Connection connection) {
        querySingle(connection, 'SELECT current_user') { ResultSet rs -> rs.getString(1) }
    }

    /**
     * Whether the connected role is a Postgres superuser, or {@code null} when the role has no
     * {@code pg_user} row (which the caller must treat as "unknown", i.e. unsafe).
     */
    static Boolean currentUserIsSuperuser(Connection connection) {
        querySingle(connection, 'SELECT usesuper FROM pg_user WHERE usename = current_user') { ResultSet rs ->
            (Boolean) rs.getBoolean('usesuper')
        }
    }

    /** A {@link DataSource} for an arbitrary role against the harness container. */
    static DataSource dataSourceFor(JdbcDatabaseContainer<?> container, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(container.jdbcUrl, username, password)
        dataSource.driverClassName = container.driverClassName
        return dataSource
    }

    /** A {@link DataSource} for {@link #APP_ROLE} — the role integration tests must run as. */
    static DataSource appDataSource(JdbcDatabaseContainer<?> container) {
        dataSourceFor(container, APP_ROLE, APP_PASSWORD)
    }

    /** A {@link DataSource} for {@link #OWNER_ROLE} — the role Flyway runs as. */
    static DataSource ownerDataSource(JdbcDatabaseContainer<?> container) {
        dataSourceFor(container, OWNER_ROLE, OWNER_PASSWORD)
    }

    private static <T> T withConnection(JdbcDatabaseContainer<?> container, String user, String password,
                                        Closure<T> body) {
        dataSourceFor(container, user, password).connection.withCloseable { Connection c -> body.call(c) }
    }

    private static void execute(Connection connection, String... statements) {
        connection.autoCommit = true
        connection.createStatement().withCloseable { Statement statement ->
            statements.each { statement.execute(it) }
        }
    }

    private static <T> T querySingle(Connection connection, String sql, Closure<T> extractor) {
        connection.createStatement().withCloseable { Statement statement ->
            ResultSet rs = statement.executeQuery(sql)
            return rs.next() ? extractor.call(rs) : null
        }
    }
}
