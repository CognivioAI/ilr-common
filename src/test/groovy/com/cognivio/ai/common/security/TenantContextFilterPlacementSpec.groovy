package com.cognivio.ai.common.security

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration
import com.cognivio.ai.common.context.TenantContextFilter
import com.cognivio.ai.common.security.testsupport.ServletFilterOrderingRules
import com.tngtech.archunit.core.importer.ClassFileImporter
import jakarta.servlet.Filter
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification

import java.time.Instant

/**
 * KAN-229: {@code TenantContextFilter} must run <b>inside</b> the Spring Security filter chain,
 * after {@code AuthorizationFilter} — never as a servlet-container filter ordered relative to it.
 *
 * <h2>The defect this guards</h2>
 * The filter used to be registered with the container via a {@code FilterRegistrationBean} carrying
 * {@code setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 10)}. Under the AWS serverless adapter
 * that order is not applied and not merely ignored — {@code ServerlessServletContext} holds its
 * registrations in a plain {@code HashMap} with no order field at all, so the value is destroyed at
 * registration time. The filter then ran <em>before</em> Spring Security, so
 * {@code SecurityContextHolder.getContext().getAuthentication()} was always {@code null} and
 * {@code TenantContext} was silently never populated on Lambda: no tenant claim, no RLS binding,
 * no exception and no log line. Placement inside the chain removes the dependency on container
 * ordering entirely.
 *
 * <p>Every case below builds the REAL chain from the REAL auto-configuration and asserts on
 * {@code SecurityFilterChain.getFilters()}, because the relative order of two filters is precisely
 * what no compiler, and no filter-level unit test, can check.
 */
class TenantContextFilterPlacementSpec extends Specification {

    def contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IlrTenantContextAutoConfiguration,
                    IlrSecurityAutoConfiguration,
                    IlrSecurityDisabledAutoConfiguration))
            .withUserConfiguration(WebSecurityInfra)

    def "runs after AuthorizationFilter in the secured chain (verified-JWT posture)"() {
        expect:
        contextRunner
                .withPropertyValues("ilr.security.enabled=true")
                .withUserConfiguration(StubJwtDecoder)
                .run { context ->
                    assertTenantFilterRunsAfterAuthorization(context)
                }
    }

    def "runs after AuthorizationFilter under dev-permit-all"() {
        expect:
        contextRunner
                .withPropertyValues(
                        "ilr.security.enabled=true",
                        "ilr.security.dev-permit-all=true")
                .run { context ->
                    assertTenantFilterRunsAfterAuthorization(context)
                }
    }

    def "runs after AuthorizationFilter in the permit-all chain installed when security is disabled"() {
        expect:
        contextRunner
                .withPropertyValues("ilr.security.enabled=false")
                .run { context ->
                    assertTenantFilterRunsAfterAuthorization(context)
                }
    }

    // -----------------------------------------------------------------------------------------
    // The double-registration trap. Do NOT "clean up" the disabled FilterRegistrationBean: without
    // it, Spring Boot auto-registers the plain TenantContextFilter bean with the servlet container,
    // that copy runs first under the adapter (auth still absent, so it is a no-op), and — being a
    // OncePerRequestFilter — it marks the request already-filtered, so the in-chain invocation
    // asserted above is SKIPPED on every request. The fix would then compile, read correctly, pass
    // the ordering tests above, and do nothing in production.
    // -----------------------------------------------------------------------------------------
    def "the TenantContextFilter registration bean exists and is disabled, so the filter is never registered twice"() {
        expect:
        contextRunner
                .withPropertyValues("ilr.security.enabled=true")
                .withUserConfiguration(StubJwtDecoder)
                .run { context ->
                    def registrations = context.getBeansOfType(FilterRegistrationBean).values()
                            .findAll { it.getFilter() instanceof TenantContextFilter }

                    assert !registrations.isEmpty():
                            'no FilterRegistrationBean wraps TenantContextFilter — if the explicit ' +
                                    'registration was removed, Spring Boot auto-registers the filter bean ' +
                                    'with the container instead and the in-chain copy is skipped (KAN-229)'
                    assert registrations.every { !it.isEnabled() }:
                            'a TenantContextFilter is registered at the servlet-container level; the ' +
                                    'OncePerRequestFilter guard then skips the in-chain invocation (KAN-229)'
                }
    }

    def "no ILR-owned filter is registered at the servlet-container level at all"() {
        expect: "the mechanism-independent invariant: this estate never depends on outer-chain ordering"
        contextRunner
                .withPropertyValues("ilr.security.enabled=true")
                .withUserConfiguration(StubJwtDecoder)
                .run { context ->
                    def enabledIlrRegistrations = context.getBeansOfType(FilterRegistrationBean).values()
                            .findAll { it.isEnabled() }
                            .findAll { it.getFilter()?.getClass()?.getName()?.startsWith("com.cognivio.ai") }

                    assert enabledIlrRegistrations.isEmpty():
                            "ILR filters registered with the servlet container: " +
                                    enabledIlrRegistrations.collect { it.getFilter().getClass().getName() }
                }
    }

    def "no production class declares a servlet-container filter order"() {
        expect: "the ArchUnit guard that keeps the KAN-229 ordering dependency from being reintroduced"
        ServletFilterOrderingRules.assertNoRegistrationBeanSetOrder("com.cognivio.ai.common")
    }

    def "that guard actually fires — it is not a rule that can never fail"() {
        when: "checked against the deliberate violation fixture (test-only, never shipped)"
        ServletFilterOrderingRules.NO_REGISTRATION_BEAN_SET_ORDER.check(
                new ClassFileImporter().importPackages("com.cognivio.ai.common.security.violationfixture"))

        then:
        AssertionError violation = thrown()
        violation.message.contains("RegistrationBean.setOrder(int)")
        violation.message.contains("KAN-229")
    }

    private static void assertTenantFilterRunsAfterAuthorization(AssertableWebApplicationContext context) {
        assert context.startupFailure == null
        List<Filter> filters = context.getBean(SecurityFilterChain).getFilters()

        int tenantIndex = filters.indexOf(context.getBean(TenantContextFilter))
        int authorizationIndex = filters.findIndexOf { it instanceof AuthorizationFilter }

        assert tenantIndex >= 0:
                'TenantContextFilter is not in the Spring Security chain at all — on Lambda it would ' +
                        'run at an arbitrary point in the container chain, before authentication (KAN-229). ' +
                        'Chain: ' + filters.collect { it.getClass().getSimpleName() }
        assert authorizationIndex >= 0:
                'no AuthorizationFilter in the chain; the assertion below would be vacuous'
        assert tenantIndex > authorizationIndex:
                'TenantContextFilter runs BEFORE AuthorizationFilter, so no Authentication is in the ' +
                        'SecurityContextHolder yet and TenantContext is never populated (KAN-229). ' +
                        'Chain: ' + filters.collect { it.getClass().getSimpleName() }
    }

    /**
     * The minimum web-security infrastructure needed to refresh the context. Note the absence of an
     * {@code mvcHandlerMappingIntrospector} bean: since KAN-226 the permit list is built from
     * pre-constructed {@code RequestMatcher}s, so nothing here resolves MVC matchers. Reintroducing
     * {@code requestMatchers(String...)} in production would make the context fail with "A Bean named
     * mvcHandlerMappingIntrospector ... is required" — which is a correct failure, not a missing stub.
     */
    @EnableWebSecurity
    static class WebSecurityInfra {

        /**
         * {@code TenantContextFilter} renders {@code MissingTenantClaimException} through the MVC
         * exception resolver; supply a no-op one so the context can refresh without pulling in full
         * WebMvc auto-configuration.
         */
        @Bean
        HandlerExceptionResolver handlerExceptionResolver() {
            return { request, response, handler, ex -> null } as HandlerExceptionResolver
        }
    }

    /**
     * Exercises the decoder-present branch without a live Cognito JWKS endpoint — the
     * auto-configuration resolves the decoder through an {@code ObjectProvider}, so a bean here
     * is enough. Never actually invoked: these specs assert on chain composition, not on requests.
     */
    static class StubJwtDecoder {

        @Bean
        JwtDecoder jwtDecoder() {
            return { String token ->
                new Jwt(token, Instant.now(), Instant.now().plusSeconds(60),
                        [alg: "none"], [sub: "test"])
            } as JwtDecoder
        }
    }
}
