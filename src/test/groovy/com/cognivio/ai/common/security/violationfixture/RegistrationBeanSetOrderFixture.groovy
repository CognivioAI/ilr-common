package com.cognivio.ai.common.security.violationfixture

import groovy.transform.CompileStatic
import jakarta.servlet.Filter
import org.springframework.boot.web.servlet.FilterRegistrationBean

/**
 * A deliberate KAN-229 violation — the ONLY {@code RegistrationBean.setOrder(int)} call site
 * that may exist in this repository.
 *
 * <p>It exists so {@code ServletFilterOrderingRules} can be proved to actually fire. The trap
 * here is that {@code setOrder} is declared on {@code RegistrationBean}, the superclass, while
 * javac records the <em>static receiver type</em> ({@code FilterRegistrationBean}) as the call's
 * owner in the bytecode: a rule written against the declaring class alone matches nothing and
 * passes happily on code containing the exact defect it forbids. The KAN-226 rule in this same
 * package had the equivalent bug with array parameter type names, which is why every rule here
 * ships with a fixture. A fitness function that cannot fail is worse than none, because it reads
 * as evidence.
 *
 * <p>Never referenced by production code, never scanned by the production rule (which imports
 * with {@code DO_NOT_INCLUDE_TESTS}), and excluded from the published {@code tests}-classifier
 * jar by the maven-jar-plugin config in this module's pom.
 *
 * <p>{@code @CompileStatic} is required: dynamic Groovy would route the call through
 * {@code ScriptBytecodeAdapter} call sites and emit no direct method call for ArchUnit to see.
 */
@CompileStatic
class RegistrationBeanSetOrderFixture {

    FilterRegistrationBean<Filter> register(Filter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<Filter>(filter)
        registration.setOrder(-90)
        return registration
    }
}
