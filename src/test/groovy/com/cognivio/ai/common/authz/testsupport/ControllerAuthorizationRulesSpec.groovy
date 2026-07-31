package com.cognivio.ai.common.authz.testsupport

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchRule
import spock.lang.Specification

/**
 * A fitness function nobody has seen fail is not a fitness function. These specs
 * prove the rule both passes on compliant code and actually goes red — naming the
 * offending methods — on the shape of omission KAN-186 found across the estate.
 *
 * <p>The fixtures are imported without {@code DO_NOT_INCLUDE_TESTS} because they
 * live in test output; consuming services use
 * {@link ControllerAuthorizationRules#assertNoUnauthorizedEndpoints(String[])},
 * which scans production classes only.
 */
class ControllerAuthorizationRulesSpec extends Specification {

    static final String FIXTURES = "com.cognivio.ai.common.authz.fixture"

    ArchRule rule = ControllerAuthorizationRules.ENDPOINTS_MUST_DECLARE_AN_AUTHORIZATION_DECISION

    def "passes when every endpoint declares an authorization decision"() {
        when:
        rule.check(new ClassFileImporter().importPackages(FIXTURES + ".compliant"))

        then:
        noExceptionThrown()
    }

    def "accepts a class-level @PreAuthorize covering every method"() {
        when:
        rule.check(new ClassFileImporter().importPackages(FIXTURES + ".classlevel"))

        then:
        noExceptionThrown()
    }

    def "fails, naming each endpoint that declares nothing"() {
        when:
        rule.check(new ClassFileImporter().importPackages(FIXTURES + ".violating"))

        then:
        AssertionError error = thrown()
        error.message.contains("writeAuditEvent")
        error.message.contains("purge")

        and: "the annotated endpoint in the same class is not reported"
        !error.message.contains("listCases")

        and: "the message tells the reader what to do about it"
        error.message.contains("@AuthenticatedOnly")
    }

    def "does not demand annotations on public methods that are not HTTP endpoints"() {
        when: "the compliant fixture contains a public, unmapped helper method"
        rule.check(new ClassFileImporter().importPackages(FIXTURES + ".compliant"))

        then:
        noExceptionThrown()
    }

    def "a package with no REST controllers passes rather than failing on an empty match"() {
        expect: "ArchUnit's default is to fail on an empty should; that would be a red build saying nothing"
        ControllerAuthorizationRules.assertNoUnauthorizedEndpoints("com.cognivio.ai.common.web")
    }

    def "assertNoUnauthorizedEndpoints scans production classes only"() {
        expect: "the violating fixture lives in test output, so a production-only scan does not see it"
        ControllerAuthorizationRules.assertNoUnauthorizedEndpoints(FIXTURES + ".violating")
    }
}
