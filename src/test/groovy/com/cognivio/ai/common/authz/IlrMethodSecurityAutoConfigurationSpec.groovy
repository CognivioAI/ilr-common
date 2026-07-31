package com.cognivio.ai.common.authz

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration
import com.cognivio.ai.common.security.IlrSecurityDisabledAutoConfiguration
import org.springframework.aop.support.AopUtils
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import spock.lang.Specification

/**
 * The wiring decision this ticket turns on.
 *
 * <p>Adding {@code @EnableMethodSecurity} to {@code IlrSecurityAutoConfiguration}
 * would have been one line, and would have produced a suite that reports green on an
 * unenforced control: that class is conditional on {@code ilr.security.enabled=true},
 * and the estate's tests all run with it {@code false}. Every {@code @PreAuthorize}
 * gate would then be inactive in precisely the configuration written to prove it
 * works. The first spec below is the regression guard for that.
 */
class IlrMethodSecurityAutoConfigurationSpec extends Specification {

    def contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IlrMethodSecurityAutoConfiguration))

    def "registers the ilrAuth bean under its contractual name"() {
        expect: "the bean name is what every @PreAuthorize(\"@ilrAuth...\") expression resolves"
        contextRunner.run { context ->
            assert context.containsBean("ilrAuth")
            assert context.getBean("ilrAuth") instanceof IlrAuthorization
        }
    }

    def "method security stays ON when ilr.security.enabled is false"() {
        expect: "otherwise the whole test estate would silently bypass every authorization gate"
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IlrTenantContextAutoConfiguration,
                        IlrSecurityDisabledAutoConfiguration,
                        IlrMethodSecurityAutoConfiguration))
                .withUserConfiguration(WebSecurityInfra, GuardedBean)
                .withPropertyValues("ilr.security.enabled=false")
                .run { context ->
                    assert context.startupFailure == null
                    assert context.containsBean("ilrAuth")
                    assert isEnforced(context.getBean(GuardedBean))
                }
    }

    def "enforces @PreAuthorize on application beans by default"() {
        expect: "asserted behaviourally: a bean-name check would pass over a non-functioning interceptor"
        contextRunner
                .withUserConfiguration(GuardedBean)
                .run { context ->
                    assert AopUtils.isAopProxy(context.getBean(GuardedBean))
                    assert isEnforced(context.getBean(GuardedBean))
                }
    }

    def "backs off entirely when method security is explicitly disabled"() {
        expect: "the documented escape hatch, independent of ilr.security.enabled"
        contextRunner
                .withUserConfiguration(GuardedBean)
                .withPropertyValues("ilr.security.method-security.enabled=false")
                .run { context ->
                    assert !context.containsBean("ilrAuth")
                    assert !AopUtils.isAopProxy(context.getBean(GuardedBean))
                    assert context.getBean(GuardedBean).privileged() == "done"
                }
    }

    /** True if invoking the guarded method was refused rather than executed. */
    private static boolean isEnforced(GuardedBean bean) {
        try {
            bean.privileged()
            return false
        } catch (AccessDeniedException ignored) {
            return true
        }
    }

    def "the registered ilrAuth denies when there is no TenantContext bean"() {
        expect: "a service missing the tenant-context auto-configuration must deny, not fail to start"
        contextRunner.run { context ->
            IlrAuthorization authorization = context.getBean("ilrAuth", IlrAuthorization)
            assert !authorization.can("CASE_SIGNOFF")
            assert !authorization.isSelf(UUID.randomUUID())
        }
    }

    def "the registered ilrAuth denies outside a request even when the bean exists"() {
        expect: "dereferencing the request-scoped proxy off-request must not blow up the caller"
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        IlrTenantContextAutoConfiguration,
                        IlrMethodSecurityAutoConfiguration))
                .withUserConfiguration(WebSecurityInfra)
                .run { context ->
                    IlrAuthorization authorization = context.getBean("ilrAuth", IlrAuthorization)
                    assert !authorization.can("CASE_SIGNOFF")
                }
    }

    def "a service may override ilrAuth with its own bean"() {
        given:
        def custom = new IlrAuthorization({ null })

        expect:
        contextRunner
                .withBean("ilrAuth", IlrAuthorization, { custom })
                .run { context ->
                    assert context.getBean("ilrAuth").is(custom)
                }
    }

    /**
     * A bean carrying a real capability gate. The expression can never be satisfied in
     * these specs (there is no request and hence no identity), so "was it refused?" is
     * a direct read on whether the interceptor is installed and working.
     */
    @Component
    static class GuardedBean {

        @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")
        String privileged() {
            return "done"
        }
    }

    @EnableWebSecurity
    static class WebSecurityInfra {

        /** The tenant-context filter registration needs an MVC handlerExceptionResolver. */
        @Bean
        HandlerExceptionResolver handlerExceptionResolver() {
            return { request, response, handler, ex -> null } as HandlerExceptionResolver
        }
    }
}
