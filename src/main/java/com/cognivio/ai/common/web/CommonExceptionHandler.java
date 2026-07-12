package com.cognivio.ai.common.web;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Shared {@link RestControllerAdvice} that maps common exceptions to the
 * standard {@link ErrorResponse} shape — the handler previously copy-pasted
 * (as {@code GlobalExceptionHandler}) into every service.
 *
 * <p>It is ordered at {@link Ordered#LOWEST_PRECEDENCE} deliberately: a
 * consuming service's own {@code @RestControllerAdvice} (default order 0) wins
 * for any exception type it also handles, so services can still add
 * service-specific handlers (or override these) without conflict. This advice
 * only ever fires for exception types the service has not itself claimed.
 *
 * <p>The {@code DomainException} handler covers every service-specific subclass
 * of {@link DomainException} (rendered at the subclass's declared status),
 * including the security package's {@code MissingTenantClaimException}.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        log.warn("Domain exception: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getHttpStatus().value(), ex.getCode(), ex.getMessage(), List.of(),
                        traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        // Class-level (cross-field) constraint violations surface as global errors — include them too.
        List<FieldErrorDetail> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldErrorDetail(ge.getObjectName(), ge.getDefaultMessage()))
                .toList();
        List<FieldErrorDetail> details = Stream.concat(fieldErrors.stream(), globalErrors.stream()).toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "Request validation failed", details, traceId()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "Request validation failed", details, traceId()));
    }

    /**
     * 409 for a concurrent-write conflict on an {@code @Version}-guarded entity
     * (read-modify-write). Without this, an optimistic-lock failure would fall
     * through to the default error page instead of the standard error shape.
     * Catches Spring's {@link OptimisticLockingFailureException}, which JPA's
     * {@code ObjectOptimisticLockingFailureException} extends.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "CONCURRENT_UPDATE_CONFLICT",
                        "This resource was updated by another request — retry with the latest version",
                        List.of(), traceId()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        FieldErrorDetail detail = new FieldErrorDetail(ex.getParameterName(), "required parameter is missing");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "Required request parameter is missing", List.of(detail), traceId()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        FieldErrorDetail detail = new FieldErrorDetail(ex.getName(), "must be a valid, correctly typed value");
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "Request parameter has the wrong type", List.of(detail), traceId()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String field = extractFieldName(ex).orElse("body");
        FieldErrorDetail detail = new FieldErrorDetail(field, "must be a valid, correctly formatted value");
        log.warn("Malformed request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                        "Request body could not be parsed", List.of(detail), traceId()));
    }

    /**
     * 401 for an authentication failure that reaches the MVC layer (e.g. thrown
     * from method security). The resource-server filter chain renders most auth
     * failures earlier via {@code RestAuthenticationEntryPoint}; this handler is
     * the MVC-layer counterpart producing the identical shape.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                        "Authentication required", List.of(), traceId()));
    }

    /** 403 for an access-denied failure that reaches the MVC layer (e.g. method security). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                        "Access denied", List.of(), traceId()));
    }

    private static Optional<String> extractFieldName(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife
                && !ife.getPath().isEmpty()) {
            return Optional.of(ife.getPath().get(ife.getPath().size() - 1).getFieldName());
        }
        return Optional.empty();
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
