package com.cognivio.ai.common.authz.testsupport

import com.cognivio.ai.common.authz.IlrRole
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.parameters.P
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Proves the harness enforces what it claims to, and — just as importantly — that
 * the estate's current harness does not.
 *
 * <p>KAN-186 asks for "403 for the wrong role" specs, of which the estate has none.
 * Written against plain {@code MockMvcBuilders.standaloneSetup(controller)} such a
 * spec would be worthless: standalone MockMvc builds no Spring context and creates
 * no proxy, so {@code @PreAuthorize} never runs and the endpoint answers 200 to
 * everybody. The last feature method below pins that, so nobody "simplifies" the
 * harness away and leaves fifteen services with green tests over an unenforced gate.
 */
class IlrMethodSecurityTestSupportSpec extends Specification {

    IlrMethodSecurityTestSupport authz = new IlrMethodSecurityTestSupport()
    MockMvc mockMvc = authz.mockMvc(new GuardedController())

    def cleanup() {
        authz.clear()
    }

    def "allows a caller whose role holds the permission"() {
        given:
        authz.authenticateAs(IlrRole.CONSULTANT)

        expect:
        mockMvc.perform(post("/cases/signoff")).andExpect(status().isOk())
    }

    def "denies a caller whose role does not hold the permission"() {
        given: "the KAN-186 finding: an applicant reaching the adviser sign-off"
        authz.authenticateAs(IlrRole.APPLICANT)

        expect:
        mockMvc.perform(post("/cases/signoff")).andExpect(status().isForbidden())
    }

    def "denies when the TenantContext was never populated"() {
        given:
        authz.authenticatedWithoutTenantContext()

        expect:
        mockMvc.perform(post("/cases/signoff")).andExpect(status().isForbidden())
    }

    def "denies when there is no authentication at all"() {
        given:
        authz.unauthenticated()

        expect: """403, not 401. The capability expressions never dereference the Spring
                   Authentication — they read TenantContext — so the interceptor reaches a
                   deny decision without ever noticing the credentials are missing. In a real
                   deployment the filter chain answers 401 long before a request gets here;
                   this only pins that a missing identity still denies."""
        mockMvc.perform(post("/cases/signoff")).andExpect(status().isForbidden())
    }

    def "leaves ungated endpoints reachable"() {
        given:
        authz.authenticateAs(IlrRole.APPLICANT)

        expect:
        mockMvc.perform(get("/open")).andExpect(status().isOk())
    }

    def "supports the isSelf predicate against the authenticated user"() {
        given:
        def tenantId = UUID.randomUUID()
        def userId = UUID.randomUUID()
        authz.authenticateAs(tenantId, userId, ["applicant"])

        expect: "own record allowed"
        mockMvc.perform(get("/users/{id}/export", userId)).andExpect(status().isOk())

        and: "a colleague's record denied — the L2 hole KAN-186 called out on the identity service"
        mockMvc.perform(get("/users/{id}/export", UUID.randomUUID())).andExpect(status().isForbidden())
    }

    def "the capability alternative also satisfies the isSelf-or-can expression"() {
        given:
        authz.authenticateAs(UUID.randomUUID(), UUID.randomUUID(), ["firm-admin"])

        expect:
        mockMvc.perform(get("/users/{id}/export", UUID.randomUUID())).andExpect(status().isOk())
    }

    def "a denial is rendered in the platform error shape, not as a raw stack trace"() {
        given:
        authz.authenticateAs(IlrRole.APPLICANT)

        when:
        def response = mockMvc.perform(post("/cases/signoff")).andReturn().response

        then:
        response.status == 403
        response.contentAsString.contains("FORBIDDEN")
    }

    def "the estate's plain standaloneSetup applies no method security at all"() {
        given: "the harness the existing specs use, with the same annotated controller"
        def unsecured = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new GuardedController())
                .build()

        and: "a caller who must be refused"
        authz.authenticateAs(IlrRole.APPLICANT)

        expect: "200 — which is why a 403 spec written against standaloneSetup proves nothing"
        unsecured.perform(post("/cases/signoff")).andExpect(status().isOk())
    }

    @RestController
    static class GuardedController {

        @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")
        @PostMapping("/cases/signoff")
        String signOff() {
            return "signed"
        }

        // Both names are stated explicitly because the Groovy compiler does not emit
        // -parameters, so neither MVC nor SpEL can recover the parameter name by
        // reflection: @PathVariable("userId") for argument binding and @P("userId") so
        // `#userId` resolves inside the expression. Java controllers in the services are
        // compiled with -parameters by the Spring Boot parent and need neither.
        @PreAuthorize("@ilrAuth.isSelf(#userId) or @ilrAuth.can('USER_DATA_EXPORT')")
        @GetMapping("/users/{userId}/export")
        String export(@P("userId") @PathVariable("userId") UUID userId) {
            return "exported"
        }

        @GetMapping("/open")
        String open() {
            return "open"
        }
    }
}
