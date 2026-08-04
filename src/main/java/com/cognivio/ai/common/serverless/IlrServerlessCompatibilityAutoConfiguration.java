package com.cognivio.ai.common.serverless;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Home for fixes that compensate for what the AWS serverless servlet adapter
 * ({@code spring-cloud-function-serverless-web}) skips relative to a real container (KAN-226).
 *
 * <p>Currently it contributes {@link RootWebApplicationContextInitializer} — see that class for the
 * root cause. Applied to every servlet web app, not just Lambda ones: the initializer is a
 * self-checking no-op on a real container, which keeps this off the adapter's classpath conditions
 * and testable without pulling the adapter into this library.
 *
 * <p>Its sibling {@link LambdaProfileDefaultsEnvironmentPostProcessor} covers the fixes that have to
 * be property defaults rather than beans, and which therefore have to be applied before
 * auto-configuration conditions are evaluated.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IlrServerlessCompatibilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RootWebApplicationContextInitializer ilrRootWebApplicationContextInitializer(
            ApplicationContext applicationContext) {
        return new RootWebApplicationContextInitializer(applicationContext);
    }
}
