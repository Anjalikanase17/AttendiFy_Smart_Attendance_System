package com.example.attendance_Backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Set<String> STATIC_PREFIXES = Set.of("/css/", "/js/", "/assets/", "/public/", "/uploads/",
            "/index.html");

    @Value("${app.request-log.slow-threshold-ms:800}")
    private long slowThresholdMs;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        for (String prefix : STATIC_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug("Incoming Request: {} {}", request.getMethod(), request.getRequestURI());
        }

        filterChain.doFilter(request, response);

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        if (elapsedMs >= slowThresholdMs) {
            log.warn("Slow request: {} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    String.format("%.2f", elapsedMs));
        } else if (log.isDebugEnabled()) {
            log.debug("Response Status: {} for {} ({} ms)", response.getStatus(), request.getRequestURI(),
                    String.format("%.2f", elapsedMs));
        }
    }
}
