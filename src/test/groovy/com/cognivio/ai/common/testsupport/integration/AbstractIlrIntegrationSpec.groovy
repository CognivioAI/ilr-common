package com.cognivio.ai.common.testsupport.integration

import com.cognivio.ai.common.authz.IlrRole
import com.cognivio.ai.common.security.IlrSecurityProperties
import com.cognivio.ai.common.support.TestJwtFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.RequestPostProcessor
import spock.lang.Specification

import javax.sql.DataSource
import java.util.function.Supplier

/**
 * Base class for every ILR integration test: a <b>real</b> Spring Boot context, on a <b>real</b>
 * PostgreSQL, with the <b>real</b> security filter chain, connecting as a role that RLS actually
 * applies to.
 *
 * <pre>{@code
 * class CaseSignOffIT extends AbstractIlrIntegrationSpec {
 *
 *     def "a consultant may sign off a case in their own tenant"() {
 *         expect:
 *         mockMvc.perform(post("/cases/${caseId}/sign-off").with(bearerFor(IlrRole.CONSULTANT)))
 *                .andExpect(status().isOk())
 *     }
 * }
 * }</pre>
 *
 * <h2>What this base class forces, and why it forces rather than defaults</h2>
 * The four properties below are set through {@link DynamicPropertySource}, which registers a
 * property source ahead of {@code application.yml}, {@code @TestPropertySource} and profile files.
 * That is deliberate and is the defining property of this harness:
 *
 * <ul>
 *   <li>{@code ilr.security.enabled=true} — the estate's entire existing test suite runs with this
 *       {@code false} (see {@code IlrMethodSecurityAutoConfiguration}'s javadoc), i.e. against the
 *       permit-all chain from {@code IlrSecurityDisabledAutoConfiguration}. A security test run in
 *       that configuration asserts nothing.</li>
 *   <li>{@code ilr.security.method-security.enabled=true} — otherwise every {@code @PreAuthorize} is
 *       inert and a "403 for the wrong role" expectation would be untestable.</li>
 *   <li>{@code ilr.security.dev-permit-all=false} — under dev-permit-all the chain permits every
 *       request and {@code TenantContextFilter} seeds a synthetic identity holding <em>all</em>
 *       roles. Left on, every authorization assertion in the suite would pass unconditionally.</li>
 *   <li>{@code ilr.rls.enabled=true} — several services ship {@code ilr.rls.enabled: false}
 *       ("staged OFF until the tenant claim is delivered end-to-end"). With it off,
 *       {@code RlsTenantBindingAspect} is never created, no tenant is ever bound, and a
 *       cross-tenant assertion is testing a disabled feature.</li>
 * </ul>
 *
 * Each of those is a way a per-spec override could quietly re-create the false assurance KAN-190
 * documents, so none of them is left to the spec author. A subclass that genuinely needs one of them
 * different is asking for something this base class is not for.
 *
 * <p>{@code spring.jpa.hibernate.ddl-auto=validate} is restated here for the same reason. It is
 * already the application default, but restating it means a service cannot weaken it under test —
 * and because the schema comes from real Flyway migrations, it is what turns a bare context-load
 * test into a check of every entity-to-column mapping in the service.
 *
 * <h2>The role split</h2>
 * Flyway connects as {@code ilr_owner}; the application connects as {@code ilr_app}. Reproducing
 * that separation is what makes RLS and the conditional GRANTs in
 * {@code V3__force_rls_and_app_role.sql} exercisable at all — see {@link IlrDatabaseRoles} for the
 * full explanation, which is the single most important thing to understand about this harness.
 * {@link #setup} re-checks it before every feature.
 *
 * <h2>Required dependencies in a consuming service</h2>
 * {@code ilr-common}'s test dependencies are {@code test}-scoped and therefore not transitive. A
 * service using this harness must declare, in its own pom:
 *
 * <pre>{@code
 * <dependency>
 *   <groupId>com.cognivio.ai</groupId><artifactId>ilr-common</artifactId>
 *   <version>0.1.0-SNAPSHOT</version><classifier>tests</classifier><scope>test</scope>
 * </dependency>
 * <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
 * <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
 * <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
 * }</pre>
 *
 * plus {@code maven-failsafe-plugin} bound to {@code integration-test} and {@code verify} with
 * {@code **}{@code /*IT.class} included (copy the block from {@code ilr-common}'s pom). Web, JPA,
 * Flyway and Spock are assumed present already, since every service has them.
 *
 * <h2>Naming</h2>
 * Subclasses are named {@code *IT}, not {@code *Spec}: surefire runs {@code *Spec} at {@code test}
 * and failsafe runs {@code *IT} at {@code verify}, so {@code mvn test} stays Docker-free and CI —
 * which already runs {@code mvn verify} in all 26 workflows — picks these up with no workflow change.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(IlrSecurityTestConfiguration)
// -----------------------------------------------------------------------------------------------
// @ContextConfiguration is REQUIRED here and must not be removed as redundant. It looks redundant
// next to @SpringBootTest, and under JUnit it would be.
//
// spock-spring 2.3 decides whether a Specification is a Spring spec in SpringExtension.isSpringSpec.
// Its primary check calls org.springframework.test.util.MetaAnnotationUtils, which Spring Framework 6
// DELETED. That reflective lookup therefore resolves to null and the check is skipped, leaving only
// the fallback — which searches for @ContextConfiguration or @ContextHierarchy and nothing else.
// @SpringBootTest is not meta-annotated with either, so on Spring Boot 3 / Spock 2.3 a spec carrying
// only @SpringBootTest is silently NOT recognised: no application context is ever created, every
// @Autowired field stays null, and the failure surfaces much later as an unrelated NPE inside a
// setup method. Nothing warns.
//
// That is a false-assurance trap of exactly the kind this harness exists to eliminate, so it is
// absorbed here, once, rather than being rediscovered in each of the 16 services.
// -----------------------------------------------------------------------------------------------
@ContextConfiguration
abstract class AbstractIlrIntegrationSpec extends Specification {

    /** Tenant A. See {@link IlrBearerTokens#DEFAULT_TENANT_ID}. */
    protected static final UUID TENANT_A = IlrBearerTokens.DEFAULT_TENANT_ID

    /** Tenant B, for cross-tenant isolation assertions. See {@link IlrBearerTokens#OTHER_TENANT_ID}. */
    protected static final UUID TENANT_B = IlrBearerTokens.OTHER_TENANT_ID

    @Autowired
    protected MockMvc mockMvc

    /** The application datasource — connected as {@code ilr_app}, verified in {@link #setup}. */
    @Autowired
    protected DataSource dataSource

    @Autowired
    protected IlrBearerTokens tokens

    @Autowired
    protected TestJwtFactory jwtFactory

    @Autowired
    protected IlrSecurityProperties securityProperties

    /**
     * Points the context at the shared container and pins the security/RLS posture.
     *
     * <p>Spring collects {@code @DynamicPropertySource} methods from the whole test-class hierarchy,
     * so subclasses inherit this without restating it, and may add their own for service-specific
     * properties (e.g. {@code spring.flyway.locations}).
     */
    @DynamicPropertySource
    static void ilrIntegrationHarnessProperties(DynamicPropertyRegistry registry) {
        // Touching the container class here is what triggers its static initialiser, so the container
        // and its roles are ready before Spring resolves the datasource.
        registry.add('spring.datasource.url', { IlrPostgresContainer.jdbcUrl() } as Supplier)
        registry.add('spring.datasource.driver-class-name', { IlrPostgresContainer.driverClassName() } as Supplier)

        // The application runs as the non-owner, non-superuser role, exactly like production.
        registry.add('spring.datasource.username', { IlrDatabaseRoles.APP_ROLE } as Supplier)
        registry.add('spring.datasource.password', { IlrDatabaseRoles.APP_PASSWORD } as Supplier)

        // Flyway runs as the schema owner, exactly like production. Spring Boot supports Flyway
        // credentials distinct from the application datasource; this split is the whole point.
        registry.add('spring.flyway.enabled', { 'true' } as Supplier)
        registry.add('spring.flyway.url', { IlrPostgresContainer.jdbcUrl() } as Supplier)
        registry.add('spring.flyway.user', { IlrDatabaseRoles.OWNER_ROLE } as Supplier)
        registry.add('spring.flyway.password', { IlrDatabaseRoles.OWNER_PASSWORD } as Supplier)

        // The schema under test is the migrated one; Hibernate must agree with it, not create it.
        registry.add('spring.jpa.hibernate.ddl-auto', { 'validate' } as Supplier)

        // Non-negotiable security posture — see the class javadoc.
        registry.add('ilr.security.enabled', { 'true' } as Supplier)
        registry.add('ilr.security.method-security.enabled', { 'true' } as Supplier)
        registry.add('ilr.security.dev-permit-all', { 'false' } as Supplier)
        registry.add('ilr.rls.enabled', { 'true' } as Supplier)
    }

    /**
     * Re-asserts the harness's own preconditions before every feature.
     *
     * <p>This is not defensive clutter. Both checks cover a configuration in which the entire suite
     * would pass while proving nothing: connecting as a superuser silently disables RLS and skips the
     * conditional GRANTs (see {@link IlrDatabaseRoles}), and {@code dev-permit-all} silently grants
     * every caller every role. Checking here — on the real, injected {@link DataSource} and the real
     * bound properties — also covers a subclass that overrode them, which a one-off check in the
     * container setup would not.
     */
    def setup() {
        IlrDatabaseRoles.assertRuntimeRoleIsNotSuperuser(dataSource)

        assert securityProperties.enabled:
                'ilr.security.enabled resolved to false inside an integration test — the permit-all ' +
                        'chain is installed and every authorization assertion in this spec is vacuous.'
        assert !securityProperties.devPermitAll:
                'ilr.security.dev-permit-all resolved to true inside an integration test — every ' +
                        'request is permitted and TenantContextFilter seeds an identity holding all roles.'
    }

    /** A bearer token for {@code role} in tenant A. See {@link IlrBearerTokens}. */
    protected RequestPostProcessor bearerFor(IlrRole role) {
        tokens.bearerFor(role)
    }

    /** A bearer token for {@code role} in an explicit tenant. */
    protected RequestPostProcessor bearerFor(IlrRole role, UUID tenantId) {
        tokens.bearerFor(role, tenantId)
    }

    /** A bearer token for {@code role} as an explicit user in an explicit tenant. */
    protected RequestPostProcessor bearerFor(IlrRole role, UUID tenantId, UUID userId) {
        tokens.bearerFor(role, tenantId, userId)
    }

    /** A bearer token for a caller holding several roles in tenant A. */
    protected RequestPostProcessor bearerForRoles(Collection<IlrRole> roles) {
        tokens.bearerForRoles(roles)
    }

    /** A bearer token with an arbitrary claim set, for negative cases. See {@link IlrBearerTokens#bearerForClaims}. */
    protected RequestPostProcessor bearerForClaims(Map<String, Object> claims) {
        tokens.bearerForClaims(claims)
    }
}
