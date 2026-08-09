package com.cognivio.ai.common.http

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * KAN-236: the caller's verified bearer token is relayed to internal ILR services and to nothing
 * else.
 *
 * <p>Every assertion here is about a failure that has no other detector. A missing relay shows up
 * only as a 401 from a deployed downstream; a relay to the wrong host shows up as nothing at all
 * until someone reads a third party's access logs.
 */
class BearerTokenRelayInterceptorSpec extends Specification {

    static final String INTERNAL = 'https://api.cognivio-ai.test/knowledge/v1'
    static final String TOKEN = 'header.payload.signature'

    InternalBaseUrlAllowList allowList = InternalBaseUrlAllowList.of([INTERNAL])
    BearerTokenRelayInterceptor interceptor = new BearerTokenRelayInterceptor(allowList)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "relays the caller's token to an allow-listed internal service"() {
        given:
        authenticatedWithJwt(TOKEN)
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then:
        request.headers.getFirst(HttpHeaders.AUTHORIZATION) == "Bearer ${TOKEN}"
    }

    def "relays nothing when the request is unauthenticated — the downstream, not this client, decides"() {
        given: "no SecurityContext at all, e.g. a scheduled job or an async thread that lost it"
        SecurityContextHolder.clearContext()
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then: "the call still goes out — fail soft, so the 401 comes from one place, not two"
        !request.headers.containsKey(HttpHeaders.AUTHORIZATION)
    }

    @Unroll
    def "relays nothing for a #description — there is no bearer credential to forward"() {
        given:
        SecurityContextHolder.context.authentication = authentication
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then:
        !request.headers.containsKey(HttpHeaders.AUTHORIZATION)

        where:
        description                     | authentication
        'anonymous caller'              | new AnonymousAuthenticationToken('key', 'anonymous',
                [new SimpleGrantedAuthority('ROLE_ANONYMOUS')])
        'username/password credential'  | new UsernamePasswordAuthenticationToken('alice', 'hunter2', [])
    }

    def "never forwards a password-shaped credential even when one is present"() {
        given: "an Authentication whose credentials are a plain secret, not a Jwt"
        SecurityContextHolder.context.authentication =
                new UsernamePasswordAuthenticationToken('alice', 's3cret-password', [])
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then:
        request.headers.getFirst(HttpHeaders.AUTHORIZATION) == null
    }

    @Unroll
    def "never relays to #description (#uri) even with a fully authenticated caller"() {
        given: "the leak this whole design exists to prevent"
        authenticatedWithJwt(TOKEN)
        HttpRequest request = requestTo(uri)

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then:
        !request.headers.containsKey(HttpHeaders.AUTHORIZATION)

        where:
        description                       | uri
        'Amazon Bedrock'                  | 'https://bedrock-runtime.eu-west-2.amazonaws.com/model/invoke'
        'a GOV.UK page fetch'             | 'https://www.gov.uk/indefinite-leave-to-remain'
        'an attacker-controlled host'     | 'https://evil.example.com/collect'
        'a look-alike host'               | 'https://api.cognivio-ai.test.evil.com/knowledge/v1/x'
        'the same host, different port'   | 'https://api.cognivio-ai.test:8443/knowledge/v1/x'
        'the same host over plain http'   | 'http://api.cognivio-ai.test/knowledge/v1/x'
        'a sibling path on the same host' | 'https://api.cognivio-ai.test/billing/v1/invoices'
        'a path-prefix look-alike'        | 'https://api.cognivio-ai.test/knowledge/v1-internal/x'
    }

    def "an absolute per-request URI cannot smuggle the token past the factory's base-url check"() {
        given: "RestClient lets a call site replace the configured base URL outright"
        authenticatedWithJwt(TOKEN)
        HttpRequest request = requestTo('https://evil.example.com/collect')

        when: "the interceptor was attached because the BASE url was allow-listed"
        interceptor.intercept(request, new byte[0], executionStub())

        then: "it still re-checks the final URI, so the base-url check cannot be bypassed"
        !request.headers.containsKey(HttpHeaders.AUTHORIZATION)
    }

    def "leaves an explicitly set Authorization header alone"() {
        given:
        authenticatedWithJwt(TOKEN)
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")
        request.headers.set(HttpHeaders.AUTHORIZATION, 'Bearer caller-supplied')

        when:
        interceptor.intercept(request, new byte[0], executionStub())

        then:
        request.headers.getFirst(HttpHeaders.AUTHORIZATION) == 'Bearer caller-supplied'
    }

    def "always executes the request, relayed or not"() {
        given:
        SecurityContextHolder.clearContext()
        HttpRequest request = requestTo('https://evil.example.com/collect')
        ClientHttpRequestExecution execution = Mock()

        when:
        interceptor.intercept(request, new byte[0], execution)

        then: "fail soft: never short-circuit, never throw"
        1 * execution.execute(request, _) >> Stub(ClientHttpResponse)
    }

    def "an interceptor built with no allow-list relays nothing"() {
        given:
        authenticatedWithJwt(TOKEN)
        HttpRequest request = requestTo("${INTERNAL}/thresholds/BR-006")

        when:
        new BearerTokenRelayInterceptor(null).intercept(request, new byte[0], executionStub())

        then: "fail closed on a missing allow-list, rather than defaulting to 'everything'"
        !request.headers.containsKey(HttpHeaders.AUTHORIZATION)
    }

    private static void authenticatedWithJwt(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header('alg', 'RS256')
                .claim('sub', '00000000-0000-0000-0000-0000000000a1')
                .claim('tenant_id', '00000000-0000-0000-0000-000000000001')
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()
        SecurityContextHolder.context.authentication =
                new JwtAuthenticationToken(jwt, [new SimpleGrantedAuthority('ROLE_APPLICANT')])
    }

    private static HttpRequest requestTo(String uri) {
        return new SimpleHttpRequest(HttpMethod.GET, URI.create(uri))
    }

    private static ClientHttpRequestExecution executionStub() {
        return { HttpRequest req, byte[] body ->
            new MockClientHttpResponse(new byte[0], HttpStatus.OK)
        } as ClientHttpRequestExecution
    }

    /** Minimal mutable {@link HttpRequest}; Spring ships no public test double for one. */
    private static class SimpleHttpRequest implements HttpRequest {

        private final HttpMethod method
        private final URI uri
        private final HttpHeaders headers = new HttpHeaders()

        SimpleHttpRequest(HttpMethod method, URI uri) {
            this.method = method
            this.uri = uri
        }

        @Override
        HttpMethod getMethod() {
            return method
        }

        @Override
        URI getURI() {
            return uri
        }

        @Override
        HttpHeaders getHeaders() {
            return headers
        }
    }
}
