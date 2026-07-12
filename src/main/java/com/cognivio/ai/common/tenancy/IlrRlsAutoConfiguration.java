package com.cognivio.ai.common.tenancy;

import com.cognivio.ai.common.context.TenantContext;
import javax.sql.DataSource;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Wires the Postgres RLS session binding: the {@link TenantSessionInitializer}
 * primitive and the {@link RlsTenantBindingAspect} that applies it at the start
 * of each {@code @Transactional} unit of work. Active only when a
 * {@link DataSource} is present and {@code ilr.rls.enabled} is {@code true}
 * (default), so a service without a database (or with RLS temporarily off) still
 * starts.
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, Aspect.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "ilr.rls", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RlsProperties.class)
@EnableAspectJAutoProxy
public class IlrRlsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantSessionInitializer tenantSessionInitializer(RlsProperties properties) {
        return new TenantSessionInitializer(properties.getSettingName());
    }

    @Bean
    @ConditionalOnMissingBean
    public RlsTenantBindingAspect rlsTenantBindingAspect(DataSource dataSource, TenantContext tenantContext,
                                                         TenantSessionInitializer initializer) {
        return new RlsTenantBindingAspect(dataSource, tenantContext, initializer);
    }
}
