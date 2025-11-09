# JWT Validation Project - Comprehensive Guide (Part 2)

## 11. cURL Command Examples

### 11.1 Basic Testing Workflow

```bash
#!/bin/bash
# Complete testing workflow

# Step 1: Verify application is running
echo "=== Step 1: Health Check ==="
curl -s http://localhost:8080/api/public/health | jq '.'

# Step 2: Generate USER token
echo -e "\n=== Step 2: Generate USER Token ==="
USER_RESPONSE=$(curl -s http://localhost:8080/api/public/mock/user-token)
echo $USER_RESPONSE | jq '.'
USER_TOKEN=$(echo $USER_RESPONSE | jq -r '.token')
echo "USER_TOKEN=$USER_TOKEN"

# Step 3: Generate ADMIN token
echo -e "\n=== Step 3: Generate ADMIN Token ==="
ADMIN_RESPONSE=$(curl -s http://localhost:8080/api/public/mock/admin-token)
echo $ADMIN_RESPONSE | jq '.'
ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | jq -r '.token')
echo "ADMIN_TOKEN=$ADMIN_TOKEN"

# Step 4: Test USER endpoints
echo -e "\n=== Step 4: Test USER Endpoints ==="
curl -s http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.'

# Step 5: Test ADMIN endpoints with USER token (should fail)
echo -e "\n=== Step 5: Test ADMIN Endpoint with USER Token (403) ==="
curl -s http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.'

# Step 6: Test ADMIN endpoints with ADMIN token (should succeed)
echo -e "\n=== Step 6: Test ADMIN Endpoint with ADMIN Token (200) ==="
curl -s http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'

# Step 7: Test without token (should fail)
echo -e "\n=== Step 7: Test Without Token (401) ==="
curl -s http://localhost:8080/api/user/profile | jq '.'

echo -e "\n=== Testing Complete ==="
```

### 11.2 Detailed cURL Examples

#### Testing with Verbose Output

```bash
# Show full HTTP request/response
curl -v http://localhost:8080/api/public/health

# Output:
> GET /api/public/health HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.88.1
> Accept: */*
>
< HTTP/1.1 200
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Fri, 08 Nov 2025 09:42:07 GMT
<
{
  "status": "UP",
  "timestamp": "2025-11-08T09:42:07.285814Z",
  "validationMode": "LOCAL"
}
```

#### Testing with Headers

```bash
# Include response headers
curl -i http://localhost:8080/api/public/health

# Save headers to file
curl -D headers.txt http://localhost:8080/api/public/health

# Custom request headers
curl -H "Accept: application/json" \
     -H "User-Agent: MyApp/1.0" \
     http://localhost:8080/api/public/health
```

#### Testing Authentication

```bash
# Generate and use token in one command
TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')

# Use token for authenticated request
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/user/profile | jq '.'

# Show only HTTP status code
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/user/profile

# Output: 200
```

#### Testing POST Endpoints

```bash
# Generate custom token
curl -X POST http://localhost:8080/api/public/mock/generate-token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-123",
    "email": "test@example.com",
    "name": "Test User",
    "roles": ["USER", "ADMIN"]
  }' | jq '.'

# Post admin data
ADMIN_TOKEN=$(curl -s http://localhost:8080/api/public/mock/admin-token | jq -r '.token')

curl -X POST http://localhost:8080/api/admin/data \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "field1": "value1",
    "field2": "value2"
  }'
```

#### Error Testing

```bash
# Test with invalid token
curl -H "Authorization: Bearer invalid.token.here" \
     http://localhost:8080/api/user/profile | jq '.'

# Output:
{
  "code": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}

# Test with expired token (manually create one with past exp)
# (requires custom token generation with past expiration)

# Test without Bearer prefix
curl -H "Authorization: eyJhbGc..." \
     http://localhost:8080/api/user/profile

# Test with malformed header
curl -H "Authorization: Bear eyJhbGc..." \
     http://localhost:8080/api/user/profile
```

### 11.3 Performance Testing with cURL

#### Single Request Timing

```bash
# Measure request time
curl -w "@curl-format.txt" -o /dev/null -s \
  http://localhost:8080/api/public/health

# curl-format.txt content:
time_namelookup:  %{time_namelookup}s\n
time_connect:     %{time_connect}s\n
time_appconnect:  %{time_appconnect}s\n
time_pretransfer: %{time_pretransfer}s\n
time_starttransfer: %{time_starttransfer}s\n
time_total:       %{time_total}s\n

# Output:
time_namelookup:  0.001s
time_connect:     0.001s
time_appconnect:  0.000s
time_pretransfer: 0.001s
time_starttransfer: 0.010s
time_total:       0.012s
```

#### Load Testing (Simple)

```bash
# Send 100 requests
for i in {1..100}; do
  curl -s http://localhost:8080/api/public/health > /dev/null
  echo "Request $i completed"
done

# Measure average response time
{
  for i in {1..100}; do
    curl -w "%{time_total}\n" -o /dev/null -s \
      http://localhost:8080/api/public/health
  done
} | awk '{sum+=$1; count++} END {print "Average:", sum/count, "seconds"}'
```

#### Concurrent Requests

```bash
# 10 concurrent requests
for i in {1..10}; do
  curl -s http://localhost:8080/api/public/health &
done
wait

# With authentication
TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')

for i in {1..10}; do
  curl -s -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/user/profile > /dev/null &
done
wait
```

### 11.4 Advanced cURL Techniques

#### Rate Limiting Test

```bash
# Test 1 request per second
for i in {1..10}; do
  curl -s http://localhost:8080/api/public/health | jq '.timestamp'
  sleep 1
done
```

#### Retry Logic

```bash
# Retry up to 3 times on failure
curl --retry 3 --retry-delay 1 --retry-max-time 10 \
  http://localhost:8080/api/public/health
```

#### Save Response and Headers

```bash
# Save response body
curl -s http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  -o response.json

# Save headers
curl -s -D headers.txt http://localhost:8080/api/public/health \
  -o response.json

# Save both in one command
curl -s -D headers-and-body.txt http://localhost:8080/api/public/health
```

#### Testing Different Accept Headers

```bash
# Request JSON
curl -H "Accept: application/json" \
     http://localhost:8080/api/user/profile

# Request XML (not supported in our API, will return JSON anyway)
curl -H "Accept: application/xml" \
     http://localhost:8080/api/user/profile

# Request any
curl -H "Accept: */*" \
     http://localhost:8080/api/user/profile
```

---

## 12. Security Implementation Details

### 12.1 Spring Security Filter Chain

```
HTTP Request Flow through Security Filters:

Client Request → [Filter Chain] → Controller

Filter Chain:
  1. DisableEncodeUrlFilter
     └─ Disable URL encoding for security

  2. WebAsyncManagerIntegrationFilter
     └─ Integrate SecurityContext with async requests

  3. SecurityContextHolderFilter
     └─ Setup SecurityContext for request

  4. HeaderWriterFilter
     └─ Add security headers (X-Frame-Options, etc.)

  5. CorsFilter
     └─ Handle CORS pre-flight requests

  6. LogoutFilter
     └─ Handle logout requests

  7. BearerTokenAuthenticationFilter ← CRITICAL FOR JWT
     ├─ Extract "Authorization: Bearer <token>"
     ├─ Call JwtDecoder or OpaqueTokenIntrospector
     ├─ Validate token
     ├─ Create Authentication object
     └─ Store in SecurityContext

  8. RequestCacheAwareFilter
     └─ Handle saved requests

  9. SecurityContextHolderAwareRequestFilter
     └─ Wrap request with security methods

  10. AnonymousAuthenticationFilter
      └─ Create anonymous auth if no auth present

  11. SessionManagementFilter
      └─ Manage session (stateless for JWT)

  12. ExceptionTranslationFilter
      └─ Handle security exceptions

  13. AuthorizationFilter ← CRITICAL FOR PERMISSIONS
      ├─ Check @PreAuthorize annotations
      ├─ Check SecurityFilterChain rules
      ├─ Verify user has required roles
      └─ Allow or deny access

If all filters pass → Dispatch to Controller
If any filter denies → Return error (401/403)
```

### 12.2 JWT Decoder Configuration

**LOCAL Mode (MockJwtConfig.java):**

```java
@Bean
public JwtDecoder jwtDecoder() {
    // For HMAC-SHA256 (symmetric key)
    SecretKey key = new SecretKeySpec(
        secretKey.getBytes(),
        "HmacSHA256"
    );

    return NimbusJwtDecoder.withSecretKey(key).build();
}

// Under the hood:
NimbusJwtDecoder:
  └─ Uses Nimbus JOSE JWT library
  └─ Validates:
     ├─ Signature (HMAC-SHA256)
     ├─ Expiration (exp claim)
     ├─ Not before (nbf claim, if present)
     └─ Issuer (iss claim, if configured)
```

**Production Mode with RS256:**

```java
@Bean
public JwtDecoder jwtDecoder() {
    // For RS256 (asymmetric key)
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
}

// Fetches public keys from Keycloak:
GET https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs

// Keys are cached in memory
// Automatic key rotation support
// Validates using public key
```

### 12.3 Authority Mapping

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter =
        new JwtGrantedAuthoritiesConverter();

    // Extract authorities from "roles" claim
    authoritiesConverter.setAuthoritiesClaimName("roles");

    // Add "ROLE_" prefix (Spring Security convention)
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
}

// Example:
// JWT claim: { "roles": ["USER", "ADMIN"] }
// →
// Spring Security authorities: ["ROLE_USER", "ROLE_ADMIN"]
```

### 12.4 Authorization Rules

**URL-Based Authorization:**

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**", "/health", "/actuator/**")
        .permitAll()  // Allow everyone

    .requestMatchers("/admin/**")
        .hasRole("ADMIN")  // Requires ROLE_ADMIN

    .requestMatchers("/user/**")
        .hasAnyRole("USER", "ADMIN")  // Requires ROLE_USER or ROLE_ADMIN

    .anyRequest()
        .authenticated()  // All other requests require authentication
)

// Precedence: First match wins!
// Order matters: Specific → General
```

**Method-Based Authorization:**

```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Map> getAdminDashboard() {
    // Only accessible if user has ROLE_ADMIN
}

// Other annotations:
@PostAuthorize("returnObject.owner == authentication.name")
@Secured("ROLE_ADMIN")
@RolesAllowed("ADMIN")
```

### 12.5 Session Management

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)

// STATELESS:
// - No JSESSIONID cookie
// - No session stored on server
// - Each request is independent
// - Perfect for JWT (token contains all info)
// - Horizontally scalable
```

### 12.6 CORS Configuration

```java
// Default CORS in Spring Security
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}

// CORS Pre-flight Request:
// OPTIONS /api/user/profile
// Access-Control-Request-Method: GET
// Access-Control-Request-Headers: Authorization
// →
// Access-Control-Allow-Origin: http://localhost:3000
// Access-Control-Allow-Methods: GET, POST, PUT, DELETE
// Access-Control-Allow-Headers: Authorization
```

### 12.7 CSRF Protection

```java
.csrf(csrf -> csrf.disable())

// Why disabled for JWT APIs?
// - CSRF protects against cross-site requests
// - CSRF relies on cookies (session)
// - JWT is stateless, no cookies
// - JWT sent in Authorization header (not accessible to other sites)
// - Therefore, CSRF not needed for JWT APIs

// When to ENABLE CSRF:
// - Using session-based auth
// - Using cookies for authentication
// - Traditional web applications
```

---

## 13. Token Anatomy

### 13.1 JWT Structure Deep Dive

```
Complete JWT Token:
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTAwMSIsImF1ZCI6Im1vY2stYXVkaWVuY2UiLCJyb2xlcyI6WyJVU0VSIl0sIm5hbWUiOiJSZWd1bGFyIFVzZXIiLCJpc3MiOiJtb2NrLWlzc3VlciIsImV4cCI6MTc2MjU5ODM5NywiaWF0IjoxNzYyNTk0Nzk3LCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20ifQ.tt86bX5JddJEV7Jlx2Jm2zDGl1Q6Am1xYmSjfoQPqXE

Parts:
┌─────────────────┬──────────────────────────────────┬─────────────────────────────┐
│     HEADER      │            PAYLOAD               │         SIGNATURE           │
├─────────────────┼──────────────────────────────────┼─────────────────────────────┤
│ eyJhbGciOiJIUzI │ eyJzdWIiOiJ1c2VyLTAwMSIsImF1Z │ tt86bX5JddJEV7Jlx2Jm2zDGl1 │
│ 1NiJ9           │ CI6Im1vY2stYXVkaWVuY2UiLCJyb2x │ Q6Am1xYmSjfoQPqXE           │
│                 │ lcyI6WyJVU0VSIl0sIm5hbWUiOiJSZ │                             │
│                 │ Wd1bGFyIFVzZXIiLCJpc3MiOiJtb2N │                             │
│                 │ rLWlzc3VlciIsImV4cCI6MTc2MjU5O │                             │
│                 │ DM5NywiaWF0IjoxNzYyNTk0Nzk3LCJ │                             │
│                 │ lbWFpbCI6InVzZXJAZXhhbXBsZS5jb │                             │
│                 │ 20ifQ                            │                             │
└─────────────────┴──────────────────────────────────┴─────────────────────────────┘
```

### 13.2 Header Analysis

```json
// Base64URL decoded HEADER:
{
  "alg": "HS256",
  "typ": "JWT"
}

// Fields:
alg (Algorithm):
  - HS256: HMAC using SHA-256 (symmetric)
  - RS256: RSA Signature using SHA-256 (asymmetric)
  - ES256: ECDSA using P-256 and SHA-256
  - none: No signature (INSECURE, never use in production)

typ (Type):
  - JWT: JSON Web Token
  - Always "JWT" for JWTs

// Additional optional fields:
kid (Key ID):
  - Identifies which key was used to sign
  - Example: "kid": "key-2025-01"
  - Used when multiple keys are in rotation

cty (Content Type):
  - Usually omitted
  - Used when JWT is nested
```

### 13.3 Payload (Claims) Analysis

```json
// Base64URL decoded PAYLOAD:
{
  "sub": "user-001",
  "aud": "mock-audience",
  "roles": ["USER"],
  "name": "Regular User",
  "iss": "mock-issuer",
  "exp": 1762598397,
  "iat": 1762594797,
  "email": "user@example.com"
}

// Standard Claims (Registered):
sub (Subject):
  - User identifier (unique ID)
  - Example: "user-001", UUID, email

iss (Issuer):
  - Who issued the token
  - Example: "https://keycloak.example.com/auth/realms/myrealm"

aud (Audience):
  - Who the token is intended for
  - Example: "my-app", "api.example.com"
  - Can be string or array

exp (Expiration Time):
  - Unix timestamp (seconds since epoch)
  - Token invalid after this time
  - Example: 1762598397 = 2025-11-08T10:42:07Z

iat (Issued At):
  - Unix timestamp when token was created
  - Example: 1762594797 = 2025-11-08T09:42:07Z

nbf (Not Before):
  - Token not valid before this time
  - Optional, rarely used

jti (JWT ID):
  - Unique identifier for this token
  - Used to prevent replay attacks
  - Example: "jti": "abc123-def456"

// Custom Claims (Application-specific):
roles: ["USER"]
  - Custom claim for authorization

name: "Regular User"
  - User's display name

email: "user@example.com"
  - User's email address

// Any JSON-serializable data can be added
// Keep payload small (included in every request)
```

### 13.4 Signature Calculation

```javascript
// HMAC-SHA256 (HS256) Signature:

// Input data
const header = base64UrlEncode(JSON.stringify({
  "alg": "HS256",
  "typ": "JWT"
}));
// Result: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"

const payload = base64UrlEncode(JSON.stringify({
  "sub": "user-001",
  "exp": 1762598397,
  // ... other claims
}));
// Result: "eyJzdWIiOiJ1c2VyLTAwMSIsImV4cCI6MTc2MjU5ODM5NywuLi4ifQ"

// Concatenate with dot
const signatureInput = header + "." + payload;

// Sign with secret key
const secret = "mySecretKeyForJWT2025mustBe32bytes!";
const signature = HMACSHA256(signatureInput, secret);

// Base64URL encode signature
const encodedSignature = base64UrlEncode(signature);
// Result: "tt86bX5JddJEV7Jlx2Jm2zDGl1Q6Am1xYmSjfoQPqXE"

// Final JWT
const jwt = header + "." + payload + "." + encodedSignature;
```

```java
// Java Implementation:

// Create signer
SecretKey key = new SecretKeySpec(
    secret.getBytes(),
    "HmacSHA256"
);
JWSSigner signer = new MACSigner(key);

// Create JWT
JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
    .subject("user-001")
    .claim("roles", List.of("USER"))
    .expirationTime(new Date(System.currentTimeMillis() + 3600000))
    .build();

SignedJWT signedJWT = new SignedJWT(
    new JWSHeader(JWSAlgorithm.HS256),
    claimsSet
);

// Sign
signedJWT.sign(signer);

// Serialize
String jwt = signedJWT.serialize();
```

### 13.5 Base64URL Encoding

```javascript
// Standard Base64 vs Base64URL

Standard Base64:
  - Uses: A-Z, a-z, 0-9, +, /
  - Padding: = at end
  - Example: "Hello World" → "SGVsbG8gV29ybGQ="

Base64URL (JWT uses this):
  - Uses: A-Z, a-z, 0-9, -, _
  - No padding (no =)
  - URL-safe (can be used in URLs)
  - Example: "Hello World" → "SGVsbG8gV29ybGQ"

Why Base64URL?
  - JWTs are often passed in URLs (OAuth2 flows)
  - + and / are special characters in URLs
  - Padding = causes issues in some contexts

// Conversion:
base64url(data) = base64(data)
                    .replace('+', '-')
                    .replace('/', '_')
                    .replace(/=+$/, '');
```

### 13.6 Token Size Analysis

```
Example Token:
  HEADER:    ~20 bytes (encoded)
  PAYLOAD:   ~200 bytes (encoded) - depends on claims
  SIGNATURE: ~43 bytes (encoded) - HS256/RS256

Total: ~260 bytes

Size Impact of Claims:
  Standard claims (sub, iss, exp, iat, aud): ~100 bytes
  Each additional string claim: ~20-50 bytes
  Each array element: ~10-30 bytes

Example:
  Minimal JWT (sub, exp, iat): ~150 bytes
  With roles array [USER, ADMIN]: ~180 bytes
  With profile data (name, email, etc.): ~250 bytes
  With extensive claims: ~500+ bytes

Best Practices:
  ✓ Keep payload small (< 500 bytes)
  ✓ Store large data in database, reference by ID
  ✗ Don't put entire user profile in JWT
  ✗ Don't put sensitive data (passwords, credit cards)

Why size matters:
  - Sent in every HTTP request
  - 1000 req/sec × 500 bytes = 500 KB/sec = 30 MB/minute
  - Larger tokens = more bandwidth, slower transmission
```

---

## 14. Configuration Deep Dive

### 14.1 application.yml Breakdown

```yaml
# ============================================
# Server Configuration
# ============================================
server:
  port: 8080                    # HTTP port
  servlet:
    context-path: /api          # Base path for all endpoints

# All endpoints will be prefixed with /api:
# http://localhost:8080/api/public/health
# http://localhost:8080/api/user/profile

# ============================================
# Spring Application Configuration
# ============================================
spring:
  application:
    name: jwt-validation-demo   # Application name (for monitoring, logging)

  # ============================================
  # Spring Security OAuth2 Configuration
  # ============================================
  security:
    oauth2:
      resourceserver:

        # ────────────────────────────────────
        # LOCAL Validation (JWT)
        # ────────────────────────────────────
        jwt:
          # Issuer URI (optional but recommended)
          # Used to validate "iss" claim in JWT
          issuer-uri: ${JWT_ISSUER_URI:https://keycloak.example.com/auth/realms/myrealm}

          # JWKS (JSON Web Key Set) URI
          # Where to fetch public keys for signature verification
          jwk-set-uri: ${JWT_JWK_SET_URI:https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs}

          # How it works:
          # 1. Spring Boot fetches public keys from jwk-set-uri
          # 2. Caches keys in memory
          # 3. Uses keys to verify JWT signatures
          # 4. Validates "iss" claim matches issuer-uri

        # ────────────────────────────────────
        # REMOTE Validation (Opaque Token)
        # ────────────────────────────────────
        opaquetoken:
          # Token introspection endpoint
          # Called for each request to validate token
          introspection-uri: ${INTROSPECTION_URI:https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect}

          # Client credentials for introspection
          # Required to call introspection endpoint
          client-id: ${OAUTH_CLIENT_ID:my-app}
          client-secret: ${OAUTH_CLIENT_SECRET:your-client-secret}

          # How it works:
          # 1. POST to introspection-uri with token
          # 2. Keycloak validates and returns user info
          # 3. Spring Security creates authentication

# ============================================
# Application-Specific Configuration
# ============================================
app:
  security:
    # ────────────────────────────────────
    # Validation Mode
    # ────────────────────────────────────
    # Options: LOCAL, REMOTE, HYBRID
    validation-mode: ${VALIDATION_MODE:LOCAL}

    # LOCAL:  Fast, cryptographic verification
    # REMOTE: Real-time introspection
    # HYBRID: Intelligent combination with caching

    # ────────────────────────────────────
    # Hybrid Mode Settings
    # ────────────────────────────────────
    hybrid:
      # Enable remote validation for critical operations
      remote-validation-enabled: ${REMOTE_VALIDATION_ENABLED:false}

      # Cache TTL in seconds
      cache-expiry-seconds: ${CACHE_EXPIRY:300}

      # Settings explained:
      # - remote-validation-enabled: true → call introspection for tokens near expiry
      # - cache-expiry-seconds: 300 → cache validated tokens for 5 minutes

    # ────────────────────────────────────
    # Mock Mode (Development/Testing)
    # ────────────────────────────────────
    # Enable mock JWT generation
    mock-enabled: ${MOCK_MODE:true}

    # Secret key for mock token signing (HMAC-SHA256)
    # Must be at least 32 bytes (256 bits)
    mock-secret: ${MOCK_SECRET:mySecretKeyForJWT2025mustBe32bytes!}

    # Why 32 bytes?
    # - HMAC-SHA256 requires 256-bit key
    # - 256 bits = 32 bytes
    # - Shorter keys = weaker security

# ============================================
# Logging Configuration
# ============================================
logging:
  level:
    # Spring Security debug logging
    org.springframework.security: DEBUG

    # Application debug logging
    com.example.jwtvalidation: DEBUG

    # Adjust for production:
    # org.springframework.security: INFO
    # com.example.jwtvalidation: INFO

  # Log pattern (optional):
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"

  # Log file (optional):
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

### 14.2 Environment-Specific Configurations

**Development (application-dev.yml):**

```yaml
app:
  security:
    validation-mode: LOCAL
    mock-enabled: true

logging:
  level:
    org.springframework.security: DEBUG
    com.example.jwtvalidation: DEBUG

server:
  port: 8080
```

**Staging (application-staging.yml):**

```yaml
app:
  security:
    validation-mode: HYBRID
    mock-enabled: false
    hybrid:
      remote-validation-enabled: true
      cache-expiry-seconds: 600

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://staging-keycloak.example.com/auth/realms/myrealm
          jwk-set-uri: https://staging-keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs

logging:
  level:
    org.springframework.security: INFO
    com.example.jwtvalidation: DEBUG

server:
  port: 8080
```

**Production (application-prod.yml):**

```yaml
app:
  security:
    validation-mode: LOCAL
    mock-enabled: false

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/auth/realms/myrealm
          jwk-set-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs

logging:
  level:
    org.springframework.security: WARN
    com.example.jwtvalidation: INFO

  file:
    name: /var/log/jwt-validation/application.log

server:
  port: 8080
  compression:
    enabled: true
  http2:
    enabled: true
```

### 14.3 Configuration Precedence

```
Spring Boot Configuration Loading Order (highest to lowest):

1. Command-line arguments
   java -jar app.jar --server.port=9090

2. SPRING_APPLICATION_JSON environment variable
   export SPRING_APPLICATION_JSON='{"server":{"port":9090}}'

3. System properties
   java -Dserver.port=9090 -jar app.jar

4. OS environment variables
   export SERVER_PORT=9090

5. application-{profile}.yml (profile-specific)
   application-prod.yml

6. application.yml (default)

7. Default values in code
   @Value("${server.port:8080}")

Example Precedence:
  application.yml: server.port=8080
  ENV variable: SERVER_PORT=9090
  Command-line: --server.port=7070

  Result: 7070 (command-line wins)
```

### 14.4 Conditional Bean Configuration

```java
// LocalValidationSecurityConfig.java
@Configuration
@ConditionalOnProperty(
    name = "app.security.validation-mode",
    havingValue = "LOCAL",
    matchIfMissing = true  // Default to LOCAL if not specified
)
public class LocalValidationSecurityConfig {
    // Beans only loaded when validation-mode=LOCAL
}

// RemoteValidationSecurityConfig.java
@Configuration
@ConditionalOnProperty(
    name = "app.security.validation-mode",
    havingValue = "REMOTE"
)
public class RemoteValidationSecurityConfig {
    // Beans only loaded when validation-mode=REMOTE
}

// HybridTokenValidator.java
@Component
@ConditionalOnProperty(
    name = "app.security.validation-mode",
    havingValue = "HYBRID"
)
public class HybridTokenValidator {
    // Bean only loaded when validation-mode=HYBRID
}

// How it works:
// 1. Spring Boot reads app.security.validation-mode
// 2. Evaluates @ConditionalOnProperty
// 3. Loads only matching configuration
// 4. Prevents bean conflicts
```

---

## 15. Code Walkthrough

### 15.1 Application Startup

```java
// JwtValidationApplication.java
@SpringBootApplication  // Composite annotation
public class JwtValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(JwtValidationApplication.class, args);
    }
}

// @SpringBootApplication includes:
// - @Configuration: Class can define beans
// - @EnableAutoConfiguration: Spring Boot auto-configuration
// - @ComponentScan: Scan for components in package and sub-packages

// Startup sequence:
// 1. Load application.yml
// 2. Create ApplicationContext
// 3. Component scanning
// 4. Auto-configuration
// 5. Bean creation and dependency injection
// 6. Initialize embedded Tomcat
// 7. Deploy application
// 8. Ready to handle requests
```

### 15.2 Security Configuration Walkthrough

```java
// LocalValidationSecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(name = "app.security.validation-mode", havingValue = "LOCAL", matchIfMissing = true)
public class LocalValidationSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (not needed for stateless JWT)
            .csrf(csrf -> csrf.disable())

            // Stateless session (no JSESSIONID)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/public/**", "/health", "/actuator/**")
                    .permitAll()

                // Admin endpoints (require ROLE_ADMIN)
                .requestMatchers("/admin/**")
                    .hasRole("ADMIN")

                // User endpoints (require ROLE_USER or ROLE_ADMIN)
                .requestMatchers("/user/**")
                    .hasAnyRole("USER", "ADMIN")

                // All other requests require authentication
                .anyRequest()
                    .authenticated()
            )

            // OAuth2 Resource Server (JWT validation)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        // Extract from "roles" claim (not default "scope")
        authoritiesConverter.setAuthoritiesClaimName("roles");

        // Add "ROLE_" prefix (Spring Security convention)
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }
}

// What happens when request arrives:
// 1. Request → SecurityFilterChain
// 2. Check if matches public/** → if yes, skip authentication
// 3. If protected, extract Bearer token
// 4. Call jwtDecoder.decode(token)
// 5. Validate signature and claims
// 6. Extract authorities using jwtAuthenticationConverter
// 7. Create Authentication object
// 8. Check authorization rules
// 9. If authorized, proceed to controller
// 10. If not authorized, return 403
```

### 15.3 Controller Example

```java
// UserController.java
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(Authentication authentication) {
        // At this point, authentication is guaranteed (filter chain passed)

        // Extract claims from JWT
        Map<String, Object> claims = extractClaims(authentication);

        String userId = (String) claims.getOrDefault("sub", "unknown");
        String email = (String) claims.getOrDefault("email", "N/A");
        String name = (String) claims.getOrDefault("name", "N/A");

        // Extract authorities (roles)
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        // Create response
        UserProfile profile = new UserProfile(userId, email, name, roles);

        return ResponseEntity.ok(profile);
    }

    private Map<String, Object> extractClaims(Authentication authentication) {
        // Check authentication type
        if (authentication instanceof JwtAuthenticationToken) {
            // LOCAL mode: JwtAuthenticationToken
            Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
            return jwt.getClaims();

        } else if (authentication.getPrincipal() instanceof Map) {
            // REMOTE mode: OAuth2AuthenticationToken
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = (Map<String, Object>) authentication.getPrincipal();
            return claims;
        }

        return new HashMap<>();
    }
}

// Request flow:
// 1. GET /api/user/profile with Bearer token
// 2. SecurityFilterChain validates token
// 3. Creates Authentication object
// 4. Spring injects Authentication into method parameter
// 5. Controller extracts user info
// 6. Returns response
```

### 15.4 Mock Token Generation

```java
// MockJwtConfig.java
public String generateMockToken(String userId, String email, String name, List<String> roles) {
    try {
        // Create HMAC signer with secret key
        JWSSigner signer = new MACSigner(secretKey.getBytes());

        // Build JWT claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(userId)                                      // sub
            .claim("email", email)                               // email
            .claim("name", name)                                 // name
            .claim("roles", roles)                               // roles
            .issuer("mock-issuer")                               // iss
            .audience("mock-audience")                           // aud
            .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000))  // exp (1 hour)
            .issueTime(new Date())                               // iat
            .build();

        // Create signed JWT
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256),  // Header
            claimsSet                            // Payload
        );

        // Sign the JWT
        signedJWT.sign(signer);

        // Serialize to compact form (header.payload.signature)
        return signedJWT.serialize();

    } catch (JOSEException e) {
        throw new RuntimeException("Failed to generate mock JWT token", e);
    }
}

// Example usage:
// String token = generateMockToken(
//     "user-001",
//     "user@example.com",
//     "John Doe",
//     List.of("USER")
// );
```

### 15.5 Error Handling

```java
// SecurityExceptionHandler.java
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // Triggered when token is missing or malformed
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("UNAUTHORIZED", "Invalid or expired token"));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
        // Triggered when JWT validation fails (signature, expiration, etc.)
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("JWT_INVALID", "JWT validation failed: " + ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Triggered when user doesn't have required role
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", "Insufficient permissions: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // Catch-all for unexpected errors
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred: " + ex.getMessage()));
    }
}

// Error response flow:
// 1. Exception thrown in filter chain or controller
// 2. @RestControllerAdvice catches exception
// 3. Matching @ExceptionHandler method called
// 4. ErrorResponse created with code and message
// 5. ResponseEntity with appropriate HTTP status
// 6. JSON response sent to client
```

---

(Continuing with more sections in the guide...)

## 16. Testing Scenarios

### 16.1 Positive Test Cases

```bash
# Test 1: Public endpoint (no auth)
curl http://localhost:8080/api/public/hello
# Expected: 200 OK

# Test 2: Generate USER token
USER_TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')

# Test 3: Access user endpoint with USER token
curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/user/profile
# Expected: 200 OK with user profile

# Test 4: Generate ADMIN token
ADMIN_TOKEN=$(curl -s http://localhost:8080/api/public/mock/admin-token | jq -r '.token')

# Test 5: Access admin endpoint with ADMIN token
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/dashboard
# Expected: 200 OK with dashboard data

# Test 6: ADMIN can access USER endpoints
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/user/profile
# Expected: 200 OK (ADMIN has USER privileges)
```

### 16.2 Negative Test Cases

```bash
# Test 1: Access protected endpoint without token
curl http://localhost:8080/api/user/profile
# Expected: 401 Unauthorized

# Test 2: Invalid token
curl -H "Authorization: Bearer invalid.token.here" \
     http://localhost:8080/api/user/profile
# Expected: 401 Unauthorized

# Test 3: USER trying to access ADMIN endpoint
curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/admin/dashboard
# Expected: 403 Forbidden

# Test 4: Malformed Authorization header
curl -H "Authorization: eyJhbGc..." \
     http://localhost:8080/api/user/profile
# Expected: 401 (missing "Bearer " prefix)

# Test 5: Wrong HTTP method
curl -X DELETE http://localhost:8080/api/admin/dashboard \
     -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: 405 Method Not Allowed
```

---

This is Part 2. Would you like me to continue with the remaining sections (Performance, Production Deployment, Advanced Topics, Troubleshooting)?
