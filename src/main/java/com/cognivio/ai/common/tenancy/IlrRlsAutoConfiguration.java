package com.cognivio.ai.common.tenancy;

import com.cognivio.ai.common.context.TenantContext;
import javax.sql.DataSource;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
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
 *
 * <p><b>KAN-189:</b> {@code @AutoConfigureAfter(DataSourceAutoConfiguration.class)} is required,
 * not cosmetic. Without it, Spring Boot's combined auto-configuration ordering has no guarantee
 * that {@code DataSourceAutoConfiguration} has already registered its {@link DataSource} bean by
 * the time this class's {@code @ConditionalOnBean(DataSource.class)} is evaluated — and in every
 * consuming service observed while verifying KAN-189, it evaluated FIRST, so the condition failed,
 * this whole auto-configuration was skipped, and {@link RlsTenantBindingAspect} was never created.
 * The RLS session binding has therefore never actually engaged in any environment, independently
 * of the ADR-011 FORCE/owner-role defect KAN-189 reports — a service with {@code ilr.rls.enabled}
 * flipped on would still never bind {@code app.current_tenant_id}, so a non-owner runtime role
 * (subject to RLS unconditionally) would see zero rows for every query. Confirmed via a
 * `spring.factories`/condition-evaluation-report inspection during KAN-189 local-stack testing.
 */
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
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
