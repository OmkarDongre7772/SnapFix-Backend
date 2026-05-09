package com.snapfix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.snapfix.auth.security.JwtAuthFilter;
import com.snapfix.auth.security.CustomAuthenticationEntryPoint;
import com.snapfix.auth.security.CustomAccessDeniedHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                               JwtAuthFilter jwtAuthFilter,
                                               CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                                               CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {

    http
    .csrf(csrf -> csrf.disable())
    .exceptionHandling(ex -> ex
        .authenticationEntryPoint(customAuthenticationEntryPoint)
        .accessDeniedHandler(customAccessDeniedHandler)
    )
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 👈 ADD THIS
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/auth/**").permitAll()
        // .requestMatchers("/user/me").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
