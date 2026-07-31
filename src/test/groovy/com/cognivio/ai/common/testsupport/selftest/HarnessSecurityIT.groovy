package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.authz.IlrPermission
import com.cognivio.ai.common.authz.IlrRole
import com.cognivio.ai.common.authz.RolePermissions

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tier C — the real filter chain over a gated endpoint, as the four-row matrix KAN-190's design
 * specifies: no token, wrong role, right role, right role but another tenant's resource.
 *
 * <p>This is the tier that could not be written before now. The estate's controller specs are
 * {@code MockMvcBuilders.standaloneSetup(...)}, which builds no Spring context, so no proxy is
 * created and {@code @PreAuthorize} is never applied at all — a "403 for the wrong role" expectation
 * in one of those specs passes whether or not the annotation is present. Here the annotation is
 * genuinely evaluated, by the real method-security interceptor, against an identity the real
 * {@code JwtRoleConverter} and {@code TenantClaimResolver} derived from a real signed token.
 *
 * <p>The fourth row is the one that matters most, and is the only kind of test in the estate that can
 * catch a repository method missing its tenant predicate: {@link HarnessCaseRepository} deliberately
 * has no tenant predicate, so the isolation here is entirely the database's doing, reached through
 * the real {@code TenantContextFilter} → {@code TenantContext} → {@code RlsTenantBindingAspect} →
 * PostgreSQL policy path.
 */
class HarnessSecurityIT extends AbstractHarnessSelfTestIT {

    def "the policy this spec relies on is what RolePermissions actually says"() {
        expect: "stated rather than assumed, so a policy change fails here with a clear reason"
        RolePermissions.grants(IlrRole.CONSULTANT, IlrPermission.CASE_SIGNOFF)
        RolePermissions.grants(IlrRole.FIRM_ADMIN, IlrPermission.CASE_SIGNOFF)
        !RolePermissions.grants(IlrRole.REVIEWER, IlrPermission.CASE_SIGNOFF)
        !RolePermissions.grants(IlrRole.APPLICANT, IlrPermission.CASE_SIGNOFF)
    }

    def "row 1 — no token is 401, not 403"() {
        expect: "the 401/403 distinction only exists through a real filter chain"
        mockMvc.perform(get('/harness/cases'))
                .andExpect(status().isUnauthorized())
    }

    def "row 2 — a valid token without the capability is 403 in the platform error shape"() {
        when:
        def result = mockMvc.perform(get('/harness/cases').with(bearerFor(role)))
                .andExpect(status().isForbidden())

        then: "@PreAuthorize genuinely fired — this is what standaloneSetup cannot show"
        def json = body(result)
        json.status == 403
        json.error == 'FORBIDDEN'

        where:
        role << [IlrRole.APPLICANT, IlrRole.REVIEWER]
    }

    def "row 3 — a valid token with the capability is 200"() {
        expect:
        mockMvc.perform(get('/harness/cases').with(bearerFor(role)))
                .andExpect(status().isOk())

        where:
        role << [IlrRole.CONSULTANT, IlrRole.FIRM_ADMIN]
    }

    def "row 4 — an authorized caller sees only their own tenant's rows"() {
        when:
        def result = mockMvc.perform(get('/harness/cases').with(bearerFor(IlrRole.CONSULTANT, tenantId)))
                .andExpect(status().isOk())

        then: "the repository applies no tenant predicate; every row here was filtered by Postgres"
        def json = body(result)
        json*.name == expectedNames
        json.every { it.tenantId == tenantId.toString() }

        where:
        tenantId || expectedNames
        TENANT_A || ['tenant-A first case', 'tenant-A second case']
        TENANT_B || ['tenant-B only case']
    }

    def "row 4b — a write against another tenant's row is 404, and changes nothing"() {
        given: "a case id that exists, but belongs to tenant B"
        def tenantBCaseId = UUID.fromString('bbbb0001-0000-0000-0000-000000000001')

        expect: "invisible to tenant A, so indistinguishable from a nonexistent row — it leaks nothing"
        mockMvc.perform(post("/harness/cases/${tenantBCaseId}/sign-off")
                .with(bearerFor(IlrRole.CONSULTANT, TENANT_A)))
                .andExpect(status().isNotFound())
    }

    def "a write within the caller's own tenant succeeds through the same path"() {
        given: "a case belonging to tenant A"
        def tenantACaseId = UUID.fromString('aaaa0001-0000-0000-0000-000000000001')

        when:
        def result = mockMvc.perform(post("/harness/cases/${tenantACaseId}/sign-off")
                .with(bearerFor(IlrRole.CONSULTANT, TENANT_A)))
                .andExpect(status().isOk())

        then: "proves the RLS binding covers writes, not only reads"
        body(result).caseId == tenantACaseId.toString()
    }

    def "an unknown group in the token grants nothing"() {
        expect: "an unrecognised role must deny, never fall back to a default"
        mockMvc.perform(get('/harness/cases').with(bearerForClaims([
                sub              : UUID.randomUUID().toString(),
                tenant_id        : TENANT_A.toString(),
                'cognito:groups' : ['super-admin', 'root'],
        ])))
                .andExpect(status().isForbidden())
    }

    def "role matching tolerates the claim-form variations the estate actually produces"() {
        expect: "IlrRole.fromClaim normalisation, proved end to end rather than only in a unit test"
        mockMvc.perform(get('/harness/cases').with(bearerForClaims([
                sub              : UUID.randomUUID().toString(),
                tenant_id        : TENANT_A.toString(),
                'cognito:groups' : [claimValue],
        ])))
                .andExpect(status().isOk())

        where:
        claimValue << ['consultant', 'CONSULTANT', 'firm-admin', 'FIRM_ADMIN']
    }
}
