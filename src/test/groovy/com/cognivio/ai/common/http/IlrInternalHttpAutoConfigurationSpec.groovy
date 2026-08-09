package com.cognivio.ai.common.http

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Specification

/**
 * KAN-236 wiring: the relay must be available to every service without any service-side wiring, and
 * must default to relaying to nothing until a service explicitly names its internal destinations.
 */
class IlrInternalHttpAutoConfigurationSpec extends Specification {

    ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration, IlrInternalHttpAutoConfiguration))

    def "the factory is auto-configured with no service-side wiring"() {
        expect:
        runner.run { context ->
            assert context.getBean(InternalServiceClientFactory) != null
            assert context.getBean(InternalBaseUrlAllowList) != null
        }
    }

    def "with no configuration the allow-list is empty — nothing is relayed until a service opts in"() {
        expect:
        runner.run { context ->
            assert context.getBean(InternalBaseUrlAllowList).isEmpty()
        }
    }

    def "configured destinations are allow-listed"() {
        expect:
        runner.withPropertyValues(
                'ilr.internal-http.allowed-base-urls[0]=https://api.test/knowledge/v1',
                'ilr.internal-http.allowed-base-urls[1]=https://api.test/eligibility/v1')
                .run { context ->
                    InternalBaseUrlAllowList allowList = context.getBean(InternalBaseUrlAllowList)
                    assert allowList.permits(URI.create('https://api.test/knowledge/v1/thresholds/BR-006'))
                    assert allowList.permits(URI.create('https://api.test/eligibility/v1/assessments'))
                    assert !allowList.permits(URI.create('https://bedrock-runtime.eu-west-2.amazonaws.com/x'))
                }
    }

    def "relay-enabled=false switches the relay off entirely, even with destinations configured"() {
        expect:
        runner.withPropertyValues(
                'ilr.internal-http.relay-enabled=false',
                'ilr.internal-http.allowed-base-urls[0]=https://api.test/knowledge/v1')
                .run { context ->
                    assert context.getBean(InternalBaseUrlAllowList).isEmpty()
                }
    }

    def "a malformed destination fails the context rather than silently disabling authentication"() {
        expect:
        runner.withPropertyValues('ilr.internal-http.allowed-base-urls[0]=knowledge-service:8090')
                .run { context ->
                    assert context.startupFailure != null
                    assert rootMessage(context.startupFailure).contains('ilr.internal-http.allowed-base-urls')
                }
    }

    private static String rootMessage(Throwable thrown) {
        Throwable cause = thrown
        while (cause.cause != null) {
            cause = cause.cause
        }
        return cause.message
    }
}
