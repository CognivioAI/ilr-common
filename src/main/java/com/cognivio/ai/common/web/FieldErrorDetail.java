package com.cognivio.ai.common.web;

/**
 * A single field-level (or cross-field) validation error, as carried in
 * {@link ErrorResponse#details()}. Same shape the services previously each
 * declared locally.
 */
public record FieldErrorDetail(String field, String message) {
}
