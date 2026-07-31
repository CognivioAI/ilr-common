package com.cognivio.ai.common.testsupport.integration

import com.cognivio.ai.common.support.TestJwtFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * Gives the integration context a working {@link JwtDecoder} without any external dependency, so
 * tests authenticate as a real, signature-verified caller.
 *
 * <h2>What this deliberately does not do</h2>
 * It does not stub, disable or replace any part of the security chain. {@code IlrSecurityAutoConfiguration}
 * resolves its decoder through an {@code ObjectProvider<JwtDecoder>} specifically so a test can supply
 * one (its javadoc calls this out as being "for testability"), which means the filter chain, the
 * {@code RestAuthenticationEntryPoint}, the {@code RestAccessDeniedHandler}, the {@code JwtRoleConverter},
 * the {@code TenantContextFilter} and the {@code @PreAuthorize} interceptors under test are all the
 * <b>production</b> ones. The only substitution is <em>which public key verifies the signature</em>.
 *
 * <p>That is the difference between this harness and the estate's existing
 * {@code MockMvcBuilders.standaloneSetup(...)} specs, and between it and the usual alternatives:
 * {@code @WithMockUser} bypasses token processing (so it cannot catch a broken role-claim mapping or
 * a missing tenant claim), and WireMock-ing a JWKS endpoint adds a moving part that proves nothing
 * extra, since {@link TestJwtFactory} already performs genuine RS256 verification against a locally
 * generated key pair.
 *
 * <p>Imported by {@link AbstractIlrIntegrationSpec}; a spec extending that base gets it for free.
 */
@TestConfiguration(proxyBeanMethods = false)
class IlrSecurityTestConfiguration {

    /** One key pair per application context. Context caching means this is generated rarely. */
    @Bean
    TestJwtFactory ilrTestJwtFactory() {
        new TestJwtFactory()
    }

    /**
     * The decoder the real filter chain will pick up via its {@code ObjectProvider}.
     *
     * <p>{@code @Primary} guards against a consuming service that also defines a {@code JwtDecoder}
     * bean: in the integration context the test key must win, or every request 401s on a signature
     * the test cannot produce.
     */
    @Bean
    @Primary
    JwtDecoder ilrTestJwtDecoder(TestJwtFactory jwtFactory) {
        jwtFactory.decoder
    }

    /** Mints the {@code Authorization: Bearer ...} request post-processors used by specs. */
    @Bean
    IlrBearerTokens ilrBearerTokens(TestJwtFactory jwtFactory) {
        new IlrBearerTokens(jwtFactory)
    }
}
