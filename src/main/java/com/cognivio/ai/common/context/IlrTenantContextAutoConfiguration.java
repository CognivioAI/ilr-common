package com.cognivio.ai.common.context;

import com.cognivio.ai.common.security.IlrSecurityProperties;
import com.cognivio.ai.common.security.JwtRoleConverter;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Wires the verified-claim tenant/caller context: the request-scoped
 * {@link TenantContext}, the {@link JwtRoleConverter} and
 * {@link TenantClaimResolver} that read it from the verified JWT, and the
 * {@link TenantContextFilter}, which is placed <em>inside</em> the Spring Security
 * filter chain (just after {@code AuthorizationFilter}) by
 * {@code IlrSecurityAutoConfiguration}. Also owns the {@link IlrSecurityProperties}
 * binding (shared with the security auto-configuration).
 */
@AutoConfiguration
@ConditionalOnClass({Jwt.class, jakarta.servlet.Filter.class})
@EnableConfigurationProperties(IlrSecurityProperties.class)
public class IlrTenantContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtRoleConverter ilrJwtRoleConverter(IlrSecurityProperties properties) {
        return new JwtRoleConverter(properties.getRoleClaims(), properties.getAuthorityPrefix(), properties.getScopeClaim());
    }

    @Bean
    @ConditionalOnMissingBean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    public TenantContext tenantContext() {
        return new TenantContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantClaimResolver tenantClaimResolver(IlrSecurityProperties properties, JwtRoleConverter roleConverter) {
        return new TenantClaimResolver(properties, roleConverter);
    }

    /**
     * The filter itself, as a plain bean. It is <b>not</b> registered with the servlet
     * container; {@code IlrSecurityAutoConfiguration} (and its disabled counterpart)
     * inject it and place it INSIDE the Spring Security chain, immediately after
     * {@code AuthorizationFilter} — see {@link #tenantContextFilterRegistration} for why
     * the servlet-level registration cannot be trusted (KAN-229).
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantContextFilter tenantContextFilter(
            TenantContext tenantContext,
            TenantClaimResolver resolver,
            IlrSecurityProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        // The synthetic dev identity is wired ONLY under dev-permit-all; otherwise both are null and
        // the filter's dev fallback is inert, so a secured deployment is never affected.
        UUID devTenantId = properties.isDevPermitAll() ? parseUuid(properties.getDevTenantId()) : null;
        UUID devUserId = properties.isDevPermitAll() ? parseUuid(properties.getDevUserId()) : null;
        return new TenantContextFilter(tenantContext, resolver, handlerExceptionResolver, devTenantId, devUserId);
    }

    /**
     * A <b>disabled</b> registration for {@link TenantContextFilter}. This bean does
     * nothing at runtime and that is precisely its job — <b>do not delete it as dead
     * code</b> (KAN-229).
     *
     * <h2>Why the servlet-level registration had to go</h2>
     * The filter previously ran as an ordinary servlet filter ordered just after Spring
     * Security's chain ({@code SecurityProperties.DEFAULT_FILTER_ORDER + 10}). That works in
     * a real servlet container and is a silent no-op under the AWS serverless adapter: the
     * adapter's {@code ServerlessServletContext} keeps its filter registrations in a plain
     * {@code HashMap} with no order field at all, so the order set here is discarded at
     * registration time — not merely ignored at dispatch. The filter then ran BEFORE the
     * security chain, {@code SecurityContextHolder} held no {@code Authentication} yet, and
     * {@code TenantContext} was never populated: no tenant claim, no RLS binding, no error.
     * Every {@code @PreAuthorize} over {@code @ilrAuth} then denied, and every tenant-scoped
     * write lost its tenant. The fix is to stop depending on outer-chain ordering entirely
     * and place the filter inside the security chain, where the order is explicit.
     *
     * <h2>Why this bean must nonetheless exist, disabled</h2>
     * Spring Boot auto-registers <em>any</em> {@code Filter}-typed bean with the servlet
     * container ({@code ServletContextInitializerBeans} wraps orphan filter beans in a
     * registration of its own). Without this explicit, disabled registration, the plain
     * {@link #tenantContextFilter} bean would be registered at the servlet level anyway,
     * that copy would run first under the adapter (same broken ordering, authentication
     * still absent, so it does nothing) — and because {@code TenantContextFilter} is a
     * {@code OncePerRequestFilter}, it would set the "already filtered" request attribute
     * and the in-chain invocation would then be SKIPPED on every request. The fix would
     * compile, read correctly and pass a naive test while doing nothing in production.
     * {@code setEnabled(false)} is what suppresses that auto-registration.
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
            TenantContextFilter tenantContextFilter) {
        FilterRegistrationBean<TenantContextFilter> registration =
                new FilterRegistrationBean<>(tenantContextFilter);
        registration.setEnabled(false);
        return registration;
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
