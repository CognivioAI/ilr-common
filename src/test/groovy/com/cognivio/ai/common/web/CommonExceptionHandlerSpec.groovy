package com.cognivio.ai.common.web

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.slf4j.LoggerFactory

import java.sql.SQLException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.core.MethodParameter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import spock.lang.Specification

class CommonExceptionHandlerSpec extends Specification {

    CommonExceptionHandler handler = new CommonExceptionHandler()

    ch.qos.logback.classic.Logger handlerLogger =
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger(CommonExceptionHandler)
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>()

    def setup() {
        logAppender.start()
        handlerLogger.addAppender(logAppender)
    }

    def cleanup() {
        handlerLogger.detachAppender(logAppender)
        logAppender.stop()
    }

    /** The single formatted line the handler emitted during the `when:` block. */
    private String loggedLine() {
        assert logAppender.list.size() == 1
        logAppender.list[0].formattedMessage
    }

    static class TestDomainException extends DomainException {
        TestDomainException() {
            super('WIDGET_NOT_FOUND', 'Widget 42 not found', HttpStatus.NOT_FOUND)
        }
    }

    def "DomainException renders at its declared status with its code"() {
        when:
        def response = handler.handleDomain(new TestDomainException())

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        with(response.body) {
            status() == 404
            error() == 'WIDGET_NOT_FOUND'
            message() == 'Widget 42 not found'
            details().isEmpty()
            traceId() != null
            timestamp() != null
        }
    }

    def "MethodArgumentNotValidException maps field AND global errors to 400 VALIDATION_ERROR"() {
        given:
        def binding = new BeanPropertyBindingResult(new Object(), 'request')
        binding.addError(new FieldError('request', 'email', 'must not be blank'))
        binding.addError(new ObjectError('request', 'cross-field rule failed'))
        def methodParameter = new MethodParameter(Object.getDeclaredMethod('toString'), -1)
        def ex = new MethodArgumentNotValidException(methodParameter, binding)

        when:
        def response = handler.handleValidation(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error() == 'VALIDATION_ERROR'
        response.body.details()*.field() as Set == ['email', 'request'] as Set
    }

    def "ConstraintViolationException maps to 400 VALIDATION_ERROR"() {
        given:
        Path path = Mock()
        path.toString() >> 'size'
        ConstraintViolation cv = Mock()
        cv.getPropertyPath() >> path
        cv.getMessage() >> 'must be positive'

        when:
        def response = handler.handleConstraintViolation(new ConstraintViolationException([cv] as Set))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.details()[0].field() == 'size'
        response.body.details()[0].message() == 'must be positive'
    }

    def "OptimisticLockingFailureException maps to 409 CONCURRENT_UPDATE_CONFLICT"() {
        when:
        def response = handler.handleOptimisticLock(new OptimisticLockingFailureException('stale version'))

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.error() == 'CONCURRENT_UPDATE_CONFLICT'
    }

    def "DataIntegrityViolationException maps to 409 CONSTRAINT_CONFLICT"() {
        when:
        def response = handler.handleDataIntegrity(new DataIntegrityViolationException('duplicate key'))

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.error() == 'CONSTRAINT_CONFLICT'
    }

    def "MissingServletRequestParameterException maps to 400 with the parameter name"() {
        when:
        def response = handler.handleMissingParam(new MissingServletRequestParameterException('page', 'int'))

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error() == 'VALIDATION_ERROR'
        response.body.details()[0].field() == 'page'
    }

    def "MethodArgumentTypeMismatchException maps to 400 with the argument name"() {
        given:
        MethodArgumentTypeMismatchException ex = Mock()
        ex.getName() >> 'assessmentId'

        when:
        def response = handler.handleTypeMismatch(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.details()[0].field() == 'assessmentId'
    }

    def "HttpMessageNotReadableException maps to 400"() {
        given:
        HttpMessageNotReadableException ex = Mock()
        ex.getCause() >> null
        ex.getMessage() >> 'JSON parse error'

        when:
        def response = handler.handleNotReadable(ex)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error() == 'VALIDATION_ERROR'
        response.body.details()[0].field() == 'body'
    }

    def "AuthenticationException maps to 401 UNAUTHORIZED"() {
        when:
        def response = handler.handleAuthentication(new BadCredentialsException('nope'))

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
        response.body.error() == 'UNAUTHORIZED'
    }

    // --- KAN-201: log output must not carry constraint details or request payload fragments ---

    /**
     * Stand-in for {@code org.hibernate.exception.ConstraintViolationException}: the handler
     * discovers {@code getConstraintName()} reflectively (Hibernate is not a compile dependency
     * of ilr-common), so any cause exposing that accessor exercises the same path.
     */
    static class FakeHibernateConstraintViolationException extends RuntimeException {
        final String constraintName

        FakeHibernateConstraintViolationException(String message, String constraintName, Throwable cause) {
            super(message, cause)
            this.constraintName = constraintName
        }
    }

    def "DataIntegrityViolationException logs the constraint name and never the offending values"() {
        given: "a PostgreSQL unique violation whose message embeds the actual key values"
        def pgDetail = 'ERROR: duplicate key value violates unique constraint "uk_case_tenant_idem"\n' +
                '  Detail: Key (tenant_id, idempotency_key)=' +
                '(7b0f1e4c-0000-4000-8000-000000000001, ada@firm.example) already exists.'
        def sqlException = new SQLException(pgDetail, '23505')
        def hibernateException = new FakeHibernateConstraintViolationException(
                pgDetail, 'uk_case_tenant_idem', sqlException)
        def ex = new DataIntegrityViolationException(pgDetail, hibernateException)

        when:
        handler.handleDataIntegrity(ex)

        then: "the safe identifiers are logged"
        def line = loggedLine()
        line.contains('DataIntegrityViolationException')
        line.contains('uk_case_tenant_idem')
        line.contains('23505')

        and: "the offending values and the raw driver message are not"
        !line.contains('ada@firm.example')
        !line.contains('7b0f1e4c-0000-4000-8000-000000000001')
        !line.contains('Detail: Key')
        !line.contains('duplicate key value')
    }

    def "DataIntegrityViolationException without a recognisable cause still omits the raw message"() {
        given:
        def ex = new DataIntegrityViolationException('Key (email)=(ada@firm.example) already exists')

        when:
        handler.handleDataIntegrity(ex)

        then:
        def line = loggedLine()
        line.contains('DataIntegrityViolationException')
        line.contains('constraint=unknown')
        line.contains('sqlState=unknown')
        !line.contains('ada@firm.example')
    }

    def "HttpMessageNotReadableException log omits the offending JSON fragment"() {
        given: "a Jackson parse failure whose message embeds the submitted value"
        HttpMessageNotReadableException ex = Mock()
        ex.getCause() >> new IllegalArgumentException('nested')
        ex.getMessage() >> 'JSON parse error: Cannot deserialize value from String ' +
                '"ada@firm.example" at [Source: {"applicantEmail":"ada@firm.example"}; line: 1]'

        when:
        def response = handler.handleNotReadable(ex)

        then: "the type and the field path are logged, the payload fragment is not"
        def line = loggedLine()
        line.contains('field=body')
        line.contains('IllegalArgumentException')
        !line.contains('ada@firm.example')
        !line.contains('applicantEmail')

        and: "the API response shape is unchanged"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error() == 'VALIDATION_ERROR'
    }

    def "AccessDeniedException maps to 403 FORBIDDEN"() {
        when:
        def response = handler.handleAccessDenied(new AccessDeniedException('nope'))

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.error() == 'FORBIDDEN'
    }
}
