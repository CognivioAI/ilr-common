package com.cognivio.ai.common.testsupport.selftest

import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * The minimal application {@code ilr-common}'s own integration tests boot.
 *
 * <p>{@code ilr-common} is a library and has no application of its own, so without this there is
 * nothing for {@code @SpringBootTest} to start and the harness could only be proved indirectly, in
 * the first consuming service — which is exactly the sequencing KAN-190 warns against (the harness
 * blocks KAN-207 to KAN-211, so it must be known-good before they start). This app is deliberately
 * as small as a service can be while still exercising every layer the harness claims to cover: an
 * entity validated against a Flyway-migrated schema, a repository under a real transaction, a
 * {@code @PreAuthorize}-gated controller behind the real filter chain, and a tenant-scoped table with
 * RLS forced on.
 *
 * <p>Incidentally this is also the first time {@code ilr-common}'s own security, tenant-context and
 * RLS auto-configurations are verified in situ rather than through
 * {@code ApplicationContextRunner}: the auto-configurations are picked up here from
 * {@code META-INF/spring/...AutoConfiguration.imports}, in ordinary Spring Boot startup order, which
 * is how the KAN-189 ordering defect became observable at all.
 *
 * <p><b>Excluded from the published {@code tests}-classifier jar</b> (see the maven-jar-plugin
 * {@code test-jar} execution in the pom). A stray {@code @SpringBootConfiguration} on a consumer's
 * test classpath would break {@code @SpringBootTest}'s configuration search in that service.
 */
@SpringBootApplication
class HarnessApplication {

    static void main(String[] args) {
        throw new UnsupportedOperationException('Test fixture application; started only by @SpringBootTest')
    }
}
