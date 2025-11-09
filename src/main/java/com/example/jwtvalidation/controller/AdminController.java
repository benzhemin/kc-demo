package com.example.jwtvalidation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * Admin-only endpoint.
     * Requires ADMIN role.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("message", "Welcome to Admin Dashboard");
        dashboard.put("totalUsers", 1234);
        dashboard.put("activeUsers", 567);
        dashboard.put("systemStatus", "Healthy");

        return ResponseEntity.ok(dashboard);
    }

    /**
     * Admin-only data endpoint.
     */
    @PostMapping("/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> postAdminData(@RequestBody Map<String, Object> data) {
        return ResponseEntity.ok("Admin data processed successfully: " + data.size() + " fields");
    }

    /**
     * Get admin info.
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getAdminInfo(Authentication authentication) {
        Map<String, String> info = new HashMap<>();
        info.put("role", "Administrator");
        info.put("access", "Full System Access");
        info.put("user", authentication.getName());

        return ResponseEntity.ok(info);
    }
}
