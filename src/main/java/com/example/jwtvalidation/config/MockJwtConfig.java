package com.example.jwtvalidation.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.List;

/**
 * Mock JWT configuration for testing without real Keycloak.
 * This allows the application to run standalone for demonstration purposes.
 */
@Configuration
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "true", matchIfMissing = false)
public class MockJwtConfig {

    @Value("${app.security.mock-secret}")
    private String secretKey;

    /**
     * Creates a JwtDecoder that validates JWT tokens signed with the mock secret.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Helper method to generate mock JWT tokens for testing.
     * This can be called from a controller or test class.
     */
    public String generateMockToken(String userId, String email, String name, List<String> roles) {
        try {
            JWSSigner signer = new MACSigner(secretKey.getBytes());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("email", email)
                .claim("name", name)
                .claim("roles", roles)
                .issuer("mock-issuer")
                .audience("mock-audience")
                .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour
                .issueTime(new Date())
                .build();

            SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claimsSet
            );

            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate mock JWT token", e);
        }
    }
}
