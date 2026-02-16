package com.velocity.carservice.infrastructure.filter;

import com.velocity.carservice.shared.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to handle correlation ID for distributed tracing and log correlation.
 * Extracts correlation ID from incoming request headers or generates a new one.
 * Adds the correlation ID to MDC for logging and to response headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = extractOrGenerateId(request, AppConstants.HEADER_CORRELATION_ID);
            String requestId = extractOrGenerateId(request, AppConstants.HEADER_REQUEST_ID);

            MDC.put(MDC_CORRELATION_ID, correlationId);
            MDC.put(MDC_REQUEST_ID, requestId);

            response.setHeader(AppConstants.HEADER_CORRELATION_ID, correlationId);
            response.setHeader(AppConstants.HEADER_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String extractOrGenerateId(HttpServletRequest request, String headerName) {
        String id = request.getHeader(headerName);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}

