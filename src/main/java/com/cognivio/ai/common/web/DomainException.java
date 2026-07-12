package com.cognivio.ai.common.web;

import org.springframework.http.HttpStatus;

/**
 * Base type for ILR domain exceptions (a stable {@code code} + the
 * {@link HttpStatus} to render). Same shape the services previously each
 * declared in their own {@code exception/DomainException}. Each service extends
 * this for its own domain errors (e.g. {@code EligibilityAssessmentNotFound}),
 * and {@link CommonExceptionHandler} renders any {@code DomainException} to the
 * shared {@link ErrorResponse} at the declared status.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    protected DomainException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
