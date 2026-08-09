package com.cognivio.ai.common.http.violationfixture.compliant.client

import com.cognivio.ai.common.http.InternalServiceClientFactory
import groovy.transform.CompileStatic
import org.springframework.web.client.RestClient

/**
 * The shape every ILR service client must take after KAN-236: the {@code RestClient} comes from
 * ilr-common's factory, so the caller's verified bearer token is relayed to allow-listed internal
 * destinations and to nothing else.
 *
 * <p>Sits in a {@code ..client..} package on purpose. It is the negative control for
 * {@code InternalClientConstructionRules}: without it, a rule that flagged <em>everything</em> in a
 * client package would look identical to a correct one in the "rule fires" test. Excluded from the
 * published {@code tests}-classifier jar along with the rest of the fixture tree.
 */
@CompileStatic
class FactoryBuiltClientFixture {

    private final RestClient restClient

    FactoryBuiltClientFixture(InternalServiceClientFactory clientFactory, String knowledgeUri) {
        this.restClient = clientFactory.forBaseUrl(knowledgeUri)
    }

    RestClient client() {
        return restClient
    }
}
