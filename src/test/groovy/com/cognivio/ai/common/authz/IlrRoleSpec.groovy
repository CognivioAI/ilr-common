package com.cognivio.ai.common.authz

import spock.lang.Specification
import spock.lang.Unroll

/**
 * The role vocabulary must absorb the case/prefix inconsistency that already exists
 * in the estate. {@code TenantContext.hasRole} is documented case-sensitive, and the
 * estate populates roles in at least two different cases, so a naive string compare
 * denies a legitimate caller depending on which code path produced their claim.
 * That failure mode is silent, which is why it is pinned here.
 */
class IlrRoleSpec extends Specification {

    @Unroll
    def "resolves '#raw' to #expected"() {
        expect:
        IlrRole.fromClaim(raw).orElse(null) == expected

        where:
        raw                | expected
        "firm-admin"       | IlrRole.FIRM_ADMIN          // JwtRoleConverter / cognito:groups wire form
        "consultant"       | IlrRole.CONSULTANT
        "reviewer"         | IlrRole.REVIEWER
        "applicant"        | IlrRole.APPLICANT
        "REVIEWER"         | IlrRole.REVIEWER            // an existing spec in the estate uses this
        "Firm-Admin"       | IlrRole.FIRM_ADMIN
        "FIRM_ADMIN"       | IlrRole.FIRM_ADMIN          // underscore treated as hyphen
        "firm_admin"       | IlrRole.FIRM_ADMIN
        "  reviewer  "     | IlrRole.REVIEWER            // whitespace from a delimited claim
        "ROLE_firm-admin"  | IlrRole.FIRM_ADMIN          // authority form, as JwtRoleConverter emits
        "role_reviewer"    | IlrRole.REVIEWER
    }

    @Unroll
    def "an unrecognised role '#raw' grants nothing rather than everything"() {
        expect:
        IlrRole.fromClaim(raw).isEmpty()

        where:
        raw << [null, "", "   ", "senior-reviewer", "admin", "ROLE_", "firmadmin"]
    }

    def "the DEV_ROLES seeded under dev-permit-all all resolve"() {
        given: "the exact set TenantContextFilter seeds when there is no JWT"
        def devRoles = ["firm-admin", "consultant", "reviewer", "applicant"]

        expect: "every one maps, or local development silently loses its capabilities"
        devRoles.every { IlrRole.fromClaim(it).isPresent() }
        devRoles.collect { IlrRole.fromClaim(it).get() } as Set == IlrRole.values() as Set
    }

    def "claimValue is the wire form and authority is the Spring form"() {
        expect:
        IlrRole.FIRM_ADMIN.claimValue() == "firm-admin"
        IlrRole.FIRM_ADMIN.authority() == "ROLE_firm-admin"
        IlrRole.REVIEWER.claimValue() == "reviewer"
    }

    def "every role round-trips through its own claim value"() {
        expect:
        IlrRole.values().every { IlrRole.fromClaim(it.claimValue()).get() == it }
    }

    def "every role round-trips through its own authority form"() {
        expect:
        IlrRole.values().every { IlrRole.fromClaim(it.authority()).get() == it }
    }
}
