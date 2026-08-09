package com.cognivio.ai.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClient;

/**
 * The one supported way to build a {@code RestClient} that talks to <b>another ILR service</b>
 * (KAN-236).
 *
 * <p>Replaces the estate-wide pattern of {@code builder.baseUrl(uri).build()} inside each service's
 * {@code ..client..} package. Those clients sent no credential at all, so every cross-service call
 * 401s once the downstream enforces authentication; see {@link BearerTokenRelayInterceptor} for why
 * Phase 1 forwards the caller's own token.
 *
 * <h2>Why a factory and not a global {@code RestClient.Builder} customizer</h2>
 * A {@code RestClientCustomizer} bean applies to every {@code RestClient.Builder} injected anywhere
 * in the application. Several ILR services build clients from that same injected builder to reach
 * Amazon Bedrock and to fetch GOV.UK pages, so a global customizer would attach the end user's
 * Cognito access token to third-party requests — a credential leak with no local symptom. Making the
 * relay opt-in per client, and additionally allow-listing the destination, keeps "who may receive
 * this user's token" a short, reviewable list instead of an emergent property of the bean graph.
 *
 * <h2>Fail closed</h2>
 * {@link #forBaseUrl(String)} attaches the relay <b>only</b> when the base URL is allow-listed. A
 * base URL that is not (a typo, an unconfigured allow-list, a third-party host) yields an ordinary,
 * fully functional {@code RestClient} with no interceptor — the call still happens, no token is
 * exposed, and a WARN says why. The failure mode is therefore "downstream 401", which is loud and
 * safe, never "token sent somewhere unexpected", which is silent and not.
 *
 * <p>An ArchUnit rule ({@code InternalClientConstructionRules}, shipped in ilr-common's
 * tests-classifier jar) fails a consuming service's build if a class under {@code ..client..}
 * constructs a {@code RestClient} directly instead of calling this factory.
 */
public class InternalServiceClientFactory {

    private static final Logger log = LoggerFactory.getLogger(InternalServiceClientFactory.class);

    private final ObjectProvider<RestClient.Builder> builderProvider;
    private final InternalBaseUrlAllowList allowList;

    public InternalServiceClientFactory(ObjectProvider<RestClient.Builder> builderProvider,
                                        InternalBaseUrlAllowList allowList) {
        this.builderProvider = builderProvider;
        this.allowList = allowList == null ? InternalBaseUrlAllowList.empty() : allowList;
    }

    /**
     * A factory backed by one specific {@code RestClient.Builder}, for callers that have a builder
     * but no {@code ObjectProvider} — chiefly a service's client specs, which bind a
     * {@code MockRestServiceServer} to a builder and need the client under test to be built from
     * that exact one.
     *
     * <p>Named rather than a second constructor because {@code ObjectProvider} and
     * {@code RestClient.Builder} are both interfaces: overloaded constructors would resolve
     * ambiguously on a {@code null} argument, and a silently-wrong overload here means a silently
     * unauthenticated client.
     */
    public static InternalServiceClientFactory withBuilder(RestClient.Builder builder,
                                                           InternalBaseUrlAllowList allowList) {
        return new InternalServiceClientFactory(new SingletonBuilderProvider(builder), allowList);
    }

    /**
     * A {@code RestClient} rooted at {@code baseUrl} which relays the caller's verified bearer token
     * when — and only when — {@code baseUrl} is allow-listed.
     *
     * @param baseUrl the downstream service's base URL, typically injected from
     *                {@code ${services.<name>.uri}} so the allow-list and the client always agree
     * @return a ready-to-use client; never {@code null}
     */
    public RestClient forBaseUrl(String baseUrl) {
        RestClient.Builder builder = freshBuilder().baseUrl(baseUrl);

        if (!allowList.permits(baseUrl)) {
            log.warn("KAN-236: internal client for {} built WITHOUT bearer-token relay — the base URL is not in "
                            + "ilr.internal-http.allowed-base-urls {}. Calls will reach the downstream service "
                            + "unauthenticated and are expected to 401. Add the URL to the allow-list if this is "
                            + "an ILR service.",
                    baseUrl, allowList);
            return builder.build();
        }

        return builder.requestInterceptor(new BearerTokenRelayInterceptor(allowList)).build();
    }

    /** The allow-list this factory enforces. Exposed for diagnostics and tests. */
    public InternalBaseUrlAllowList allowList() {
        return allowList;
    }

    /**
     * A builder instance this factory may mutate freely.
     *
     * <p>Spring Boot's {@code RestClient.Builder} bean is prototype-scoped, so each
     * {@code getIfAvailable} call yields a fresh instance; {@code clone()} makes that guarantee hold
     * even if a service overrides the bean with a singleton, where mutating the shared builder would
     * otherwise leak this interceptor onto every other client in the application — precisely the
     * global-customizer leak this class exists to avoid.
     */
    private RestClient.Builder freshBuilder() {
        RestClient.Builder builder =
                builderProvider == null ? RestClient.builder() : builderProvider.getIfAvailable(RestClient::builder);
        return builder.clone();
    }

    /** Adapts a single builder instance to the {@link ObjectProvider} the factory consumes. */
    private static final class SingletonBuilderProvider implements ObjectProvider<RestClient.Builder> {

        private final RestClient.Builder builder;

        private SingletonBuilderProvider(RestClient.Builder builder) {
            this.builder = builder;
        }

        @Override
        public RestClient.Builder getObject() {
            return builder;
        }

        @Override
        public RestClient.Builder getObject(Object... args) {
            return builder;
        }

        @Override
        public RestClient.Builder getIfAvailable() {
            return builder;
        }

        @Override
        public RestClient.Builder getIfUnique() {
            return builder;
        }
    }
}
