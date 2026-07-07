package com.ofni.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var path = httpReq.getRequestURI();

        if (path.equals("/api/auth/login") && "POST".equalsIgnoreCase(httpReq.getMethod())) {
            var ip = httpReq.getRemoteAddr();
            var record = attempts.compute(ip, (k, v) -> {
                if (v == null || System.currentTimeMillis() > v.windowEnd()) {
                    return new Attempts(1, System.currentTimeMillis() + WINDOW_MS);
                }
                return new Attempts(v.count() + 1, v.windowEnd());
            });

            if (record.count() > MAX_ATTEMPTS) {
                log.warn("Rate limit exceeded for IP: {}", ip);
                ((HttpServletResponse) response).setStatus(429);
                response.getWriter().write("{\"error\":\"Demasiados intentos. Espera un minuto.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private record Attempts(int count, long windowEnd) {}
}
