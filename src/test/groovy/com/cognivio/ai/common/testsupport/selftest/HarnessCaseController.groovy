package com.cognivio.ai.common.testsupport.selftest

import com.cognivio.ai.common.authz.AuthenticatedOnly
import com.cognivio.ai.common.context.TenantContext
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The gated surface {@code HarnessSecurityIT} drives, standing in for a real service's
 * security-critical endpoints (case sign-off, erasure request, purge, and so on).
 *
 * <p>The gate is {@code @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")} rather than a role literal,
 * matching the KAN-206 model: {@code CASE_SIGNOFF} is held by {@code CONSULTANT} and
 * {@code FIRM_ADMIN} but not by {@code REVIEWER} or {@code APPLICANT}, which gives the harness a real
 * allow case and a real deny case without inventing a policy of its own.
 *
 * <p>Note what {@code /harness/cases} does <em>not</em> do: it never reads
 * {@code TenantContext.getTenantId()} and never passes a tenant to the repository. Every isolation
 * property the harness demonstrates therefore comes from the database, which is what makes those
 * assertions meaningful.
 */
@RestController
@RequestMapping('/harness')
class HarnessCaseController {

    private final HarnessCaseService service
    private final TenantContext tenantContext

    HarnessCaseController(HarnessCaseService service, TenantContext tenantContext) {
        this.service = service
        this.tenantContext = tenantContext
    }

    /** Requires the {@code CASE_SIGNOFF} capability; returns only rows RLS admits. */
    @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")
    @GetMapping('/cases')
    List<Map<String, Object>> listCases() {
        service.visibleCases().collect { HarnessCase c ->
            [id: c.id?.toString(), tenantId: c.tenantId?.toString(), name: c.name] as Map<String, Object>
        }
    }

    /** Requires the {@code CASE_SIGNOFF} capability; 404 when the row is invisible to the caller's tenant. */
    @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")
    // The path variable is named explicitly: gmavenplus does not compile with `-parameters`, so
    // Spring cannot infer it by reflection here. Service controllers are Java, which
    // spring-boot-starter-parent does compile with `-parameters`, so this is a fixture concern only.
    @PostMapping('/cases/{caseId}/sign-off')
    ResponseEntity<Map<String, Object>> signOff(@PathVariable('caseId') UUID caseId) {
        boolean signed = service.signOff(caseId, tenantContext.userId)
        return signed
                ? ResponseEntity.ok([caseId: caseId.toString(), signedOffBy: tenantContext.userId?.toString()] as Map<String, Object>)
                : ResponseEntity.notFound().build()
    }

    /**
     * No capability gate — used to assert that the identity the platform derived from the token is
     * the one the request actually runs under (tenant, user and roles all resolved by the real
     * {@code TenantClaimResolver}, not by the test).
     */
    @AuthenticatedOnly('Diagnostic echo of the caller identity; exposes only what the caller already sent.')
    @GetMapping('/me')
    Map<String, Object> me() {
        [
                tenantId: tenantContext.tenantId?.toString(),
                firmId  : tenantContext.firmId?.toString(),
                userId  : tenantContext.userId?.toString(),
                roles   : tenantContext.roles.toSorted(),
        ] as Map<String, Object>
    }
}
