package com.cognivio.ai.common.web;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
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

    /** Upper bound on how far {@link #causeChain(Throwable)} walks, guarding against cyclic causes. */
    private static final int MAX_CAUSE_DEPTH = 10;

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

    /**
     * 409 safety net for a database UNIQUE / integrity constraint violation that
     * reaches the boundary. Services normally catch and resolve these idempotently
     * in the service layer (returning the existing row); this ensures any that slip
     * through return a 409 in the standard shape rather than a raw 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        // KAN-201: never log ex.getMessage() here — PostgreSQL puts the offending values in the
        // message ("Detail: Key (tenant_id, idempotency_key)=(<uuid>, <value>) already exists").
        log.warn("Data integrity violation reached the boundary: type={} constraint={} sqlState={}",
                ex.getClass().getSimpleName(), constraintName(ex).orElse("unknown"),
                sqlState(ex).orElse("unknown"));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "CONSTRAINT_CONFLICT",
                        "The request conflicts with existing data — retry", List.of(), traceId()));
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
        // KAN-201: Jackson embeds the offending JSON fragment in its message — log the
        // exception type and the target field path only, never the value that failed.
        log.warn("Malformed request body: type={} cause={} field={}",
                ex.getClass().getSimpleName(), causeType(ex), field);
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

    /** Simple name of the immediate cause, for a log line that omits the raw message (KAN-201). */
    private static String causeType(Throwable ex) {
        return ex.getCause() == null ? "none" : ex.getCause().getClass().getSimpleName();
    }

    /**
     * Best-effort constraint name for a data-integrity failure, taken from the first cause in the
     * chain exposing a {@code getConstraintName()} accessor (Hibernate's
     * {@code org.hibernate.exception.ConstraintViolationException}).
     *
     * <p>Resolved reflectively on purpose: {@code ilr-common} is consumed by services that do not
     * put Hibernate on the classpath, so this class must not carry a compile-time reference to it.
     * A constraint name is a schema identifier and safe to log; the exception message is not.
     */
    private static Optional<String> constraintName(Throwable ex) {
        for (Throwable cause : causeChain(ex)) {
            try {
                Object name = cause.getClass().getMethod("getConstraintName").invoke(cause);
                if (name instanceof String s && !s.isBlank()) {
                    return Optional.of(s);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // This cause is not a Hibernate ConstraintViolationException — keep walking.
            }
        }
        return Optional.empty();
    }

    /** Best-effort SQLState (e.g. {@code 23505} unique violation) from the underlying JDBC failure. */
    private static Optional<String> sqlState(Throwable ex) {
        for (Throwable cause : causeChain(ex)) {
            if (cause instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return Optional.of(sqlException.getSQLState());
            }
        }
        return Optional.empty();
    }

    /** The exception and its causes, depth-bounded so a self- or cyclic cause cannot spin. */
    private static List<Throwable> causeChain(Throwable ex) {
        List<Throwable> chain = new ArrayList<>();
        Throwable current = ex;
        while (current != null && chain.size() < MAX_CAUSE_DEPTH && !chain.contains(current)) {
            chain.add(current);
            current = current.getCause();
        }
        return chain;
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }
}
