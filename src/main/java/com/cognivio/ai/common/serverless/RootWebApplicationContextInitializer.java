package com.cognivio.ai.common.serverless;

import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Publishes the running {@link WebApplicationContext} as the {@code ServletContext} root-context
 * attribute when nothing else has (KAN-226).
 *
 * <p><b>Why this exists.</b> On a real servlet container Spring Boot's
 * {@code ServletWebServerApplicationContext.selfInitialize(ServletContext)} sets
 * {@link WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE} (via
 * {@code prepareWebApplicationContext}) before running the {@code ServletContextInitializer} beans.
 * Under the AWS serverless adapter ({@code spring-cloud-function-serverless-web}, used for Lambda),
 * {@code ServerlessServletWebServerFactory.getWebServer(...)} discards the initializers entirely, so
 * {@code selfInitialize} never runs; the factory's {@code afterPropertiesSet()} reimplements only the
 * last step of it (iterating {@code ServletContextInitializerBeans}) and never sets the attribute.
 *
 * <p>Anything resolving the root context off the {@code ServletContext} then blows up with
 * {@code IllegalStateException: No WebApplicationContext found: no ContextLoaderListener
 * registered?}. Today the only caller in our stack is Actuator: {@code HealthEndpointWebExtension}'s
 * health operation resolves its {@code WebServerNamespace} argument through
 * {@code WebApplicationContextUtils.getRequiredWebApplicationContext(ServletContext)}, which is why
 * {@code /actuator/health} returned 500 on Lambda while {@code /actuator/info} returned 200 on the
 * same deployment. This is not fixable by a version bump — every 4.x release of the adapter we can
 * run on Boot 3.3.4 has the same gap.
 *
 * <p><b>Do not "clean this up" as dead code.</b> It is deliberately unconditional for servlet web
 * apps rather than gated on the adapter jar: on a real container Boot has already set the attribute
 * by the time this runs, so the null check below makes it a provable no-op there.
 *
 * <p><b>Ordering.</b> Registering this as a {@code ServletContextInitializer} bean is what puts it
 * after {@code DispatcherServlet.init()} in the adapter's sequence, which is where it must run. Do
 * not move it to an earlier hook (e.g. a {@code ContextRefreshedEvent} listener).
 *
 * <p><b>Known, unfixed siblings.</b> The same missing {@code prepareWebApplicationContext} call also
 * means the {@code "application"} bean scope is never registered and
 * {@code WebApplicationContextUtils.registerEnvironmentBeans} never runs under the adapter; both are
 * currently unused by this estate and are left alone deliberately.
 */
public class RootWebApplicationContextInitializer implements ServletContextInitializer {

    private static final Logger log = LoggerFactory.getLogger(RootWebApplicationContextInitializer.class);

    private final ApplicationContext applicationContext;

    public RootWebApplicationContextInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        if (!(applicationContext instanceof WebApplicationContext webApplicationContext)) {
            // The attribute is typed as a WebApplicationContext; anything else would break its readers.
            return;
        }
        if (servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null) {
            // Real container: Boot's selfInitialize already published the context. Leave it alone.
            return;
        }
        log.debug("Publishing root WebApplicationContext on the ServletContext; the serverless adapter skipped it");
        servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
    }
}
