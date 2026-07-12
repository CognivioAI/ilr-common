package com.cognivio.ai.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Installed when {@code ilr.security.enabled=false}. Provides a permit-all
 * {@link SecurityFilterChain} so a service can start (and run full-context
 * MockMvc tests) with authentication effectively disabled, without every
 * endpoint 401-ing because no resource server is configured.
 */
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "ilr.security", name = "enabled", havingValue = "false")
public class IlrSecurityDisabledAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "ilrSecurityFilterChain")
    public SecurityFilterChain ilrSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
