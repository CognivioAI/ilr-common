package com.cognivio.ai.common.web

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.core.MethodParameter
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

    def "AccessDeniedException maps to 403 FORBIDDEN"() {
        when:
        def response = handler.handleAccessDenied(new AccessDeniedException('nope'))

        then:
        response.statusCode == HttpStatus.FORBIDDEN
        response.body.error() == 'FORBIDDEN'
    }
}
