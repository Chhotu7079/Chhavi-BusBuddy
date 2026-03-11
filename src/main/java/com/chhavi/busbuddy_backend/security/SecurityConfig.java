package com.chhavi.busbuddy_backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.http.HttpStatus;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/verify-custom-token",
                                "/buses/**",
                                "/routes/**",
                                "/routes/*/live",
                                "/routes/search",
                                "/routes/search-live",
                                "/stops/**").permitAll()
                        // Allow only the email lookup variant of /companies to be public.
                        .requestMatchers(request -> HttpMethod.GET.matches(request.getMethod())
                                && "/companies".equals(request.getRequestURI())
                                && request.getParameter("email") != null
                                && !request.getParameter("email").isBlank()).permitAll()
                        .requestMatchers(HttpMethod.POST, "/companies/signup", "/auth/company/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/gps/ingest", "/gps/ingest/query", "/webhooks/gps").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/routes",
                                "/buses",
                                "/routes/*/timetable/minutes",
                                "/routes/*/stops/reached",
                                "/routes/*/history/fix-gaps",
                                "/routes/*/delays/recompute")
                            .hasAnyRole("ADMIN", "OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/buses/*/route").hasAnyRole("ADMIN", "OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/routes/*").hasAnyRole("ADMIN", "OWNER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/companies/*/employees").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.GET, "/companies/*/employees").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.GET, "/companies/*/audit-logs").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.POST, "/companies/*/gps-devices").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.GET, "/companies/*/gps-devices").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers(HttpMethod.POST, "/admin/migrations/company-id").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/migrations/search-keys").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/routes/delays/recompute").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new AccessDeniedHandlerImpl()));

        return http.build();
    }
}
