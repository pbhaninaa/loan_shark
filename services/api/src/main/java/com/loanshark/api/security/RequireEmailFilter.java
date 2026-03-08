package com.loanshark.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanshark.api.entity.UserRole;
import com.loanshark.api.entity.Borrower;
import com.loanshark.api.entity.User;
import com.loanshark.api.repository.BorrowerRepository;
import com.loanshark.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks all authenticated requests except GET /auth/me, PUT /auth/me/email, and GET /settings/loan-interest/expected-amount
 * when the current user has no email in the database. Forces users to add their email first.
 */
@Component
public class RequireEmailFilter extends OncePerRequestFilter {

    private static final String GET_ME = "/auth/me";
    private static final String PUT_ME_EMAIL = "/auth/me/email";
    private static final String GET_EXPECTED_AMOUNT = "/settings/loan-interest/expected-amount";

    private final UserRepository userRepository;
    private final BorrowerRepository borrowerRepository;
    private final ObjectMapper objectMapper;

    public RequireEmailFilter(
        UserRepository userRepository,
        BorrowerRepository borrowerRepository,
        ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.borrowerRepository = borrowerRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }
        String path = normalizePath(request.getServletPath() != null ? request.getServletPath() : request.getRequestURI());
        if ("GET".equalsIgnoreCase(request.getMethod()) && GET_ME.equals(path)) {
            return true;
        }
        if ("PUT".equalsIgnoreCase(request.getMethod()) && PUT_ME_EMAIL.equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && GET_EXPECTED_AMOUNT.equals(path)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean hasEmail;
        if (user.getRole() == UserRole.BORROWER) {
            hasEmail = borrowerRepository.findByUserId(user.getId())
                .map(Borrower::getEmail)
                .map(e -> e != null && !e.isBlank())
                .orElse(false);
        } else {
            String e = user.getEmail();
            hasEmail = e != null && !e.isBlank();
        }

        if (!hasEmail) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                "code", "EMAIL_REQUIRED",
                "message", "You must add your email in My account before using the system."
            )));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String normalizePath(String path) {
        if (path == null) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }
}
