package com.cognivio.ai.common.serverless;

import java.util.Collections;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

/**
 * Contributes shared property defaults that only make sense when a service runs as an AWS Lambda,
 * i.e. when the {@code lambda} profile is active (KAN-226).
 *
 * <p><b>Why an {@code EnvironmentPostProcessor} and not the auto-configuration.</b> The single
 * default we set here, {@code management.health.diskspace.enabled}, is read by
 * {@code @ConditionalOnEnabledHealthIndicator} while auto-configuration conditions are being
 * evaluated, so a bean or a property source contributed from
 * {@link IlrServerlessCompatibilityAutoConfiguration} would land too late to suppress the indicator.
 * A property default also cannot come from {@code application.yml} in this library: profile blocks
 * live in each consuming service's own config, and a jar cannot merge YAML into them. The
 * environment hook is the only place a shared library can set a default that condition evaluation
 * will see.
 *
 * <p><b>Why disk space is disabled under Lambda.</b> Lambda mounts the deployment package read-only
 * at {@code /var/task}, which is the working directory and therefore the path
 * {@code DiskSpaceHealthIndicator} inspects. It permanently reports 0 free bytes there — it is not
 * writable storage at all (that is {@code /tmp}, with its own ~512MB of ephemeral space). So the
 * indicator reports {@code DOWN} on every healthy invocation, which drags the aggregate
 * {@code /actuator/health} to HTTP 503 even while liveness, readiness and db all report {@code UP}.
 * The check is structurally meaningless on Lambda rather than merely noisy, so it is switched off
 * rather than re-thresholded.
 *
 * <p><b>Scope.</b> Gated on the {@code lambda} profile on purpose. On the container/ECS deployment
 * target free disk space is a real, actionable signal and the indicator stays on. The defaults are
 * added as the lowest-precedence property source, so any service that explicitly sets one of these
 * properties still wins.
 *
 * <p><b>Ordering.</b> {@link Ordered#LOWEST_PRECEDENCE} so this runs after
 * {@code ConfigDataEnvironmentPostProcessor}, which is what makes profiles activated from
 * {@code application.yml} (not just from {@code SPRING_PROFILES_ACTIVE}) visible here.
 */
public class LambdaProfileDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Profile every Lambda-packaged ILR service runs with. */
    public static final String LAMBDA_PROFILE = "lambda";

    static final String PROPERTY_SOURCE_NAME = "ilrLambdaProfileDefaults";

    static final String DISK_SPACE_HEALTH_ENABLED = "management.health.diskspace.enabled";

    private static final Map<String, Object> LAMBDA_DEFAULTS =
            Collections.singletonMap(DISK_SPACE_HEALTH_ENABLED, "false");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of(LAMBDA_PROFILE))) {
            return;
        }
        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            // Environment post-processors can run more than once (e.g. a parent/child application).
            return;
        }
        propertySources.addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, LAMBDA_DEFAULTS));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
