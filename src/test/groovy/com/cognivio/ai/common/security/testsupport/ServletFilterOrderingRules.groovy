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
 * Fitness function for KAN-229: <b>no production class in this estate may call
 * {@code RegistrationBean#setOrder(int)}</b> — i.e. no ILR filter may declare an order at
 * the raw servlet-container level.
 *
 * <p>Every ILR service is deployable as an AWS Lambda behind
 * {@code aws-serverless-java-container-springboot3}. That adapter's synthetic
 * {@code ServerlessServletContext} stores filter registrations in a plain {@code HashMap}
 * and has no order field at all, so an order set via {@code FilterRegistrationBean} is
 * <em>discarded at registration time</em> — not merely ignored at dispatch. The resulting
 * filter order is effectively arbitrary.
 *
 * <p>That produced the KAN-229 defect: {@code TenantContextFilter}, ordered just after Spring
 * Security, actually ran <em>before</em> it on Lambda, found no {@code Authentication} in the
 * {@code SecurityContextHolder}, and silently left {@code TenantContext} empty on every request
 * — no tenant claim, no RLS binding, no error, no log line. A filter that depends on another
 * filter having run must be placed inside the Spring Security chain
 * ({@code http.addFilterAfter(...)}), where the ordering is explicit and adapter-independent,
 * not ordered relative to it in the outer container chain.
 *
 * <p>The rule is deliberately broader than the one defect: it forbids <em>declaring an order</em>
 * at all, because any ILR filter relying on container ordering is exposed to the same failure.
 * The failure is invisible locally, in MockMvc tests and to the compiler — the shape of defect a
 * fitness function exists for.
 *
 * <p><b>Note on the target:</b> {@code setOrder(int)} is declared on
 * {@code org.springframework.boot.web.servlet.RegistrationBean}, the superclass, not on
 * {@code FilterRegistrationBean}; javac records the static receiver type as the call's owner,
 * so a naive owner-equals-{@code RegistrationBean} check matches nothing at a
 * {@code FilterRegistrationBean}-typed call site. The check below therefore accepts any owner
 * assignable to {@code RegistrationBean}. {@code ServletFilterOrderingRulesSpec} proves the rule
 * fires against a deliberate violation, for exactly this reason.
 *
 * <h2>Using it from a service</h2>
 * <pre>{@code
 * class ServletFilterOrderingSpec extends Specification {
 *     def "no ILR filter declares a servlet-container order"() {
 *         expect:
 *         ServletFilterOrderingRules.assertNoRegistrationBeanSetOrder("com.cognivio.ai.casemanagement")
 *     }
 * }
 * }</pre>
 *
 * Requires, in the service's pom: {@code ilr-common} with
 * {@code <classifier>tests</classifier>} and a test-scoped {@code com.tngtech.archunit:archunit}.
 */
final class ServletFilterOrderingRules {

    private static final String SET_ORDER = "setOrder"
    private static final String REGISTRATION_BEAN = "org.springframework.boot.web.servlet.RegistrationBean"
    private static final String INT = int.class.getName()

    private ServletFilterOrderingRules() {
        throw new AssertionError("rule holder; not instantiable")
    }

    /**
     * {@code allowEmptyShould(true)}: most services register no filters of their own and so import
     * classes that match nothing here; that must not fail the build.
     */
    static final ArchRule NO_REGISTRATION_BEAN_SET_ORDER = noClasses()
            .should(callRegistrationBeanSetOrder())
            .because("the AWS serverless adapter's ServletContext keeps filter registrations in an "
                    + "unordered HashMap and drops the declared order entirely, so servlet-level "
                    + "filter ordering is arbitrary on Lambda. A filter that must run after another "
                    + "filter belongs inside the Spring Security chain via addFilterAfter(...) "
                    + "(KAN-229)")
            .allowEmptyShould(true)

    /**
     * Imports the given packages (production classes only) and checks the rule.
     *
     * @param packages base packages to scan, e.g. {@code "com.cognivio.ai.eligibility"}
     * @return {@code true} so it reads naturally in a Spock {@code expect:} block
     */
    static boolean assertNoRegistrationBeanSetOrder(String... packages) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(packages)
        NO_REGISTRATION_BEAN_SET_ORDER.check(classes)
        return true
    }

    private static ArchCondition<JavaClass> callRegistrationBeanSetOrder() {
        return new ArchCondition<JavaClass>("call RegistrationBean.setOrder(int)") {
            @Override
            void check(JavaClass type, ConditionEvents events) {
                for (JavaMethodCall call : type.getMethodCallsFromSelf()) {
                    if (isRegistrationBeanSetOrder(call)) {
                        events.add(SimpleConditionEvent.satisfied(call,
                                "${call.getOriginOwner().getName()} calls RegistrationBean.setOrder(int) at " +
                                        "${call.getSourceCodeLocation()} — place the filter inside the Spring " +
                                        "Security chain with http.addFilterAfter(...) instead"))
                    }
                }
            }
        }
    }

    private static boolean isRegistrationBeanSetOrder(JavaMethodCall call) {
        def target = call.getTarget()
        if (target.getName() != SET_ORDER) {
            return false
        }
        def parameterTypes = target.getRawParameterTypes()
        if (parameterTypes.size() != 1 || parameterTypes.get(0).getName() != INT) {
            return false
        }
        // The owner recorded in the bytecode is the STATIC RECEIVER TYPE (e.g. FilterRegistrationBean),
        // not the class declaring setOrder (RegistrationBean) — hence assignability, not equality.
        def owner = target.getOwner()
        return owner.getName() == REGISTRATION_BEAN || owner.isAssignableTo(REGISTRATION_BEAN)
    }
}
