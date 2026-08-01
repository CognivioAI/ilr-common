package com.cognivio.ai.common.authz;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The single source of policy truth: which {@link IlrRole} holds which
 * {@link IlrPermission}. Nothing else in the platform may state this — a role
 * literal appearing in a service repository is a bug, because it puts policy back
 * in 16 places.
 *
 * <h2>Design notes on the mapping below</h2>
 * <ul>
 *   <li><b>Firm admin is deliberately not a superuser.</b> It does not hold
 *       {@link IlrPermission#REVIEW_DECIDE}: deciding a human review is a
 *       competence-based act performed by a reviewer, not an administrative one.
 *       Administering a firm and adjudicating its work are different authorities and
 *       collapsing them would defeat the point of having a reviewer role.</li>
 *   <li><b>{@link IlrPermission#CASE_SIGNOFF} is held by the adviser roles only.</b>
 *       Sign-off is the IAA 1999 s84 control; an applicant signing off their own case
 *       is precisely the finding KAN-186 raised.</li>
 *   <li><b>{@link IlrPermission#DOCUMENT_PURGE_REQUEST} is held by {@code APPLICANT},
 *       {@code CONSULTANT} and {@code FIRM_ADMIN} (corrected by KAN-210).</b>
 *       {@code ilr-document-service}'s {@code PurgeService.requestPurge} is
 *       documented "US-019 / BR-010 <em>user-initiated</em> document purge" and
 *       applies no role check of its own — the caller is simply the case's owner or
 *       the firm staff acting for them. The original table granted this to
 *       {@code FIRM_ADMIN} alone, which would have 403'd the applicant the feature
 *       exists for.</li>
 *   <li><b>{@link IlrPermission#DOCUMENT_PURGE_CONSENT} is held by {@code CONSULTANT}
 *       only (corrected by KAN-210).</b> {@code PurgeService.decideConsent} has its
 *       own L2 check — {@code request.getConsultantUserId().equals(actingUserId)} —
 *       so only the case's <em>assigned</em> consultant may approve or decline,
 *       never the applicant. The original table granted the L1 capability to
 *       {@code APPLICANT}, which would have denied the one caller the flow is built
 *       for at the gate, before the L2 check ever ran. {@code FIRM_ADMIN} is
 *       deliberately excluded too: the L2 check is an exact
 *       assigned-consultant-identity match, so granting a firm admin the L1
 *       capability would be a no-op grant that only invites confusion about who can
 *       actually decide.</li>
 *   <li><b>{@link IlrPermission#AUDIT_WRITE} is granted to no {@link IlrRole} at all — resolved by
 *       KAN-211.</b> The caller is an inter-service client-credentials token (audit-writer et al.),
 *       never a human, so the grant lives entirely on the scope path: {@link IlrPermission#fromScope}
 *       plus {@code IlrAuthorization.hasPermission}, not this role-keyed table. An empty entry here
 *       is therefore correct and permanent, not a placeholder — a role ever appearing against
 *       {@code AUDIT_WRITE} would mean a human caller can write the audit trail directly, which
 *       BR-009 does not intend.</li>
 *   <li><b>{@link IlrPermission#REMINDER_DISPATCH} is granted to no role at all.</b> This endpoint
 *       looks like an inter-service caller rather than a human, and whether it carries a
 *       client-credentials token (gated on {@code scope}, the same mechanism KAN-211 resolved for
 *       {@code AUDIT_WRITE}) or a human Cognito token (gated on {@code cognito:groups}) is the
 *       blocking open question recorded on KAN-186 for this permission specifically. Until that is
 *       answered the fail-closed choice is an empty grant, which is inert while no endpoint is
 *       annotated and denies rather than over-grants once one is.</li>
 *   <li><b>The mapping itself has not been signed off by the architect.</b> KAN-186's
 *       design specifies that this class exists and what shape it takes, but not its
 *       contents. It is stated here so it can be reviewed as a diff in one file; the
 *       per-service stories KAN-207–KAN-212 should confirm each entry they rely on
 *       before annotating an endpoint with it.</li>
 * </ul>
 */
public final class RolePermissions {

    private static final Map<IlrRole, Set<IlrPermission>> POLICY = buildPolicy();

    private RolePermissions() {
        throw new AssertionError("static policy holder; not instantiable");
    }

    private static Map<IlrRole, Set<IlrPermission>> buildPolicy() {
        Map<IlrRole, Set<IlrPermission>> policy = new EnumMap<>(IlrRole.class);

        // Firm administration, data-subject request handling and finance.
        // Notably NOT review adjudication (see class javadoc).
        policy.put(IlrRole.FIRM_ADMIN, unmodifiable(
                IlrPermission.FIRM_SETTINGS_WRITE,
                IlrPermission.USER_DATA_EXPORT,
                IlrPermission.USER_ERASURE_REQUEST,
                IlrPermission.DOCUMENT_PURGE_REQUEST,
                IlrPermission.PAYMENT_RECONCILE,
                IlrPermission.CASE_IMPORT,
                IlrPermission.CASE_SIGNOFF,
                IlrPermission.REVIEW_ENQUEUE,
                IlrPermission.EXTRACTION_CONFIRM,
                IlrPermission.TRANSCRIPT_READ_ANY));

        // The regulated adviser: everything needed to advance a case, including the
        // s84 sign-off itself, plus the assigned-consultant consent decision on a
        // client's purge request (KAN-210 — the L2 assigned-consultant check in
        // PurgeService.decideConsent narrows this from "any consultant" to "this
        // case's consultant").
        policy.put(IlrRole.CONSULTANT, unmodifiable(
                IlrPermission.CASE_SIGNOFF,
                IlrPermission.CASE_IMPORT,
                IlrPermission.REVIEW_ENQUEUE,
                IlrPermission.EXTRACTION_CONFIRM,
                IlrPermission.TRANSCRIPT_READ_ANY,
                IlrPermission.DOCUMENT_PURGE_REQUEST,
                IlrPermission.DOCUMENT_PURGE_CONSENT));

        // Works the review queue and nothing else.
        policy.put(IlrRole.REVIEWER, unmodifiable(
                IlrPermission.REVIEW_ENQUEUE,
                IlrPermission.REVIEW_CLAIM,
                IlrPermission.REVIEW_DECIDE));

        // The data subject. Holds no firm-level capability. Requesting purge of their
        // own case's documents is the one L1 capability an applicant needs (KAN-210) —
        // everything else an applicant legitimately does is their own record, which is
        // an L2 ownership check, not an L1 capability. NOT DOCUMENT_PURGE_CONSENT: that
        // decision belongs to the case's assigned consultant, never the applicant (see
        // class javadoc).
        policy.put(IlrRole.APPLICANT, unmodifiable(
                IlrPermission.DOCUMENT_PURGE_REQUEST));

        return Map.copyOf(policy);
    }

    /** Immutable grant set. {@link Set#of} rejects duplicates, so a copy-paste slip in the policy fails fast. */
    private static Set<IlrPermission> unmodifiable(IlrPermission... permissions) {
        return Set.of(permissions);
    }

    /** The full policy, unmodifiable. Exposed for tests, diagnostics and documentation. */
    public static Map<IlrRole, Set<IlrPermission>> policy() {
        return POLICY;
    }

    /** The permissions held by a role; empty for {@code null} or an unmapped role. */
    public static Set<IlrPermission> forRole(IlrRole role) {
        if (role == null) {
            return Set.of();
        }
        return POLICY.getOrDefault(role, Set.of());
    }

    /** True if {@code role} holds {@code permission}. Null-safe; {@code null} grants nothing. */
    public static boolean grants(IlrRole role, IlrPermission permission) {
        return permission != null && forRole(role).contains(permission);
    }

    /**
     * True if <em>any</em> of the given raw role strings (as found in
     * {@code TenantContext.getRoles()}) grants {@code permission}. Each value is
     * normalised via {@link IlrRole#fromClaim(String)}, so case and {@code ROLE_}
     * prefixing do not matter and an unrecognised role simply grants nothing.
     *
     * @param rawRoles role/group values; may be {@code null} or empty
     * @param permission the required permission; {@code null} grants nothing
     */
    public static boolean grantsAny(Collection<String> rawRoles, IlrPermission permission) {
        if (rawRoles == null || rawRoles.isEmpty() || permission == null) {
            return false;
        }
        for (String raw : rawRoles) {
            Optional<IlrRole> role = IlrRole.fromClaim(raw);
            if (role.isPresent() && grants(role.get(), permission)) {
                return true;
            }
        }
        return false;
    }

    /** Every permission held by any of the given raw role strings. */
    public static Set<IlrPermission> permissionsFor(Collection<String> rawRoles) {
        Set<IlrPermission> granted = EnumSet.noneOf(IlrPermission.class);
        if (rawRoles == null) {
            return Set.copyOf(granted);
        }
        for (String raw : rawRoles) {
            IlrRole.fromClaim(raw).ifPresent(role -> granted.addAll(forRole(role)));
        }
        return Set.copyOf(granted);
    }

    /** The roles that hold {@code permission}; empty if none do (see AUDIT_WRITE). */
    public static Set<IlrRole> rolesGranting(IlrPermission permission) {
        Set<IlrRole> roles = EnumSet.noneOf(IlrRole.class);
        for (IlrRole role : IlrRole.values()) {
            if (grants(role, permission)) {
                roles.add(role);
            }
        }
        return Set.copyOf(roles);
    }
}
