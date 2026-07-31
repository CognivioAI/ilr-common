package com.cognivio.ai.common.testsupport.integration

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

import javax.sql.DataSource
import java.sql.Connection

/**
 * The one PostgreSQL container the whole integration suite shares.
 *
 * <h2>Singleton, not per-spec</h2>
 * Started once per JVM from a static initialiser and never stopped — Testcontainers' Ryuk sidecar
 * removes it when the JVM exits. This is a deliberate departure from
 * {@code RlsForceEnforcementIT}, which uses {@code @Shared} + {@code setupSpec}/{@code cleanupSpec}.
 * That is the right lifecycle for a single self-contained spec, but it starts and stops a container
 * per <em>specification</em>. A service with five integration specs would pay five container starts
 * (~5-15s each) on every build, in every one of 16 repositories. The singleton pays it once.
 *
 * <p>The image tag is pinned, not floated: {@code postgres:16-alpine}, matching KAN-189 exactly.
 * Integration tests whose whole purpose is to pin database-engine behaviour (RLS, FORCE, role
 * exemptions) must not silently change engine version between runs.
 *
 * <p>{@link IlrDatabaseRoles#createIn} runs as part of the same static initialisation, so by the
 * time any test can observe the container, {@code ilr_owner} and {@code ilr_app} exist. There is no
 * window in which a spec could connect before the roles are ready.
 *
 * <h2>Docker is required</h2>
 * These classes are only reachable from {@code *IT} tests, which run under maven-failsafe at
 * {@code verify}. {@code mvn test} does not touch them, so a developer without a Docker daemon still
 * has a working unit-test build.
 */
final class IlrPostgresContainer {

    /** Pinned engine image. Must stay in step with {@code RlsForceEnforcementIT}. */
    static final String IMAGE = 'postgres:16-alpine'

    /**
     * Started eagerly in the static initialiser. Class initialisation is thread-safe and happens
     * exactly once per class loader, which is what makes "singleton" true here without any explicit
     * locking or double-checked idiom.
     */
    private static final PostgreSQLContainer<?> INSTANCE = startWithRoles()

    private IlrPostgresContainer() {
        throw new AssertionError('static holder; not instantiable')
    }

    /** The shared, already-started container with {@code ilr_owner} / {@code ilr_app} created. */
    static PostgreSQLContainer<?> instance() {
        INSTANCE
    }

    /** JDBC URL of the shared container. */
    static String jdbcUrl() {
        INSTANCE.jdbcUrl
    }

    /** Driver class name, for property registration. */
    static String driverClassName() {
        INSTANCE.driverClassName
    }

    /** A {@link DataSource} connecting as {@link IlrDatabaseRoles#APP_ROLE}. */
    static DataSource appDataSource() {
        IlrDatabaseRoles.appDataSource(INSTANCE)
    }

    /** A {@link DataSource} connecting as {@link IlrDatabaseRoles#OWNER_ROLE} (what Flyway uses). */
    static DataSource ownerDataSource() {
        IlrDatabaseRoles.ownerDataSource(INSTANCE)
    }

    /**
     * A {@link DataSource} connecting as the container's bootstrap <b>superuser</b>.
     *
     * <p>Exposed only so the harness's own guard can be proved to fire against it (see
     * {@code HarnessFalseAssuranceIT}) and for one-off diagnostics. <b>Never</b> point
     * {@code spring.datasource.username} at this role: see {@link IlrDatabaseRoles} for why every
     * RLS assertion made over it is vacuous.
     */
    static DataSource superuserDataSource() {
        IlrDatabaseRoles.dataSourceFor(INSTANCE, INSTANCE.username, INSTANCE.password)
    }

    /** Runs {@code body} with a connection as the given role, closing it afterwards. */
    static <T> T withRole(String username, String password, Closure<T> body) {
        IlrDatabaseRoles.dataSourceFor(INSTANCE, username, password)
                .connection.withCloseable { Connection c -> body.call(c) }
    }

    /** Runs {@code body} with a connection as {@link IlrDatabaseRoles#APP_ROLE}. */
    static <T> T withAppRole(Closure<T> body) {
        withRole(IlrDatabaseRoles.APP_ROLE, IlrDatabaseRoles.APP_PASSWORD, body)
    }

    /** Runs {@code body} with a connection as {@link IlrDatabaseRoles#OWNER_ROLE}. */
    static <T> T withOwnerRole(Closure<T> body) {
        withRole(IlrDatabaseRoles.OWNER_ROLE, IlrDatabaseRoles.OWNER_PASSWORD, body)
    }

    private static PostgreSQLContainer<?> startWithRoles() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse(IMAGE))
        container.start()
        IlrDatabaseRoles.createIn(container)
        return container
    }
}
