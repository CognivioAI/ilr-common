package com.cognivio.ai.common.security;

import com.cognivio.ai.common.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Renders a 401 as the shared {@link ErrorResponse} JSON when an unauthenticated
 * request hits a secured endpoint (missing/invalid/expired bearer token). Without
 * this, the resource-server filter chain would emit an empty body with a
 * {@code WWW-Authenticate} header, which does not match the platform error shape.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                "Authentication required", List.of(), UUID.randomUUID().toString());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
