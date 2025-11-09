package com.example.security;

import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Filter that intercepts HTTP requests and validates JWT tokens
 * from the Authorization header
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator tokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // Step 1: Extract JWT token from Authorization header
            String token = extractToken(request);

            if (token != null) {
                // Step 2: Validate token locally
                if (tokenValidator.validateTokenLocally(token)) {
                    // Step 3: Extract claims and create authentication object
                    Claims claims = tokenValidator.getUnverifiedClaims(token);
                    
                    String username = claims.getSubject();
                    Collection<GrantedAuthority> authorities = extractAuthorities(claims);

                    // Step 4: Set authentication in SecurityContext
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("✅ User authenticated: " + username);
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Authentication failed: " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header
     * Format: "Bearer <token>"
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer " prefix
        }
        
        return null;
    }

    /**
     * Extracts user authorities/roles from JWT claims
     * Assumes roles are stored in a "roles" claim as a list
     */
    private Collection<GrantedAuthority> extractAuthorities(Claims claims) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");

        if (roles != null) {
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
            }
        }

        // Also check for standard 'scope' claim
        String scope = claims.get("scope", String.class);
        if (scope != null) {
            for (String authority : scope.split(" ")) {
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }

        return authorities;
    }
}
