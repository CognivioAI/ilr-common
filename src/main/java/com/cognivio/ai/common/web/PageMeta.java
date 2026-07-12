package com.cognivio.ai.common.web;

/**
 * Pagination metadata for list responses. Same shape the services previously
 * each declared locally in {@code dto/response/PageMeta}.
 */
public record PageMeta(int number, int size, long totalElements, int totalPages) {
}
