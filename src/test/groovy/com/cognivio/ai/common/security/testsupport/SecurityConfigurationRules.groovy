package com.cognivio.ai.common.security.testsupport

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
 * Fitness function for KAN-226: <b>no production class in this estate may call
 * {@code AuthorizeHttpRequestsConfigurer...requestMatchers(String...)}.</b>
 *
 * <p>Every ILR service is deployable as an AWS Lambda behind
 * {@code aws-serverless-java-container-springboot3}. The {@code String} overload of
 * {@code requestMatchers} defers the MVC-vs-Ant decision to
 * {@code AbstractRequestMatcherRegistry.resolve()}, which reads
 * {@code ServletRegistration.getMappings()} off the {@code ServletContext}. The adapter's
 * synthetic {@code ServletContext} returns {@code null} there, so Spring Security NPEs and
 * <em>every</em> request 500s. It is an unimplemented upstream stub, so no version bump
 * fixes it — the call site simply must not exist.
 *
 * <p>The failure only appears in a deployed Lambda: it is invisible in local runs, in
 * MockMvc tests, and to the compiler. That is exactly the shape of defect a fitness
 * function is for. Build the matchers explicitly instead — see
 * {@code IlrSecurityAutoConfiguration.toPermitMatchers}, which also documents why the
 * {@code UrlPathHelper} argument is mandatory.
 *
 * <h2>Using it from a service</h2>
 * <pre>{@code
 * class SecurityConfigurationSpec extends Specification {
 *     def "no requestMatchers(String...) call sites"() {
 *         expect:
 *         SecurityConfigurationRules.assertNoStringRequestMatchers("com.cognivio.ai.casemanagement")
 *     }
 * }
 * }</pre>
 *
 * Requires, in the service's pom: {@code ilr-common} with
 * {@code <classifier>tests</classifier>} and a test-scoped {@code com.tngtech.archunit:archunit}.
 */
final class SecurityConfigurationRules {

    private static final String MATCHER_METHOD = "requestMatchers"
    /** {@code [Ljava.lang.String;} — ArchUnit reports array parameter types by their JVM binary name. */
    private static final String STRING_ARRAY = String[].class.getName()

    private SecurityConfigurationRules() {
        throw new AssertionError("rule holder; not instantiable")
    }

    /**
     * {@code allowEmptyShould(true)}: a service with no security configuration of its own
     * (the common case — it inherits {@code IlrSecurityAutoConfiguration}) imports classes
     * that match nothing here, and must not fail for it.
     */
    static final ArchRule NO_STRING_REQUEST_MATCHERS = noClasses()
            .should(callRequestMatchersWithStrings())
            .because("requestMatchers(String...) resolves servlet mappings from the ServletContext, "
                    + "which the AWS serverless adapter stubs out to null — every request then 500s "
                    + "with an NPE inside Spring Security. Pass pre-built RequestMatcher instances "
                    + "instead (KAN-226)")
            .allowEmptyShould(true)

    /**
     * Imports the given packages (production classes only) and checks the rule.
     *
     * @param packages base packages to scan, e.g. {@code "com.cognivio.ai.eligibility"}
     * @return {@code true} so it reads naturally in a Spock {@code expect:} block
     */
    static boolean assertNoStringRequestMatchers(String... packages) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packages)
        NO_STRING_REQUEST_MATCHERS.check(classes)
        return true
    }

    private static ArchCondition<JavaClass> callRequestMatchersWithStrings() {
        return new ArchCondition<JavaClass>("call requestMatchers(String...)") {
            @Override
            void check(JavaClass type, ConditionEvents events) {
                for (JavaMethodCall call : type.getMethodCallsFromSelf()) {
                    if (isStringRequestMatchers(call)) {
                        events.add(SimpleConditionEvent.satisfied(call,
                                "${call.getOriginOwner().getName()} calls requestMatchers(String...) at " +
                                        "${call.getSourceCodeLocation()} — use requestMatchers(RequestMatcher...)"))
                    }
                }
            }
        }
    }

    private static boolean isStringRequestMatchers(JavaMethodCall call) {
        def target = call.getTarget()
        if (target.getName() != MATCHER_METHOD) {
            return false
        }
        // Varargs compile to a single String[] parameter; that is the vulnerable overload.
        def parameterTypes = target.getRawParameterTypes()
        return parameterTypes.size() == 1 && parameterTypes.get(0).getName() == STRING_ARRAY
    }
}
