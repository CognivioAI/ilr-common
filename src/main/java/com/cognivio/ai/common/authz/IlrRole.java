package com.cognivio.ai.common.authz;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The platform's canonical role vocabulary.
 *
 * <p><b>The enum constant is not the wire form.</b> Roles travel on the wire as
 * Cognito group names, which are lowercase-hyphenated ({@code firm-admin}), while
 * Java enum constants are upper-snake ({@code FIRM_ADMIN}). {@link #claimValue()}
 * is the wire form; {@link #fromClaim(String)} maps in the other direction.
 *
 * <p><b>Why {@link #fromClaim(String)} is lenient.</b> The same logical role
 * reaches this code in more than one shape today:
 * <ul>
 *   <li>{@code JwtRoleConverter} extracts the raw {@code cognito:groups} value —
 *       lowercase-hyphenated, e.g. {@code reviewer};</li>
 *   <li>{@code TenantContextFilter}'s {@code DEV_ROLES} seed uses the same
 *       lowercase-hyphenated form;</li>
 *   <li>at least one existing spec in the estate populates {@code "REVIEWER"} in
 *       upper case;</li>
 *   <li>{@code JwtRoleConverter} prefixes {@code ROLE_} when producing Spring
 *       authorities, so a {@code ROLE_}-prefixed string can leak in from code that
 *       reads authorities rather than {@code TenantContext}.</li>
 * </ul>
 * {@code TenantContext.hasRole(String)} is documented case-<em>sensitive</em>, so a
 * naive {@code getRoles().contains("firm-admin")} silently denies a caller whose
 * claim happened to be upper case. Normalising centrally, once, is the fix — every
 * comparison in this package goes through {@link #fromClaim(String)} rather than
 * comparing strings directly.
 *
 * <p>Normalisation is: trim, strip a leading {@code ROLE_} (any case), lower-case,
 * then treat {@code _} and {@code -} as equivalent. Unrecognised values yield
 * {@link Optional#empty()} — an unknown role grants nothing, never everything.
 */
public enum IlrRole {

    /** Firm administrator: firm settings, membership, billing and data-subject request handling. */
    FIRM_ADMIN("firm-admin"),

    /** Regulated immigration adviser. Holder of the IAA 1999 s84 sign-off capability. */
    CONSULTANT("consultant"),

    /** Human reviewer working the review queue: claims and decides review items. */
    REVIEWER("reviewer"),

    /** End applicant. Owns their own case data; holds no firm-level capability. */
    APPLICANT("applicant");

    private static final Map<String, IlrRole> BY_NORMALISED_CLAIM = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(
                    role -> normalise(role.claimValue), Function.identity()));

    private final String claimValue;

    IlrRole(String claimValue) {
        this.claimValue = claimValue;
    }

    /**
     * The canonical wire form — the Cognito group name and the value stored,
     * unprefixed, in {@code TenantContext.getRoles()}.
     */
    public String claimValue() {
        return claimValue;
    }

    /** The Spring authority form, i.e. {@code ROLE_} + {@link #claimValue()}. */
    public String authority() {
        return "ROLE_" + claimValue;
    }

    /**
     * Maps a raw role string from any of the forms described in the class javadoc
     * to its canonical role.
     *
     * @param raw a role/group value; may be {@code null} or blank
     * @return the role, or {@link Optional#empty()} if the value is blank or is not
     *         a role this platform knows about
     */
    public static Optional<IlrRole> fromClaim(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalised = normalise(raw);
        if (normalised.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NORMALISED_CLAIM.get(normalised));
    }

    /**
     * Normalises a raw role string to the comparison key. Package-visible only via
     * {@link #fromClaim(String)}; kept private so there is exactly one normalisation
     * rule in the codebase.
     */
    private static String normalise(String raw) {
        String value = raw.trim();
        if (value.regionMatches(true, 0, "ROLE_", 0, 5)) {
            value = value.substring(5);
        }
        return value.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
