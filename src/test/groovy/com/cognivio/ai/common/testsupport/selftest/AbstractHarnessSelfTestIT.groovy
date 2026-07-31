package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.testsupport.integration.AbstractIlrIntegrationSpec
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.ResultActions

/**
 * Base for {@code ilr-common}'s own harness proof tests.
 *
 * <p>Adds only the one thing a real service would not need: {@code spring.flyway.locations}, because
 * the harness schema deliberately lives at {@code db/harness/migration} rather than the Flyway
 * default {@code db/migration}. Keeping it off the default path stops these fixtures being picked up
 * by any consumer whose Flyway locations include the classpath default, and is belt-and-braces with
 * the {@code test-jar} exclusion in the pom.
 *
 * <p>Everything else — container, role split, forced security and RLS posture, bearer tokens —
 * comes from {@link AbstractIlrIntegrationSpec} unchanged, which is the point: if these tests needed
 * to work around the base class, the base class would be wrong for the services too.
 */
@TestPropertySource(properties = ['spring.flyway.locations=classpath:db/harness/migration'])
abstract class AbstractHarnessSelfTestIT extends AbstractIlrIntegrationSpec {

    /** Jackson rather than {@code JsonSlurper}: already on the classpath, so no dependency is added
     *  purely for a test helper. */
    private static final ObjectMapper JSON = new ObjectMapper()

    /** The parsed JSON body of a MockMvc result — a {@code Map} or {@code List}. */
    protected static Object body(ResultActions result) {
        JSON.readValue(result.andReturn().response.contentAsString, Object)
    }
}
