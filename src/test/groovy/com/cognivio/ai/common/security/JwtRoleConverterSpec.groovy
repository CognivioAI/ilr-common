package com.cognivio.ai.common.security

import com.cognivio.ai.common.support.TestJwtFactory
import spock.lang.Specification

class JwtRoleConverterSpec extends Specification {

    TestJwtFactory jwts = new TestJwtFactory()
    JwtRoleConverter converter = new JwtRoleConverter(['cognito:groups', 'roles'], 'ROLE_')

    def "maps cognito:groups to prefixed authorities"() {
        given:
        def jwt = jwts.verifiedJwt([
                sub          : UUID.randomUUID().toString(),
                tenant_id    : UUID.randomUUID().toString(),
                'cognito:groups': ['firm-admin', 'consultant']
        ])

        when:
        def authorities = converter.convert(jwt)*.authority as Set

        then:
        authorities == ['ROLE_firm-admin', 'ROLE_consultant'] as Set
    }

    def "merges and de-duplicates roles across both claims"() {
        given:
        def jwt = jwts.verifiedJwt([
                sub          : UUID.randomUUID().toString(),
                tenant_id    : UUID.randomUUID().toString(),
                'cognito:groups': ['applicant'],
                roles        : 'applicant sponsor'
        ])

        when:
        def roles = converter.extractRoles(jwt)

        then:
        roles == ['applicant', 'sponsor'] as Set
    }

    def "produces no authorities when no role claims are present"() {
        given:
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString(), tenant_id: UUID.randomUUID().toString()])

        expect:
        converter.convert(jwt).isEmpty()
    }

    def "extractScopes reads the single space-delimited OAuth2 scope claim (KAN-211)"() {
        given:
        def jwt = jwts.verifiedJwt([
                sub  : UUID.randomUUID().toString(),
                tenant_id: UUID.randomUUID().toString(),
                scope: 'ilr-audit/audit-write ilr-audit/audit-read'
        ])

        expect:
        converter.extractScopes(jwt) == ['ilr-audit/audit-write', 'ilr-audit/audit-read'] as Set
    }

    def "extractScopes is empty when the scope claim is absent"() {
        given:
        def jwt = jwts.verifiedJwt([sub: UUID.randomUUID().toString(), tenant_id: UUID.randomUUID().toString()])

        expect:
        converter.extractScopes(jwt).isEmpty()
    }

    def "extractScopes ignores a scope claim shaped as a JSON array, unlike role claims"() {
        given: "RFC 6749/8693 fix the scope claim's shape to one delimited string, never an array"
        def jwt = jwts.verifiedJwt([
                sub  : UUID.randomUUID().toString(),
                tenant_id: UUID.randomUUID().toString(),
                scope: ['audit-write', 'audit-read']
        ])

        expect:
        converter.extractScopes(jwt).isEmpty()
    }

    def "extractScopes honours a configurable scope claim name"() {
        given:
        def custom = new JwtRoleConverter(['cognito:groups'], 'ROLE_', 'custom:scope')
        def jwt = jwts.verifiedJwt([
                sub           : UUID.randomUUID().toString(),
                tenant_id     : UUID.randomUUID().toString(),
                'custom:scope': 'audit-write'
        ])

        expect:
        custom.extractScopes(jwt) == ['audit-write'] as Set
    }
}
