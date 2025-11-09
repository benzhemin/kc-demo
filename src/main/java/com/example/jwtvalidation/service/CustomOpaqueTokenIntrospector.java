package com.example.jwtvalidation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom implementation of OpaqueTokenIntrospector for remote token validation.
 * This calls Keycloak's introspection endpoint to validate tokens.
 */
@Component
@ConditionalOnProperty(name = "app.security.validation-mode", havingValue = "REMOTE")
public class CustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    private final RestTemplate restTemplate;
    private final String introspectionUri;
    private final String clientId;
    private final String clientSecret;

    @Autowired
    public CustomOpaqueTokenIntrospector(
            RestTemplate restTemplate,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}") String introspectionUri,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}") String clientId,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.introspectionUri = introspectionUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        // Prepare request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Prepare request body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // Call Keycloak introspection endpoint
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                introspectionUri,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();

            // Check if token is active
            if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("active"))) {
                throw new BadOpaqueTokenException("Token is not active");
            }

            // Convert response to OAuth2AuthenticatedPrincipal
            return new DefaultOAuth2AuthenticatedPrincipal(
                (String) responseBody.get("sub"),
                responseBody,
                extractAuthorities(responseBody)
            );

        } catch (Exception e) {
            throw new BadOpaqueTokenException("Failed to introspect token: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts authorities from the introspection response.
     * Looks for roles in 'realm_access.roles' or 'roles' claim.
     */
    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> attributes) {
        // Try realm_access.roles first
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");

        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            if (roles != null) {
                return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
            }
        }

        // Fallback to 'roles' claim
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) attributes.get("roles");

        if (roles != null) {
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
