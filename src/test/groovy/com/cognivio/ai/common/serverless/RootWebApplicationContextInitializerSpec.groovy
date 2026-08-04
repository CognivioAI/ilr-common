package com.cognivio.ai.common.serverless

import jakarta.servlet.ServletContext
import org.springframework.context.ApplicationContext
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

/**
 * KAN-226: the AWS serverless servlet adapter never runs Boot's {@code selfInitialize}, so the root
 * WebApplicationContext is missing from the ServletContext and {@code /actuator/health} 500s on
 * Lambda. This initializer fills that gap without disturbing a real container, where Boot has
 * already published the context before the initializer runs.
 */
class RootWebApplicationContextInitializerSpec extends Specification {

    static final String ATTRIBUTE = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE

    def "publishes the web application context when the ServletContext attribute is absent"() {
        given:
        def context = Mock(WebApplicationContext)
        def servletContext = Mock(ServletContext) { getAttribute(ATTRIBUTE) >> null }

        when:
        new RootWebApplicationContextInitializer(context).onStartup(servletContext)

        then:
        1 * servletContext.setAttribute(ATTRIBUTE, context)
    }

    def "leaves an already-published root context untouched"() {
        given:
        def alreadyPublished = Mock(WebApplicationContext)
        def context = Mock(WebApplicationContext)
        def servletContext = Mock(ServletContext) { getAttribute(ATTRIBUTE) >> alreadyPublished }

        when:
        new RootWebApplicationContextInitializer(context).onStartup(servletContext)

        then:
        0 * servletContext.setAttribute(_, _)
    }

    def "does nothing when the application context is not a WebApplicationContext"() {
        given:
        def servletContext = Mock(ServletContext)

        when:
        new RootWebApplicationContextInitializer(Mock(ApplicationContext)).onStartup(servletContext)

        then:
        0 * servletContext.setAttribute(_, _)
    }
}
