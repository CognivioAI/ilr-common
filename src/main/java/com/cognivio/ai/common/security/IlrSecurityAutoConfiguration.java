package com.cognivio.ai.common.security;

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration;
import com.cognivio.ai.common.context.TenantContextFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;

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
            AccessDeniedHandler accessDeniedHandler,
            TenantContextFilter tenantContextFilter) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        // KAN-229: the tenant context is populated INSIDE this chain, immediately after
        // AuthorizationFilter — the last filter in the chain — so the verified Authentication is
        // guaranteed to be in the SecurityContextHolder by the time it runs. It used to be an
        // ordinary servlet filter ordered after the chain, which the AWS serverless adapter
        // silently reorders (see IlrTenantContextAutoConfiguration for the full mechanism), leaving
        // TenantContext empty on every Lambda request. Added before the three-way branch below so
        // all three postures — verified JWT, dev-permit-all, fail-closed — get identical placement.
        http.addFilterAfter(tenantContextFilter, AuthorizationFilter.class);

        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable(() -> buildDecoder(properties));
        RequestMatcher[] permitMatchers = toPermitMatchers(properties.getPermitList());

        if (decoder != null) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers(permitMatchers).permitAll()
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

    /**
     * Builds the permit-list matchers explicitly. <b>Never call
     * {@code requestMatchers(String...)} anywhere in this estate</b> (KAN-226).
     *
     * <p>Two independent reasons, both of which produce a broken deployment rather than a
     * compile error, which is why the construction is isolated here:
     *
     * <ol>
     *   <li><b>The String overload NPEs under the serverless adapter.</b> It defers to
     *       {@code AbstractRequestMatcherRegistry.resolve()}, which calls
     *       {@code ServletRegistration.getMappings()} on the {@code ServletContext} to decide
     *       between MVC and Ant matching. The synthetic {@code ServletContext} of
     *       {@code aws-serverless-java-container} returns {@code null} there (an unimplemented
     *       upstream stub, not a version-bump fix), so every request 500s with an NPE inside
     *       Spring Security. That is the whole of KAN-226.</li>
     *   <li><b>The {@link UrlPathHelper} argument is load-bearing.</b> Without it,
     *       {@link AntPathRequestMatcher} matches on {@code servletPath + pathInfo}. The
     *       adapter's {@code ServerlessHttpServletRequest} hard-codes {@code servletPath = ""}
     *       and never populates it, so the permit list would silently match nothing and
     *       {@code /actuator/health} would 401 instead of bypassing authentication — a
     *       quieter, worse failure than the NPE. With the helper, matching uses
     *       {@code requestURI - contextPath}, which is correct in a real servlet container
     *       <em>and</em> under the adapter.</li>
     * </ol>
     *
     * <p>{@link AntPathRequestMatcher} is deprecated in Spring Security 6.5 and removed in 7.0;
     * keeping every construction in this one method is what makes the eventual migration to
     * {@code PathPatternRequestMatcher} a single-method change.
     *
     * <p>Package-private rather than private so
     * {@code IlrSecurityAutoConfigurationSpec} can assert the real production matchers against
     * both request shapes; the test exists precisely because both failure modes above are
     * invisible to the compiler.
     */
    static RequestMatcher[] toPermitMatchers(List<String> patterns) {
        UrlPathHelper pathHelper = new UrlPathHelper();
        return patterns.stream()
                .map(pattern -> (RequestMatcher) new AntPathRequestMatcher(pattern, null, true, pathHelper))
                .toArray(RequestMatcher[]::new);
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
