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
    private Set<String> scopes = Set.of();

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

    /**
     * Scopes from the verified OAuth2 {@code scope} claim (KAN-211) — populated for a
     * client-credentials service caller (e.g. audit-writer, reminder-dispatcher), empty for a human
     * token that carries none.
     */
    public Set<String> getScopes() {
        return scopes;
    }

    /** True once a verified JWT identity has been bound to this request. */
    public boolean isPresent() {
        return tenantId != null;
    }

    /** True if the resolved identity holds the given role (case-sensitive, unprefixed). */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Populates the context from resolved, already-verified claim values. No scopes (see the overload). */
    public void populate(UUID tenantId, UUID firmId, UUID userId, Set<String> roles) {
        populate(tenantId, firmId, userId, roles, Set.of());
    }

    /**
     * Populates the context from resolved, already-verified claim values, including the caller's
     * scopes (KAN-211). Added as an overload, rather than extending the four-argument
     * {@link #populate(UUID, UUID, UUID, Set)}, so the many existing test fixtures across the estate
     * that construct a role-only context keep compiling unchanged.
     */
    public void populate(UUID tenantId, UUID firmId, UUID userId, Set<String> roles, Set<String> scopes) {
        this.tenantId = tenantId;
        this.firmId = firmId != null ? firmId : tenantId;
        this.userId = userId;
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
        this.scopes = scopes != null ? Set.copyOf(scopes) : Set.of();
    }

    /** Clears the context (used defensively; the request-scoped bean is normally discarded per request). */
    public void clear() {
        this.tenantId = null;
        this.firmId = null;
        this.userId = null;
        this.roles = Set.of();
        this.scopes = Set.of();
    }
}
