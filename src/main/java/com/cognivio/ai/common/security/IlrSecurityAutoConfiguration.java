package com.cognivio.ai.common.security;

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

/**
 * OAuth2 resource-server security for every ILR service. Verifies the incoming
 * bearer JWT against the configured Cognito issuer/JWKS, requires authentication
 * on everything except the configured permit-list, and maps Cognito group/role
 * claims to Spring authorities.
 *
 * <p>Active when {@code ilr.security.enabled} is {@code true} (the default).
 * When it is {@code false}, {@link IlrSecurityDisabledAutoConfiguration} installs
 * a permit-all chain instead so a service can start / run standalone MockMvc
 * tests with security effectively off.
 *
 * <p><b>Testability:</b> the {@link JwtDecoder} is resolved via an
 * {@link ObjectProvider}, so a test can register its own {@code JwtDecoder} bean
 * (e.g. a Nimbus decoder over a local RSA test key) and this configuration will
 * use it — no live Cognito JWKS endpoint is required. If no decoder bean is
 * supplied and neither {@code issuer-uri} nor {@code jwks-uri} is set, the chain
 * <b>fails closed</b>: startup fails unless {@code ilr.security.dev-permit-all=true}
 * is explicitly set, so a misconfiguration never silently disables authentication.
 */
@AutoConfiguration
@AutoConfigureAfter(IlrTenantContextAutoConfiguration.class)
@ConditionalOnClass({SecurityFilterChain.class, JwtDecoder.class})
@ConditionalOnProperty(prefix = "ilr.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IlrSecurityProperties.class)
public class IlrSecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IlrSecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationConverter ilrJwtAuthenticationConverter(JwtRoleConverter roleConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter::convert);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint ilrAuthenticationEntryPoint(ObjectProvider<ObjectMapper> objectMapper) {
        return new RestAuthenticationEntryPoint(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler ilrAccessDeniedHandler(ObjectProvider<ObjectMapper> objectMapper) {
        return new RestAccessDeniedHandler(objectMapper.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean(name = "ilrSecurityFilterChain")
    public SecurityFilterChain ilrSecurityFilterChain(
            HttpSecurity http,
            IlrSecurityProperties properties,
            ObjectProvider<JwtDecoder> jwtDecoderProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable(() -> buildDecoder(properties));
        String[] permitList = properties.getPermitList().toArray(String[]::new);

        if (decoder != null) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers(permitList).permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                            .decoder(decoder)
                            .jwtAuthenticationConverter(jwtAuthenticationConverter)));
        } else if (properties.isDevPermitAll()) {
            // Only reachable via the explicit ilr.security.dev-permit-all=true opt-in.
            log.warn("ilr-common: security enabled but no JwtDecoder/issuer/jwks configured, and "
                    + "ilr.security.dev-permit-all=true — running DEV MODE with ALL endpoints permitted. "
                    + "This must NEVER be set outside local development.");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            // Fail CLOSED: a security foundation must never boot wide open on a misconfiguration.
            // A missing issuer/jwks in a real deployment refuses to start rather than silently
            // disabling authentication. Use ilr.security.enabled=false (tests) or dev-permit-all=true
            // (local dev) to intentionally run without token verification.
            throw new IllegalStateException(
                    "ilr-common: security is enabled but no JwtDecoder is available and neither "
                            + "ilr.security.issuer-uri nor ilr.security.jwks-uri is set. Refusing to start "
                            + "with authentication silently disabled (fail-closed). Set an issuer/jwks (or "
                            + "provide a JwtDecoder bean); or set ilr.security.enabled=false for tests, or "
                            + "ilr.security.dev-permit-all=true for local development.");
        }
        return http.build();
    }

    /** Builds a Nimbus decoder from jwks-uri (preferred) or issuer-uri; {@code null} if neither is set. */
    private static JwtDecoder buildDecoder(IlrSecurityProperties properties) {
        if (StringUtils.hasText(properties.getJwksUri())) {
            return NimbusJwtDecoder.withJwkSetUri(properties.getJwksUri()).build();
        }
        if (StringUtils.hasText(properties.getIssuerUri())) {
            // Discovers JWKS from the issuer's OIDC metadata and validates iss/exp/nbf.
            return JwtDecoders.fromIssuerLocation(properties.getIssuerUri());
        }
        return null;
    }
}
