package com.cognivio.ai.common.context

import com.cognivio.ai.common.security.IlrSecurityProperties
import com.cognivio.ai.common.security.JwtRoleConverter
import com.cognivio.ai.common.support.TestJwtFactory
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification

class TenantContextFilterSpec extends Specification {

    TestJwtFactory jwts = new TestJwtFactory()
    IlrSecurityProperties props = new IlrSecurityProperties()
    JwtRoleConverter roleConverter = new JwtRoleConverter(props.roleClaims, props.authorityPrefix)
    TenantClaimResolver resolver = new TenantClaimResolver(props, roleConverter)
    HandlerExceptionResolver exceptionResolver = Mock()

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "populates TenantContext from the verified JWT and IGNORES a client X-Tenant-Id header"() {
        given: 'a verified JWT whose tenant_id differs from a spoofed header'
        def jwtTenant = UUID.randomUUID()
        def spoofedTenant = UUID.randomUUID()
        def sub = UUID.randomUUID()
        def jwt = jwts.verifiedJwt([
                sub          : sub.toString(),
                tenant_id    : jwtTenant.toString(),
                'cognito:groups': ['firm-admin']
        ])
        SecurityContextHolder.context.authentication = new JwtAuthenticationToken(jwt)

        and:
        def ctx = new TenantContext()
        def filter = new TenantContextFilter(ctx, resolver, exceptionResolver)
        def request = new MockHttpServletRequest()
        request.addHeader('X-Tenant-Id', spoofedTenant.toString())
        def chain = new MockFilterChain()

        when:
        filter.doFilter(request, new MockHttpServletResponse(), chain)

        then: 'the JWT claim wins; the spoofed header is never consulted'
        ctx.tenantId == jwtTenant
        ctx.tenantId != spoofedTenant
        ctx.userId == sub
        ctx.hasRole('firm-admin')
        chain.request != null   // request proceeded down the chain
        0 * exceptionResolver.resolveException(_, _, _, _)
    }

    def "leaves TenantContext empty when there is no authenticated JWT (permit-listed endpoint)"() {
        given:
        def ctx = new TenantContext()
        def filter = new TenantContextFilter(ctx, resolver, exceptionResolver)
        def chain = new MockFilterChain()

        when:
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain)

        then:
        !ctx.present
        chain.request != null
    }

    def "renders MissingTenantClaimException via the exception resolver and stops the chain"() {
        given: 'an authenticated JWT with no tenant claim'
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString()])
        SecurityContextHolder.context.authentication = new JwtAuthenticationToken(jwt)
        def ctx = new TenantContext()
        def filter = new TenantContextFilter(ctx, resolver, exceptionResolver)
        def chain = new MockFilterChain()

        when:
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain)

        then:
        1 * exceptionResolver.resolveException(_, _, null, { it instanceof MissingTenantClaimException })
        chain.request == null   // chain did NOT proceed
    }
}
