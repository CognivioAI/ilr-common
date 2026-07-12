package com.cognivio.ai.common.context;

import com.cognivio.ai.common.security.IlrSecurityProperties;
import com.cognivio.ai.common.security.JwtRoleConverter;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Pure (no servlet dependencies) logic that reads a <b>verified</b> {@link Jwt}
 * and produces the {@link TenantContext} values from its claims. Split out from
 * {@link TenantContextFilter} so it is directly unit-testable with a locally
 * decoded test JWT.
 *
 * <p>It only ever consults JWT claims — it has no access to, and never reads, an
 * {@code X-Tenant-Id} (or any other) request header.
 */
public class TenantClaimResolver {

    private final IlrSecurityProperties properties;
    private final JwtRoleConverter roleConverter;

    public TenantClaimResolver(IlrSecurityProperties properties, JwtRoleConverter roleConverter) {
        this.properties = properties;
        this.roleConverter = roleConverter;
    }

    /**
     * Populates {@code context} from the verified JWT.
     *
     * @throws MissingTenantClaimException if the token carries no usable tenant claim
     */
    public void resolveInto(Jwt jwt, TenantContext context) {
        UUID tenantId = requiredUuidClaim(jwt, properties.getTenantClaim());
        UUID firmId = optionalUuidClaim(jwt, properties.getFirmClaim());
        UUID userId = parseUuid(jwt.getSubject());
        Set<String> roles = roleConverter.extractRoles(jwt);
        context.populate(tenantId, firmId, userId, roles);
    }

    private UUID requiredUuidClaim(Jwt jwt, String claimName) {
        Object raw = jwt.getClaim(claimName);
        if (raw == null || raw.toString().isBlank()) {
            throw new MissingTenantClaimException(
                    "Verified token is missing required tenant claim '" + claimName + "'");
        }
        UUID parsed = parseUuid(raw.toString());
        if (parsed == null) {
            throw new MissingTenantClaimException(
                    "Verified tenant claim '" + claimName + "' is not a valid UUID");
        }
        return parsed;
    }

    private static UUID optionalUuidClaim(Jwt jwt, String claimName) {
        Object raw = jwt.getClaim(claimName);
        return raw == null ? null : parseUuid(raw.toString());
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
