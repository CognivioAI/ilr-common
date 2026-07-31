package com.cognivio.ai.common.authz.fixture.violating

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Fixture: the failure the rule exists to catch — a new endpoint added with no
 * authorization decision, silently reachable by any authenticated caller. Two of the
 * three methods here are violations; the annotated one proves the rule reports the
 * offenders rather than the whole class.
 */
@RestController
class UnguardedController {

    @PostMapping("/audit-events")
    String writeAuditEvent() {
        return "written"
    }

    @DeleteMapping("/cases/{caseId}/documents/purge")
    String purge() {
        return "purged"
    }

    @PreAuthorize("@ilrAuth.can('CASE_IMPORT')")
    @GetMapping("/cases")
    String listCases() {
        return "cases"
    }
}
