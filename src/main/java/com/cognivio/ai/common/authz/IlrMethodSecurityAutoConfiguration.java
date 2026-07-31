package com.cognivio.ai.common.authz;

import com.cognivio.ai.common.context.IlrTenantContextAutoConfiguration;
import com.cognivio.ai.common.context.TenantContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Turns on Spring method security across the estate and registers the
 * {@code ilrAuth} bean that {@code @PreAuthorize} expressions call into.
 *
 * <h2>Why this is a separate auto-configuration</h2>
 * It would be one line to add {@code @EnableMethodSecurity} to
 * {@code IlrSecurityAutoConfiguration}. That would be wrong. That class is
 * {@code @ConditionalOnProperty(ilr.security.enabled=true)}, and the estate's entire
 * test suite runs with {@code ilr.security.enabled=false} — the variant that installs
 * a permit-all chain. Riding the same condition would mean method security is off in
 * exactly the configuration the tests use, so every authorization gate would be
 * bypassed by the tests meant to prove it works, and the suite would report green on
 * an unenforced control. Method security therefore switches independently, on
 * {@code ilr.security.method-security.enabled} (default {@code true}).
 *
 * <h2>Why enabling it by default is safe</h2>
 * {@code @EnableMethodSecurity} is inert until something is annotated: its pointcut
 * matches no method, so no bean gains a proxy and no behaviour changes. A service
 * that has not yet been migrated is unaffected, and the per-service stories
 * (KAN-207–KAN-212) each take effect at their own merge. That is the staged rollout
 * we want, rather than a single flag day across 16 services.
 *
 * <p>The escape hatch, {@code ilr.security.method-security.enabled=false}, exists for
 * a service that needs to opt out temporarily. Setting it disables every
 * {@code @PreAuthorize} in that service, so it belongs in a PR description, not in a
 * production profile.
 */
@AutoConfiguration(after = IlrTenantContextAutoConfiguration.class)
@ConditionalOnClass({PreAuthorize.class, EnableMethodSecurity.class})
@ConditionalOnProperty(
        prefix = "ilr.security.method-security",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableMethodSecurity
public class IlrMethodSecurityAutoConfiguration {

    /**
     * Bean name {@code ilrAuth} is part of the public contract — it is what every
     * {@code @PreAuthorize("@ilrAuth...")} expression in all 16 services resolves.
     * Renaming it breaks them all at runtime rather than at compile time, so it must
     * not change.
     *
     * <p>The {@link TenantContext} is taken as an {@link ObjectProvider} rather than
     * injected directly: it is request-scoped, and a service without the tenant-context
     * auto-configuration must degrade to "deny" rather than fail to start.
     */
    @Bean(name = "ilrAuth")
    @ConditionalOnMissingBean(name = "ilrAuth")
    public IlrAuthorization ilrAuth(ObjectProvider<TenantContext> tenantContextProvider) {
        return new IlrAuthorization(tenantContextProvider::getIfAvailable);
    }
}
