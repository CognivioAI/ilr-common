package com.cognivio.ai.common.context

import com.cognivio.ai.common.security.IlrSecurityProperties
import com.cognivio.ai.common.security.JwtRoleConverter
import com.cognivio.ai.common.support.TestJwtFactory
import spock.lang.Specification

class TenantClaimResolverSpec extends Specification {

    TestJwtFactory jwts = new TestJwtFactory()
    IlrSecurityProperties props = new IlrSecurityProperties()
    JwtRoleConverter roleConverter = new JwtRoleConverter(props.roleClaims, props.authorityPrefix)
    TenantClaimResolver resolver = new TenantClaimResolver(props, roleConverter)

    def "populates tenant, firm, user and roles from verified claims (cognito:groups)"() {
        given:
        def tenant = UUID.randomUUID()
        def firm = UUID.randomUUID()
        def sub = UUID.randomUUID()
        def jwt = jwts.verifiedJwt([
                sub          : sub.toString(),
                tenant_id    : tenant.toString(),
                firm_id      : firm.toString(),
                'cognito:groups': ['firm-admin', 'consultant']
        ])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        ctx.present
        ctx.tenantId == tenant
        ctx.firmId == firm
        ctx.userId == sub
        ctx.roles == ['firm-admin', 'consultant'] as Set
        ctx.hasRole('firm-admin')
    }

    def "firmId defaults to tenantId when no firm_id claim is present"() {
        given:
        def tenant = UUID.randomUUID()
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString(), tenant_id: tenant.toString()])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        ctx.tenantId == tenant
        ctx.firmId == tenant
    }

    def "supports a plain 'roles' claim (space-delimited string) as well as cognito:groups"() {
        given:
        def jwt = jwts.verifiedJwt([
                sub      : UUID.randomUUID().toString(),
                tenant_id: UUID.randomUUID().toString(),
                roles    : 'applicant sponsor'
        ])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        ctx.roles == ['applicant', 'sponsor'] as Set
    }

    def "throws MissingTenantClaimException when the tenant claim is absent"() {
        given:
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString()])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        def ex = thrown(MissingTenantClaimException)
        ex.code == 'TENANT_CLAIM_MISSING'
        ex.httpStatus.value() == 403
    }

    def "throws MissingTenantClaimException when the tenant claim is not a UUID"() {
        given:
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString(), tenant_id: 'not-a-uuid'])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        thrown(MissingTenantClaimException)
    }

    def "honours a configurable tenant claim name"() {
        given:
        props.tenantClaim = 'custom:tenant'
        def tenant = UUID.randomUUID()
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString(), 'custom:tenant': tenant.toString()])
        def ctx = new TenantContext()

        when:
        resolver.resolveInto(jwt, ctx)

        then:
        ctx.tenantId == tenant
    }
}
