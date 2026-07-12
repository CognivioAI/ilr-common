package com.cognivio.ai.common.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code ilr.security.*} configuration namespace. A consuming service
 * typically sets only {@code ilr.security.issuer-uri} (the Cognito user-pool
 * issuer); everything else has a sensible default.
 */
@ConfigurationProperties("ilr.security")
public class IlrSecurityProperties {

    /**
     * Master switch. When {@code false} the library installs a permit-all filter
     * chain so a service can start (and run its standalone MockMvc unit tests)
     * with security effectively disabled. Default {@code true}.
     */
    private boolean enabled = true;

    /**
     * Explicit opt-in for the insecure "no decoder configured → permit everything"
     * dev fallback. Default {@code false}, so security being enabled but
     * misconfigured (no {@link #issuerUri}/{@link #jwksUri} and no {@code JwtDecoder}
     * bean) makes the service <b>fail fast on startup</b> rather than boot wide open.
     * A security foundation must fail CLOSED: a forgotten issuer URI in production
     * must never silently disable authentication. Set this to {@code true} only for
     * genuine local development where you knowingly want no token verification.
     */
    private boolean devPermitAll = false;

    /**
     * Cognito user-pool issuer URI, e.g.
     * {@code https://cognito-idp.eu-west-2.amazonaws.com/<userPoolId>}. When set,
     * the resource server discovers the JWKS from {@code <issuer>/.well-known/...}
     * and validates the {@code iss} claim. Either this or {@link #jwksUri} enables
     * real JWT verification.
     */
    private String issuerUri;

    /**
     * Explicit JWKS URI. Set this instead of (or in addition to) {@link #issuerUri}
     * when the JWKS location cannot be derived from the issuer. Takes precedence
     * over {@link #issuerUri} when building the decoder.
     */
    private String jwksUri;

    /** JWT claim carrying the tenant id (firm id for firm users). Default {@code tenant_id}. */
    private String tenantClaim = "tenant_id";

    /** JWT claim carrying the firm id, when distinct from the tenant. Default {@code firm_id}. */
    private String firmClaim = "firm_id";

    /**
     * Claims inspected (in order) for role/group values. Cognito emits groups in
     * {@code cognito:groups}; a plain {@code roles} claim is also supported.
     */
    private List<String> roleClaims = List.of("cognito:groups", "roles");

    /** Authority prefix applied to each mapped role. Default {@code ROLE_}. */
    private String authorityPrefix = "ROLE_";

    /** Ant-style paths that do NOT require authentication. */
    private List<String> permitList = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDevPermitAll() {
        return devPermitAll;
    }

    public void setDevPermitAll(boolean devPermitAll) {
        this.devPermitAll = devPermitAll;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getTenantClaim() {
        return tenantClaim;
    }

    public void setTenantClaim(String tenantClaim) {
        this.tenantClaim = tenantClaim;
    }

    public String getFirmClaim() {
        return firmClaim;
    }

    public void setFirmClaim(String firmClaim) {
        this.firmClaim = firmClaim;
    }

    public List<String> getRoleClaims() {
        return roleClaims;
    }

    public void setRoleClaims(List<String> roleClaims) {
        this.roleClaims = roleClaims;
    }

    public String getAuthorityPrefix() {
        return authorityPrefix;
    }

    public void setAuthorityPrefix(String authorityPrefix) {
        this.authorityPrefix = authorityPrefix;
    }

    public List<String> getPermitList() {
        return permitList;
    }

    public void setPermitList(List<String> permitList) {
        this.permitList = permitList;
    }
}
