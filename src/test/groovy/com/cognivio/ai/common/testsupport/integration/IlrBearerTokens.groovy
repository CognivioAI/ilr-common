package com.cognivio.ai.common.testsupport.integration

import com.cognivio.ai.common.authz.IlrRole
import com.cognivio.ai.common.support.TestJwtFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.request.RequestPostProcessor

/**
 * Mints signed bearer tokens as MockMvc {@link RequestPostProcessor}s, so a spec against a real
 * Spring context reads:
 *
 * <pre>{@code
 * mockMvc.perform(get('/cases').with(bearerFor(IlrRole.CONSULTANT, tenantA)))
 *        .andExpect(status().isOk())
 * }</pre>
 *
 * <p>The token is genuinely RS256-signed and genuinely verified by the production filter chain, so
 * a spec written with this exercises {@code JwtRoleConverter} (claim → authority), the
 * {@code TenantClaimResolver} (claim → {@code TenantContext}) and, through them, the RLS tenant
 * binding. A test using {@code @WithMockUser} instead would short-circuit all three.
 *
 * <h2>Claim shape</h2>
 * Tokens carry the claims the platform actually reads, under the names
 * {@code IlrSecurityProperties} defaults to:
 * <ul>
 *   <li>{@code sub} — the caller's user id, which becomes {@code TenantContext.getUserId()} and
 *       therefore backs {@code @ilrAuth.isSelf(...)};</li>
 *   <li>{@code tenant_id} — required; a token without it is rejected by
 *       {@code TenantClaimResolver} with {@code MissingTenantClaimException} (a legitimate case to
 *       test, via {@link #bearerForClaims});</li>
 *   <li>{@code firm_id} — defaults to the tenant id, matching a firm user;</li>
 *   <li>{@code cognito:groups} — role claim values in their wire form ({@code firm-admin}, not
 *       {@code FIRM_ADMIN}), obtained from {@link IlrRole#claimValue()} rather than hand-written, so
 *       a spec cannot drift from the enum.</li>
 * </ul>
 */
class IlrBearerTokens {

    /** Tenant A — the default tenant for a spec that does not care which tenant it is. */
    static final UUID DEFAULT_TENANT_ID = UUID.fromString('11111111-1111-1111-1111-111111111111')

    /** Tenant B — the "other firm" for cross-tenant isolation assertions. */
    static final UUID OTHER_TENANT_ID = UUID.fromString('22222222-2222-2222-2222-222222222222')

    /** Default calling user within {@link #DEFAULT_TENANT_ID}. */
    static final UUID DEFAULT_USER_ID = UUID.fromString('aaaaaaaa-0000-0000-0000-0000000000a1')

    /** Default calling user within {@link #OTHER_TENANT_ID}. */
    static final UUID OTHER_USER_ID = UUID.fromString('bbbbbbbb-0000-0000-0000-0000000000b1')

    private final TestJwtFactory jwtFactory

    IlrBearerTokens(TestJwtFactory jwtFactory) {
        this.jwtFactory = jwtFactory
    }

    /** A bearer token for a single role, tenant and user. */
    RequestPostProcessor bearerFor(IlrRole role,
                                   UUID tenantId = DEFAULT_TENANT_ID,
                                   UUID userId = DEFAULT_USER_ID) {
        bearerForRoles([role], tenantId, userId)
    }

    /** A bearer token for a caller holding several roles at once. */
    RequestPostProcessor bearerForRoles(Collection<IlrRole> roles,
                                        UUID tenantId = DEFAULT_TENANT_ID,
                                        UUID userId = DEFAULT_USER_ID) {
        bearerForClaims(claimsFor(roles, tenantId, userId))
    }

    /**
     * Full control over the claim set, for negative cases the typed helpers cannot express — a token
     * with no {@code tenant_id}, an unknown group, a malformed uuid, and so on.
     *
     * <p>Null-valued entries are dropped rather than serialised as {@code null}, so
     * {@code bearerForClaims([tenant_id: null, ...])} produces a token with the claim genuinely
     * absent, which is the case worth testing.
     */
    RequestPostProcessor bearerForClaims(Map<String, Object> claims) {
        String token = tokenForClaims(claims)
        return { MockHttpServletRequest request ->
            request.addHeader('Authorization', "Bearer ${token}")
            return request
        } as RequestPostProcessor
    }

    /** The raw serialised token, for a caller that needs the string rather than a post-processor. */
    String tokenFor(IlrRole role, UUID tenantId = DEFAULT_TENANT_ID, UUID userId = DEFAULT_USER_ID) {
        tokenForClaims(claimsFor([role], tenantId, userId))
    }

    /** The raw serialised token for an arbitrary claim set. */
    String tokenForClaims(Map<String, Object> claims) {
        jwtFactory.signedToken(claims.findAll { it.value != null } as Map<String, Object>)
    }

    private static Map<String, Object> claimsFor(Collection<IlrRole> roles, UUID tenantId, UUID userId) {
        [
                sub              : userId?.toString(),
                tenant_id        : tenantId?.toString(),
                firm_id          : tenantId?.toString(),
                'cognito:groups' : (roles ?: []).collect { it.claimValue() },
        ] as Map<String, Object>
    }
}
