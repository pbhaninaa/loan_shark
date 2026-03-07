package com.loanshark.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS_PER_MINUTE = 20;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/auth") && !path.startsWith("/loans/apply")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + path;
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(Instant.now().getEpochSecond(), 0));
        long currentMinute = Instant.now().getEpochSecond() / 60;
        synchronized (counter) {
            if (counter.minute != currentMinute) {
                counter.minute = currentMinute;
                counter.count = 0;
            }
            counter.count++;
            if (counter.count > MAX_ATTEMPTS_PER_MINUTE) {
                response.sendError(429, "Too many requests");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static final class WindowCounter {
        private long minute;
        private int count;

        private WindowCounter(long epochSeconds, int count) {
            this.minute = epochSeconds / 60;
            this.count = count;
        }
    }
}
