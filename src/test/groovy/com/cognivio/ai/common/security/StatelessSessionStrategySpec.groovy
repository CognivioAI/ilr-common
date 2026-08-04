package com.cognivio.ai.common.security

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration
import jakarta.servlet.http.HttpSession
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.session.AbstractSessionFixationProtectionStrategy
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy
import org.springframework.security.web.session.SessionManagementFilter
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification

import java.time.Instant

/**
 * KAN-229 (third defect): a stateless resource server must install
 * {@link NullAuthenticatedSessionStrategy} explicitly. {@code SessionCreationPolicy.STATELESS}
 * alone does not do it.
 *
 * <h2>The defect this guards</h2>
 * Every authenticated request on the deployed Lambda 500'd with
 * {@code UnsupportedOperationException} thrown from
 * {@code ServerlessHttpServletRequest.changeSessionId()}, reached via Spring Security's default
 * {@code ChangeSessionIdAuthenticationStrategy} inside {@code SessionManagementFilter}.
 *
 * <p>{@code SessionManagementConfigurer.getSessionAuthenticationStrategy(H)} never consults
 * {@code isStateless()} — the policy only selects the {@code SecurityContextRepository} and
 * {@code RequestCache}. So the session-fixation strategy is installed regardless. In a real
 * servlet container that is harmless: for a bearer-token request the strategy's own guards
 * ({@code getSession(false) != null} and {@code isRequestedSessionIdValid()}) are false, so it
 * returns immediately. The AWS serverless adapter makes both guards permanently true —
 * {@code ServerlessHttpServletRequest.getSession(false)} ignores the {@code create} argument and
 * always lazily creates a session, and its constructor hard-codes
 * {@code requestedSessionIdValid = true} — so the strategy always reaches the unimplemented
 * {@code changeSessionId()} stub.
 *
 * <h2>Why the tests below drive real requests</h2>
 * A structural "is a {@code NullAuthenticatedSessionStrategy} configured" assertion would pass
 * against several fixes that do not work, and would not explain the failure. Each case here builds
 * the REAL chain from the REAL auto-configuration and pushes a bearer-token request through
 * {@link FilterChainProxy} with a request object that reproduces the three adapter behaviours
 * above. The mutation case proves the mechanism is genuinely reachable: the same request against a
 * chain configured the pre-fix way still blows up.
 */
class StatelessSessionStrategySpec extends Specification {

    private static final String TENANT_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
    private static final String USER_ID = "3f2504e0-4f89-11d3-9a0c-0305e82c3302"

    def contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IlrTenantContextAutoConfiguration,
                    IlrSecurityAutoConfiguration))
            .withUserConfiguration(WebSecurityInfra, StubJwtDecoder)
            .withPropertyValues("ilr.security.enabled=true")

    def "an authenticated request completes through the real chain under the serverless request shape"() {
        expect:
        contextRunner.run { context ->
            assert context.startupFailure == null

            def response = driveBearerRequest(context.getBean(SecurityFilterChain))

            assert response.status == 200:
                    'the authenticated request did not reach the application; body: ' +
                            response.contentAsString
        }
    }

    def "proves the bug is real: the same request against the pre-fix configuration throws"() {
        when: "the chain is configured the way it was before this fix — STATELESS and nothing else"
        contextRunner
                .withUserConfiguration(PreFixSessionManagementChain)
                .run { context ->
                    assert context.startupFailure == null
                    driveBearerRequest(context.getBean(SecurityFilterChain))
                }

        then: "ChangeSessionIdAuthenticationStrategy reaches the adapter's unimplemented stub"
        UnsupportedOperationException blowUp = thrown()
        blowUp.message.contains("changeSessionId")
    }

    def "no session-fixation strategy survives anywhere in the configured strategy graph"() {
        expect: "the structural companion to the behavioural cases above — it names the required fix"
        contextRunner.run { context ->
            def sessionFilter = context.getBean(SecurityFilterChain).getFilters()
                    .find { it instanceof SessionManagementFilter }

            if (sessionFilter == null) {
                return // no SessionManagementFilter at all would also be a correct outcome
            }
            // Spring wraps whatever is configured in a CompositeSessionAuthenticationStrategy, so
            // asserting on the top-level type alone would be meaningless — flatten it first.
            def strategies = flatten(fieldValue(sessionFilter, "sessionAuthenticationStrategy"))

            assert strategies.any { it instanceof NullAuthenticatedSessionStrategy }:
                    'no NullAuthenticatedSessionStrategy configured; found ' +
                            strategies.collect { it.getClass().getSimpleName() } + ' (KAN-229)'
            assert !strategies.any { it instanceof AbstractSessionFixationProtectionStrategy }:
                    'a session-fixation strategy is still installed under a STATELESS policy — the ' +
                            'policy does NOT suppress it, and under the serverless adapter it calls ' +
                            'changeSessionId() on every authenticated request (KAN-229). Found: ' +
                            strategies.collect { it.getClass().getSimpleName() }
        }
    }

    def "NullAuthenticatedSessionStrategy never touches the request"() {
        given: "the adapter request, whose changeSessionId() throws"
        def request = new ServerlessRequestStub("GET", "/api/v1/protected")

        when:
        new NullAuthenticatedSessionStrategy().onAuthentication(
                null, request, new MockHttpServletResponse())

        then: "no exception — it is a no-op by contract, which is why it is the correct strategy here"
        noExceptionThrown()
    }

    /**
     * Pushes a bearer-token request through the real chain. The request is bound to
     * {@code RequestContextHolder} because {@code TenantContext} is request-scoped and
     * {@code TenantContextFilter} runs inside this chain.
     */
    private static MockHttpServletResponse driveBearerRequest(SecurityFilterChain chain) {
        def request = new ServerlessRequestStub("GET", "/api/v1/protected")
        request.addHeader("Authorization", "Bearer any-token-the-stub-decoder-accepts")
        def response = new MockHttpServletResponse()

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response))
        try {
            new FilterChainProxy(chain).doFilter(request, response, new MockFilterChain())
        } finally {
            RequestContextHolder.resetRequestAttributes()
        }
        return response
    }

    private static Object fieldValue(Object target, String name) {
        def field = target.getClass().getDeclaredField(name)
        field.setAccessible(true)
        return field.get(target)
    }

    /** Expands a {@code CompositeSessionAuthenticationStrategy} into the strategies it delegates to. */
    private static List<SessionAuthenticationStrategy> flatten(SessionAuthenticationStrategy strategy) {
        if (!(strategy instanceof CompositeSessionAuthenticationStrategy)) {
            return [strategy]
        }
        return ((List<SessionAuthenticationStrategy>) fieldValue(strategy, "delegateStrategies"))
                .collectMany { flatten(it) }
    }

    /**
     * Reproduces the three behaviours of {@code ServerlessHttpServletRequest} that together make
     * {@code ChangeSessionIdAuthenticationStrategy}'s "no session yet" escape hatch unreachable.
     * Nothing else about the request is stubbed — a real container differs from this only in
     * returning {@code null} from {@code getSession(false)} for a fresh bearer-token request, which
     * is exactly why the defect was invisible in every existing test.
     */
    static class ServerlessRequestStub extends org.springframework.mock.web.MockHttpServletRequest {

        ServerlessRequestStub(String method, String uri) {
            super(method, uri)
            setServletPath("")
            setContextPath("")
            setPathInfo(null)
        }

        /** The adapter ignores {@code create} and always creates the session lazily. */
        @Override
        HttpSession getSession(boolean create) {
            return super.getSession(true)
        }

        @Override
        HttpSession getSession() {
            return getSession(true)
        }

        /** The adapter's constructor hard-codes this to true. */
        @Override
        boolean isRequestedSessionIdValid() {
            return true
        }

        /** The adapter leaves this an unimplemented stub. */
        @Override
        String changeSessionId() {
            throw new UnsupportedOperationException(
                    "ServerlessHttpServletRequest.changeSessionId() is not implemented")
        }
    }

    /**
     * The chain exactly as it was configured before this fix: {@code SessionCreationPolicy.STATELESS}
     * with no explicit session-authentication strategy. Registered under the production bean name so
     * {@code IlrSecurityAutoConfiguration}'s {@code @ConditionalOnMissingBean(name=...)} backs off.
     * Test-only mutation fixture — it exists to prove the guard above can fail.
     */
    static class PreFixSessionManagementChain {

        @Bean
        SecurityFilterChain ilrSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) {
            http.csrf({ csrf -> csrf.disable() } as Customizer)
                    .sessionManagement({ sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    } as Customizer)
                    .authorizeHttpRequests({ auth -> auth.anyRequest().authenticated() } as Customizer)
                    .oauth2ResourceServer({ oauth ->
                        oauth.jwt({ jwt -> jwt.decoder(jwtDecoder) } as Customizer)
                    } as Customizer)
            return http.build()
        }
    }

    @EnableWebSecurity
    static class WebSecurityInfra {

        @Bean
        HandlerExceptionResolver handlerExceptionResolver() {
            return { request, response, handler, ex -> null } as HandlerExceptionResolver
        }
    }

    /**
     * Accepts any token and returns a verified {@link Jwt} carrying the tenant claim, so the request
     * gets past {@code TenantContextFilter} (which runs inside this chain since KAN-229) and reaches
     * the application. No live Cognito JWKS endpoint is involved.
     */
    static class StubJwtDecoder {

        @Bean
        JwtDecoder jwtDecoder() {
            return { String token ->
                new Jwt(token, Instant.now(), Instant.now().plusSeconds(60),
                        [alg: "none"],
                        [sub: USER_ID, tenant_id: TENANT_ID])
            } as JwtDecoder
        }
    }
}
