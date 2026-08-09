package com.cognivio.ai.common.http;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Relays the <b>caller's own already-verified</b> bearer token onto an internal service-to-service
 * request (KAN-236, Phase 1 of the M2M plan agreed in KAN-233).
 *
 * <p>Before this existed, no ILR outbound HTTP client sent any credential at all, so every
 * cross-service call 401'd the moment {@code ilr.security.enabled} was true — the estate only ever
 * appeared to work because the downstream services were unsecured. Phase 1 deliberately forwards the
 * user's token as-is rather than minting a client-credentials token: the downstream service then
 * evaluates the request under the <em>real</em> caller's identity, tenant and roles, so RLS and the
 * KAN-186 authorization gates keep working end-to-end with no new trust relationships and no new
 * Cognito app clients. Its known limitation (the downstream cannot distinguish "the user asked" from
 * "a peer service asked on the user's behalf") is what later phases address.
 *
 * <h2>Two deliberate safety properties</h2>
 * <ol>
 *   <li><b>Allow-listed destinations only.</b> The interceptor re-checks every request URI against
 *       {@link InternalBaseUrlAllowList}, even though {@link InternalServiceClientFactory} already
 *       checked the base URL. That is not belt-and-braces for its own sake: {@code RestClient} lets
 *       a call site pass an absolute {@code URI} that replaces the configured base URL entirely, so
 *       a construction-time check alone can be bypassed by one line in a request builder. A token
 *       leak has no compile-time or test-time symptom, so the check has to sit where the URI is
 *       finally known.</li>
 *   <li><b>Fail soft, never fail silent.</b> With no verified token in the {@link SecurityContextHolder}
 *       — a scheduled job, an async thread that lost the context, an unauthenticated entry point —
 *       the request is sent <b>without</b> an {@code Authorization} header and a WARN is logged. It
 *       is not blocked locally: letting the downstream return its own 401 keeps a single, honest
 *       authorization decision point, whereas throwing here would turn every context-propagation gap
 *       into a 500 and invite call sites to catch and ignore it.</li>
 * </ol>
 *
 * <p>The token itself is <b>never</b> logged, at any level. Log messages carry only the destination
 * origin and path.
 */
public class BearerTokenRelayInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenRelayInterceptor.class);

    private final InternalBaseUrlAllowList allowList;

    public BearerTokenRelayInterceptor(InternalBaseUrlAllowList allowList) {
        this.allowList = allowList == null ? InternalBaseUrlAllowList.empty() : allowList;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        URI uri = request.getURI();

        if (!allowList.permits(uri)) {
            log.warn("KAN-236: refusing to relay the caller's bearer token to {} — not in "
                            + "ilr.internal-http.allowed-base-urls {}. Sending the request unauthenticated.",
                    safeDestination(uri), allowList);
            return execution.execute(request, body);
        }

        if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            // An explicit per-request Authorization header always wins; overwriting it would break
            // any future call site that deliberately presents a different credential.
            return execution.execute(request, body);
        }

        String token = currentVerifiedToken();
        if (token == null) {
            log.warn("KAN-236: no verified bearer token in the SecurityContext for the internal call to {} — "
                            + "sending it unauthenticated; the downstream service will decide (expect 401).",
                    safeDestination(uri));
            return execution.execute(request, body);
        }

        request.getHeaders().setBearerAuth(token);
        return execution.execute(request, body);
    }

    /**
     * The raw token value of the current request's verified JWT, or {@code null} when the request is
     * not running under one.
     *
     * <p>Only a {@link Jwt} is accepted. Anything else — an anonymous token, a test
     * {@code UsernamePasswordAuthenticationToken}, a pre-auth principal — has no bearer credential to
     * forward, and inventing one from {@code getCredentials().toString()} would risk relaying a
     * password. The {@code Jwt} here has necessarily already been verified against the Cognito JWKS
     * by the resource-server filter chain; this class never validates or mints anything.
     */
    private static String currentVerifiedToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        if (authentication.getPrincipal() instanceof Jwt principal) {
            return principal.getTokenValue();
        }
        if (authentication.getCredentials() instanceof Jwt credentials) {
            return credentials.getTokenValue();
        }
        return null;
    }

    /** Scheme, host, port and path only — never the query string, which can carry identifiers. */
    private static String safeDestination(URI uri) {
        if (uri == null) {
            return "(no uri)";
        }
        StringBuilder destination = new StringBuilder();
        destination.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() != -1) {
            destination.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            destination.append(uri.getRawPath());
        }
        return destination.toString();
    }
}
