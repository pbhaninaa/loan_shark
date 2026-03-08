package com.loanshark.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequireEmailFilter requireEmailFilter;
    private final RateLimitFilter rateLimitFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RequireEmailFilter requireEmailFilter,
        RateLimitFilter rateLimitFilter,
        UserDetailsServiceImpl userDetailsService,
        ObjectMapper objectMapper
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requireEmailFilter = requireEmailFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    private AuthenticationEntryPoint json401EntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            try {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "message", authException != null ? authException.getMessage() : "Unauthorized"
                )));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // OPTIONS for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Health check
                .requestMatchers("/actuator/health", "/actuator/health/").permitAll()
                // Public auth endpoints (with and without trailing slash for proxy compatibility)
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/login/").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/forgot-password", "/auth/forgot-password/").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/reset-password", "/auth/reset-password/").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register/owner", "/auth/register/owner/").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register/borrower", "/auth/register/borrower/").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/setup-status", "/auth/setup-status/").permitAll()
                // Authenticated endpoints
                .requestMatchers(HttpMethod.GET, "/auth/me", "/auth/me/").authenticated()
                .requestMatchers(HttpMethod.PUT, "/auth/me/email", "/auth/me/email/").authenticated()
                .requestMatchers(HttpMethod.GET, "/auth/business-capital", "/auth/business-capital/").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/business-capital/top-up", "/auth/business-capital/top-up/").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST, "/auth/change-password", "/auth/change-password/").authenticated()
                .requestMatchers(HttpMethod.POST, "/auth/reset-user-password", "/auth/reset-user-password/").hasRole("OWNER")
                .requestMatchers(HttpMethod.POST, "/auth/register/staff", "/auth/register/staff/").hasRole("OWNER")
                .requestMatchers("/settings/loan-interest", "/settings/loan-interest/").permitAll()
                .requestMatchers("/settings/loan-interest/expected-amount", "/settings/loan-interest/expected-amount/").authenticated()
                .requestMatchers("/settings/**").authenticated()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(json401EntryPoint()))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requireEmailFilter, JwtAuthenticationFilter.class);

        return http.build();
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