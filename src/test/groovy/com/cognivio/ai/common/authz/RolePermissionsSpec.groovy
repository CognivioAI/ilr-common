package com.cognivio.ai.common.authz

import spock.lang.Specification
import spock.lang.Unroll

/**
 * This class is the platform's access-control policy. These specs are not really
 * testing code — they are pinning policy decisions so that changing one shows up as
 * a deliberate red test rather than a quiet diff in a map literal.
 */
class RolePermissionsSpec extends Specification {

    @Unroll
    def "#role holds #permission"() {
        expect:
        RolePermissions.grants(role, permission)

        where:
        role                | permission
        IlrRole.CONSULTANT  | IlrPermission.CASE_SIGNOFF
        IlrRole.FIRM_ADMIN  | IlrPermission.CASE_SIGNOFF
        IlrRole.FIRM_ADMIN  | IlrPermission.FIRM_SETTINGS_WRITE
        IlrRole.FIRM_ADMIN  | IlrPermission.USER_DATA_EXPORT
        IlrRole.FIRM_ADMIN  | IlrPermission.USER_ERASURE_REQUEST
        IlrRole.FIRM_ADMIN  | IlrPermission.DOCUMENT_PURGE_REQUEST
        IlrRole.FIRM_ADMIN  | IlrPermission.PAYMENT_RECONCILE
        IlrRole.REVIEWER    | IlrPermission.REVIEW_CLAIM
        IlrRole.REVIEWER    | IlrPermission.REVIEW_DECIDE
        // KAN-210: purge is user-initiated (applicant or consultant); consent is
        // decided by the assigned consultant only — see RolePermissions javadoc.
        IlrRole.APPLICANT   | IlrPermission.DOCUMENT_PURGE_REQUEST
        IlrRole.CONSULTANT  | IlrPermission.DOCUMENT_PURGE_REQUEST
        IlrRole.CONSULTANT  | IlrPermission.DOCUMENT_PURGE_CONSENT
        // KAN-212: TRACKER_WRITE is a firm-side act (manual logging, no UKVI API
        // integration per A-04) — held by consultant and firm admin only.
        IlrRole.CONSULTANT  | IlrPermission.TRACKER_WRITE
        IlrRole.FIRM_ADMIN  | IlrPermission.TRACKER_WRITE
    }

    @Unroll
    def "#role must NOT hold #permission"() {
        expect:
        !RolePermissions.grants(role, permission)

        where:
        role                | permission
        // The KAN-186 finding, stated as a test: an applicant could sign off their own
        // case, decide their own review and erase a colleague's personal data.
        IlrRole.APPLICANT   | IlrPermission.CASE_SIGNOFF
        IlrRole.APPLICANT   | IlrPermission.REVIEW_DECIDE
        IlrRole.APPLICANT   | IlrPermission.USER_ERASURE_REQUEST
        IlrRole.APPLICANT   | IlrPermission.USER_DATA_EXPORT
        IlrRole.APPLICANT   | IlrPermission.CASE_IMPORT
        IlrRole.APPLICANT   | IlrPermission.FIRM_SETTINGS_WRITE
        IlrRole.APPLICANT   | IlrPermission.TRANSCRIPT_READ_ANY
        // Deciding a human review is a competence-based act, not an administrative one.
        // Firm admin is deliberately not a superuser.
        IlrRole.FIRM_ADMIN  | IlrPermission.REVIEW_DECIDE
        IlrRole.FIRM_ADMIN  | IlrPermission.REVIEW_CLAIM
        // A reviewer works the queue and does nothing else.
        IlrRole.REVIEWER    | IlrPermission.CASE_SIGNOFF
        IlrRole.REVIEWER    | IlrPermission.FIRM_SETTINGS_WRITE
        IlrRole.REVIEWER    | IlrPermission.USER_ERASURE_REQUEST
        IlrRole.CONSULTANT  | IlrPermission.FIRM_SETTINGS_WRITE
        IlrRole.CONSULTANT  | IlrPermission.USER_ERASURE_REQUEST
        IlrRole.CONSULTANT  | IlrPermission.REVIEW_DECIDE
        // KAN-210: an applicant is never the assigned consultant, so never holds the
        // consent decision; firm admin holding it would be a no-op grant against the
        // decideConsent L2 identity check, so it is deliberately excluded too.
        IlrRole.APPLICANT   | IlrPermission.DOCUMENT_PURGE_CONSENT
        IlrRole.FIRM_ADMIN  | IlrPermission.DOCUMENT_PURGE_CONSENT
        // KAN-212: manual UKVI submission logging is a firm-side act, not an
        // applicant one — see RolePermissions javadoc.
        IlrRole.APPLICANT   | IlrPermission.TRACKER_WRITE
        IlrRole.REVIEWER    | IlrPermission.TRACKER_WRITE
    }

    @Unroll
    def "#permission is granted to no role pending the machine-to-machine decision"() {
        expect: "empty, not over-granted — see KAN-186's blocking open question on service principals"
        RolePermissions.rolesGranting(permission).isEmpty()

        where:
        permission << [IlrPermission.AUDIT_WRITE, IlrPermission.REMINDER_DISPATCH,
                       IlrPermission.PROCESSING_JOB_SUBMIT, IlrPermission.EXTRACTION_SUBMIT]
    }

    def "grantsAny tolerates the role-string forms that actually occur"() {
        expect:
        RolePermissions.grantsAny(roles, IlrPermission.REVIEW_DECIDE) == granted

        where:
        roles                              | granted
        ["reviewer"] as Set                | true
        ["REVIEWER"] as Set                | true    // uppercase, as one existing spec produces
        ["ROLE_reviewer"] as Set           | true    // authority form
        ["applicant", "reviewer"] as Set   | true    // any one role suffices
        ["applicant"] as Set               | false
        ["unknown-role"] as Set            | false   // unknown grants nothing
        [] as Set                          | false
        null                               | false
    }

    def "the dev identity holds every capability that any role holds"() {
        given: "the DEV_ROLES TenantContextFilter seeds under dev-permit-all"
        def devRoles = ["firm-admin", "consultant", "reviewer", "applicant"]

        when:
        def devPermissions = RolePermissions.permissionsFor(devRoles)

        then: "local development is not silently denied anything a real role could do"
        devPermissions == IlrPermission.values().findAll {
            !RolePermissions.rolesGranting(it).isEmpty()
        } as Set

        and: "which today is everything except the two pending the M2M decision"
        !devPermissions.contains(IlrPermission.AUDIT_WRITE)
        !devPermissions.contains(IlrPermission.REMINDER_DISPATCH)
    }

    def "null inputs grant nothing"() {
        expect:
        !RolePermissions.grants(null, IlrPermission.CASE_SIGNOFF)
        !RolePermissions.grants(IlrRole.FIRM_ADMIN, null)
        RolePermissions.forRole(null).isEmpty()
        RolePermissions.permissionsFor(null).isEmpty()
    }

    def "the policy map is immutable"() {
        when:
        RolePermissions.policy().put(IlrRole.APPLICANT, IlrPermission.values() as Set)

        then:
        thrown(UnsupportedOperationException)
    }

    def "a role's grant set is immutable"() {
        when:
        RolePermissions.forRole(IlrRole.APPLICANT).add(IlrPermission.CASE_SIGNOFF)

        then:
        thrown(UnsupportedOperationException)
    }

    def "every role appears in the policy so an unmapped role cannot be an accident"() {
        expect:
        RolePermissions.policy().keySet() == IlrRole.values() as Set
    }
}
