package com.cognivio.ai.common.http.violationfixture.violating.client

import groovy.transform.CompileStatic
import org.springframework.web.client.RestClient

/**
 * Deliberate KAN-236 violations — the ONLY hand-rolled {@code RestClient} constructions inside a
 * {@code ..client..} package that may exist in this repository.
 *
 * <p>They exist so {@code InternalClientConstructionRules} can be proved to actually fire. A fitness
 * function that cannot fail is worse than none, because it reads as evidence that the estate is
 * clean when nothing was ever checked — the KAN-226 rule shipped in exactly that broken state once
 * (see {@code StringRequestMatchersFixture}).
 *
 * <p>Never referenced by production code, never scanned by the production rule (which imports with
 * {@code DO_NOT_INCLUDE_TESTS}), and excluded from the published {@code tests}-classifier jar by the
 * maven-jar-plugin config in this module's pom — shipping it would put a violating class onto every
 * consumer's test classpath, where the consumer's own copy of the rule would flag it.
 *
 * <p>{@code @CompileStatic} is required: dynamic Groovy routes calls through
 * {@code ScriptBytecodeAdapter} and emits no direct method call for ArchUnit to see.
 */
@CompileStatic
class DirectRestClientFixture {

    /** The exact pre-KAN-236 pattern every ILR client used: an injected builder, no credential. */
    RestClient fromInjectedBuilder(RestClient.Builder builder) {
        return builder.baseUrl('http://knowledge-service:8090').build()
    }

    /** The static shortcut — same defect, different entry point. */
    RestClient fromStaticFactory() {
        return RestClient.create('http://knowledge-service:8090')
    }

    /** Also banned: {@code RestClient.builder()} sidesteps the injected, customized builder entirely. */
    RestClient fromStaticBuilder() {
        return RestClient.builder().baseUrl('http://knowledge-service:8090').build()
    }
}
