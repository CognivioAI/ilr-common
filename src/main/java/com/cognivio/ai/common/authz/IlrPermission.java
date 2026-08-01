package com.cognivio.ai.common.authz;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A named <em>capability</em> — "may a caller of some role perform this kind of
 * action at all?". This is layer L1 of the authorization model (see KAN-186): it is
 * a static property of an endpoint and says nothing about <em>which record</em> the
 * caller may touch (L2, a domain check) or which tenant the row belongs to (L3, a
 * repository predicate plus RLS).
 *
 * <p><b>Why permissions and not roles in the annotations.</b> Writing
 * {@code @PreAuthorize("hasRole('reviewer')")} spreads role literals across 16
 * repositories, so introducing (say) a {@code senior-reviewer} group becomes a
 * 16-repository sweep. Naming the capability instead keeps the policy in one place —
 * {@link RolePermissions} — and makes the annotation read as the business rule it is.
 *
 * <p>The enum constant name <em>is</em> the wire name used in SpEL, e.g.
 * {@code @PreAuthorize("@ilrAuth.can('CASE_SIGNOFF')")}.
 */
public enum IlrPermission {

    /**
     * Record the regulated adviser's sign-off on a case. This is the control that
     * keeps the product on the guidance side of the IAA 1999 s84 boundary, so it is
     * the single highest-value gate in the estate.
     */
    CASE_SIGNOFF,

    /** Bulk-import cases (e.g. the CSV import endpoint). */
    CASE_IMPORT,

    /** Place an item on the human-review queue. */
    REVIEW_ENQUEUE,

    /** Claim an item off the human-review queue for oneself. */
    REVIEW_CLAIM,

    /** Record the outcome of a human review. */
    REVIEW_DECIDE,

    /** Export a user's personal data (GDPR Art. 15 subject access request). */
    USER_DATA_EXPORT,

    /** Raise an erasure request against a user (GDPR Art. 17). */
    USER_ERASURE_REQUEST,

    /** Change firm-level settings. */
    FIRM_SETTINGS_WRITE,

    /** Request irreversible destruction of a case's documents. */
    DOCUMENT_PURGE_REQUEST,

    /** Give (or withdraw) the data subject's consent to a document purge. */
    DOCUMENT_PURGE_CONSENT,

    /**
     * Write to the audit trail. Granted to no role pending the machine-to-machine
     * decision recorded on KAN-186 — see {@link RolePermissions}.
     */
    AUDIT_WRITE,

    /** Reconcile a payment. */
    PAYMENT_RECONCILE,

    /** Confirm (accept as correct) a field the AI extracted — a human judgement act. */
    EXTRACTION_CONFIRM,

    /**
     * Dispatch reminder notifications. Granted to no role pending the
     * machine-to-machine decision recorded on KAN-186 — see {@link RolePermissions}.
     */
    REMINDER_DISPATCH,

    /**
     * Read an assistant transcript belonging to <em>another</em> user. Named here so
     * KAN-198's consultant override can be expressed as a capability rather than a
     * hardcoded role literal; on its own this permission does not satisfy KAN-198,
     * which additionally needs the L2 ownership predicate.
     */
    TRANSCRIPT_READ_ANY;

    private static final Map<String, IlrPermission> BY_NORMALISED_NAME = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(
                    permission -> normalise(permission.name()), Function.identity()));

    /**
     * Resolves a permission from the string used in a SpEL expression. Tolerates
     * surrounding whitespace, lower case and hyphens; anything else is unknown.
     *
     * @param raw the permission name; may be {@code null} or blank
     * @return the permission, or {@link Optional#empty()} if it is not a known
     *         permission. Callers must treat "unknown" as <b>deny</b>: a typo in an
     *         annotation must fail closed, never open.
     */
    public static Optional<IlrPermission> fromName(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalised = normalise(raw);
        if (normalised.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NORMALISED_NAME.get(normalised));
    }

    /**
     * Resolves a permission from a raw OAuth2 scope value carried on a client-credentials token
     * (KAN-211), e.g. {@code "ilr-audit/audit-write"} or plain {@code "audit-write"}. Any
     * {@code resourceServerIdentifier/} prefix — the text up to and including the last {@code /} — is
     * stripped first, then the remainder is resolved exactly as {@link #fromName(String)} would.
     *
     * @param rawScope a single scope token; may be {@code null} or blank
     * @return the permission, or {@link Optional#empty()} for an unrecognised or unparseable scope —
     *         callers must treat this as deny, never as a wildcard grant
     */
    public static Optional<IlrPermission> fromScope(String rawScope) {
        if (rawScope == null) {
            return Optional.empty();
        }
        String trimmed = rawScope.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        int lastSlash = trimmed.lastIndexOf('/');
        String withoutPrefix = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
        return fromName(withoutPrefix);
    }

    private static String normalise(String raw) {
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_')
                .replace(':', '_');
    }
}
