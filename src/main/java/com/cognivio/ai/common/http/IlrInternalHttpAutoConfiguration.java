package com.cognivio.ai.common.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

/**
 * Wires KAN-236's internal-call client factory into every consuming service.
 *
 * <p>Nothing here changes the behaviour of any existing {@code RestClient}: the relay is opt-in per
 * client via {@link InternalServiceClientFactory#forBaseUrl(String)}. That is the whole point — see
 * {@link InternalServiceClientFactory} for why a global {@code RestClientCustomizer} was rejected.
 */
@AutoConfiguration
@ConditionalOnClass({RestClient.class, SecurityContextHolder.class})
@EnableConfigurationProperties(IlrInternalHttpProperties.class)
public class IlrInternalHttpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IlrInternalHttpAutoConfiguration.class);

    /**
     * The allow-list of destinations that may receive the caller's token.
     *
     * <p>Built eagerly at startup so a malformed entry fails the context rather than silently
     * degrading every internal call to unauthenticated (which would only surface as a downstream 401
     * in a deployed environment).
     */
    @Bean
    @ConditionalOnMissingBean
    public InternalBaseUrlAllowList internalBaseUrlAllowList(IlrInternalHttpProperties properties) {
        if (!properties.isRelayEnabled()) {
            log.warn("ilr-common: ilr.internal-http.relay-enabled=false — the caller's bearer token will NOT be "
                    + "relayed on internal service-to-service calls, which are expected to 401 (KAN-236).");
            return InternalBaseUrlAllowList.empty();
        }
        InternalBaseUrlAllowList allowList = InternalBaseUrlAllowList.of(properties.getAllowedBaseUrls());
        if (allowList.isEmpty()) {
            log.warn("ilr-common: ilr.internal-http.allowed-base-urls is empty — no internal call will carry the "
                    + "caller's bearer token, so cross-service calls are expected to 401 (KAN-236).");
        } else {
            log.info("ilr-common: bearer-token relay enabled for internal destinations {} (KAN-236).", allowList);
        }
        return allowList;
    }

    @Bean
    @ConditionalOnMissingBean
    public InternalServiceClientFactory internalServiceClientFactory(
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            InternalBaseUrlAllowList allowList) {
        return new InternalServiceClientFactory(restClientBuilderProvider, allowList);
    }
}
