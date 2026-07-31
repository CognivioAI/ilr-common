package com.cognivio.ai.common.authz.fixture.compliant

import com.cognivio.ai.common.authz.AuthenticatedOnly
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * Fixture for {@code ControllerAuthorizationRulesSpec}: every shape of endpoint the
 * rule must accept. Not a real controller and never component-scanned.
 */
@RestController
class CompliantController {

    /** Capability gate — the normal case. */
    @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")
    @PostMapping("/cases/{caseId}/signoff")
    String signOff(@PathVariable("caseId") UUID caseId) {
        return "signed"
    }

    /** Capability gate combined with the self check. */
    @PreAuthorize("@ilrAuth.isSelf(#userId) or @ilrAuth.can('USER_DATA_EXPORT')")
    @GetMapping("/users/{userId}/data-export")
    String export(@PathVariable("userId") UUID userId) {
        return "exported"
    }

    /** Deliberately open to any authenticated caller, on the record. */
    @AuthenticatedOnly("Reference data, identical for every caller in the tenant.")
    @GetMapping("/rules")
    String rules() {
        return "rules"
    }

    /** A bare @RequestMapping counts as mapped, not only the @GetMapping shorthands. */
    @PreAuthorize("@ilrAuth.can('CASE_IMPORT')")
    @RequestMapping(value = "/cases/import", method = RequestMethod.POST)
    String importCases() {
        return "imported"
    }

    /**
     * Public but not mapped to any HTTP verb, and therefore not an endpoint. The rule
     * must not demand an annotation here — a noisy fitness function gets suppressed.
     */
    String notAnEndpoint() {
        return "internal"
    }
}
