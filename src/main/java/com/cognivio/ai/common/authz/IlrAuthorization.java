package com.cognivio.ai.common.authz;

import com.cognivio.ai.common.context.TenantContext;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bean behind {@code @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")}.
 *
 * <p><b>Why this reads {@link TenantContext} and not Spring authorities.</b> The
 * obvious form, {@code @PreAuthorize("hasRole('reviewer')")}, would break local
 * development. Under {@code ilr.security.dev-permit-all=true} there is no JWT, so
 * Spring holds no granted authority at all; {@code TenantContextFilter} instead
 * seeds a synthetic dev identity with the full {@code DEV_ROLES} set straight into
 * {@code TenantContext}. A {@code hasRole()} gate reads authorities and would
 * therefore deny <em>everything</em> locally — silently, and only in dev, which is
 * the worst possible place for a security check to behave differently. Reading
 * {@code TenantContext} makes the gate agree with the identity the rest of the
 * request already uses (tenant id, user id, RLS binding), in dev and in production
 * alike.
 *
 * <p><b>Fail closed.</b> Every path that cannot positively establish the caller's
 * capability returns {@code false}:
 * <ul>
 *   <li>no {@code TenantContext} bean, or no request bound to the thread;</li>
 *   <li>a context that was never populated ({@code isPresent()} false), e.g. a
 *       permit-listed endpoint;</li>
 *   <li>a permission name that does not resolve — a typo in an annotation denies,
 *       it never opens the endpoint up. This is logged at WARN because it is a bug,
 *       not a legitimate denial.</li>
 * </ul>
 *
 * <p>This class covers layer L1 (capability) only. "Is this <em>specific record</em>
 * one the caller may touch" is L2 and belongs in the service layer — with the
 * exception of {@link #isSelf(UUID)}, which is the one L2 predicate expressible in
 * an annotation and is provided here so identity endpoints can write
 * {@code @PreAuthorize("@ilrAuth.isSelf(#userId) or @ilrAuth.can('USER_ERASURE_REQUEST')")}.
 */
public class IlrAuthorization {

    private static final Logger log = LoggerFactory.getLogger(IlrAuthorization.class);

    private final Supplier<TenantContext> tenantContextSupplier;

    /**
     * @param tenantContextSupplier supplies the current request's context. In the
     *        application this is the request-scoped {@code TenantContext} bean's
     *        {@code ObjectProvider}; in tests it can be any supplier. May itself
     *        return {@code null} or throw when no request is bound — both are
     *        treated as "no identity", i.e. deny.
     */
    public IlrAuthorization(Supplier<TenantContext> tenantContextSupplier) {
        this.tenantContextSupplier = tenantContextSupplier;
    }

    /**
     * The SpEL entry point. {@code true} only if the current caller's roles grant
     * the named permission.
     *
     * @param permissionName an {@link IlrPermission} name, e.g. {@code "CASE_SIGNOFF"}
     */
    public boolean can(String permissionName) {
        IlrPermission permission = IlrPermission.fromName(permissionName).orElse(null);
        if (permission == null) {
            // Deny, loudly. An unresolvable permission is an annotation bug; treating it
            // as "allow" would turn a typo into an open endpoint.
            log.warn("Denying access: '{}' is not a known IlrPermission. Check the @PreAuthorize "
                    + "expression against com.cognivio.ai.common.authz.IlrPermission.", permissionName);
            return false;
        }
        return hasPermission(permission);
    }

    /**
     * Typed equivalent of {@link #can(String)} for Java callers. Grants if the caller's roles hold
     * the permission (the human path) OR any of the caller's OAuth2 scopes resolves, via
     * {@link IlrPermission#fromScope(String)}, to it (the KAN-211 service-caller path — a
     * client-credentials token carries scopes, typically no roles at all). Either is sufficient; a
     * caller need not have both.
     */
    public boolean hasPermission(IlrPermission permission) {
        TenantContext context = currentContext();
        if (context == null) {
            return false;
        }
        if (RolePermissions.grantsAny(context.getRoles(), permission)) {
            return true;
        }
        return context.getScopes().stream()
                .map(IlrPermission::fromScope)
                .flatMap(Optional::stream)
                .anyMatch(permission::equals);
    }

    /**
     * True if the caller holds the given role, comparing leniently via
     * {@link IlrRole#fromClaim(String)} so case and {@code ROLE_} prefixing do not
     * matter. Prefer {@link #can(String)}: a role literal in a service repository is
     * exactly the duplication this package exists to remove. Provided for the rare
     * case where the gate genuinely is about identity rather than capability.
     */
    public boolean hasRole(String role) {
        IlrRole required = IlrRole.fromClaim(role).orElse(null);
        if (required == null) {
            log.warn("Denying access: '{}' is not a known IlrRole.", role);
            return false;
        }
        TenantContext context = currentContext();
        if (context == null) {
            return false;
        }
        return context.getRoles().stream()
                .anyMatch(raw -> IlrRole.fromClaim(raw).filter(required::equals).isPresent());
    }

    /**
     * True if {@code userId} identifies the calling user. The one L2 (relationship)
     * predicate that is expressible in an annotation, so that identity endpoints can
     * write
     * {@code @PreAuthorize("@ilrAuth.isSelf(#userId) or @ilrAuth.can('USER_ERASURE_REQUEST')")}.
     *
     * <p><b>Deliberately takes {@code Object}, not overloaded on {@code UUID} and
     * {@code String}.</b> Path variables across the estate are declared as both
     * types, and SpEL resolves overloads by argument type at runtime — a
     * {@code null} or unexpectedly-typed argument against two candidates is an
     * ambiguity that surfaces as a 500 on a security-critical path. One method that
     * accepts anything and decides for itself cannot be ambiguous.
     *
     * <p>Anything that is not a {@link UUID} or a UUID-shaped string denies rather
     * than throwing, so a malformed path cannot turn a 403 into a 500.
     *
     * @param userId a {@link UUID}, a UUID string, or {@code null}
     */
    public boolean isSelf(Object userId) {
        TenantContext context = currentContext();
        if (context == null || userId == null) {
            return false;
        }
        UUID callerId = context.getUserId();
        if (callerId == null) {
            return false;
        }
        if (userId instanceof UUID uuid) {
            return callerId.equals(uuid);
        }
        if (userId instanceof CharSequence text) {
            String trimmed = text.toString().trim();
            if (trimmed.isEmpty()) {
                return false;
            }
            try {
                return callerId.equals(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ex) {
                log.debug("Denying isSelf: '{}' is not a UUID", trimmed);
                return false;
            }
        }
        log.debug("Denying isSelf: unsupported argument type {}", userId.getClass().getName());
        return false;
    }

    /** Every permission the current caller holds; empty when there is no identity. */
    public Set<IlrPermission> permissions() {
        TenantContext context = currentContext();
        if (context == null) {
            return Set.of();
        }
        return RolePermissions.permissionsFor(context.getRoles());
    }

    /**
     * The populated context for the current request, or {@code null} if there is
     * none. Dereferencing the request-scoped proxy outside a request throws, and a
     * service that somehow has no {@code TenantContext} bean supplies {@code null};
     * both mean "no identity", which must deny rather than propagate.
     */
    private TenantContext currentContext() {
        Supplier<TenantContext> supplier = this.tenantContextSupplier;
        if (supplier == null) {
            return null;
        }
        try {
            TenantContext context = supplier.get();
            return context != null && context.isPresent() ? context : null;
        } catch (RuntimeException ex) {
            // Typically IllegalStateException("No thread-bound request found") on a non-request
            // thread. Deliberately broad: any failure to resolve identity must deny.
            log.debug("No tenant context available for authorization; denying", ex);
            return null;
        }
    }
}
