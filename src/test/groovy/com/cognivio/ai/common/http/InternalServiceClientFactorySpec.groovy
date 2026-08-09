package com.cognivio.ai.common.http

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequest
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import spock.lang.Specification

import java.time.Instant

/**
 * KAN-236: what the factory actually puts on the wire.
 *
 * <p>{@link BearerTokenRelayInterceptorSpec} pins the interceptor's decisions in isolation; this
 * spec proves the factory wires that interceptor into the client for an allow-listed base URL, does
 * <b>not</b> for anything else, and never mutates the injected builder (which would leak the relay
 * onto the Bedrock and GOV.UK clients built from it).
 *
 * <p>A recording {@code ClientHttpRequestFactory} is used rather than a real socket server: the
 * assertion is about the outbound headers, and a no-network test cannot flake on a CI sandbox.
 */
class InternalServiceClientFactorySpec extends Specification {

    static final String TOKEN = 'header.payload.signature'
    static final String INTERNAL = 'https://api.cognivio-ai.test/knowledge/v1'

    RecordingRequestFactory recorder = new RecordingRequestFactory()

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "a client for an allow-listed base url sends the caller's token"() {
        given:
        authenticatedWithJwt()
        RestClient client = factoryAllowing(INTERNAL).forBaseUrl(INTERNAL)

        when:
        client.get().uri('/thresholds/BR-006').retrieve().toBodilessEntity()

        then:
        recorder.lastRequest.URI.toString() == "${INTERNAL}/thresholds/BR-006"
        recorder.lastRequest.headers.getFirst(HttpHeaders.AUTHORIZATION) == "Bearer ${TOKEN}"
    }

    def "a client for a base url that is NOT allow-listed is built plain — fail closed"() {
        given: "a fully authenticated caller, so only the allow-list can stop the relay"
        authenticatedWithJwt()
        RestClient client = factoryAllowing('https://somewhere-else.test').forBaseUrl(INTERNAL)

        when:
        client.get().uri('/thresholds/BR-006').retrieve().toBodilessEntity()

        then: "the call still happens; it simply carries no credential"
        recorder.lastRequest.headers.getFirst(HttpHeaders.AUTHORIZATION) == null
    }

    def "an unauthenticated caller reaches the downstream with no Authorization header"() {
        given:
        SecurityContextHolder.clearContext()
        recorder.status = HttpStatus.UNAUTHORIZED
        RestClient client = factoryAllowing(INTERNAL).forBaseUrl(INTERNAL)

        when: "fail soft — the downstream returns its own 401 rather than this client throwing early"
        client.get().uri('/thresholds/BR-006').retrieve().toBodilessEntity()

        then:
        thrown(HttpClientErrorException.Unauthorized)
        recorder.lastRequest.headers.getFirst(HttpHeaders.AUTHORIZATION) == null
    }

    def "a 200 JSON body served with a text/plain Content-Type still deserializes (KAN-236 adapter workaround)"() {
        // Every lambda-profile ILR service answers a 200 with Content-Type: text/plain instead of
        // application/json (a spring-cloud-function-serverless-web adapter defect, confirmed
        // independent of the request's Accept header — see InternalServiceClientFactory). Before
        // KAN-236 no internal client was strict about response Content-Type, so this was invisible;
        // RestClient IS strict, and without this workaround every internal read would throw
        // UnknownContentTypeException and silently fall back to seed/fail-safe defaults.
        given:
        authenticatedWithJwt()
        recorder.responseBody = '{"route":"SKILLED_WORKER","version":"1"}'.bytes
        recorder.contentType = org.springframework.http.MediaType.TEXT_PLAIN
        RestClient client = factoryAllowing(INTERNAL).forBaseUrl(INTERNAL)

        when:
        Map body = client.get().uri('/ukvi-mappings').retrieve().body(Map)

        then:
        body == [route: 'SKILLED_WORKER', version: '1']
    }

    def "building an internal client never mutates the injected builder"() {
        given: "the leak the factory exists to avoid: an interceptor escaping onto other clients"
        authenticatedWithJwt()
        RestClient.Builder sharedBuilder = RestClient.builder().requestFactory(recorder)
        InternalServiceClientFactory.withBuilder(sharedBuilder, InternalBaseUrlAllowList.of([INTERNAL]))
                .forBaseUrl(INTERNAL)

        and: "a third-party client built afterwards from that same builder — Bedrock, a GOV.UK fetch"
        RestClient thirdParty = sharedBuilder.baseUrl('https://bedrock-runtime.eu-west-2.amazonaws.com').build()

        when:
        thirdParty.get().uri('/model/invoke').retrieve().toBodilessEntity()

        then: "no token leaks to it"
        recorder.lastRequest.headers.getFirst(HttpHeaders.AUTHORIZATION) == null
    }

    // NOTE: the "no RestClient.Builder bean available" fallback in InternalServiceClientFactory is
    // deliberately not covered by a spec here. Exercising it means letting RestClient.builder() pick
    // its own default request factory, which on the JDK path opens an NIO Selector — and this
    // repository's CI/dev machines include one where Selector.open() itself fails ("Unable to
    // establish loopback connection"), unrelated to any code under test. A spec that fails there
    // would be noise, not signal; the branch is a one-line defensive default for direct
    // instantiation, and IlrInternalHttpAutoConfigurationSpec proves the wired-up path.

    def "a factory with a null allow-list relays nothing"() {
        given:
        authenticatedWithJwt()
        InternalServiceClientFactory factory =
                InternalServiceClientFactory.withBuilder(RestClient.builder().requestFactory(recorder), null)

        expect:
        factory.allowList().isEmpty()

        when:
        factory.forBaseUrl(INTERNAL).get().uri('/x').retrieve().toBodilessEntity()

        then:
        recorder.lastRequest.headers.getFirst(HttpHeaders.AUTHORIZATION) == null
    }

    private InternalServiceClientFactory factoryAllowing(String... allowed) {
        return InternalServiceClientFactory.withBuilder(
                RestClient.builder().requestFactory(recorder), InternalBaseUrlAllowList.of(allowed.toList()))
    }

    private static void authenticatedWithJwt() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header('alg', 'RS256')
                .claim('sub', '00000000-0000-0000-0000-0000000000a1')
                .claim('tenant_id', '00000000-0000-0000-0000-000000000001')
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()
        SecurityContextHolder.context.authentication =
                new JwtAuthenticationToken(jwt, [new SimpleGrantedAuthority('ROLE_APPLICANT')])
    }

    /** Records the request that would have gone out and answers with a canned response. */
    private static class RecordingRequestFactory implements ClientHttpRequestFactory {

        MockClientHttpRequest lastRequest
        HttpStatus status = HttpStatus.OK
        byte[] responseBody = new byte[0]
        org.springframework.http.MediaType contentType = null

        @Override
        ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri)
            MockClientHttpResponse response = new MockClientHttpResponse(responseBody, status)
            if (contentType != null) {
                response.headers.contentType = contentType
            }
            request.setResponse(response)
            lastRequest = request
            return request
        }
    }
}
