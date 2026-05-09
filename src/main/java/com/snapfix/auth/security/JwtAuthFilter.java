package com.snapfix.auth.security;

import com.snapfix.common.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token → pass through, let Spring Security handle it
        // (AuthenticationEntryPoint will fire for protected routes)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        
        // check blacklist first
        if(tokenBlacklistService.isBlacklisted(token)){
            writeUnauthorized(response, "Token has been revoked");
            return;
        }
        
        String email;

        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // Token is malformed/invalid → write 401 directly
            writeUnauthorized(response, "JWT token is invalid or malformed");
            return; // ← do NOT call filterChain, stop here
        }

        // Token parsed but already expired or fails validation
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "JWT token is expired or invalid");
            return; // ← stop here, do NOT continue filter chain
        }

        // Valid token → set authentication in SecurityContext
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(String.format(
            "{\"error\":\"Unauthorized\",\"message\":\"%s\",\"status\":401,\"timestamp\":%d}",
            message, System.currentTimeMillis()
        ));
    }
}