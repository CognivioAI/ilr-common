package com.cognivio.ai.common.authz

import spock.lang.Specification
import spock.lang.Unroll

class IlrPermissionSpec extends Specification {

    def "every permission resolves from its own name"() {
        expect:
        IlrPermission.values().every { IlrPermission.fromName(it.name()).get() == it }
    }

    @Unroll
    def "resolves '#raw' to #expected"() {
        expect:
        IlrPermission.fromName(raw).orElse(null) == expected

        where:
        raw               | expected
        "CASE_SIGNOFF"    | IlrPermission.CASE_SIGNOFF
        "case_signoff"    | IlrPermission.CASE_SIGNOFF
        "case-signoff"    | IlrPermission.CASE_SIGNOFF
        " CASE_SIGNOFF "  | IlrPermission.CASE_SIGNOFF
        "Review_Decide"   | IlrPermission.REVIEW_DECIDE
    }

    @Unroll
    def "an unknown name '#raw' resolves to nothing, so the caller must deny"() {
        expect:
        IlrPermission.fromName(raw).isEmpty()

        where:
        raw << [null, "", "   ", "CASE_SIGNOF", "case signoff", "SIGNOFF", "hasRole('reviewer')"]
    }

    def "the permissions named by the architecture design are all present"() {
        given:
        def required = [
                "CASE_SIGNOFF", "CASE_IMPORT",
                "REVIEW_ENQUEUE", "REVIEW_CLAIM", "REVIEW_DECIDE",
                "USER_DATA_EXPORT", "USER_ERASURE_REQUEST",
                "FIRM_SETTINGS_WRITE",
                "DOCUMENT_PURGE_REQUEST", "DOCUMENT_PURGE_CONSENT",
                "AUDIT_WRITE", "PAYMENT_RECONCILE",
                "EXTRACTION_CONFIRM", "REMINDER_DISPATCH",
                "TRANSCRIPT_READ_ANY"]

        expect:
        required.every { IlrPermission.fromName(it).isPresent() }
    }
}
