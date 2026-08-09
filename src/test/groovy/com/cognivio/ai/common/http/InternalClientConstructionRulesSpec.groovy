package com.cognivio.ai.common.http

import com.cognivio.ai.common.http.testsupport.InternalClientConstructionRules
import com.tngtech.archunit.core.importer.ClassFileImporter
import spock.lang.Specification

/**
 * Proves the KAN-236 fitness function both fires on the defect and stays quiet on the fix.
 *
 * <p>The second half matters as much as the first: the KAN-226 rule was once written in a form that
 * matched nothing and passed happily over the exact call site it forbade. A rule that cannot fail —
 * or one that flags everything — is worse than no rule, because it reads as evidence.
 */
class InternalClientConstructionRulesSpec extends Specification {

    static final String VIOLATING = 'com.cognivio.ai.common.http.violationfixture.violating'
    static final String COMPLIANT = 'com.cognivio.ai.common.http.violationfixture.compliant'

    def "the rule fires on a client package that constructs a RestClient directly"() {
        when:
        InternalClientConstructionRules.NO_DIRECT_REST_CLIENT_CONSTRUCTION.check(
                new ClassFileImporter().importPackages(VIOLATING))

        then:
        AssertionError violation = thrown()
        violation.message.contains('constructs a RestClient directly')
        violation.message.contains('KAN-236')
    }

    def "it names every banned entry point, not just the first"() {
        when:
        InternalClientConstructionRules.NO_DIRECT_REST_CLIENT_CONSTRUCTION.check(
                new ClassFileImporter().importPackages(VIOLATING))

        then:
        AssertionError violation = thrown()
        violation.message.count('constructs a RestClient directly') >= 3
    }

    def "it does NOT fire on a client built through InternalServiceClientFactory"() {
        when:
        InternalClientConstructionRules.NO_DIRECT_REST_CLIENT_CONSTRUCTION.check(
                new ClassFileImporter().importPackages(COMPLIANT))

        then:
        noExceptionThrown()
    }

    def "it passes over a package with no client package at all"() {
        expect: "ilr-common itself builds no service clients; allowEmptyShould(true) must hold"
        InternalClientConstructionRules.assertNoDirectRestClientConstruction('com.cognivio.ai.common')
    }
}
