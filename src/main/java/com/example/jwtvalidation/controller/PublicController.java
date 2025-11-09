package com.example.jwtvalidation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Value("${app.security.validation-mode:LOCAL}")
    private String validationMode;

    /**
     * Public endpoint - no authentication required.
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello! This is a public endpoint.");
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("validationMode", validationMode);

        return ResponseEntity.ok(health);
    }

    /**
     * API information endpoint.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        Map<String, String> info = new HashMap<>();
        info.put("application", "JWT Validation Demo");
        info.put("version", "1.0.0");
        info.put("validationMode", validationMode);
        info.put("description", "Demo application showing Local, Remote, and Hybrid JWT validation");

        return ResponseEntity.ok(info);
    }
}
