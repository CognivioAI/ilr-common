package com.cognivio.ai.common.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * The set of base URLs a caller's bearer token may be relayed to (KAN-236, Phase 1 M2M).
 *
 * <p>This type is the reason the relay is implemented as a dedicated client factory rather than a
 * global {@code RestClient.Builder} customizer. A global customizer attaches the interceptor to
 * <b>every</b> {@code RestClient} in the application, including the ones that call Amazon Bedrock
 * and fetch GOV.UK pages — which would put the end user's Cognito access token into an outbound
 * request to a third party. An allow-list makes the blast radius explicit and reviewable: adding a
 * destination is a deliberate configuration change, not a side effect of constructing a client.
 *
 * <p>Matching is on the <b>origin</b> (scheme + host + effective port) plus the base path prefix,
 * all normalized. Comparing the whole string would break the moment a query parameter or a differing
 * trailing slash appeared; comparing only the host would relay to
 * {@code https://internal-host/../some-other-tenant-app}. Both host and scheme are compared because
 * downgrading {@code https} to {@code http} on the same host must not silently keep the token.
 *
 * <p>Instances are immutable and safe to share across threads.
 */
public final class InternalBaseUrlAllowList {

    private static final int NO_PORT = -1;
    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;

    private final List<Origin> allowed;

    private InternalBaseUrlAllowList(List<Origin> allowed) {
        this.allowed = List.copyOf(allowed);
    }

    /**
     * Builds an allow-list from configured base URLs.
     *
     * <p>Fails fast (rather than silently dropping the entry) when an entry is not an absolute
     * http/https URL with a host: a typo in this property would otherwise degrade every internal
     * call to unauthenticated, which surfaces only as a downstream 401 in a deployed environment.
     * A configuration error must break startup, not authentication.
     *
     * @param baseUrls configured base URLs; blank entries are ignored, {@code null} is treated as empty
     * @throws IllegalArgumentException if an entry is not a syntactically valid absolute http(s) URL
     */
    public static InternalBaseUrlAllowList of(Collection<String> baseUrls) {
        List<Origin> origins = new ArrayList<>();
        if (baseUrls != null) {
            for (String baseUrl : baseUrls) {
                if (baseUrl != null && !baseUrl.isBlank()) {
                    origins.add(parse(baseUrl.trim()));
                }
            }
        }
        return new InternalBaseUrlAllowList(origins);
    }

    /** An allow-list that permits nothing — no token is ever relayed. */
    public static InternalBaseUrlAllowList empty() {
        return new InternalBaseUrlAllowList(List.of());
    }

    /** True when nothing is allow-listed, i.e. the relay is effectively switched off. */
    public boolean isEmpty() {
        return allowed.isEmpty();
    }

    /**
     * Whether the caller's bearer token may be relayed on a request to this URI.
     *
     * @param uri an absolute request URI; a relative or host-less URI is never permitted
     */
    public boolean permits(URI uri) {
        if (uri == null || uri.getHost() == null || uri.getScheme() == null) {
            return false;
        }
        Origin candidate = toOrigin(uri);
        for (Origin origin : allowed) {
            if (origin.covers(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a base URL string may carry the relay. Used by
     * {@link InternalServiceClientFactory} at construction time; a malformed base URL is
     * <b>not</b> permitted rather than an error, because a service configuring a downstream URI it
     * cannot parse should get a working (if unauthenticated) client, not a startup crash from a
     * dependency's URL.
     */
    public boolean permits(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            return permits(new URI(baseUrl.trim()));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** The allow-listed origins, rendered for logging. Never contains credentials. */
    @Override
    public String toString() {
        return allowed.stream().map(Origin::render).toList().toString();
    }

    private static Origin parse(String baseUrl) {
        URI uri;
        try {
            uri = new URI(baseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "ilr.internal-http.allowed-base-urls contains a malformed URL: " + baseUrl, e);
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "ilr.internal-http.allowed-base-urls entries must be absolute http(s) URLs with a host, "
                            + "e.g. https://api.example.com/knowledge/v1 — got: " + baseUrl);
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!HTTP.equals(scheme) && !HTTPS.equals(scheme)) {
            throw new IllegalArgumentException(
                    "ilr.internal-http.allowed-base-urls supports only http and https — got: " + baseUrl);
        }
        return toOrigin(uri);
    }

    private static Origin toOrigin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        return new Origin(scheme,
                uri.getHost().toLowerCase(Locale.ROOT),
                effectivePort(scheme, uri.getPort()),
                normalizePath(uri.getRawPath()));
    }

    private static int effectivePort(String scheme, int port) {
        if (port != NO_PORT) {
            return port;
        }
        return HTTPS.equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    /** Strips a trailing slash so {@code https://h/knowledge/v1} and {@code .../v1/} are one origin. */
    private static String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            return "";
        }
        return rawPath.endsWith("/") ? rawPath.substring(0, rawPath.length() - 1) : rawPath;
    }

    /** A normalized scheme/host/port/base-path tuple. */
    private record Origin(String scheme, String host, int port, String path) {

        /**
         * True when {@code candidate} sits at this origin and under this base path. The
         * {@code path + "/"} check is what stops {@code /knowledge/v1} from covering
         * {@code /knowledge/v1-internal}.
         */
        boolean covers(Origin candidate) {
            if (!scheme.equals(candidate.scheme) || !host.equals(candidate.host) || port != candidate.port) {
                return false;
            }
            if (path.isEmpty()) {
                return true;
            }
            return candidate.path.equals(path) || candidate.path.startsWith(path + "/");
        }

        String render() {
            return scheme + "://" + host + ":" + port + path;
        }
    }
}
