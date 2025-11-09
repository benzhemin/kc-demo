package com.example.jwtvalidation.controller;

import com.example.jwtvalidation.config.MockJwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for generating mock JWT tokens for testing.
 * Only available when mock mode is enabled.
 */
@RestController
@RequestMapping("/public/mock")
@ConditionalOnProperty(name = "app.security.mock-enabled", havingValue = "true")
public class MockTokenController {

    @Autowired
    private MockJwtConfig mockJwtConfig;

    /**
     * Generate a mock JWT token for testing.
     * This endpoint is only for development/testing purposes.
     */
    @PostMapping("/generate-token")
    public ResponseEntity<Map<String, String>> generateToken(@RequestBody(required = false) TokenRequest request) {
        if (request == null) {
            request = new TokenRequest();
        }

        String userId = request.userId != null ? request.userId : "test-user-123";
        String email = request.email != null ? request.email : "test@example.com";
        String name = request.name != null ? request.name : "Test User";
        List<String> roles = request.roles != null && !request.roles.isEmpty()
            ? request.roles
            : List.of("USER");

        String token = mockJwtConfig.generateMockToken(userId, email, name, roles);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("usage", "Add to Authorization header as: Bearer " + token);

        return ResponseEntity.ok(response);
    }

    /**
     * Generate a user token (with USER role).
     */
    @GetMapping("/user-token")
    public ResponseEntity<Map<String, String>> generateUserToken() {
        String token = mockJwtConfig.generateMockToken(
            "user-001",
            "user@example.com",
            "Regular User",
            List.of("USER")
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("role", "USER");

        return ResponseEntity.ok(response);
    }

    /**
     * Generate an admin token (with ADMIN role).
     */
    @GetMapping("/admin-token")
    public ResponseEntity<Map<String, String>> generateAdminToken() {
        String token = mockJwtConfig.generateMockToken(
            "admin-001",
            "admin@example.com",
            "Admin User",
            List.of("ADMIN", "USER")
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("role", "ADMIN");

        return ResponseEntity.ok(response);
    }

    // Inner class for request body
    private static class TokenRequest {
        public String userId;
        public String email;
        public String name;
        public List<String> roles;
    }
}
