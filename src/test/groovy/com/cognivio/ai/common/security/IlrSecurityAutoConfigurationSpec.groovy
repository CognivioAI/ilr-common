package com.cognivio.ai.common.security

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration
import com.cognivio.ai.common.security.testsupport.SecurityConfigurationRules
import com.tngtech.archunit.core.importer.ClassFileImporter
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification
import spock.lang.Unroll

/**
 * The security foundation must <b>fail closed</b>. When {@code ilr.security} is
 * enabled but no {@code JwtDecoder} / {@code issuer-uri} / {@code jwks-uri} is
 * configured, a service must REFUSE TO START rather than silently boot with every
 * endpoint permitted. The only way to run without token verification is the two
 * explicit opt-ins: {@code ilr.security.enabled=false} (tests) or
 * {@code ilr.security.dev-permit-all=true} (local dev).
 *
 * <p>This is the regression guard for the original fail-open default, where a
 * forgotten issuer URI in production would have disabled authentication entirely.
 */
class IlrSecurityAutoConfigurationSpec extends Specification {

    def contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IlrTenantContextAutoConfiguration,
                    IlrSecurityAutoConfiguration))
            .withUserConfiguration(WebSecurityInfra)

    def "fails to start when security is enabled but no issuer/jwks/decoder is configured"() {
        expect: "startup fails with the fail-closed IllegalStateException rather than booting permit-all"
        contextRunner
                .withPropertyValues("ilr.security.enabled=true")
                .run { context ->
                    assert context.startupFailure != null
                    def cause = rootCause(context.startupFailure, IllegalStateException)
                    assert cause != null
                    assert cause.message.contains("fail-closed")
                }
    }

    def "starts with a permit-all chain only when dev-permit-all is explicitly enabled"() {
        expect:
        contextRunner
                .withPropertyValues(
                        "ilr.security.enabled=true",
                        "ilr.security.dev-permit-all=true")
                .run { context ->
                    assert context.startupFailure == null
                    assert context.getBean(SecurityFilterChain) != null
                }
    }

    // ---------------------------------------------------------------------
    // KAN-226: the permit-list matchers must work under BOTH request shapes.
    //
    // ilr-eligibility-service deployed as a real Lambda 500'd on every request: the
    // String overload of requestMatchers() asks the ServletContext for its servlet
    // mappings, and the serverless adapter's synthetic ServletContext returns null
    // there. The fix builds AntPathRequestMatchers directly — but the obvious version
    // of that fix (AntPathRequestMatcher.antMatcher(pattern), no UrlPathHelper) fails
    // just as badly and far more quietly: the adapter hard-codes servletPath="", so a
    // servletPath-based matcher matches NOTHING and /actuator/health starts returning
    // 401 instead of being permitted. Neither failure is visible to the compiler, and
    // neither shows up in a normal-container test. Hence both shapes below.
    // ---------------------------------------------------------------------

    @Unroll
    def "permit-list matches '#requestUri' under the serverless adapter's request shape"() {
        given: "the adapter's ServerlessHttpServletRequest: empty servletPath and contextPath, full requestURI"
        def request = new MockHttpServletRequest("GET", requestUri)
        request.setServletPath("")
        request.setContextPath("")
        request.setPathInfo(null)

        expect:
        matchesPermitList(request) == permitted

        where:
        requestUri                     || permitted
        "/actuator/health"             || true
        "/actuator/health/liveness"    || true
        "/actuator/info"               || true
        "/v3/api-docs/swagger-config"  || true
        "/swagger-ui.html"             || true
        "/api/v1/assessments"          || false
        "/actuator/env"                || false
    }

    @Unroll
    def "permit-list matches '#requestUri' under a normal servlet container's request shape"() {
        given: "a real container behind a context path: servletPath set, requestURI = contextPath + servletPath"
        def request = new MockHttpServletRequest("GET", requestUri)
        request.setContextPath(contextPath)
        request.setServletPath(requestUri.substring(contextPath.length()))
        request.setPathInfo(null)

        expect:
        matchesPermitList(request) == permitted

        where:
        contextPath       | requestUri                                  || permitted
        "/eligibility/v1" | "/eligibility/v1/actuator/health"           || true
        "/eligibility/v1" | "/eligibility/v1/actuator/health/readiness" || true
        "/eligibility/v1" | "/eligibility/v1/v3/api-docs/x"             || true
        "/eligibility/v1" | "/eligibility/v1/api/v1/assessments"        || false
        ""                | "/actuator/health"                         || true
        ""                | "/api/v1/assessments"                      || false
    }

    def "a servletPath-based matcher — the tempting wrong fix — permits nothing under the adapter"() {
        given: "the adapter's request shape"
        def request = new MockHttpServletRequest("GET", "/actuator/health")
        request.setServletPath("")
        request.setContextPath("")
        request.setPathInfo(null)

        expect: "AntPathRequestMatcher.antMatcher(...) matches on servletPath + pathInfo, both empty here"
        !org.springframework.security.web.util.matcher.AntPathRequestMatcher
                .antMatcher("/actuator/health").matches(request)

        and: "so /actuator/health would 401 — this is why toPermitMatchers passes a UrlPathHelper"
        matchesPermitList(request)
    }

    def "the permit list is never wired through the requestMatchers(String...) overload"() {
        expect: "the ArchUnit guard that keeps the KAN-226 NPE from being reintroduced"
        SecurityConfigurationRules.assertNoStringRequestMatchers("com.cognivio.ai.common")
    }

    def "that guard actually fires — it is not a rule that can never fail"() {
        when: "checked against the deliberate violation fixture (test-only, never shipped)"
        SecurityConfigurationRules.NO_STRING_REQUEST_MATCHERS.check(
                new ClassFileImporter().importPackages("com.cognivio.ai.common.security.violationfixture"))

        then:
        AssertionError violation = thrown()
        violation.message.contains("requestMatchers(String...)")
        violation.message.contains("KAN-226")
    }

    /** Runs the REAL production matchers, so a regression in their construction fails here. */
    private static boolean matchesPermitList(MockHttpServletRequest request) {
        RequestMatcher[] matchers = IlrSecurityAutoConfiguration
                .toPermitMatchers(new IlrSecurityProperties().getPermitList())
        assert matchers.length == new IlrSecurityProperties().getPermitList().size()
        return matchers.any { it.matches(request) }
    }

    private static Throwable rootCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable c = thrown; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return c
            }
        }
        null
    }

    @EnableWebSecurity
    static class WebSecurityInfra {

        /**
         * The tenant-context filter registration depends on the MVC
         * {@code handlerExceptionResolver}; supply a no-op one so the context can
         * refresh without pulling in full WebMvc auto-configuration.
         */
        @Bean
        HandlerExceptionResolver handlerExceptionResolver() {
            return { request, response, handler, ex -> null } as HandlerExceptionResolver
        }
    }
}
