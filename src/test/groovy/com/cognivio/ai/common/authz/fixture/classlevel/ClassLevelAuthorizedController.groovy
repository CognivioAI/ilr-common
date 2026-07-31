package com.cognivio.ai.common.authz.fixture.classlevel

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Fixture: a class-level {@code @PreAuthorize} applies to every method, so the rule
 * must accept methods carrying no annotation of their own.
 */
@RestController
@PreAuthorize("@ilrAuth.can('FIRM_SETTINGS_WRITE')")
class ClassLevelAuthorizedController {

    @GetMapping("/firm/settings")
    String read() {
        return "settings"
    }

    @PostMapping("/firm/settings")
    String write() {
        return "written"
    }
}
