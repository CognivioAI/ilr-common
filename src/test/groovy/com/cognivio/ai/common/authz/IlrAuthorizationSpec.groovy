package com.cognivio.ai.common.authz

import com.cognivio.ai.common.context.TenantContext
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Supplier

/**
 * The bean every {@code @PreAuthorize("@ilrAuth...")} expression calls into. Its one
 * non-negotiable property is that it fails <b>closed</b>: every way of failing to
 * establish who the caller is must deny, because the alternative — an exception
 * escaping into a 500, or worse a {@code true} — turns a security control into a
 * liability.
 */
class IlrAuthorizationSpec extends Specification {

    TenantContext context = new TenantContext()
    IlrAuthorization authorization = new IlrAuthorization({ context })

    def "grants a permission the caller's role holds"() {
        given:
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["consultant"] as Set)

        expect:
        authorization.can("CASE_SIGNOFF")
        authorization.hasPermission(IlrPermission.CASE_SIGNOFF)
    }

    def "denies a permission the caller's role does not hold"() {
        given: "the KAN-186 scenario: an applicant reaching the sign-off endpoint"
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["applicant"] as Set)

        expect:
        !authorization.can("CASE_SIGNOFF")
        !authorization.can("REVIEW_DECIDE")
        !authorization.can("USER_ERASURE_REQUEST")
    }

    def "matches roles regardless of the case the claim arrived in"() {
        given: "the uppercase form that at least one existing spec produces"
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["REVIEWER"] as Set)

        expect: "a case-sensitive compare here would have silently denied a legitimate reviewer"
        authorization.can("REVIEW_DECIDE")
        authorization.hasRole("reviewer")
    }

    def "works for the synthetic dev identity seeded under dev-permit-all"() {
        given: "no JWT and therefore no Spring authority — only TenantContext is populated"
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(),
                ["firm-admin", "consultant", "reviewer", "applicant"] as Set)

        expect: "a hasRole()-based gate would have denied all of these locally"
        authorization.can("CASE_SIGNOFF")
        authorization.can("REVIEW_DECIDE")
        authorization.can("FIRM_SETTINGS_WRITE")
    }

    @Unroll
    def "denies when the identity is unusable: #scenario"() {
        given:
        def auth = new IlrAuthorization(supplier as Supplier)

        expect:
        !auth.can("CASE_SIGNOFF")
        !auth.hasRole("firm-admin")
        !auth.isSelf(UUID.randomUUID())
        auth.permissions().isEmpty()

        where:
        scenario                            | supplier
        "no TenantContext bean at all"      | { null }
        "context never populated"           | { new TenantContext() }
        "no request bound to the thread"    | { throw new IllegalStateException("No thread-bound request found") }
        "supplier blows up"                 | { throw new RuntimeException("boom") }
    }

    def "denies when the supplier itself is null"() {
        given:
        def auth = new IlrAuthorization(null)

        expect:
        !auth.can("CASE_SIGNOFF")
        auth.permissions().isEmpty()
    }

    @Unroll
    def "an unresolvable permission name '#name' denies rather than opening the endpoint"() {
        given: "a caller holding every role there is"
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(),
                IlrRole.values().collect { it.claimValue() } as Set)

        expect: "a typo in a @PreAuthorize expression must fail closed"
        !authorization.can(name)

        where:
        name << [null, "", "  ", "CASE_SIGNOF", "case signoff", "ROLE_ADMIN", "*"]
    }

    def "resolves permission names leniently for case and hyphens"() {
        given:
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["reviewer"] as Set)

        expect:
        authorization.can("REVIEW_DECIDE")
        authorization.can("review_decide")
        authorization.can("review-decide")
        authorization.can(" REVIEW_DECIDE ")
    }

    def "isSelf is true only for the calling user"() {
        given:
        def userId = UUID.randomUUID()
        context.populate(UUID.randomUUID(), null, userId, ["applicant"] as Set)

        expect:
        authorization.isSelf(userId)
        authorization.isSelf(userId.toString())
        !authorization.isSelf(UUID.randomUUID())
        !authorization.isSelf(UUID.randomUUID().toString())
    }

    @Unroll
    def "isSelf(#raw) denies rather than throwing"() {
        given:
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["applicant"] as Set)

        expect: "a malformed or unexpected path variable must not become a 500"
        !authorization.isSelf(raw)

        where:
        raw << [null, "", "   ", "not-a-uuid", "12345", 42, new Object()]
    }

    def "isSelf denies when the context carries no user id"() {
        given: "a populated tenant but no sub claim"
        context.populate(UUID.randomUUID(), null, null, ["applicant"] as Set)

        expect:
        !authorization.isSelf(UUID.randomUUID())
        !authorization.isSelf(UUID.randomUUID().toString())
    }

    def "permissions reports the caller's full capability set"() {
        given:
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["reviewer"] as Set)

        expect:
        authorization.permissions() == [
                IlrPermission.REVIEW_ENQUEUE,
                IlrPermission.REVIEW_CLAIM,
                IlrPermission.REVIEW_DECIDE] as Set
    }

    @Unroll
    def "hasRole denies the unknown role '#role'"() {
        given:
        context.populate(UUID.randomUUID(), null, UUID.randomUUID(), ["firm-admin"] as Set)

        expect:
        !authorization.hasRole(role)

        where:
        role << [null, "", "senior-reviewer", "reviewer"]
    }
}
