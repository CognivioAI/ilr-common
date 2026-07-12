package com.cognivio.ai.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response body shared by every ILR service. Byte-for-byte the
 * same shape (and {@code of(...)} factory) the services previously each declared
 * in their own {@code dto/response/ErrorResponse}, so moving to this type is a
 * drop-in replacement that keeps the OpenAPI {@code ErrorResponse} contract.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorDetail> details,
        String traceId
) {
    public static ErrorResponse of(int status, String error, String message, List<FieldErrorDetail> details,
                                   String traceId) {
        return new ErrorResponse(Instant.now(), status, error, message, details, traceId);
    }
}
