# JWT Token Validation in Spring Boot: Local vs Remote

## Overview
When a Spring Boot app receives a JWT token from a client, it must validate it before processing the request. There are two main approaches:

1. **Local Validation**: Verify the token signature using the public key
2. **Remote Validation**: Call Keycloak's introspection endpoint to verify the token

---

## Architecture Flow with Validation

```
┌─────────────┐          ┌──────────┐          ┌────────────┐
│   Client    │          │ Keycloak │          │ Spring Boot│
│ (Web/Mobile)│          │  Server  │          │   App      │
└─────────────┘          └──────────┘          └────────────┘
      │                        │                      │
      │─── Login Request ─────→│                      │
      │                        │                      │
      │←─ JWT Token ────────────│                      │
      │   (HS256/RS256)         │                      │
      │                        │                      │
      │─ API Call w/ JWT ─────────────────────────→  │
      │   (Authorization header)                      │
      │                        │                      │
      │                        │  ┌─ LOCAL VALIDATION ──┐
      │                        │  │ 1. Decode JWT       │
      │                        │  │ 2. Verify Signature │
      │                        │  │ 3. Check Claims     │
      │                        │  │ 4. Validate Expiry  │
      │                        │  └────────────────────┘
      │                        │                      │
      │                        │  ┌ REMOTE VALIDATION ─┐
      │                        │──→ Call /token/introspect
      │                        │  │ 2. Receive result   │
      │                        │  └────────────────────┘
      │                        │                      │
      │←───── Response ────────────────────────────   │
```

---

## Part 1: LOCAL VALIDATION (Recommended for High Performance)

### Step 1: Add Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-resource-server</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>

<!-- For RS256 (asymmetric) signature verification -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
</dependency>
```

### Step 2: Configure application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # For local validation with asymmetric key (RS256)
          issuer-uri: https://keycloak.example.com/auth/realms/myrealm
          jwk-set-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
          
          # Alternative: For symmetric key (HS256) - less secure
          # key-value: "your-secret-key-here"

server:
  servlet:
    context-path: /api
```

### Step 3: Spring Security Configuration

```java
// SecurityConfig.java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .jwt()
                    .jwtAuthenticationConverter(jwtAuthenticationConverter());
        
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Extract authorities from 'roles' claim instead of default 'scope'
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
```

### Step 4: How Local Validation Works (Under the Hood)

When the JWT arrives:

```java
// Step 1: Extract JWT from Authorization header
// Header: "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
String token = request.getHeader("Authorization").replace("Bearer ", "");

// Step 2: Decode the JWT (3 parts separated by dots)
// Part 1 (Header): eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9
//   Decoded: {"alg": "RS256", "typ": "JWT"}

// Part 2 (Payload): eyJzdWIiOiI1MDQ4MGMzYS0..."
//   Decoded: {
//     "sub": "user-id",
//     "email": "user@example.com",
//     "iss": "https://keycloak.example.com/auth/realms/myrealm",
//     "aud": "my-app",
//     "iat": 1630000000,
//     "exp": 1630003600
//   }

// Part 3 (Signature): SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
//   This is created by: HMAC-SHA256(header + "." + payload, secret_key)

// Step 3: Fetch public key from Keycloak's JWKS endpoint
// GET https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
// Response:
// {
//   "keys": [
//     {
//       "kid": "key-id-1",
//       "kty": "RSA",
//       "n": "xjlCAoqwbybONKKSO8wnOcqgVCfaK...",  // Modulus
//       "e": "AQAB"                                 // Exponent
//     }
//   ]
// }

// Step 4: Verify Signature
// Spring Security uses the public key to verify:
// signature_valid = verify(
//   actual_signature = Part3,
//   expected_signature = HMAC-SHA256(Part1 + "." + Part2, public_key)
// )

// Step 5: Validate Claims
// - Check "iss" (issuer) matches expected issuer
// - Check "aud" (audience) includes your app
// - Check "iat" (issued at) is not in future
// - Check "exp" (expiration) is not in past
// - Check custom claims (roles, permissions, etc.)

// Step 6: Extract User Information
// Spring Security automatically creates Authentication object
// with principal = user info, authorities = roles from JWT
```

### Step 5: Use in Controllers

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        // JWT has been validated at this point
        
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) jwt.getClaim("roles");
        
        // Your business logic
        return ResponseEntity.ok(new UserProfile(userId, email, roles));
    }
    
    @PostMapping("/admin/data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("Admin access granted");
    }
}
```

---

## Part 2: REMOTE VALIDATION (Keycloak Introspection)

### When to Use Remote Validation:
- Need real-time token revocation checking
- Token permissions changed after issuance
- Want immediate logout across all instances
- Lower performance requirements

### Step 1: Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          # Remote token introspection
          introspection-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect
          client-id: my-app
          client-secret: your-client-secret
```

### Step 2: Spring Security Configuration for Remote Validation

```java
@Configuration
@EnableWebSecurity
public class SecurityConfigRemote {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .oauth2ResourceServer()
                .opaqueToken()  // Use opaque token validation instead of JWT
                    .introspectionUri("https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect")
                    .introspectionClientCredentials("my-app", "your-client-secret");
        
        return http.build();
    }
}
```

### Step 3: How Remote Validation Works (Step by Step)

```
1. Client sends request with JWT token
   GET /api/users/profile
   Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

2. Spring Boot receives request → Security Filter Chain
   ├─ Extract token from Authorization header
   
3. Spring sends INTROSPECTION REQUEST to Keycloak:
   POST /auth/realms/myrealm/protocol/openid-connect/token/introspect
   Content-Type: application/x-www-form-urlencoded
   
   Body:
   token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
   client_id=my-app
   client_secret=your-client-secret

4. Keycloak VALIDATES TOKEN and responds:
   
   SUCCESS (200 OK):
   {
     "active": true,
     "sub": "user-id-123",
     "email": "user@example.com",
     "email_verified": true,
     "name": "John Doe",
     "iat": 1630000000,
     "exp": 1630003600,
     "iss": "https://keycloak.example.com/auth/realms/myrealm",
     "aud": "my-app",
     "client_id": "my-app",
     "scope": "openid email profile",
     "realm_access": {
       "roles": ["user", "admin"]
     }
   }
   
   OR INVALID (200 OK):
   {
     "active": false
   }

5. Spring Boot receives response
   ├─ If active=false → Reject request (403 Forbidden)
   ├─ If active=true → Extract user info and proceed
   ├─ Cache result for X seconds (configurable)

6. Request proceeds to controller with user context
```

### Step 4: Custom Introspection Implementation

```java
// If you need more control over remote validation
@Component
public class CustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    
    private final RestTemplate restTemplate;
    private final String introspectionUri;
    private final String clientId;
    private final String clientSecret;
    
    @Autowired
    public CustomOpaqueTokenIntrospector(RestTemplate restTemplate,
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
        // Prepare request
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
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
            
            Map<String, Object> body_response = response.getBody();
            
            // Check if token is active
            if (body_response == null || !(Boolean) body_response.get("active")) {
                throw new BadOpaqueTokenException("Token is not active");
            }
            
            // Convert response to OAuth2AuthenticatedPrincipal
            return new DefaultOAuth2AuthenticatedPrincipal(
                (String) body_response.get("sub"),
                body_response,
                extractAuthorities(body_response)
            );
            
        } catch (Exception e) {
            throw new BadOpaqueTokenException("Failed to introspect token", e);
        }
    }
    
    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> attributes) {
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
        
        if (realmAccess == null) {
            return Collections.emptyList();
        }
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}

@Configuration
public class OpaqueTokenConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

---

## Comparison: Local vs Remote Validation

| Aspect | Local Validation | Remote Validation |
|--------|-----------------|-------------------|
| **Performance** | ✅ Fastest (in-memory) | ❌ Slower (network call) |
| **Latency** | ~1ms | ~50-200ms |
| **Scalability** | ✅ Linear (no external calls) | ❌ Depends on Keycloak |
| **Real-time Revocation** | ❌ Not immediate | ✅ Immediate |
| **Load on Keycloak** | ✅ Low (JWKS fetched once) | ❌ High (per request) |
| **Offline Support** | ✅ Can work offline | ❌ Requires network |
| **Caching** | ✅ Keys cached | ✅ Results can be cached |
| **Security** | ✅ Cryptographically verified | ✅ Server-verified |
| **Best For** | High-traffic APIs | Real-time revocation needed |

---

## Part 3: HYBRID APPROACH (Best of Both Worlds)

```java
@Component
public class HybridTokenValidator {
    
    private final JwtDecoder jwtDecoder;
    private final OpaqueTokenIntrospector introspector;
    private final Cache<String, Boolean> tokenCache;
    private final boolean enableRemoteValidation;
    private final int cacheExpiry; // in seconds
    
    @Autowired
    public HybridTokenValidator(
            JwtDecoder jwtDecoder,
            OpaqueTokenIntrospector introspector,
            @Value("${security.token.remote-validation-enabled:false}") boolean enableRemoteValidation,
            @Value("${security.token.cache-expiry:300}") int cacheExpiry) {
        this.jwtDecoder = jwtDecoder;
        this.introspector = introspector;
        this.enableRemoteValidation = enableRemoteValidation;
        this.cacheExpiry = cacheExpiry;
        this.tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)
            .build();
    }
    
    public OAuth2AuthenticatedPrincipal validateToken(String token) {
        // Check cache first
        Boolean cachedResult = tokenCache.getIfPresent(token);
        if (cachedResult != null && cachedResult) {
            // Token was validated and cached
            return introspector.introspect(token); // or return from cache
        }
        
        try {
            // Try local validation first (fast path)
            Jwt jwt = jwtDecoder.decode(token);
            
            // If local validation succeeds but remote validation enabled
            // and we have suspicion (optional additional check)
            if (enableRemoteValidation && shouldRemoteValidate(jwt)) {
                return introspector.introspect(token);
            }
            
            // Local validation succeeded
            tokenCache.put(token, true);
            return convertJwtToPrincipal(jwt);
            
        } catch (JwtException e) {
            // Local validation failed, try remote
            if (enableRemoteValidation) {
                return introspector.introspect(token);
            }
            throw e;
        }
    }
    
    private boolean shouldRemoteValidate(Jwt jwt) {
        // Check if token is close to expiry (within 1 minute)
        Instant expiry = jwt.getExpiresAt();
        return expiry.isBefore(Instant.now().plusSeconds(60));
    }
    
    private OAuth2AuthenticatedPrincipal convertJwtToPrincipal(Jwt jwt) {
        Map<String, Object> attributes = jwt.getClaims();
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new DefaultOAuth2AuthenticatedPrincipal(
            jwt.getSubject(),
            attributes,
            authorities
        );
    }
    
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) jwt.getClaim("roles");
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}
```

---

## Part 4: Error Handling

```java
@RestControllerAdvice
public class SecurityExceptionHandler {
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", "Invalid or expired token"));
    }
    
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("JWT_INVALID", ex.getMessage()));
    }
    
    @ExceptionHandler(BadOpaqueTokenException.class)
    public ResponseEntity<ErrorResponse> handleBadOpaqueToken(BadOpaqueTokenException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("TOKEN_INTROSPECTION_FAILED", ex.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", "Insufficient permissions"));
    }
}

@Data
@AllArgsConstructor
class ErrorResponse {
    private String code;
    private String message;
}
```

---

## Summary Flowchart

```
Request arrives with JWT
        ↓
┌───────────────────────┐
│ LOCAL VALIDATION      │
├───────────────────────┤
│ 1. Decode JWT         │ → Fast (~1ms)
│ 2. Extract parts      │
│ 3. Verify signature   │
│    with public key    │
│ 4. Validate claims    │
│ 5. Check expiry       │
└───────────────────────┘
        ↓
    ✅ Valid?
    /      \
  Yes      No
  │        │
  ✓        └─→ ┌────────────────────────┐
               │ REMOTE VALIDATION      │
               ├────────────────────────┤
               │ 1. Call introspection  │
               │    endpoint            │
               │ 2. Keycloak verifies   │
               │ 3. Returns active:true │
               │    or false            │
               └────────────────────────┘
                      ↓
                   ✅ Active?
                   /      \
                 Yes      No
                  │        │
                  ✓        ✗ Reject (403)
                  │
         ┌────────────────────┐
         │ Request Authorized │
         │ Proceed to Handler │
         └────────────────────┘
```

---

## Configuration Summary

**For LOCAL Validation (Recommended):**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/auth/realms/myrealm
          jwk-set-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs
```

**For REMOTE Validation:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect
          client-id: my-app
          client-secret: your-client-secret
```
