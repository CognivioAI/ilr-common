package com.cognivio.ai.common.tenancy

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

/**
 * KAN-189: proves the defence-in-depth property ADR-011 claims for Postgres RLS, rather than just
 * asserting the policy SQL exists. {@link RlsTenantBindingAspect}'s javadoc identifies this as the
 * missing verification — "end-to-end proof that RLS then filters rows requires a Postgres
 * integration test (Testcontainers)".
 *
 * <p>Two roles are created, mirroring the real topology every service migration now uses:
 * <ul>
 *   <li>{@code owner} — the schema-owning/migration role (what Flyway runs as). Deliberately
 *       {@code NOSUPERUSER}, matching a real RDS master user (which is <b>not</b> a true Postgres
 *       superuser) rather than the Testcontainers bootstrap role, which is — a true superuser
 *       bypasses row security regardless of FORCE, so testing against one would prove nothing.</li>
 *   <li>{@code app_role} — the non-owner runtime role the application actually connects as.</li>
 * </ul>
 *
 * <p>{@code cases} carries the fixed shape (ENABLE + FORCE + non-owner role). {@code cases_pre_fix}
 * reproduces the estate's state <em>before</em> KAN-189 (ENABLE only, same owner role for
 * everything) so the regression this ticket fixes is pinned by an assertion, not just described.
 *
 * <p><b>Renamed from {@code RlsForceEnforcementSpec} by KAN-222.</b> It needs Docker, so leaving it
 * as a {@code *Spec} under surefire would have kept {@code mvn test} Docker-dependent — which is the
 * whole justification for KAN-222 putting integration tests behind maven-failsafe at {@code verify}.
 * The body is otherwise unchanged. It deliberately keeps its own per-spec container rather than
 * using {@code IlrPostgresContainer}, because it builds a synthetic pre-fix schema that must not be
 * visible to any other test.
 */
class RlsForceEnforcementIT extends Specification {

    private static final String TENANT_A = "11111111-1111-1111-1111-111111111111"
    private static final String TENANT_B = "22222222-2222-2222-2222-222222222222"

    @Shared
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))

    def setupSpec() {
        postgres.start()

        // Bootstrap: the Testcontainers default user IS a true superuser — used only to create the
        // two roles the rest of this spec actually exercises, never to run assertions against.
        withConnection(postgres.username, postgres.password) { Connection root ->
            exec(root,
                    "CREATE ROLE owner LOGIN PASSWORD 'owner' NOSUPERUSER CREATEDB",
                    "CREATE ROLE app_role LOGIN PASSWORD 'app_role' NOSUPERUSER",
                    "GRANT CREATE ON SCHEMA public TO owner")
        }

        // Everything from here runs as `owner`, exactly like Flyway does in the real services.
        withConnection("owner", "owner") { Connection c ->
            exec(c,
                    // The fixed shape (KAN-189): ENABLE + FORCE + a non-owner runtime role. Seed
                    // BEFORE forcing — the owner is still exempt at that point (matches how a real
                    // V1 migration seeds demo data as owner, then a later migration forces RLS).
                    """CREATE TABLE cases (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         tenant_id UUID NOT NULL,
                         name TEXT NOT NULL)""",
                    "ALTER TABLE cases ENABLE ROW LEVEL SECURITY",
                    """CREATE POLICY tenant_isolation ON cases
                         USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid)""",
                    "GRANT SELECT, INSERT, UPDATE, DELETE ON cases TO app_role",
                    seedSql("cases"),
                    "ALTER TABLE cases FORCE ROW LEVEL SECURITY",

                    // The pre-KAN-189 shape: ENABLE only, no FORCE, same owner role for
                    // migration AND runtime (mirrors every V1__create_*_tables.sql before this fix).
                    """CREATE TABLE cases_pre_fix (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         tenant_id UUID NOT NULL,
                         name TEXT NOT NULL)""",
                    "ALTER TABLE cases_pre_fix ENABLE ROW LEVEL SECURITY",
                    """CREATE POLICY tenant_isolation ON cases_pre_fix
                         USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid)""",
                    seedSql("cases_pre_fix"))
        }
    }

    def cleanupSpec() {
        postgres.stop()
    }

    def "the non-owner app role, tenant bound, sees only that tenant's row"() {
        given:
        def initializer = new TenantSessionInitializer()

        expect: "set_config(..., is_local => true) only survives for the transaction it was set in"
        withConnection("app_role", "app_role") { Connection c ->
            c.autoCommit = false
            initializer.apply(c, UUID.fromString(TENANT_A))
            def result = visibleNames(c, "cases") == ["tenant-A case"]
            c.commit()
            result
        }
    }

    def "a skipped tenant bind — the exact application-layer bug this defends against — fails CLOSED, not open"() {
        expect: "no set_config call at all, simulating RlsTenantBindingAspect never having run"
        withConnection("app_role", "app_role") { Connection c ->
            visibleNames(c, "cases").isEmpty()
        }
    }

    def "FORCE ROW LEVEL SECURITY also protects an owner/migration-role connection with no tenant bound"() {
        expect: "the schema-owning role is no longer exempt on the fixed table"
        withConnection("owner", "owner") { Connection c ->
            visibleNames(c, "cases").isEmpty()
        }
    }

    def "the pre-fix shape (ENABLE only) is exactly the reported defect: the owner sees every tenant's rows unfiltered"() {
        expect: "reproduces KAN-189 — this is what 'RLS is inert' looked like before this fix"
        withConnection("owner", "owner") { Connection c ->
            visibleNames(c, "cases_pre_fix").size() == 2
        }
    }

    private static String seedSql(String table) {
        """INSERT INTO ${table} (tenant_id, name) VALUES
             ('${TENANT_A}', 'tenant-A case'),
             ('${TENANT_B}', 'tenant-B case')"""
    }

    private static void exec(Connection c, String... statements) {
        c.autoCommit = true
        c.createStatement().withCloseable { Statement s ->
            statements.each { s.execute(it) }
        }
    }

    private static List<String> visibleNames(Connection c, String table) {
        List<String> names = []
        c.createStatement().withCloseable { Statement s ->
            ResultSet rs = s.executeQuery("SELECT name FROM ${table} ORDER BY name")
            while (rs.next()) {
                names << rs.getString("name")
            }
        }
        names
    }

    private <T> T withConnection(String user, String password, Closure<T> body) {
        Connection connection = DriverManager.getConnection(postgres.jdbcUrl, user, password)
        try {
            return body.call(connection)
        } finally {
            connection.close()
        }
    }
}
