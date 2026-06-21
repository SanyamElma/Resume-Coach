package com.resumeanalyzer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.common.dto.ApiResponse;
import com.resumeanalyzer.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Returns a JSON 401 envelope for unauthenticated access instead of a redirect. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.of("UNAUTHORIZED",
                "Authentication required to access this resource", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(error));
    }
}
