package com.cognivio.ai.common.serverless

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.support.SpringFactoriesLoader
import spock.lang.Specification

/**
 * KAN-226: under Lambda the working directory is the read-only {@code /var/task} deployment mount,
 * which always reports 0 free bytes, so {@code DiskSpaceHealthIndicator} pins the aggregate
 * {@code /actuator/health} to DOWN (HTTP 503) even when every individual group is UP. This post
 * processor turns that indicator off — but only for the {@code lambda} profile, because on the
 * container deployment target free disk space is a real signal worth monitoring.
 */
class LambdaProfileDefaultsEnvironmentPostProcessorSpec extends Specification {

    static final String PROPERTY = LambdaProfileDefaultsEnvironmentPostProcessor.DISK_SPACE_HEALTH_ENABLED

    def postProcessor = new LambdaProfileDefaultsEnvironmentPostProcessor()

    def "disables the disk space health indicator when the lambda profile is active"() {
        given:
        def environment = environmentWithProfiles(profiles as String[])

        when:
        postProcessor.postProcessEnvironment(environment, Mock(SpringApplication))

        then:
        environment.getProperty(PROPERTY) == 'false'

        where:
        profiles << [['lambda'], ['lambda', 'production'], ['production', 'lambda']]
    }

    def "leaves the disk space health indicator alone without the lambda profile"() {
        given:
        def environment = environmentWithProfiles(profiles as String[])

        when:
        postProcessor.postProcessEnvironment(environment, Mock(SpringApplication))

        then:
        environment.getProperty(PROPERTY) == null
        !environment.propertySources.contains(LambdaProfileDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)

        where:
        profiles << [[] as List<String>, ['production'], ['local', 'production'], ['lambda-ish']]
    }

    def "does not override a disk space setting the service made itself"() {
        given:
        def environment = environmentWithProfiles('lambda')
        environment.propertySources.addFirst(new MapPropertySource('service', [(PROPERTY): 'true']))

        when:
        postProcessor.postProcessEnvironment(environment, Mock(SpringApplication))

        then:
        environment.getProperty(PROPERTY) == 'true'
    }

    def "is idempotent when run more than once against the same environment"() {
        given:
        def environment = environmentWithProfiles('lambda')

        when:
        2.times { postProcessor.postProcessEnvironment(environment, Mock(SpringApplication)) }

        then:
        environment.propertySources.count {
            it.name == LambdaProfileDefaultsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME
        } == 1
    }

    def "is registered so Boot picks it up in a real service"() {
        expect:
        SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor, getClass().classLoader)
                .contains(LambdaProfileDefaultsEnvironmentPostProcessor.name)
    }

    def "the property it sets is the one that actually suppresses the indicator bean"() {
        given: 'the defaults this post processor contributes under the lambda profile'
        def environment = environmentWithProfiles('lambda')
        postProcessor.postProcessEnvironment(environment, Mock(SpringApplication))

        expect: 'Actuator drops the disk space indicator when they are applied'
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DiskSpaceHealthContributorAutoConfiguration))
                .withPropertyValues("${PROPERTY}=${environment.getProperty(PROPERTY)}")
                .run { context -> assert !context.containsBean('diskSpaceHealthIndicator') }

        and: 'and keeps it without them, as a container deployment must'
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DiskSpaceHealthContributorAutoConfiguration))
                .run { context -> assert context.containsBean('diskSpaceHealthIndicator') }
    }

    private static StandardEnvironment environmentWithProfiles(String... profiles) {
        def environment = new StandardEnvironment()
        environment.setActiveProfiles(profiles)
        environment
    }
}
