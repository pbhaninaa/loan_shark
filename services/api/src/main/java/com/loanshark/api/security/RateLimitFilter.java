package com.loanshark.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.max-attempts-per-minute:20}")
    private int maxAttemptsPerMinute;

    @Value("${app.rate-limit.window-seconds:60}")
    private long windowSeconds;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Allow preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Only rate limit sensitive endpoints
        if (!path.startsWith("/auth") && !path.startsWith("/loans/apply")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        String key = ip + ":" + path;

        long now = Instant.now().getEpochSecond();

        counters.compute(key, (k, counter) -> {

            if (counter == null) {
                return new WindowCounter(now, 1);
            }

            if (now - counter.windowStart > windowSeconds) {
                return new WindowCounter(now, 1);
            }

            counter.count++;
            return counter;
        });

        WindowCounter counter = counters.get(key);

        if (counter.count > maxAttemptsPerMinute) {
            response.setStatus(429);
            response.getWriter().write("Too many requests. Try again later.");
            return;
        }

        // Cleanup old entries to prevent memory leak
        counters.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > windowSeconds * 5
        );

        filterChain.doFilter(request, response);
    }

    private static class WindowCounter {

        long windowStart;
        int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}