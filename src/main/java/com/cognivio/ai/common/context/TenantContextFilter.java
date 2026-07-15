package com.cognivio.ai.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
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

    // Roles granted to the synthetic dev identity so role-gated endpoints are reachable locally.
    private static final Set<String> DEV_ROLES = Set.of("firm-admin", "consultant", "reviewer", "applicant");

    private final TenantContext tenantContext;
    private final TenantClaimResolver resolver;
    private final HandlerExceptionResolver handlerExceptionResolver;
    // Dev fallback identity, active ONLY under dev-permit-all (never in a secured deployment).
    private final UUID devTenantId;
    private final UUID devUserId;

    public TenantContextFilter(TenantContext tenantContext, TenantClaimResolver resolver,
                               HandlerExceptionResolver handlerExceptionResolver,
                               UUID devTenantId, UUID devUserId) {
        this.tenantContext = tenantContext;
        this.resolver = resolver;
        this.handlerExceptionResolver = handlerExceptionResolver;
        this.devTenantId = devTenantId;
        this.devUserId = devUserId;
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
        } else if (devTenantId != null) {
            // dev-permit-all mode: no JWT, so seed a deterministic dev tenant/user. Without this,
            // TenantContext stays empty and every write fails the NOT NULL tenant_id constraint.
            // devTenantId is non-null ONLY when dev-permit-all is enabled (see the auto-configuration),
            // so this branch can never activate in a secured deployment.
            tenantContext.populate(devTenantId, devTenantId, devUserId, DEV_ROLES);
        }
        chain.doFilter(request, response);
    }
}
