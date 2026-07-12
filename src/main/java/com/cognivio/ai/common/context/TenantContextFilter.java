package com.cognivio.ai.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Populates the request-scoped {@link TenantContext} from the <b>verified</b>
 * {@link Jwt} that Spring Security has already placed in the
 * {@link SecurityContextHolder}. Registered to run <em>after</em> the Spring
 * Security filter chain (see {@code IlrTenantContextAutoConfiguration}), so
 * authentication has already happened by the time it runs.
 *
 * <p>Replaces the old {@code TenantHeaderFilter}, which trusted an
 * {@code X-Tenant-Id} header. This filter reads nothing from client headers.
 *
 * <p>If the request is not authenticated with a JWT (e.g. a permit-listed
 * endpoint), the context is simply left empty. If it <em>is</em> authenticated
 * but the token has no tenant claim, {@link MissingTenantClaimException} is
 * raised and rendered (as 403) through the MVC {@link HandlerExceptionResolver},
 * i.e. via {@code CommonExceptionHandler}, so the response matches the platform
 * error shape.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantContext tenantContext;
    private final TenantClaimResolver resolver;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public TenantContextFilter(TenantContext tenantContext, TenantClaimResolver resolver,
                               HandlerExceptionResolver handlerExceptionResolver) {
        this.tenantContext = tenantContext;
        this.resolver = resolver;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getToken() instanceof Jwt jwt) {
            try {
                resolver.resolveInto(jwt, tenantContext);
            } catch (MissingTenantClaimException ex) {
                // Render via the MVC exception resolver (CommonExceptionHandler) and stop the chain.
                handlerExceptionResolver.resolveException(request, response, null, ex);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
