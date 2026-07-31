package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.authz.IlrRole
import com.cognivio.ai.common.security.RestAuthenticationEntryPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tier A — the full-context smoke test, and the one every service gets first.
 *
 * <p>It is a larger amount of assurance than it looks. Booting this context proves, in one file:
 * every {@code @PreAuthorize} SpEL expression parses and every {@code @ilrAuth} bean reference
 * resolves; the security filter chain assembles with a real {@code JwtDecoder} rather than
 * fail-closing; Flyway's migrations apply; and — because {@code ddl-auto=validate} runs against that
 * migrated schema — every entity-to-column mapping agrees with the database. None of that is
 * reachable from a {@code MockMvcBuilders.standaloneSetup(...)} spec, which builds no context at all.
 *
 * <p>What it deliberately does not prove is that a {@code @PreAuthorize} expression <em>denies</em>
 * correctly: a bad expression fails at evaluation time, not at startup. That is
 * {@link HarnessSecurityIT}'s job.
 */
class ApplicationContextIT extends AbstractHarnessSelfTestIT {

    @Autowired
    ApplicationContext applicationContext

    def "the context boots with the real security chain and a real JwtDecoder"() {
        expect: "the production filter chain is present — not the permit-all one"
        applicationContext.getBeanNamesForType(SecurityFilterChain).length > 0
        applicationContext.containsBean('ilrSecurityFilterChain')

        and: "a decoder is wired, so the chain did not take its fail-closed or dev-permit-all branch"
        applicationContext.getBean(JwtDecoder) != null

        and: "the capability gate bean @PreAuthorize expressions resolve is present"
        applicationContext.containsBean('ilrAuth')
    }

    def "the harness posture is actually in force, not merely requested"() {
        expect: "these are the four flags whose absence would make every other assertion vacuous"
        securityProperties.enabled
        securityProperties.methodSecurity.enabled
        !securityProperties.devPermitAll

        and: "the RLS aspect exists — its absence is the KAN-189 ordering defect, and it is silent"
        applicationContext.getBeanNamesForType(
                com.cognivio.ai.common.tenancy.RlsTenantBindingAspect).length == 1
    }

    def "hibernate validated its mappings against the Flyway-migrated schema"() {
        expect: "ddl-auto=validate ran at startup; reaching this line at all is the assertion"
        applicationContext.environment.getProperty('spring.jpa.hibernate.ddl-auto') == 'validate'

        and: "and the schema really came from Flyway, not from Hibernate having created it"
        applicationContext.environment.getProperty('spring.flyway.enabled') == 'true'
    }

    def "a permit-listed endpoint is reachable without a token"() {
        expect: "the permit-list in IlrSecurityProperties applies through the real chain"
        mockMvc.perform(get('/actuator/health'))
                .andExpect(status().isOk())
    }

    def "a gated endpoint with no token is 401 in the platform error shape"() {
        when:
        def result = mockMvc.perform(get('/harness/cases')).andExpect(status().isUnauthorized())

        then: "rendered by RestAuthenticationEntryPoint, not by the servlet container's HTML page"
        def json = body(result)
        json.status == 401
        json.error == 'UNAUTHORIZED'
        json.message == 'Authentication required'
        json.traceId != null
        json.details == []

        and: "which is the entry point the chain was actually configured with"
        applicationContext.getBean(org.springframework.security.web.AuthenticationEntryPoint) instanceof RestAuthenticationEntryPoint
    }

    def "a gated endpoint with a garbage token is 401, so signature verification is genuinely running"() {
        expect:
        mockMvc.perform(get('/harness/cases').header('Authorization', 'Bearer not-a-jwt'))
                .andExpect(status().isUnauthorized())
    }

    def "a valid token resolves the caller identity through the real claim resolver"() {
        when: "an authenticated call to an endpoint with no capability gate"
        def result = mockMvc.perform(get('/harness/me').with(bearerFor(IlrRole.CONSULTANT)))
                .andExpect(status().isOk())

        then: "tenant, user and roles all came from the verified token, via TenantClaimResolver"
        def json = body(result)
        json.tenantId == TENANT_A.toString()
        json.firmId == TENANT_A.toString()
        json.userId == com.cognivio.ai.common.testsupport.integration.IlrBearerTokens.DEFAULT_USER_ID.toString()
        json.roles == [IlrRole.CONSULTANT.claimValue()]
    }

    def "a valid token with no tenant claim is refused rather than defaulted"() {
        expect: "MissingTenantClaimException renders through CommonExceptionHandler as 403"
        mockMvc.perform(get('/harness/me').with(bearerForClaims([
                sub              : com.cognivio.ai.common.testsupport.integration.IlrBearerTokens.DEFAULT_USER_ID.toString(),
                'cognito:groups' : [IlrRole.CONSULTANT.claimValue()],
        ])))
                .andExpect(status().isForbidden())
    }
}
