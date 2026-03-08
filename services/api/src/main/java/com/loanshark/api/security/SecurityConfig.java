package com.loanshark.api.security;

import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RateLimitFilter rateLimitFilter,
        UserDetailsServiceImpl userDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
    }

    /** Public auth paths: matched by path so this chain runs first and permits without JWT. */
    private static final RequestMatcher PUBLIC_AUTH_MATCHER = request -> {
        String path = normalizePath(request);
        if (path == null) return false;
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return isPublicAuthPath(path, "/auth/login", "/auth/forgot-password", "/auth/reset-password",
                "/auth/register/owner", "/auth/register/borrower", "/auth/setup-status");
        }
        if ("POST".equalsIgnoreCase(method)) {
            return isPublicAuthPath(path, "/auth/login", "/auth/forgot-password", "/auth/reset-password",
                "/auth/register/owner", "/auth/register/borrower");
        }
        return "GET".equalsIgnoreCase(method) && "/auth/setup-status".equals(path);
    };

    /** Chain 1: public auth only — no JWT, no rate limit; permits and continues to controller. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain publicAuthFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(PUBLIC_AUTH_MATCHER)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Explicit public auth paths (checked first; works with default path resolution on Railway).
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/forgot-password", "/auth/reset-password",
                    "/auth/register/owner", "/auth/register/borrower").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/setup-status").permitAll()
                // Public auth fallback: match by normalized path for proxy/context path (e.g. Railway).
                .requestMatchers(request -> {
                    jakarta.servlet.http.HttpServletRequest req = request;
                    String path = normalizePath(req);
                    if (path == null) return false;
                    String method = req.getMethod();
                    if ("POST".equalsIgnoreCase(method)) {
                        return isPublicAuthPath(path, "/auth/login", "/auth/forgot-password", "/auth/reset-password",
                            "/auth/register/owner", "/auth/register/borrower");
                    }
                    return "GET".equalsIgnoreCase(method) && isPublicAuthPath(path, "/auth/setup-status");
                }).permitAll()
                .requestMatchers(request -> {
                    if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
                    String path = normalizePath(request);
                    return path != null && "/settings/loan-interest".equals(path);
                }).permitAll()
                // Authenticated / owner-only auth endpoints (checked before /auth/** so JWT is enforced)
                .requestMatchers(HttpMethod.GET, "/auth/business-capital").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/business-capital/top-up").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST, "/auth/change-password").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/reset-user-password").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST, "/auth/register/staff").hasRole("OWNER")
                // Public auth (login, register, forgot-password, reset-password, setup-status)
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/settings/**").authenticated()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Path as seen by the app: servlet path, or request URI with context path stripped (for Railway/proxy). */
    private static String normalizePath(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getServletPath();
        if (path != null && !path.isEmpty()) {
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
            return path;
        }
        path = request.getRequestURI();
        if (path == null) return null;
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.length() == ctx.length() ? "/" : path.substring(ctx.length());
        }
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path.isEmpty() ? "/" : path;
    }

    /** True if path equals or ends with any of the given public paths (handles context path / proxy prefix). */
    private static boolean isPublicAuthPath(String path, String... allowed) {
        for (String a : allowed) {
            if (path.equals(a) || path.endsWith("/" + a.substring(1))) return true;
        }
        return false;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
