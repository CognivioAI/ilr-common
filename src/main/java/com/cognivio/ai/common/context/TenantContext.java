package com.cognivio.ai.common.context;

import java.util.Set;
import java.util.UUID;

/**
 * Request-scoped holder for the identity resolved from the <b>verified</b> JWT
 * (ADR-011: {@code tenant_id} comes from the verified token, never from client
 * input). Replaces every service's placeholder {@code TenantContext} /
 * {@code CallerContext} that trusted an {@code X-Tenant-Id} / {@code X-User-Id}
 * header.
 *
 * <p>For a permit-listed (unauthenticated) endpoint this stays empty
 * ({@link #isPresent()} is {@code false}). It is populated by
 * {@link TenantContextFilter} after Spring Security has authenticated the
 * request.
 */
public class TenantContext {

    private UUID tenantId;
    private UUID firmId;
    private UUID userId;
    private Set<String> roles = Set.of();

    /** Tenant id (the firm id for firm users) from the verified {@code tenant_id} claim. */
    public UUID getTenantId() {
        return tenantId;
    }

    /** Firm id — equals {@link #getTenantId()} unless a distinct {@code firm_id} claim is present. */
    public UUID getFirmId() {
        return firmId;
    }

    /** Acting user id — the verified JWT {@code sub}. */
    public UUID getUserId() {
        return userId;
    }

    /** Roles from the verified role/group claims (without the authority prefix). */
    public Set<String> getRoles() {
        return roles;
    }

    /** True once a verified JWT identity has been bound to this request. */
    public boolean isPresent() {
        return tenantId != null;
    }

    /** True if the resolved identity holds the given role (case-sensitive, unprefixed). */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Populates the context from resolved, already-verified claim values. */
    public void populate(UUID tenantId, UUID firmId, UUID userId, Set<String> roles) {
        this.tenantId = tenantId;
        this.firmId = firmId != null ? firmId : tenantId;
        this.userId = userId;
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
    }

    /** Clears the context (used defensively; the request-scoped bean is normally discarded per request). */
    public void clear() {
        this.tenantId = null;
        this.firmId = null;
        this.userId = null;
        this.roles = Set.of();
    }
}
