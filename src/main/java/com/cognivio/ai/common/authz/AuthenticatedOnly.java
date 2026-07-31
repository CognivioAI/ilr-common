package com.cognivio.ai.common.authz;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a REST endpoint as deliberately requiring nothing beyond a valid token —
 * "any authenticated caller in this tenant may do this".
 *
 * <p><b>This annotation enforces nothing.</b> Authentication is already enforced by
 * the filter chain ({@code anyRequest().authenticated()}). Its whole purpose is to
 * make the <em>absence</em> of a capability gate an explicit, reviewable decision
 * rather than an oversight: the shared ArchUnit rule
 * ({@code ControllerAuthorizationRules}) fails the build for any mapped controller
 * method carrying neither {@code @PreAuthorize} nor this marker.
 *
 * <p>That is the fail-closed property {@code @PreAuthorize} lacks on its own. An
 * endpoint added next quarter with no annotation is, today, silently open to any
 * authenticated user — which is exactly how the estate reached zero role checks
 * (KAN-186). With the rule in place, forgetting becomes a build failure.
 *
 * <p>Always give a reason, so a reviewer can judge it:
 * <pre>{@code
 * @AuthenticatedOnly("Reference data; identical for every caller in the tenant.")
 * @GetMapping("/rules")
 * public List<RuleDto> listRules() { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthenticatedOnly {

    /** Why this endpoint needs no capability gate. Required in review, if not by the compiler. */
    String value() default "";
}
