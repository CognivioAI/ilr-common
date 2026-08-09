package com.cognivio.ai.common.http;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code ilr.internal-http.*} namespace — the configuration behind KAN-236's
 * caller-token relay.
 *
 * <p>A consuming service normally points {@link #allowedBaseUrls} at the very same properties its
 * clients are constructed from, so the two can never drift:
 *
 * <pre>{@code
 * ilr:
 *   internal-http:
 *     allowed-base-urls:
 *       - ${services.eligibility.uri:http://localhost:8082}
 *       - ${services.residency.uri:http://localhost:8083}
 *       - ${services.knowledge.uri:http://localhost:8090}
 * }</pre>
 */
@ConfigurationProperties("ilr.internal-http")
public class IlrInternalHttpProperties {

    /**
     * Master switch for the bearer-token relay. When {@code false}, clients from
     * {@link InternalServiceClientFactory} carry no interceptor at all and no token is ever
     * forwarded. Default {@code true}.
     */
    private boolean relayEnabled = true;

    /**
     * Base URLs of the <b>internal ILR services</b> the caller's bearer token may be forwarded to.
     *
     * <p>Empty by default, which is the fail-closed posture: a service that has not opted in relays
     * nothing. Never add a third-party endpoint here — Bedrock, GOV.UK and any other external host
     * must never receive an end user's Cognito token.
     */
    private List<String> allowedBaseUrls = new ArrayList<>();

    public boolean isRelayEnabled() {
        return relayEnabled;
    }

    public void setRelayEnabled(boolean relayEnabled) {
        this.relayEnabled = relayEnabled;
    }

    public List<String> getAllowedBaseUrls() {
        return allowedBaseUrls;
    }

    public void setAllowedBaseUrls(List<String> allowedBaseUrls) {
        this.allowedBaseUrls = allowedBaseUrls == null ? new ArrayList<>() : new ArrayList<>(allowedBaseUrls);
    }
}
