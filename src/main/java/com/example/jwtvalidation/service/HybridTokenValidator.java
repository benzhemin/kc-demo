package com.example.jwtvalidation.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Hybrid token validator that combines local and remote validation.
 *
 * Strategy:
 * 1. Try local validation first (fast path)
 * 2. Cache validated tokens to reduce load
 * 3. Fall back to remote validation if needed
 * 4. Optionally remote-validate tokens close to expiry
 *
 * Best of both worlds:
 * - Fast validation for most requests (local)
 * - Real-time revocation checking when needed (remote)
 */
@Component
@ConditionalOnProperty(name = "app.security.validation-mode", havingValue = "HYBRID")
public class HybridTokenValidator {

    private final JwtDecoder jwtDecoder;
    private final OpaqueTokenIntrospector introspector;
    private final Cache<String, Boolean> tokenCache;
    private final boolean enableRemoteValidation;
    private final int cacheExpiry;

    @Autowired
    public HybridTokenValidator(
            JwtDecoder jwtDecoder,
            OpaqueTokenIntrospector introspector,
            @Value("${app.security.hybrid.remote-validation-enabled:false}") boolean enableRemoteValidation,
            @Value("${app.security.hybrid.cache-expiry-seconds:300}") int cacheExpiry) {
        this.jwtDecoder = jwtDecoder;
        this.introspector = introspector;
        this.enableRemoteValidation = enableRemoteValidation;
        this.cacheExpiry = cacheExpiry;
        this.tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();
    }

    /**
     * Validates a token using hybrid approach.
     */
    public OAuth2AuthenticatedPrincipal validateToken(String token) {
        // Check cache first
        Boolean cachedResult = tokenCache.getIfPresent(token);
        if (cachedResult != null && cachedResult) {
            // Token was validated and cached - return from local validation
            try {
                Jwt jwt = jwtDecoder.decode(token);
                return convertJwtToPrincipal(jwt);
            } catch (JwtException e) {
                // Cache was stale, remove it
                tokenCache.invalidate(token);
            }
        }

        try {
            // Try local validation first (fast path)
            Jwt jwt = jwtDecoder.decode(token);

            // If local validation succeeds but remote validation enabled
            // and we have suspicion (token close to expiry), verify remotely
            if (enableRemoteValidation && shouldRemoteValidate(jwt)) {
                return introspector.introspect(token);
            }

            // Local validation succeeded
            tokenCache.put(token, true);
            return convertJwtToPrincipal(jwt);

        } catch (JwtException e) {
            // Local validation failed, try remote if enabled
            if (enableRemoteValidation) {
                OAuth2AuthenticatedPrincipal principal = introspector.introspect(token);
                tokenCache.put(token, true);
                return principal;
            }
            throw e;
        }
    }

    /**
     * Determines if token should be validated remotely.
     * Currently checks if token is close to expiry (within 1 minute).
     */
    private boolean shouldRemoteValidate(Jwt jwt) {
        Instant expiry = jwt.getExpiresAt();
        if (expiry == null) {
            return false;
        }
        // Remote validate if token expires within 60 seconds
        return expiry.isBefore(Instant.now().plusSeconds(60));
    }

    /**
     * Converts JWT to OAuth2AuthenticatedPrincipal.
     */
    private OAuth2AuthenticatedPrincipal convertJwtToPrincipal(Jwt jwt) {
        Map<String, Object> attributes = jwt.getClaims();
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new DefaultOAuth2AuthenticatedPrincipal(
            jwt.getSubject(),
            attributes,
            authorities
        );
    }

    /**
     * Extracts authorities from JWT claims.
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) jwt.getClaim("roles");

        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * Invalidates cached token.
     */
    public void invalidateToken(String token) {
        tokenCache.invalidate(token);
    }

    /**
     * Clears all cached tokens.
     */
    public void clearCache() {
        tokenCache.invalidateAll();
    }

    /**
     * Gets cache statistics.
     */
    public String getCacheStats() {
        return String.format("Cache size: %d, Hit rate: %.2f%%",
            tokenCache.size(),
            tokenCache.stats().hitRate() * 100);
    }
}
