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
import org.springframework.boot.autoconfigure.security.SecurityProperties;
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
 * {@link TenantContextFilter} that runs just after the Spring Security filter
 * chain. Also owns the {@link IlrSecurityProperties} binding (shared with the
 * security auto-configuration).
 */
@AutoConfiguration
@ConditionalOnClass({Jwt.class, jakarta.servlet.Filter.class})
@EnableConfigurationProperties(IlrSecurityProperties.class)
public class IlrTenantContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtRoleConverter ilrJwtRoleConverter(IlrSecurityProperties properties) {
        return new JwtRoleConverter(properties.getRoleClaims(), properties.getAuthorityPrefix());
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

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
            TenantContext tenantContext,
            TenantClaimResolver resolver,
            IlrSecurityProperties properties,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        // The synthetic dev identity is wired ONLY under dev-permit-all; otherwise both are null and
        // the filter's dev fallback is inert, so a secured deployment is never affected.
        UUID devTenantId = properties.isDevPermitAll() ? parseUuid(properties.getDevTenantId()) : null;
        UUID devUserId = properties.isDevPermitAll() ? parseUuid(properties.getDevUserId()) : null;
        TenantContextFilter filter =
                new TenantContextFilter(tenantContext, resolver, handlerExceptionResolver, devTenantId, devUserId);
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        // Run immediately after Spring Security's filter chain (DEFAULT_FILTER_ORDER = -100),
        // so authentication is present in the SecurityContext, but before the controller.
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 10);
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
