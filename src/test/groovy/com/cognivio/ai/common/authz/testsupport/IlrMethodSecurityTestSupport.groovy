package com.cognivio.ai.common.authz.testsupport

import com.cognivio.ai.common.authz.IlrAuthorization
import com.cognivio.ai.common.authz.IlrRole
import com.cognivio.ai.common.context.TenantContext
import com.cognivio.ai.common.web.CommonExceptionHandler
import org.springframework.aop.framework.ProxyFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder

/**
 * Makes {@code @PreAuthorize} actually fire in a standalone MockMvc spec.
 *
 * <h2>Why this exists</h2>
 * The estate's controller specs are all of the form
 * {@code MockMvcBuilders.standaloneSetup(new ReviewController(...))}. Standalone
 * MockMvc builds no Spring context and creates no AOP proxy, so <b>method security
 * is not applied in that harness at all</b>. A "403 for the wrong role" spec written
 * against it passes identically whether the annotation is present, absent or
 * misspelt — a test that proves nothing while looking like proof. Rewriting fifteen
 * services' specs as {@code @WebMvcTest} slices would be correct but is a very large
 * diff per service, so instead this harness wires the real Spring Security
 * {@code @PreAuthorize} interceptor into the standalone build. Migrating an existing
 * spec is a one-line change.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * class ReviewControllerSpec extends Specification {
 *
 *     def authz = new IlrMethodSecurityTestSupport()
 *     def service = Mock(ReviewService)
 *     // was: MockMvcBuilders.standaloneSetup(new ReviewController(service)).build()
 *     def mockMvc = authz.mockMvc(new ReviewController(service))
 *
 *     def cleanup() { authz.clear() }
 *
 *     def "a reviewer may decide a review item"() {
 *         given:
 *         authz.authenticateAs(IlrRole.REVIEWER)
 *
 *         expect:
 *         mockMvc.perform(post("/review-items/{id}/decision", id)).andExpect(status().isOk())
 *     }
 *
 *     def "an applicant may not decide a review item"() {
 *         given:
 *         authz.authenticateAs(IlrRole.APPLICANT)
 *
 *         expect:
 *         mockMvc.perform(post("/review-items/{id}/decision", id)).andExpect(status().isForbidden())
 *     }
 * }
 * }</pre>
 *
 * <p>Always {@link #clear()} in {@code cleanup()}: the Spring
 * {@code SecurityContextHolder} is thread-local and leaks into the next feature
 * method otherwise, which shows up as a test that only passes when run alone.
 *
 * <h2>Scope and limits</h2>
 * <ul>
 *   <li>Wires {@code @PreAuthorize} only. {@code @PostAuthorize}, {@code @PreFilter}
 *       and {@code @PostFilter} are not applied; a spec needing those wants a real
 *       {@code @WebMvcTest} slice.</li>
 *   <li>Uses a CGLIB proxy, so the controller class and its handler methods must not
 *       be {@code final}.</li>
 *   <li>Registers {@link CommonExceptionHandler} as controller advice so a denial
 *       surfaces as the platform's 403 error body rather than an exception escaping
 *       the dispatcher. Pass your own advice to {@link #standaloneSetup} if the
 *       service adds more.</li>
 * </ul>
 */
class IlrMethodSecurityTestSupport {

    /**
     * The context the {@code @PreAuthorize} expressions read. Exposed so a spec can
     * assert on it, or populate it directly for cases {@link #authenticateAs} does
     * not cover.
     */
    final TenantContext tenantContext = new TenantContext()

    /** The same {@code ilrAuth} bean the application registers, over {@link #tenantContext}. */
    final IlrAuthorization authorization = new IlrAuthorization({ tenantContext })

    private final GenericApplicationContext beanContext

    IlrMethodSecurityTestSupport() {
        beanContext = new GenericApplicationContext()
        // Bean name must be `ilrAuth`: it is what @PreAuthorize("@ilrAuth...") resolves.
        beanContext.getBeanFactory().registerSingleton("ilrAuth", authorization)
        beanContext.refresh()
    }

    /**
     * Wraps a controller so its {@code @PreAuthorize} annotations are enforced,
     * without changing how it is constructed.
     */
    def <T> T secured(T controller) {
        ProxyFactory factory = new ProxyFactory(controller)
        factory.setProxyTargetClass(true)
        factory.addAdvisor(preAuthorizeAdvisor())
        return (T) factory.getProxy()
    }

    /** A standalone MockMvc builder over method-secured controllers, for further customisation. */
    StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
        Object[] secured = controllers.collect { secured(it) }.toArray()
        return MockMvcBuilders.standaloneSetup(secured)
                .setControllerAdvice(new CommonExceptionHandler())
    }

    /** The common case: a built MockMvc over method-secured controllers. */
    MockMvc mockMvc(Object... controllers) {
        return standaloneSetup(controllers).build()
    }

    /**
     * Authenticates as a user holding the given roles, with generated tenant and user
     * ids. Use {@link #authenticateAs(UUID, UUID, Collection)} when the spec asserts
     * on identity as well as capability.
     */
    void authenticateAs(IlrRole... roles) {
        authenticateAs(UUID.randomUUID(), UUID.randomUUID(), roles*.claimValue())
    }

    /** Authenticates as the given user, holding the given raw role strings. */
    void authenticateAs(UUID tenantId, UUID userId, Collection<String> roles) {
        tenantContext.populate(tenantId, tenantId, userId, roles as Set)
        // A Spring Authentication must be present or the interceptor throws
        // AuthenticationCredentialsNotFoundException (401) before it ever evaluates the
        // expression, and the spec would assert 401 where it means 403. The authorities
        // are set to match so a service still using hasRole() behaves consistently.
        List<GrantedAuthority> authorities = roles.collect { new SimpleGrantedAuthority("ROLE_" + it) }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId?.toString() ?: "test-user", "n/a", authorities))
    }

    /**
     * An authenticated caller whose {@code TenantContext} was never populated — the
     * shape of a permit-listed request, and the case every gate must deny.
     */
    void authenticatedWithoutTenantContext() {
        tenantContext.clear()
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("ilr-test", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))))
    }

    /** No authentication at all — a gated endpoint should answer 401, not 403. */
    void unauthenticated() {
        tenantContext.clear()
        SecurityContextHolder.clearContext()
    }

    /** Resets thread-local and request state. Call from {@code cleanup()}. */
    void clear() {
        tenantContext.clear()
        SecurityContextHolder.clearContext()
    }

    private AuthorizationManagerBeforeMethodInterceptor preAuthorizeAdvisor() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler()
        // Supplies the BeanResolver that makes `@ilrAuth` resolvable inside the SpEL.
        expressionHandler.setApplicationContext(beanContext)
        PreAuthorizeAuthorizationManager manager = new PreAuthorizeAuthorizationManager()
        manager.setExpressionHandler(expressionHandler)
        return AuthorizationManagerBeforeMethodInterceptor.preAuthorize(manager)
    }
}
