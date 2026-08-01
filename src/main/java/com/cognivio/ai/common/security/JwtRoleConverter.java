package com.cognivio.ai.common.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps a verified {@link Jwt}'s role/group claims to Spring authorities. Reads
 * each configured role claim (default {@code cognito:groups} then {@code roles})
 * and emits one authority per distinct role value, prefixed with the configured
 * authority prefix (default {@code ROLE_}) so {@code hasRole('firm-admin')} and
 * {@code hasAuthority('ROLE_firm-admin')} both work.
 *
 * <p>A claim value may be a JSON array ({@code ["applicant","sponsor"]}) or a
 * single space/comma-delimited string; both are handled.
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final List<String> roleClaims;
    private final String authorityPrefix;
    private final String scopeClaim;

    /** Defaults {@link #scopeClaim} to {@code "scope"} (the {@code IlrSecurityProperties} default). */
    public JwtRoleConverter(List<String> roleClaims, String authorityPrefix) {
        this(roleClaims, authorityPrefix, "scope");
    }

    public JwtRoleConverter(List<String> roleClaims, String authorityPrefix, String scopeClaim) {
        this.roleClaims = roleClaims;
        this.authorityPrefix = authorityPrefix;
        this.scopeClaim = scopeClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return extractRoles(jwt).stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(authorityPrefix + role))
                .toList();
    }

    /** The raw role strings (without authority prefix) found across the configured claims, de-duplicated. */
    public Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();
        for (String claim : roleClaims) {
            Object value = jwt.getClaim(claim);
            if (value instanceof Collection<?> collection) {
                for (Object element : collection) {
                    if (element != null) {
                        addTokens(roles, element.toString());
                    }
                }
            } else if (value instanceof String s) {
                addTokens(roles, s);
            }
        }
        return roles;
    }

    /**
     * The raw scope strings found in the single space-delimited OAuth2 {@code scope} claim (RFC
     * 6749 §3.3 / RFC 8693) of a client-credentials token, e.g. {@code "ilr-audit/audit-write"}.
     * Unlike {@link #extractRoles(Jwt)}, this claim is never a JSON array — the OAuth2 spec fixes
     * its shape to one space-delimited string — so only that form is read.
     *
     * @param jwt the verified token
     * @return the individual scope tokens, de-duplicated; empty if the claim is absent or blank
     */
    public Set<String> extractScopes(Jwt jwt) {
        Set<String> scopes = new LinkedHashSet<>();
        Object value = jwt.getClaim(scopeClaim);
        if (value instanceof String s) {
            addTokens(scopes, s);
        }
        return scopes;
    }

    private static void addTokens(Set<String> target, String raw) {
        for (String token : raw.split("[,\\s]+")) {
            if (!token.isBlank()) {
                target.add(token.trim());
            }
        }
    }
}
