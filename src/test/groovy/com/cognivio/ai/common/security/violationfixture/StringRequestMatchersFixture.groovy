package com.cognivio.ai.common.security.violationfixture

import groovy.transform.CompileStatic
import org.springframework.security.config.annotation.web.builders.HttpSecurity

/**
 * A deliberate KAN-226 violation — the ONLY {@code requestMatchers(String...)} call site
 * that may exist in this repository.
 *
 * <p>It exists so {@code SecurityConfigurationRules} can be proved to actually fire. The
 * first draft of that rule compared the parameter type against {@code "java.lang.String[]"}
 * while ArchUnit reports array types by their JVM binary name ({@code [Ljava.lang.String;}),
 * so it matched nothing and passed happily on code containing the exact defect it was written
 * to forbid. A fitness function that cannot fail is worse than none, because it reads as
 * evidence.
 *
 * <p>Never referenced by production code, never scanned by the production rule (which imports
 * with {@code DO_NOT_INCLUDE_TESTS}), and excluded from the published {@code tests}-classifier
 * jar by the maven-jar-plugin config in this module's pom.
 *
 * <p>{@code @CompileStatic} is required: dynamic Groovy would route the call through
 * {@code ScriptBytecodeAdapter} call sites and emit no direct method call for ArchUnit to see.
 */
@CompileStatic
class StringRequestMatchersFixture {

    void wire(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health").permitAll())
    }
}
