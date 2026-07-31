package com.cognivio.ai.common.authz.testsupport

import com.cognivio.ai.common.authz.AuthenticatedOnly
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods

/**
 * The fail-closed half of the authorization design.
 *
 * <p>{@code @PreAuthorize} is opt-in, and opt-in controls decay. An endpoint added
 * next quarter with no annotation is silently reachable by any authenticated user —
 * which is not a hypothetical, it is precisely the state KAN-186 found across all 16
 * services. This rule converts "someone remembered" into "the build passed":
 *
 * <blockquote>Every HTTP-mapped method on a {@code @RestController} must carry
 * {@code @PreAuthorize} or the explicit {@link AuthenticatedOnly} marker (on the
 * method or on its class).</blockquote>
 *
 * <p><b>Scope note.</b> The rule checks methods that are actually mapped to an HTTP
 * verb ({@code @GetMapping}, {@code @PostMapping}, … or {@code @RequestMapping}),
 * not every public method on the class. A public helper on a controller is not
 * reachable over HTTP, so requiring an annotation on it would add noise without
 * closing a hole — and a noisy fitness function is one that gets suppressed.
 *
 * <h2>Using it from a service</h2>
 * <pre>{@code
 * class ControllerAuthorizationSpec extends Specification {
 *     def "every endpoint declares an authorization decision"() {
 *         expect:
 *         ControllerAuthorizationRules.assertNoUnauthorizedEndpoints("com.cognivio.ai.casemanagement")
 *     }
 * }
 * }</pre>
 *
 * Requires, in the service's pom: {@code ilr-common} with
 * {@code <classifier>tests</classifier>} and a test-scoped {@code com.tngtech.archunit:archunit}.
 */
final class ControllerAuthorizationRules {

    private ControllerAuthorizationRules() {
        throw new AssertionError("rule holder; not instantiable")
    }

    /**
     * The rule itself, for callers that want to compose it or run it over an
     * already-imported {@link JavaClasses}.
     *
     * <p>{@code allowEmptyShould(true)} is deliberate: a service with no REST
     * controllers at all (or one whose controllers live in a package not passed in)
     * must not fail. ArchUnit's default is to fail on an empty match, which here
     * would produce a red build that says nothing about authorization.
     */
    static final ArchRule ENDPOINTS_MUST_DECLARE_AN_AUTHORIZATION_DECISION = methods()
            .that(areHttpMappedControllerMethods())
            .should(declareAnAuthorizationDecision())
            .because("an endpoint with no @PreAuthorize is reachable by any authenticated user; "
                    + "if that is genuinely intended, say so with @AuthenticatedOnly(\"reason\") "
                    + "so it is a decision on the record rather than an omission (KAN-186)")
            .allowEmptyShould(true)

    /**
     * Imports the given packages (production classes only) and checks the rule.
     * Throws {@link AssertionError} listing every offending method if any endpoint
     * declares no authorization decision.
     *
     * @param packages base packages to scan, e.g. {@code "com.cognivio.ai.casemanagement"}
     * @return {@code true} so it reads naturally in a Spock {@code expect:} block
     */
    static boolean assertNoUnauthorizedEndpoints(String... packages) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packages)
        ENDPOINTS_MUST_DECLARE_AN_AUTHORIZATION_DECISION.check(classes)
        return true
    }

    private static DescribedPredicate<JavaMethod> areHttpMappedControllerMethods() {
        return new DescribedPredicate<JavaMethod>("HTTP-mapped methods on a @RestController") {
            @Override
            boolean test(JavaMethod method) {
                return isRestController(method.getOwner()) && isHttpMapped(method)
            }
        }
    }

    private static ArchCondition<JavaMethod> declareAnAuthorizationDecision() {
        return new ArchCondition<JavaMethod>(
                "be annotated with @PreAuthorize or @AuthenticatedOnly") {
            @Override
            void check(JavaMethod method, ConditionEvents events) {
                boolean satisfied = declaresAuthorization(method) || declaresAuthorization(method.getOwner())
                String message = satisfied
                        ? "${method.getFullName()} declares an authorization decision"
                        : "${method.getFullName()} is an HTTP endpoint with neither @PreAuthorize nor " +
                          "@AuthenticatedOnly, so it is open to any authenticated caller"
                events.add(new SimpleConditionEvent(method, satisfied, message))
            }
        }
    }

    private static boolean isRestController(JavaClass type) {
        // isMetaAnnotatedWith covers a service-local composed annotation built on @RestController.
        return type.isAnnotatedWith(RestController) || type.isMetaAnnotatedWith(RestController)
    }

    private static boolean isHttpMapped(JavaMethod method) {
        // @GetMapping and friends are meta-annotated with @RequestMapping; a bare
        // @RequestMapping is directly annotated. Both count.
        return method.isAnnotatedWith(RequestMapping) || method.isMetaAnnotatedWith(RequestMapping)
    }

    private static boolean declaresAuthorization(JavaMethod method) {
        return method.isAnnotatedWith(PreAuthorize) || method.isAnnotatedWith(AuthenticatedOnly)
    }

    private static boolean declaresAuthorization(JavaClass type) {
        return type.isAnnotatedWith(PreAuthorize) || type.isAnnotatedWith(AuthenticatedOnly)
    }
}
