package com.example.jwtvalidation.controller;

import com.example.jwtvalidation.model.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * Get current user profile.
     * Accessible by any authenticated user.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        Map<String, Object> claims = extractClaims(authentication);

        String userId = (String) claims.getOrDefault("sub", "unknown");
        String email = (String) claims.getOrDefault("email", "N/A");
        String name = (String) claims.getOrDefault("name", "N/A");

        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return ResponseEntity.ok(new UserProfile(userId, email, name, roles));
    }

    /**
     * Get JWT token details.
     * Shows all claims in the token.
     */
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(Authentication authentication) {
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("principal", authentication.getPrincipal().toString());
        tokenInfo.put("authorities", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
        tokenInfo.put("claims", extractClaims(authentication));
        tokenInfo.put("authenticated", authentication.isAuthenticated());

        return ResponseEntity.ok(tokenInfo);
    }

    /**
     * Test endpoint for regular users.
     */
    @GetMapping("/hello")
    public ResponseEntity<String> hello(Authentication authentication) {
        String name = extractClaims(authentication).getOrDefault("name", "User").toString();
        return ResponseEntity.ok("Hello, " + name + "! You have USER access.");
    }

    /**
     * Helper method to extract claims from different authentication types.
     */
    private Map<String, Object> extractClaims(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaims();
        } else if (authentication.getPrincipal() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = (Map<String, Object>) authentication.getPrincipal();
            return claims;
        }
        return new HashMap<>();
    }
}
