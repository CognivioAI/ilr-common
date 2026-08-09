package com.cognivio.ai.common.http

import spock.lang.Specification
import spock.lang.Unroll

/**
 * KAN-236: the allow-list is the only thing standing between an end user's Cognito access token and
 * a third party, so its matching semantics are pinned rather than assumed.
 */
class InternalBaseUrlAllowListSpec extends Specification {

    @Unroll
    def "#candidate is #verb against allow-list #allowed"() {
        expect:
        InternalBaseUrlAllowList.of(allowed).permits(URI.create(candidate)) == permitted

        where:
        allowed                                        | candidate                                              || permitted
        ['https://api.test/knowledge/v1']              | 'https://api.test/knowledge/v1'                        || true
        ['https://api.test/knowledge/v1']              | 'https://api.test/knowledge/v1/thresholds/BR-006'      || true
        ['https://api.test/knowledge/v1/']             | 'https://api.test/knowledge/v1/thresholds'             || true
        ['https://API.TEST/knowledge/v1']              | 'https://api.test/knowledge/v1/x'                      || true
        ['HTTPS://api.test/knowledge/v1']              | 'https://api.test/knowledge/v1/x'                      || true
        ['https://api.test:443/knowledge/v1']          | 'https://api.test/knowledge/v1/x'                      || true
        ['http://knowledge-service:8090']              | 'http://knowledge-service:8090/thresholds/BR-006'      || true
        ['http://knowledge-service:8090']              | 'http://knowledge-service:8090/x?route=SW'             || true
        ['http://localhost:8090']                      | 'http://localhost:8090'                                || true

        // Origin mismatches
        ['https://api.test/knowledge/v1']              | 'https://other.test/knowledge/v1'                      || false
        ['https://api.test/knowledge/v1']              | 'http://api.test/knowledge/v1'                         || false
        ['https://api.test/knowledge/v1']              | 'https://api.test:8443/knowledge/v1'                   || false
        ['http://knowledge-service:8090']              | 'http://knowledge-service:9090/x'                       || false
        ['https://api.test/knowledge/v1']              | 'https://api.test.evil.com/knowledge/v1'               || false

        // Path scoping
        ['https://api.test/knowledge/v1']              | 'https://api.test/billing/v1/invoices'                 || false
        ['https://api.test/knowledge/v1']              | 'https://api.test/knowledge/v1-internal/x'             || false
        ['https://api.test/knowledge/v1']              | 'https://api.test'                                     || false

        // A host-only entry covers every path on that origin, by design
        ['https://api.test']                           | 'https://api.test/anything/at/all'                     || true

        // Multiple entries
        ['https://a.test', 'https://b.test']           | 'https://b.test/x'                                     || true
        ['https://a.test', 'https://b.test']           | 'https://c.test/x'                                     || false

        verb = permitted ? 'permitted' : 'refused'
    }

    def "an empty allow-list permits nothing"() {
        expect:
        InternalBaseUrlAllowList.of([]).isEmpty()
        !InternalBaseUrlAllowList.of([]).permits(URI.create('https://api.test/x'))
        !InternalBaseUrlAllowList.empty().permits(URI.create('https://api.test/x'))
    }

    @Unroll
    def "a #description URI is never permitted"() {
        expect:
        !InternalBaseUrlAllowList.of(['https://api.test']).permits((URI) uri)

        where:
        description | uri
        'null'      | null
        'relative'  | URI.create('/knowledge/v1/thresholds')
        'opaque'    | URI.create('mailto:someone@example.com')
    }

    @Unroll
    def "a malformed allow-list entry (#entry) fails startup rather than silently disabling the relay"() {
        when:
        InternalBaseUrlAllowList.of([entry])

        then:
        IllegalArgumentException e = thrown()
        e.message.contains('ilr.internal-http.allowed-base-urls')

        where:
        entry << ['knowledge-service:8090', '/knowledge/v1', 'ftp://api.test', 'not a url at all']
    }

    def "blank and null entries are ignored rather than rejected"() {
        expect: "an unresolved optional placeholder must not break the context"
        InternalBaseUrlAllowList.of(['  ', null, 'https://api.test']).permits(URI.create('https://api.test/x'))
    }

    def "a malformed base URL string is refused, not thrown — a bad downstream URI must not crash startup"() {
        expect:
        !InternalBaseUrlAllowList.of(['https://api.test']).permits('http://[not a uri')
        !InternalBaseUrlAllowList.of(['https://api.test']).permits('')
        !InternalBaseUrlAllowList.of(['https://api.test']).permits((String) null)
    }

    def "toString renders origins for logging and never anything credential-shaped"() {
        expect:
        InternalBaseUrlAllowList.of(['https://api.test/knowledge/v1']).toString() ==
                '[https://api.test:443/knowledge/v1]'
    }

    def "a null collection is treated as empty"() {
        expect:
        InternalBaseUrlAllowList.of(null).isEmpty()
    }
}
