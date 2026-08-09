package com.cognivio.ai.common.http.testsupport

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * Fitness function for KAN-236: <b>no class in a service's {@code ..client..} package may construct a
 * {@code RestClient} itself.</b> It must go through
 * {@code InternalServiceClientFactory.forBaseUrl(...)} from ilr-common, which is what attaches the
 * caller's-bearer-token relay and enforces the destination allow-list.
 *
 * <p>The rule exists because the defect it prevents is invisible everywhere it would normally be
 * caught. A client written the old way — {@code builder.baseUrl(uri).build()} — compiles, passes its
 * unit tests (which stub the server), and passes local runs (where {@code ilr.security.enabled} is
 * false). It fails only as a 401 from a deployed downstream service, which is exactly the failure
 * KAN-236 was raised to remove. Without this rule, the next client added to any of the sixteen
 * services silently reintroduces it.
 *
 * <p>Scope is deliberately narrow — {@code ..client..} packages only. Services legitimately build
 * plain {@code RestClient}s for third-party endpoints (Amazon Bedrock, GOV.UK fetches), and those
 * must NOT receive the relay; a blanket ban would push authors toward the wrong fix. By convention
 * in this estate {@code ..client..} means "a client to another ILR service", so that is where the
 * ban belongs.
 *
 * <h2>Using it from a service</h2>
 * <pre>{@code
 * class InternalClientConstructionSpec extends Specification {
 *     def "internal clients are built through ilr-common's factory"() {
 *         expect:
 *         InternalClientConstructionRules.assertNoDirectRestClientConstruction("com.cognivio.ai.compliance")
 *     }
 * }
 * }</pre>
 *
 * Requires, in the service's pom: {@code ilr-common} with {@code <classifier>tests</classifier>} and
 * a test-scoped {@code com.tngtech.archunit:archunit}.
 */
final class InternalClientConstructionRules {

    private static final String REST_CLIENT = 'org.springframework.web.client.RestClient'
    private static final String REST_CLIENT_BUILDER = 'org.springframework.web.client.RestClient$Builder'

    /** {@code RestClient.create(..)} and {@code RestClient.builder(..)} — the static entry points. */
    private static final Set<String> BANNED_STATIC_FACTORIES = ['create', 'builder'] as Set

    private InternalClientConstructionRules() {
        throw new AssertionError('rule holder; not instantiable')
    }

    /**
     * {@code allowEmptyShould(true)}: most services have a {@code ..client..} package with no
     * offending call, and several have no such package at all (ilr-common itself is one). Neither
     * must fail the build.
     */
    static final ArchRule NO_DIRECT_REST_CLIENT_CONSTRUCTION = noClasses()
            .that().resideInAPackage('..client..')
            .should(constructARestClientDirectly())
            .because('a client to another ILR service must be built via '
                    + 'InternalServiceClientFactory.forBaseUrl(...) from ilr-common, which relays the caller\'s '
                    + 'verified bearer token to allow-listed internal destinations. A directly constructed '
                    + 'RestClient sends no credential, so the call 401s against any secured downstream — and '
                    + 'that failure appears only in a deployed environment (KAN-236)')
            .allowEmptyShould(true)

    /**
     * Imports the given packages (production classes only) and checks the rule.
     *
     * @param packages base packages to scan, e.g. {@code "com.cognivio.ai.applicationbuilder"}
     * @return {@code true} so it reads naturally in a Spock {@code expect:} block
     */
    static boolean assertNoDirectRestClientConstruction(String... packages) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packages)
        NO_DIRECT_REST_CLIENT_CONSTRUCTION.check(classes)
        return true
    }

    private static ArchCondition<JavaClass> constructARestClientDirectly() {
        return new ArchCondition<JavaClass>('construct a RestClient directly') {
            @Override
            void check(JavaClass type, ConditionEvents events) {
                for (JavaMethodCall call : type.getMethodCallsFromSelf()) {
                    if (isDirectConstruction(call)) {
                        events.add(SimpleConditionEvent.satisfied(call,
                                "${call.getOriginOwner().getName()} constructs a RestClient directly at " +
                                        "${call.getSourceCodeLocation()} — use " +
                                        'InternalServiceClientFactory.forBaseUrl(baseUrl) instead'))
                    }
                }
            }
        }
    }

    private static boolean isDirectConstruction(JavaMethodCall call) {
        String owner = call.getTargetOwner().getName()
        String name = call.getTarget().getName()
        // RestClient.Builder#build() is the terminal call of every hand-rolled construction, whether
        // the builder was injected or obtained from RestClient.builder().
        if (owner == REST_CLIENT_BUILDER && name == 'build') {
            return true
        }
        return owner == REST_CLIENT && BANNED_STATIC_FACTORIES.contains(name)
    }
}
