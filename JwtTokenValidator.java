package com.example.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenValidator {

    @Value("${jwt.secret:your-secret-key-here}")
    private String jwtSecret;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    private final RestTemplate restTemplate;

    public JwtTokenValidator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ==================== LOCAL VALIDATION ====================
    /**
     * Validates JWT token locally without calling Keycloak
     * Steps:
     * 1. Decode JWT
     * 2. Verify Signature
     * 3. Check Claims
     * 4. Validate Expiry
     */
    public boolean validateTokenLocally(String token) {
        try {
            // Step 1 & 2: Decode and verify signature using secret key
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Step 3: Check claims exist and are valid
            if (claims.getSubject() == null || claims.getSubject().isEmpty()) {
                System.out.println("❌ Subject claim is missing");
                return false;
            }

            // Step 4: Validate expiry
            Date expirationTime = claims.getExpiration();
            if (expirationTime.before(new Date())) {
                System.out.println("❌ Token has expired");
                return false;
            }

            System.out.println("✅ Token validated successfully (Local)");
            System.out.println("   Subject: " + claims.getSubject());
            System.out.println("   Issued At: " + claims.getIssuedAt());
            System.out.println("   Expires: " + expirationTime);
            
            return true;

        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token expired: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ Unsupported JWT: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            System.out.println("❌ Invalid JWT format: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            System.out.println("❌ Invalid signature: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            System.out.println("❌ JWT claims string is empty: " + e.getMessage());
            return false;
        }
    }

    // ==================== REMOTE VALIDATION ====================
    /**
     * Validates JWT token by calling Keycloak's token introspection endpoint
     * This is more secure as it checks real-time token revocation status
     */
    public boolean validateTokenRemotely(String token) {
        try {
            String introspectUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
            
            // Prepare introspection request
            Map<String, String> request = Map.of(
                    "token", token,
                    "client_id", "your-client-id",
                    "client_secret", "your-client-secret"
            );

            // Call Keycloak introspection endpoint
            IntrospectionResponse response = restTemplate.postForObject(
                    introspectUrl,
                    request,
                    IntrospectionResponse.class
            );

            if (response != null && response.isActive()) {
                System.out.println("✅ Token validated successfully (Remote)");
                System.out.println("   Username: " + response.getUsername());
                System.out.println("   Exp: " + response.getExp());
                return true;
            } else {
                System.out.println("❌ Token is not active or has been revoked");
                return false;
            }

        } catch (Exception e) {
            System.out.println("❌ Remote validation failed: " + e.getMessage());
            return false;
        }
    }

    // Get claims from token (without verification - for claims inspection)
    public Claims getUnverifiedClaims(String token) {
        return Jwts.parserBuilder()
                .build()
                .parseClaimsJwt(token)
                .getBody();
    }

    // DTO for Keycloak introspection response
    public static class IntrospectionResponse {
        private boolean active;
        private String username;
        private String sub;
        private Long exp;
        private Long iat;
        private String client_id;

        // Getters
        public boolean isActive() { return active; }
        public String getUsername() { return username; }
        public String getSub() { return sub; }
        public Long getExp() { return exp; }
        public Long getIat() { return iat; }
        public String getClient_id() { return client_id; }

        // Setters
        public void setActive(boolean active) { this.active = active; }
        public void setUsername(String username) { this.username = username; }
        public void setSub(String sub) { this.sub = sub; }
        public void setExp(Long exp) { this.exp = exp; }
        public void setIat(Long iat) { this.iat = iat; }
        public void setClient_id(String client_id) { this.client_id = client_id; }
    }
}
