package com.cognivio.ai.common.security;

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration;
import com.cognivio.ai.common.context.TenantContextFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Installed when {@code ilr.security.enabled=false}. Provides a permit-all
 * {@link SecurityFilterChain} so a service can start (and run full-context
 * MockMvc tests) with authentication effectively disabled, without every
 * endpoint 401-ing because no resource server is configured.
 */
@AutoConfiguration
@AutoConfigureAfter(IlrTenantContextAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "ilr.security", name = "enabled", havingValue = "false")
public class IlrSecurityDisabledAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "ilrSecurityFilterChain")
    public SecurityFilterChain ilrSecurityFilterChain(
            HttpSecurity http, ObjectProvider<TenantContextFilter> tenantContextFilter) throws Exception {
        // KAN-229: this chain deliberately does NOT call .sessionManagement(...). Do not "harmonize"
        // it with IlrSecurityAutoConfiguration by adding sessionCreationPolicy(STATELESS) here —
        // that is what would INTRODUCE the bug, not prevent it. Without a sessionManagement() call
        // no SessionManagementConfigurer is applied, so no SessionManagementFilter and no
        // session-fixation strategy exist in this chain at all. Adding the policy would install
        // both, and the default strategy (ChangeSessionIdAuthenticationStrategy — the policy does
        // not suppress it, see the comment in IlrSecurityAutoConfiguration) calls
        // request.changeSessionId(), an unimplemented stub that throws under the AWS serverless
        // adapter. If a session policy is ever genuinely needed here, it must be paired with
        // .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()) exactly as the
        // secured chain does.
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        // KAN-229: same in-chain placement as the secured chain, for the same reason — the filter
        // must never be registered at the servlet-container level, where the AWS serverless
        // adapter discards its declared order. Optional here: this configuration is conditional
        // only on ilr.security.enabled=false, so it can be active in a context where
        // IlrTenantContextAutoConfiguration is not (no Jwt/Filter on the classpath, or a service
        // that supplied its own context wiring). Under this permit-all chain the filter's only
        // effect is the dev-permit-all identity seed; with no bean, tenant context is simply
        // unpopulated, which is exactly what happened before this change.
        tenantContextFilter.ifAvailable(filter -> http.addFilterAfter(filter, AuthorizationFilter.class));

        return http.build();
    }
}
