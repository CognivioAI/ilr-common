package com.cognivio.ai.common.security

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification

/**
 * The security foundation must <b>fail closed</b>. When {@code ilr.security} is
 * enabled but no {@code JwtDecoder} / {@code issuer-uri} / {@code jwks-uri} is
 * configured, a service must REFUSE TO START rather than silently boot with every
 * endpoint permitted. The only way to run without token verification is the two
 * explicit opt-ins: {@code ilr.security.enabled=false} (tests) or
 * {@code ilr.security.dev-permit-all=true} (local dev).
 *
 * <p>This is the regression guard for the original fail-open default, where a
 * forgotten issuer URI in production would have disabled authentication entirely.
 */
class IlrSecurityAutoConfigurationSpec extends Specification {

    def contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    IlrTenantContextAutoConfiguration,
                    IlrSecurityAutoConfiguration))
            .withUserConfiguration(WebSecurityInfra)

    def "fails to start when security is enabled but no issuer/jwks/decoder is configured"() {
        expect: "startup fails with the fail-closed IllegalStateException rather than booting permit-all"
        contextRunner
                .withPropertyValues("ilr.security.enabled=true")
                .run { context ->
                    assert context.startupFailure != null
                    def cause = rootCause(context.startupFailure, IllegalStateException)
                    assert cause != null
                    assert cause.message.contains("fail-closed")
                }
    }

    def "starts with a permit-all chain only when dev-permit-all is explicitly enabled"() {
        expect:
        contextRunner
                .withPropertyValues(
                        "ilr.security.enabled=true",
                        "ilr.security.dev-permit-all=true")
                .run { context ->
                    assert context.startupFailure == null
                    assert context.getBean(SecurityFilterChain) != null
                }
    }

    private static Throwable rootCause(Throwable thrown, Class<? extends Throwable> type) {
        for (Throwable c = thrown; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return c
            }
        }
        null
    }

    @EnableWebSecurity
    static class WebSecurityInfra {

        /**
         * The tenant-context filter registration depends on the MVC
         * {@code handlerExceptionResolver}; supply a no-op one so the context can
         * refresh without pulling in full WebMvc auto-configuration.
         */
        @Bean
        HandlerExceptionResolver handlerExceptionResolver() {
            return { request, response, handler, ex -> null } as HandlerExceptionResolver
        }
    }
}
