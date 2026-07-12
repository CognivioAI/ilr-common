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
}
