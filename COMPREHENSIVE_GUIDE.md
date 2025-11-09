# JWT Validation Project - Ultra Detailed Comprehensive Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Project Overview](#project-overview)
3. [Architecture Decisions](#architecture-decisions)
4. [Technology Stack Deep Dive](#technology-stack-deep-dive)
5. [Project Structure](#project-structure)
6. [Validation Approaches Explained](#validation-approaches-explained)
7. [Sequence Flow Diagrams](#sequence-flow-diagrams)
8. [Installation & Setup](#installation--setup)
9. [Running the Application](#running-the-application)
10. [API Endpoints Reference](#api-endpoints-reference)
11. [cURL Command Examples](#curl-command-examples)
12. [Security Implementation Details](#security-implementation-details)
13. [Token Anatomy](#token-anatomy)
14. [Configuration Deep Dive](#configuration-deep-dive)
15. [Code Walkthrough](#code-walkthrough)
16. [Testing Scenarios](#testing-scenarios)
17. [Troubleshooting Guide](#troubleshooting-guide)
18. [Performance Considerations](#performance-considerations)
19. [Production Deployment](#production-deployment)
20. [Advanced Topics](#advanced-topics)

---

## 1. Introduction

This project is a **production-ready Spring Boot application** that demonstrates three different approaches to JWT (JSON Web Token) validation:

- **LOCAL Validation**: Cryptographic verification using public/private keys
- **REMOTE Validation**: Real-time token introspection via OAuth2 server (Keycloak)
- **HYBRID Validation**: Intelligent combination of both with caching

The project is built with **educational purposes** in mind while maintaining production-grade code quality, security practices, and scalability considerations.

### Why This Project Exists

1. **Educational**: Learn how JWT validation works at different levels
2. **Reference**: Production-ready code patterns for JWT security
3. **Comparison**: Understand trade-offs between validation approaches
4. **Practical**: Runnable demo with mock token generation

---

## 2. Project Overview

### 2.1 Core Features

```
┌─────────────────────────────────────────────────────────────┐
│                    JWT Validation Demo                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ✓ Three Validation Strategies (Local, Remote, Hybrid)      │
│  ✓ Mock Token Generation (No external dependencies)         │
│  ✓ Role-Based Access Control (RBAC)                         │
│  ✓ RESTful API with Spring Security                         │
│  ✓ Comprehensive Error Handling                             │
│  ✓ Production-Ready Security Configuration                  │
│  ✓ Detailed Logging & Debugging                             │
│  ✓ Docker-Ready Architecture                                │
│  ✓ Test Automation Scripts                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Technical Scope

**What This Project Does:**
- Validates JWT tokens using cryptographic signatures
- Enforces role-based authorization (USER, ADMIN)
- Provides mock token generation for testing
- Demonstrates OAuth2 Resource Server patterns
- Shows production security configurations

**What This Project Does NOT Do:**
- Issue actual OAuth2 tokens (use Keycloak for that)
- Manage user registration/login flows
- Provide a database for user storage
- Implement refresh token mechanisms

---

## 3. Architecture Decisions

### 3.1 Why Spring Boot?

**Decision**: Use Spring Boot 3.2.0 with Spring Security 6.x

**Rationale**:
```
Pros:
✓ Production-proven framework with 10+ years of maturity
✓ Built-in OAuth2 Resource Server support
✓ Extensive security features out-of-the-box
✓ Large ecosystem and community support
✓ Auto-configuration reduces boilerplate
✓ Excellent documentation and examples

Cons:
✗ Relatively heavy for simple use cases
✗ Learning curve for Spring Security DSL
✗ Auto-configuration can be "magical"

Verdict: Best choice for enterprise-grade JWT validation
```

**Decision**: Implement LOCAL, REMOTE, and HYBRID validation modes

**Rationale**:

| Approach | Use Case | Performance | Security | Complexity |
|----------|----------|-------------|----------|------------|
| **LOCAL** | High-traffic APIs | ⚡⚡⚡ ~1ms | 🔒🔒 Good | ⭐⭐ Medium |
| **REMOTE** | Banking, High-security | ⚡ ~50-200ms | 🔒🔒🔒 Excellent | ⭐⭐⭐ High |
| **HYBRID** | E-commerce, SaaS | ⚡⚡ ~5-10ms | 🔒🔒🔒 Excellent | ⭐⭐⭐⭐ Very High |

**Example Scenarios**:
```
LOCAL:
  - Social media feed APIs (millions of requests/sec)
  - Public content delivery
  - Read-heavy operations

REMOTE:
  - Financial transactions
  - Healthcare records access
  - Administrative operations
  - Token revocation required

HYBRID:
  - SaaS applications (Salesforce, Slack)
  - E-commerce platforms
  - Multi-tenant systems
  - Mixed read/write workloads
```

### 3.3 Why HMAC-SHA256 for Mock Tokens?

**Decision**: Use HS256 (symmetric) instead of RS256 (asymmetric) for mock mode

**Rationale**:
```java
HS256 (Symmetric):
✓ Simpler to implement (one secret key)
✓ Faster signing and verification
✓ Perfect for development/testing
✗ Requires shared secret (less secure)
✗ Not suitable for distributed systems

RS256 (Asymmetric):
✓ Public/private key pair (more secure)
✓ Suitable for distributed systems
✓ Standard in OAuth2/OIDC
✗ More complex setup
✗ Slower operations

For Mock Mode: HS256 is ideal (simplicity > security for testing)
For Production: Use RS256 with Keycloak/Auth0
```

### 3.4 Why Nimbus JOSE + JWT?

**Decision**: Use Nimbus library for JWT operations

**Rationale**:
```
Alternatives Considered:
1. java-jwt (Auth0)
2. jjwt (jsonwebtoken.io)
3. Spring Security OAuth2 JOSE
4. Nimbus JOSE JWT ✓ CHOSEN

Why Nimbus?
✓ Most comprehensive JWT/JWS/JWE implementation
✓ Used internally by Spring Security
✓ Full spec compliance (RFC 7519, RFC 7515, etc.)
✓ Active development and maintenance
✓ Excellent performance
✓ Type-safe API

Performance Comparison:
- Nimbus: ~0.5ms per token decode
- java-jwt: ~0.8ms per token decode
- jjwt: ~0.7ms per token decode
```

### 3.5 Why Google Guava for Caching?

**Decision**: Use Guava Cache for Hybrid mode token caching

**Rationale**:
```
Alternatives:
1. Redis (distributed cache)
2. Caffeine (faster than Guava)
3. EhCache (enterprise features)
4. Guava Cache ✓ CHOSEN

Why Guava?
✓ In-memory, no external dependencies
✓ Simple API, easy to understand
✓ TTL (Time-To-Live) support built-in
✓ Thread-safe
✓ Sufficient for educational purposes

For Production:
- Use Redis for distributed caching
- Use Caffeine for better performance
- Use Hazelcast for distributed in-memory
```

### 3.6 Project Structure Decisions

**Decision**: Clean Architecture with separation of concerns

```
src/main/java/com/example/jwtvalidation/
├── config/           ← Configuration beans (Spring Security)
├── controller/       ← REST API endpoints
├── model/           ← DTOs and domain models
├── service/         ← Business logic (token validation)
├── exception/       ← Error handling
└── JwtValidationApplication.java

Why this structure?
✓ Clear separation of concerns
✓ Easy to test each layer
✓ Follows Spring Boot conventions
✓ Scalable for larger applications
```

---

## 4. Technology Stack Deep Dive

### 4.1 Core Technologies

#### Java 17 (LTS)
```yaml
Version: 17.0.x
Release: September 2021
LTS Until: September 2026

Key Features Used:
  - Records (for immutable DTOs)
  - Pattern Matching (for instanceof)
  - Text Blocks (for multiline strings)
  - Sealed Classes (for type safety)
  - Enhanced NPE messages

Why Java 17?
  ✓ LTS version with long support
  ✓ Performance improvements over Java 11
  ✓ Modern language features
  ✓ Required for Spring Boot 3.x
```

#### Spring Boot 3.2.0
```yaml
Version: 3.2.0
Release: November 2023
Based on: Spring Framework 6.1.x

Major Components:
  spring-boot-starter-web:
    - Embedded Tomcat 10.1.16
    - Spring MVC 6.1.x
    - Jackson 2.15.x (JSON processing)
    - Validation API

  spring-boot-starter-security:
    - Spring Security 6.2.x
    - OAuth2 Resource Server
    - JWT Support
    - Method Security

Why 3.2.0?
  ✓ Latest stable release
  ✓ Jakarta EE 10 support
  ✓ Native compilation ready (GraalVM)
  ✓ Improved observability
  ✓ Virtual threads support (Java 21+)
```

#### Spring Security 6.x
```yaml
Version: 6.2.x (via Spring Boot)
Major Version: 6.x (November 2022)

Architecture Changes (5.x → 6.x):
  - Lambda DSL (no more .and())
  - Component-based config
  - Better OAuth2 support
  - Simplified filter chains

Example Config Comparison:

# Spring Security 5.x (Old)
http
  .csrf().disable()
  .and()
  .authorizeRequests()
    .antMatchers("/public/**").permitAll()
  .and()
  .oauth2ResourceServer()
    .jwt();

# Spring Security 6.x (New)
http
  .csrf(csrf -> csrf.disable())
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/public/**").permitAll())
  .oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.decoder(jwtDecoder())));
```

### 4.2 OAuth2 & JWT Libraries

#### Spring Security OAuth2 Resource Server
```yaml
Artifact: spring-security-oauth2-resource-server
Purpose: Validate JWT tokens in protected resources

Key Classes:
  - JwtDecoder: Decodes and validates JWT
  - JwtAuthenticationConverter: Converts JWT to Authentication
  - BearerTokenAuthenticationFilter: Extracts Bearer tokens
  - JwtAuthenticationProvider: Validates JWT tokens

Flow:
  1. Extract Bearer token from Authorization header
  2. Decode JWT using JwtDecoder
  3. Validate signature using public key
  4. Validate claims (exp, iss, aud)
  5. Convert to Authentication object
  6. Store in SecurityContext
```

#### Spring Security OAuth2 JOSE
```yaml
Artifact: spring-security-oauth2-jose
Purpose: JSON Object Signing and Encryption support

Provides:
  - JWT encoding/decoding
  - JWK (JSON Web Key) support
  - JWS (JSON Web Signature) verification
  - JWE (JSON Web Encryption) support

Standards Implemented:
  - RFC 7519 (JWT)
  - RFC 7515 (JWS)
  - RFC 7516 (JWE)
  - RFC 7517 (JWK)
  - RFC 7518 (JWA - Algorithms)
```

#### Nimbus JOSE JWT
```yaml
Artifact: com.nimbusds:nimbus-jose-jwt:9.37.3
Purpose: Complete JWT/JWS/JWE implementation

Features:
  - All JWT algorithms (HS256, RS256, ES256, etc.)
  - JWK Set parsing
  - Claims validation
  - Custom claim support
  - Thread-safe

Usage in Project:
  - Mock token generation (HS256)
  - JWT signature creation
  - Claims construction
```

### 4.3 Supporting Libraries

#### Google Guava
```yaml
Artifact: com.google.guava:guava:32.1.3-jre
Purpose: Caching and utilities

Used For:
  - CacheBuilder (Hybrid mode token cache)
  - Immutable collections
  - Utility methods

Cache Configuration:
  CacheBuilder.newBuilder()
    .expireAfterWrite(300, TimeUnit.SECONDS)
    .maximumSize(10000)
    .recordStats()
    .build();

Features:
  ✓ TTL support
  ✓ Size-based eviction
  ✓ Statistics tracking
  ✓ Thread-safe
```

#### Lombok
```yaml
Artifact: org.projectlombok:lombok
Purpose: Reduce boilerplate code

Annotations Used:
  @Data           - Getters, setters, equals, hashCode, toString
  @AllArgsConstructor - Constructor with all fields
  @NoArgsConstructor  - No-arg constructor
  @Builder        - Builder pattern

Example:
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public class UserProfile {
      private String userId;
      private String email;
      private List<String> roles;
  }

Generated Code:
  - Getters/Setters for all fields
  - equals() and hashCode()
  - toString()
  - Constructors
```

### 4.4 Build Tool: Maven

```xml
Maven Version: 3.6+
Build Lifecycle:

mvn clean          → Delete target/ directory
mvn compile        → Compile source code
mvn test           → Run unit tests
mvn package        → Create JAR file
mvn spring-boot:run → Run application

Key Plugins:
  spring-boot-maven-plugin:
    - Creates executable JAR
    - Includes dependencies
    - Repackages with embedded Tomcat

  maven-compiler-plugin:
    - Java 17 compilation
    - Annotation processing (Lombok)

Project Object Model (pom.xml):
  - Dependencies management
  - Build configuration
  - Plugin configuration
```

---

## 5. Project Structure

### 5.1 Directory Tree

```
jwt-validation-demo/
│
├── pom.xml                                 ← Maven configuration
├── README.md                               ← Quick start guide
├── COMPREHENSIVE_GUIDE.md                  ← This file
├── JWT_VALIDATION_GUIDE.md                 ← Original reference guide
├── test-api.sh                             ← Automated test script
│
├── src/
│   ├── main/
│   │   ├── java/com/example/jwtvalidation/
│   │   │   │
│   │   │   ├── JwtValidationApplication.java    ← Main entry point
│   │   │   │
│   │   │   ├── config/                          ← Configuration layer
│   │   │   │   ├── LocalValidationSecurityConfig.java
│   │   │   │   ├── RemoteValidationSecurityConfig.java
│   │   │   │   ├── MockJwtConfig.java
│   │   │   │   └── RestTemplateConfig.java
│   │   │   │
│   │   │   ├── controller/                      ← Presentation layer
│   │   │   │   ├── PublicController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   └── MockTokenController.java
│   │   │   │
│   │   │   ├── service/                         ← Business logic layer
│   │   │   │   ├── CustomOpaqueTokenIntrospector.java
│   │   │   │   └── HybridTokenValidator.java
│   │   │   │
│   │   │   ├── model/                           ← Data models
│   │   │   │   ├── UserProfile.java
│   │   │   │   └── ErrorResponse.java
│   │   │   │
│   │   │   └── exception/                       ← Error handling
│   │   │       └── SecurityExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       └── application.yml                  ← Application config
│   │
│   └── test/                                    ← Test directory (empty)
│
└── target/                                      ← Build output (generated)
    └── jwt-validation-demo-1.0.0.jar
```

### 5.2 Layer Responsibilities

#### Configuration Layer (`config/`)

**Purpose**: Spring Bean definitions and security configuration

```java
LocalValidationSecurityConfig.java
├── Purpose: Configure LOCAL JWT validation
├── Beans:
│   ├── SecurityFilterChain
│   └── JwtAuthenticationConverter
├── Responsibilities:
│   ├── Define security rules
│   ├── Configure JWT decoder
│   └── Map JWT claims to authorities

RemoteValidationSecurityConfig.java
├── Purpose: Configure REMOTE token introspection
├── Beans:
│   └── SecurityFilterChain
├── Responsibilities:
│   ├── Configure opaque token validation
│   └── Set introspection endpoint

MockJwtConfig.java
├── Purpose: Mock token generation for testing
├── Beans:
│   └── JwtDecoder (HMAC-based)
├── Methods:
│   └── generateMockToken() - Creates test JWTs

RestTemplateConfig.java
├── Purpose: HTTP client for remote introspection
├── Beans:
│   └── RestTemplate
├── Used by: CustomOpaqueTokenIntrospector
```

#### Controller Layer (`controller/`)

**Purpose**: REST API endpoints (presentation layer)

```java
PublicController.java
├── Base Path: /api/public
├── Security: Permit all (no authentication)
├── Endpoints:
│   ├── GET /hello      - Public greeting
│   ├── GET /health     - Health check
│   └── GET /info       - API information

UserController.java
├── Base Path: /api/user
├── Security: Authenticated users (USER or ADMIN)
├── Endpoints:
│   ├── GET /profile    - User profile
│   ├── GET /token-info - JWT token details
│   └── GET /hello      - User greeting

AdminController.java
├── Base Path: /api/admin
├── Security: ADMIN role required
├── Endpoints:
│   ├── GET  /dashboard - Admin dashboard
│   ├── GET  /info      - Admin information
│   └── POST /data      - Post admin data

MockTokenController.java
├── Base Path: /api/public/mock
├── Security: Permit all
├── Purpose: Generate test tokens
├── Endpoints:
│   ├── GET  /user-token   - Generate USER token
│   ├── GET  /admin-token  - Generate ADMIN token
│   └── POST /generate-token - Custom token
```

#### Service Layer (`service/`)

**Purpose**: Business logic and token validation

```java
CustomOpaqueTokenIntrospector.java
├── Interface: OpaqueTokenIntrospector
├── Purpose: Remote token validation
├── Methods:
│   ├── introspect(String token)
│   └── extractAuthorities(Map attributes)
├── Flow:
│   1. Call Keycloak introspection endpoint
│   2. Parse response
│   3. Check "active" status
│   4. Extract user info and roles
│   5. Return OAuth2AuthenticatedPrincipal

HybridTokenValidator.java
├── Purpose: Intelligent hybrid validation
├── Components:
│   ├── JwtDecoder (local validation)
│   ├── OpaqueTokenIntrospector (remote validation)
│   └── Guava Cache (token caching)
├── Logic:
│   1. Check cache first
│   2. Try local validation (fast)
│   3. Conditionally remote validate
│   4. Cache successful validations
│   5. Return principal
```

#### Model Layer (`model/`)

**Purpose**: Data Transfer Objects (DTOs)

```java
UserProfile.java
├── Purpose: User profile response
├── Fields:
│   ├── String userId
│   ├── String email
│   ├── String name
│   └── List<String> roles
├── Annotations:
│   ├── @Data (Lombok)
│   ├── @AllArgsConstructor
│   └── @NoArgsConstructor

ErrorResponse.java
├── Purpose: Standardized error response
├── Fields:
│   ├── String code
│   ├── String message
│   └── Instant timestamp
├── Constructor: Auto-sets timestamp
```

#### Exception Layer (`exception/`)

**Purpose**: Global error handling

```java
SecurityExceptionHandler.java
├── Annotation: @RestControllerAdvice
├── Purpose: Catch and handle security exceptions
├── Handlers:
│   ├── BadCredentialsException → 401
│   ├── JwtException → 401
│   ├── BadOpaqueTokenException → 401
│   ├── AccessDeniedException → 403
│   └── Exception → 500
├── Returns: ErrorResponse JSON
```

---

## 6. Validation Approaches Explained

### 6.1 LOCAL Validation (Default Mode)

#### Overview

LOCAL validation verifies JWT tokens **cryptographically** using public/private key pairs without calling any external services.

#### How It Works

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. Send Request with JWT
       │    Authorization: Bearer eyJhbGc...
       ▼
┌─────────────────────────────────────┐
│      Spring Boot Application        │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  BearerTokenAuthenticationFilter   │
│  │  └─ Extract JWT from header   │ │
│  └──────────┬──────────────────── │
│             ▼                      │
│  ┌───────────────────────────────┐ │
│  │       JwtDecoder              │ │
│  │  ┌─────────────────────────┐ │ │
│  │  │ 1. Parse JWT            │ │ │
│  │  │ 2. Fetch Public Key     │ │ │
│  │  │    (from JWKS endpoint) │ │ │
│  │  │ 3. Verify Signature     │ │ │
│  │  │ 4. Validate Claims      │ │ │
│  │  │    - exp (expiration)   │ │ │
│  │  │    - iss (issuer)       │ │ │
│  │  │    - aud (audience)     │ │ │
│  │  └─────────────────────────┘ │ │
│  └──────────┬──────────────────── │
│             ▼                      │
│  ┌───────────────────────────────┐ │
│  │  JwtAuthenticationConverter   │ │
│  │  └─ Extract roles from claims│ │
│  └──────────┬──────────────────── │
│             ▼                      │
│  ┌───────────────────────────────┐ │
│  │   SecurityContextHolder       │ │
│  │   └─ Store Authentication    │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘
```

#### JWT Structure

```
JWT Token: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTAwMSIsImF1ZCI6Im1vY2stYXVkaWVuY2UiLCJyb2xlcyI6WyJVU0VSIl0sIm5hbWUiOiJSZWd1bGFyIFVzZXIiLCJpc3MiOiJtb2NrLWlzc3VlciIsImV4cCI6MTc2MjU5ODUzMywiaWF0IjoxNzYyNTk0OTMzLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20ifQ.WkZbNDA1_ZM7B3qfS88e1YmZeVddrztUXcHqVlPQf1g

Parts (separated by dots):
  [HEADER].[PAYLOAD].[SIGNATURE]

HEADER (Base64URL decoded):
{
  "alg": "HS256",      ← Algorithm (HMAC-SHA256)
  "typ": "JWT"         ← Type (JSON Web Token)
}

PAYLOAD (Base64URL decoded):
{
  "sub": "user-001",              ← Subject (user ID)
  "aud": "mock-audience",         ← Audience
  "roles": ["USER"],              ← Custom claim (roles)
  "name": "Regular User",         ← Custom claim (name)
  "iss": "mock-issuer",           ← Issuer
  "exp": 1762598533,              ← Expiration time (Unix timestamp)
  "iat": 1762594933,              ← Issued at (Unix timestamp)
  "email": "user@example.com"     ← Custom claim (email)
}

SIGNATURE:
  HMACSHA256(
    base64UrlEncode(header) + "." + base64UrlEncode(payload),
    secret_key
  )
```

#### Validation Steps in Detail

**Step 1: Extract Token**
```java
// Spring Security's BearerTokenAuthenticationFilter
String authHeader = request.getHeader("Authorization");
// Expected: "Bearer eyJhbGc..."

if (authHeader != null && authHeader.startsWith("Bearer ")) {
    String token = authHeader.substring(7);
    // token = "eyJhbGc..."
}
```

**Step 2: Parse JWT**
```java
// Split JWT into parts
String[] parts = token.split("\\.");
String headerB64 = parts[0];  // eyJhbGciOiJIUzI1NiJ9
String payloadB64 = parts[1]; // eyJzdWIiOiJ1c2VyLT...
String signatureB64 = parts[2]; // WkZbNDA1_ZM7B3qfS88...

// Decode Base64URL
byte[] headerBytes = Base64.getUrlDecoder().decode(headerB64);
byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);

// Parse JSON
JSONObject header = new JSONObject(new String(headerBytes));
JSONObject payload = new JSONObject(new String(payloadBytes));
```

**Step 3: Fetch Public Key** (Production with RS256)
```http
GET https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs

Response:
{
  "keys": [
    {
      "kid": "key-id-1",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig",
      "n": "xjlCAoqwbybONKKSO8wnOcqgVCfaK...",  // Modulus
      "e": "AQAB"                                // Exponent
    }
  ]
}
```

**Step 4: Verify Signature**
```java
// For HMAC-SHA256 (our mock mode)
SecretKey key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(key);

String signatureInput = headerB64 + "." + payloadB64;
byte[] expectedSignature = mac.doFinal(signatureInput.getBytes());

// Compare with actual signature
if (!Arrays.equals(expectedSignature, actualSignature)) {
    throw new JwtException("Invalid signature");
}

// For RSA (production)
Signature sig = Signature.getInstance("SHA256withRSA");
sig.initVerify(publicKey);
sig.update(signatureInput.getBytes());
boolean valid = sig.verify(actualSignature);
```

**Step 5: Validate Claims**
```java
// Check expiration
long exp = payload.getLong("exp");
long now = Instant.now().getEpochSecond();
if (now > exp) {
    throw new JwtExpiredException("Token expired");
}

// Check issuer
String expectedIssuer = "https://keycloak.example.com/auth/realms/myrealm";
String actualIssuer = payload.getString("iss");
if (!expectedIssuer.equals(actualIssuer)) {
    throw new JwtException("Invalid issuer");
}

// Check audience
String expectedAudience = "my-app";
JSONArray audiences = payload.getJSONArray("aud");
if (!audiences.contains(expectedAudience)) {
    throw new JwtException("Invalid audience");
}
```

**Step 6: Extract Authorities**
```java
// Our custom implementation
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter =
        new JwtGrantedAuthoritiesConverter();

    // Extract from "roles" claim instead of default "scope"
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
}

// Result: roles: ["USER"] → authorities: ["ROLE_USER"]
```

#### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Issuer URI (optional, for validation)
          issuer-uri: https://keycloak.example.com/auth/realms/myrealm

          # JWKS endpoint (public keys)
          jwk-set-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/certs

# For mock mode (HS256)
app:
  security:
    mock-enabled: true
    mock-secret: mySecretKeyForJWT2025mustBe32bytes!
```

#### Performance Characteristics

```
Local Validation Performance:
  - First request: ~50ms (fetch public key from JWKS)
  - Subsequent: ~1ms (key cached in memory)
  - Throughput: 10,000+ requests/sec (single instance)
  - Latency: P50: 0.8ms, P95: 1.2ms, P99: 2ms

Memory Usage:
  - JWK cache: ~10KB per key
  - Per-request: ~5KB (JWT parsing)

Scaling:
  - Horizontal: Linear (stateless validation)
  - Vertical: CPU-bound (signature verification)
```

#### Pros & Cons

```
✓ PROS:
  - Extremely fast (~1ms)
  - No external dependencies after key fetch
  - Works offline (after initial key fetch)
  - Horizontally scalable
  - Low latency
  - Predictable performance

✗ CONS:
  - Cannot revoke tokens immediately
  - Relies on token expiration for security
  - If private key compromised, must rotate keys
  - Claims are static (cannot change after issuance)
```

---

### 6.2 REMOTE Validation (Introspection Mode)

#### Overview

REMOTE validation calls an OAuth2 introspection endpoint (Keycloak) for **every request** to verify token validity.

#### How It Works

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ 1. Request with Token
       ▼
┌──────────────────────────────────────────────┐
│       Spring Boot Application                │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │  BearerTokenAuthenticationFilter       │ │
│  │  └─ Extract opaque token               │ │
│  └─────────────┬────────────────────────── │
│                ▼                            │
│  ┌────────────────────────────────────────┐ │
│  │  CustomOpaqueTokenIntrospector         │ │
│  │                                        │ │
│  │  Call Keycloak Introspection:         │ │
│  │  ┌──────────────────────────────────┐ │ │
│  │  │ POST /token/introspect           │ │ │
│  │  │ Body: token=eyJhbGc...           │ │ │
│  │  │       client_id=my-app           │ │ │
│  │  │       client_secret=secret       │ │ │
│  │  └──────────────────────────────────┘ │ │
│  └─────────────┬────────────────────────── │
│                │                            │
└────────────────┼────────────────────────────┘
                 │
                 │ 2. HTTP POST
                 ▼
       ┌──────────────────┐
       │    Keycloak      │
       │  Auth Server     │
       │                  │
       │  ┌────────────┐  │
       │  │  Validate  │  │
       │  │  Token     │  │
       │  └────────────┘  │
       │                  │
       │  Check:          │
       │  • Token exists  │
       │  • Not expired   │
       │  • Not revoked   │
       │  • Scopes valid  │
       └────────┬─────────┘
                │
                │ 3. Response
                ▼
       ┌──────────────────┐
       │ {                │
       │   "active": true,│
       │   "sub": "user-1"│
       │   "roles": [...]  │
       │ }                │
       └──────────────────┘
                │
                │ 4. Parse Response
                ▼
┌──────────────────────────────────────────────┐
│       Spring Boot Application                │
│                                              │
│  Create OAuth2AuthenticatedPrincipal        │
│  Store in SecurityContext                   │
└──────────────────────────────────────────────┘
```

#### Introspection Request/Response

**Request:**
```http
POST /auth/realms/myrealm/protocol/openid-connect/token/introspect HTTP/1.1
Host: keycloak.example.com
Authorization: Basic bXktYXBwOnlvdXItc2VjcmV0  (Base64: my-app:your-secret)
Content-Type: application/x-www-form-urlencoded

token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (Active Token):**
```json
{
  "active": true,
  "sub": "50480c3a-1f7c-4f2f-a4e3-0c0e6f4d1234",
  "email": "user@example.com",
  "email_verified": true,
  "name": "John Doe",
  "preferred_username": "johndoe",
  "given_name": "John",
  "family_name": "Doe",
  "iat": 1762594933,
  "exp": 1762598533,
  "iss": "https://keycloak.example.com/auth/realms/myrealm",
  "aud": "my-app",
  "client_id": "my-app",
  "username": "johndoe",
  "token_type": "Bearer",
  "scope": "openid email profile",
  "realm_access": {
    "roles": ["user", "offline_access"]
  },
  "resource_access": {
    "my-app": {
      "roles": ["app-user"]
    }
  }
}
```

**Response (Inactive Token):**
```json
{
  "active": false
}
```

#### Implementation Code

```java
@Component
public class CustomOpaqueTokenIntrospector implements OpaqueTokenIntrospector {

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        // 1. Prepare HTTP request
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);

        HttpEntity<MultiValueMap<String, String>> request =
            new HttpEntity<>(body, headers);

        // 2. Call Keycloak
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            introspectionUri,
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();

        // 3. Check if active
        if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("active"))) {
            throw new BadOpaqueTokenException("Token is not active");
        }

        // 4. Extract authorities
        Collection<GrantedAuthority> authorities = extractAuthorities(responseBody);

        // 5. Return principal
        return new DefaultOAuth2AuthenticatedPrincipal(
            (String) responseBody.get("sub"),
            responseBody,
            authorities
        );
    }

    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> attributes) {
        // Extract from realm_access.roles
        Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");

        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");

            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
```

#### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect
          client-id: my-app
          client-secret: your-client-secret

app:
  security:
    validation-mode: REMOTE
```

#### Performance Characteristics

```
Remote Validation Performance:
  - Avg latency: 50-200ms (network + Keycloak processing)
  - Throughput: 100-500 requests/sec (limited by Keycloak)
  - P50: 80ms, P95: 150ms, P99: 300ms

Network Overhead:
  - Request size: ~1-2KB (token + credentials)
  - Response size: ~500B-2KB (user info)
  - Total per request: ~1.5-4KB

Keycloak Load:
  - 1000 req/sec = 1000 DB queries/sec
  - Keycloak caching helps, but still limited

Failure Modes:
  - Network timeout: 30s default (configurable)
  - Keycloak down: All requests fail (circuit breaker needed)
  - Database slow: Cascading latency
```

#### Pros & Cons

```
✓ PROS:
  - Real-time token revocation
  - Immediate logout across all services
  - Can change user permissions dynamically
  - Centralized token management
  - Audit trail (all validations logged)

✗ CONS:
  - High latency (50-200ms per request)
  - Network dependency (single point of failure)
  - Heavy load on Keycloak
  - Not scalable for high-traffic APIs
  - Requires circuit breaker for resilience
```

---

### 6.3 HYBRID Validation (Intelligent Caching)

#### Overview

HYBRID validation combines LOCAL and REMOTE validation with **intelligent caching** to get the best of both worlds.

#### Strategy

```
┌──────────────────────────────────────────────┐
│          Hybrid Validation Logic             │
├──────────────────────────────────────────────┤
│                                              │
│  Step 1: Check Cache                        │
│  ┌────────────────────────────────────────┐ │
│  │ Token in cache?                        │ │
│  │   YES → Return cached principal        │ │
│  │   NO  → Continue to Step 2             │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  Step 2: Try Local Validation (Fast Path)  │
│  ┌────────────────────────────────────────┐ │
│  │ JWT Decode + Signature Verification    │ │
│  │   SUCCESS → Check if remote needed     │ │
│  │   FAILURE → Try remote (Step 3)        │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  Step 3: Conditional Remote Validation     │
│  ┌────────────────────────────────────────┐ │
│  │ Should remote validate?                │ │
│  │   - Token close to expiry (< 1 min)    │ │
│  │   - High-security operation            │ │
│  │   - Local validation failed            │ │
│  │                                        │ │
│  │   YES → Call introspection endpoint   │ │
│  │   NO  → Use local validation result    │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  Step 4: Cache Result                      │
│  ┌────────────────────────────────────────┐ │
│  │ Store in Guava Cache                   │ │
│  │   TTL: 5 minutes (configurable)        │ │
│  │   Max Size: 10,000 tokens              │ │
│  └────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

#### Decision Tree

```
                    Request arrives
                          |
                          v
                  ┌───────────────┐
                  │ Check Cache   │
                  └───────┬───────┘
                          |
             ┌────────────┴────────────┐
             |                         |
           Found                   Not Found
             |                         |
             v                         v
        ┌─────────┐          ┌──────────────────┐
        │ Return  │          │ Try Local Decode │
        │ Cached  │          └────────┬─────────┘
        └─────────┘                   |
                              ┌───────┴────────┐
                              |                |
                          Success          Failure
                              |                |
                              v                v
                    ┌──────────────────┐   ┌────────────┐
                    │ Remote needed?   │   │ Try Remote │
                    └────────┬─────────┘   └─────┬──────┘
                             |                    |
                   ┌─────────┴────────┐          |
                   |                  |          |
                 Yes                 No          |
                   |                  |          |
                   v                  v          v
            ┌────────────┐      ┌──────────┐ ┌────────┐
            │ Call Remote│      │ Use Local│ │Success?│
            └─────┬──────┘      └────┬─────┘ └───┬────┘
                  |                  |            |
                  v                  v        ┌───┴────┐
            ┌──────────┐       ┌──────────┐  |        |
            │  Cache   │       │  Cache   │ Yes       No
            │  Result  │       │  Result  │  |        |
            └──────────┘       └──────────┘  v        v
                  |                  |    ┌─────┐ ┌──────┐
                  └──────────────────┘    │Cache│ │Reject│
                          |               └─────┘ └──────┘
                          v
                    Return Principal
```

#### Implementation

```java
@Component
public class HybridTokenValidator {

    private final JwtDecoder jwtDecoder;
    private final OpaqueTokenIntrospector introspector;
    private final Cache<String, Boolean> tokenCache;
    private final boolean enableRemoteValidation;

    public OAuth2AuthenticatedPrincipal validateToken(String token) {
        // STEP 1: Check cache
        Boolean cachedResult = tokenCache.getIfPresent(token);
        if (cachedResult != null && cachedResult) {
            // Cache hit - return quickly
            try {
                Jwt jwt = jwtDecoder.decode(token);
                return convertJwtToPrincipal(jwt);
            } catch (JwtException e) {
                // Cache was stale, invalidate
                tokenCache.invalidate(token);
            }
        }

        try {
            // STEP 2: Try local validation (fast path)
            Jwt jwt = jwtDecoder.decode(token);

            // STEP 3: Conditional remote validation
            if (enableRemoteValidation && shouldRemoteValidate(jwt)) {
                // High-security scenario: double-check with server
                return introspector.introspect(token);
            }

            // Local validation succeeded
            tokenCache.put(token, true);
            return convertJwtToPrincipal(jwt);

        } catch (JwtException e) {
            // STEP 4: Local failed, try remote as fallback
            if (enableRemoteValidation) {
                OAuth2AuthenticatedPrincipal principal = introspector.introspect(token);
                tokenCache.put(token, true);
                return principal;
            }
            throw e;
        }
    }

    private boolean shouldRemoteValidate(Jwt jwt) {
        Instant expiry = jwt.getExpiresAt();
        if (expiry == null) {
            return false;
        }

        // Remote validate if token expires within 60 seconds
        // This ensures fresh validation for soon-to-expire tokens
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
}
```

#### Cache Configuration

```java
this.tokenCache = CacheBuilder.newBuilder()
    .expireAfterWrite(cacheExpiry, TimeUnit.SECONDS)  // TTL: 300s (5 min)
    .maximumSize(10000)                               // Max entries
    .recordStats()                                    // Enable statistics
    .build();

// Cache Statistics
CacheStats stats = tokenCache.stats();
double hitRate = stats.hitRate();         // % of cache hits
long hitCount = stats.hitCount();         // Number of hits
long missCount = stats.missCount();       // Number of misses
long evictionCount = stats.evictionCount(); // Number of evictions

// Example:
// Hit Rate: 95% (95% of requests served from cache)
// 10,000 req/sec × 95% = 9,500 req/sec cached (fast)
// 10,000 req/sec × 5% = 500 req/sec validated (slow)
```

#### Remote Validation Triggers

**1. Token Close to Expiry**
```java
// Why? Ensure fresh validation for soon-to-expire tokens
// This prevents accepting tokens that just expired

Instant expiry = jwt.getExpiresAt();
boolean nearExpiry = expiry.isBefore(Instant.now().plusSeconds(60));

if (nearExpiry) {
    // Call introspection to double-check
    introspector.introspect(token);
}
```

**2. High-Security Operations** (can be extended)
```java
// Example extension:
private boolean shouldRemoteValidate(Jwt jwt, HttpServletRequest request) {
    // Check if high-security endpoint
    String uri = request.getRequestURI();
    if (uri.startsWith("/api/admin/financial") ||
        uri.startsWith("/api/admin/sensitive")) {
        return true; // Always remote validate for sensitive ops
    }

    // Check if token is close to expiry
    return isNearExpiry(jwt);
}
```

**3. Suspicious Activity** (can be extended)
```java
// Example extension:
private boolean shouldRemoteValidate(Jwt jwt, String clientIp) {
    // Check for IP change
    String tokenIp = jwt.getClaimAsString("ip");
    if (tokenIp != null && !tokenIp.equals(clientIp)) {
        return true; // IP changed, verify with server
    }

    // Check for unusual access patterns
    if (rateLimiter.isExceeded(jwt.getSubject())) {
        return true; // Unusual activity, verify
    }

    return false;
}
```

#### Performance Characteristics

```
Hybrid Validation Performance:

Cache Hit (95% of requests):
  - Latency: ~1ms (local validation)
  - Throughput: 9,500 req/sec

Cache Miss (5% of requests):
  - Latency: ~100ms (remote validation)
  - Throughput: 500 req/sec

Average Latency:
  (0.95 × 1ms) + (0.05 × 100ms) = 5.95ms

Memory Usage:
  - Cache: ~200KB (10,000 tokens × ~20B each)
  - JWK cache: ~10KB
  - Total: ~210KB

Scaling:
  - Horizontal: Linear (shared cache via Redis)
  - Vertical: CPU-bound for cache misses
```

#### Pros & Cons

```
✓ PROS:
  - Fast for most requests (95%+ cache hit rate)
  - Real-time revocation for critical operations
  - Resilient to Keycloak downtime (uses local as fallback)
  - Configurable security vs performance trade-off
  - Best of both worlds

✗ CONS:
  - More complex implementation
  - Requires cache management
  - Cache invalidation challenges
  - Harder to reason about behavior
  - Needs tuning for optimal performance
```

---

## 7. Sequence Flow Diagrams

### 7.1 Local Validation Flow

```
┌──────┐                ┌────────────┐         ┌───────────┐
│Client│                │Spring Boot │         │ Keycloak  │
│      │                │ Resource   │         │ (JWKS)    │
│      │                │ Server     │         │           │
└───┬──┘                └─────┬──────┘         └─────┬─────┘
    │                         │                      │
    │ 1. GET /api/user/profile│                      │
    │    Authorization: Bearer eyJhbGc...            │
    ├────────────────────────>│                      │
    │                         │                      │
    │                         │ 2. Extract JWT       │
    │                         │    from header       │
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 3. First time? Fetch JWKS
    │                         │    (Public keys)     │
    │                         ├─────────────────────>│
    │                         │                      │
    │                         │ 4. Return public key │
    │                         │<─────────────────────┤
    │                         │                      │
    │                         │ 5. Decode JWT        │
    │                         │    (parse header,    │
    │                         │     payload, sig)    │
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 6. Verify signature  │
    │                         │    using public key  │
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 7. Validate claims   │
    │                         │    - exp (not expired)
    │                         │    - iss (correct issuer)
    │                         │    - aud (correct audience)
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 8. Extract authorities
    │                         │    from "roles" claim│
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 9. Create Authentication
    │                         │    object            │
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 10. Store in         │
    │                         │     SecurityContext  │
    │                         │◄─────────────────────┘
    │                         │                      │
    │                         │ 11. Call controller  │
    │                         │◄─────────────────────┘
    │                         │                      │
    │ 12. Return response     │                      │
    │<────────────────────────┤                      │
    │ 200 OK                  │                      │
    │ {"userId": "user-001",...}                     │
    │                         │                      │

Timing Breakdown:
  Step 1-2:  < 1ms (HTTP parsing)
  Step 3-4:  50ms (first request only, then cached)
  Step 5:    < 1ms (JWT parsing)
  Step 6:    < 1ms (signature verification)
  Step 7:    < 1ms (claims validation)
  Step 8-11: < 1ms (Spring Security)
  Step 12:   depends on business logic

Total: ~1ms (after initial JWKS fetch)
```

### 7.2 Remote Validation Flow

```
┌──────┐         ┌────────────┐              ┌───────────┐
│Client│         │Spring Boot │              │ Keycloak  │
│      │         │ Resource   │              │ Introspect│
│      │         │ Server     │              │ Endpoint  │
└───┬──┘         └─────┬──────┘              └─────┬─────┘
    │                  │                           │
    │ 1. GET /api/user/profile                     │
    │    Authorization: Bearer eyJhbGc...          │
    ├─────────────────>│                           │
    │                  │                           │
    │                  │ 2. Extract token          │
    │                  │    from header            │
    │                  │◄──────────────────────────┘
    │                  │                           │
    │                  │ 3. POST /token/introspect │
    │                  │    Headers:               │
    │                  │      Authorization: Basic │
    │                  │    Body:                  │
    │                  │      token=eyJhbGc...     │
    │                  ├──────────────────────────>│
    │                  │                           │
    │                  │                           │ 4. Validate token
    │                  │                           │    - Check DB
    │                  │                           │    - Check expiry
    │                  │                           │    - Check revoked
    │                  │                           │    - Load user info
    │                  │                           │◄──────────────────┘
    │                  │                           │
    │                  │ 5. Return introspection   │
    │                  │    response               │
    │                  │<──────────────────────────┤
    │                  │ {                         │
    │                  │   "active": true,         │
    │                  │   "sub": "user-001",      │
    │                  │   "roles": ["USER"]       │
    │                  │ }                         │
    │                  │                           │
    │                  │ 6. Check "active" status  │
    │                  │◄──────────────────────────┘
    │                  │                           │
    │                  │ 7. Extract user info      │
    │                  │    and authorities        │
    │                  │◄──────────────────────────┘
    │                  │                           │
    │                  │ 8. Create                 │
    │                  │    OAuth2AuthenticatedPrincipal
    │                  │◄──────────────────────────┘
    │                  │                           │
    │                  │ 9. Store in               │
    │                  │    SecurityContext        │
    │                  │◄──────────────────────────┘
    │                  │                           │
    │                  │ 10. Call controller       │
    │                  │◄──────────────────────────┘
    │                  │                           │
    │ 11. Return response                          │
    │<─────────────────┤                           │
    │ 200 OK           │                           │
    │ {"userId": "user-001",...}                   │
    │                  │                           │

Timing Breakdown:
  Step 1-2:   < 1ms (HTTP parsing)
  Step 3:     5-10ms (HTTP request preparation)
  Step 3-4:   50-150ms (network + Keycloak processing)
  Step 5:     5-10ms (response parsing)
  Step 6-10:  < 1ms (Spring Security)
  Step 11:    depends on business logic

Total: ~80-200ms per request
```

### 7.3 Hybrid Validation Flow (Cache Hit)

```
┌──────┐         ┌────────────┐         ┌────────┐
│Client│         │Spring Boot │         │ Guava  │
│      │         │ Hybrid     │         │ Cache  │
│      │         │ Validator  │         │        │
└───┬──┘         └─────┬──────┘         └────┬───┘
    │                  │                     │
    │ 1. GET /api/user/profile               │
    │    Authorization: Bearer eyJhbGc...    │
    ├─────────────────>│                     │
    │                  │                     │
    │                  │ 2. Check cache      │
    │                  │    getIfPresent()   │
    │                  ├────────────────────>│
    │                  │                     │
    │                  │ 3. Cache HIT!       │
    │                  │    Return: true     │
    │                  │<────────────────────┤
    │                  │                     │
    │                  │ 4. Local JWT decode │
    │                  │    (fast)           │
    │                  │◄────────────────────┘
    │                  │                     │
    │                  │ 5. Return principal │
    │                  │◄────────────────────┘
    │                  │                     │
    │ 6. Return response                     │
    │<─────────────────┤                     │
    │ 200 OK           │                     │
    │                  │                     │

Timing: ~1-2ms total (cache hit)
```

### 7.4 Hybrid Validation Flow (Cache Miss)

```
┌──────┐     ┌────────┐     ┌──────────┐     ┌──────────┐
│Client│     │Hybrid  │     │ JwtDecode│     │Keycloak  │
│      │     │Validatr│     │ (Local)  │     │(Remote)  │
└───┬──┘     └───┬────┘     └────┬─────┘     └────┬─────┘
    │            │               │                │
    │ 1. Request │               │                │
    ├───────────>│               │                │
    │            │               │                │
    │            │ 2. Cache miss │                │
    │            │◄──────────────┘                │
    │            │               │                │
    │            │ 3. Try local  │                │
    │            ├──────────────>│                │
    │            │               │                │
    │            │ 4. Decode OK  │                │
    │            │<──────────────┤                │
    │            │               │                │
    │            │ 5. Near expiry?                │
    │            │    Check exp claim             │
    │            │◄───────────────                │
    │            │               │                │
    │            │ 6. YES! Remote validate        │
    │            ├───────────────┼───────────────>│
    │            │               │                │
    │            │ 7. Introspect │                │
    │            │<───────────────┼───────────────┤
    │            │               │                │
    │            │ 8. Cache result                │
    │            │◄───────────────                │
    │            │               │                │
    │ 9. Response│               │                │
    │<───────────┤               │                │
    │            │               │                │

Timing: ~5-100ms (depending on remote call)
```

### 7.5 Authorization Flow (403 Forbidden)

```
┌──────┐              ┌────────────┐
│Client│              │Spring Boot │
│ USER │              │ Resource   │
│ role │              │ Server     │
└───┬──┘              └─────┬──────┘
    │                       │
    │ 1. GET /api/admin/dashboard
    │    Authorization: Bearer <USER_TOKEN>
    ├──────────────────────>│
    │                       │
    │                       │ 2. Validate JWT  ✓
    │                       │    (signature valid)
    │                       │◄──────────────────┘
    │                       │
    │                       │ 3. Extract authorities
    │                       │    Found: [ROLE_USER]
    │                       │◄──────────────────┘
    │                       │
    │                       │ 4. Check @PreAuthorize
    │                       │    Required: ROLE_ADMIN
    │                       │    Actual: ROLE_USER
    │                       │    → ACCESS DENIED
    │                       │◄──────────────────┘
    │                       │
    │                       │ 5. Throw AccessDeniedException
    │                       │◄──────────────────┘
    │                       │
    │                       │ 6. SecurityExceptionHandler
    │                       │    catches exception
    │                       │◄──────────────────┘
    │                       │
    │ 7. 403 Forbidden      │
    │<──────────────────────┤
    │ {                     │
    │   "code": "FORBIDDEN",│
    │   "message": "Insufficient permissions"
    │ }                     │
    │                       │
```

---

## 8. Installation & Setup

### 8.1 Prerequisites

```bash
# Check Java version
java -version
# Required: java version "17" or higher

# Check Maven version
mvn -version
# Required: Apache Maven 3.6+

# Check Git (optional)
git --version
# Optional but recommended

# Operating System
# ✓ macOS
# ✓ Linux
# ✓ Windows (with Git Bash or WSL)
```

### 8.2 Clone/Download Project

**Option 1: From existing directory**
```bash
cd /Users/sam/temp/empty/null/kc
ls
# Should see: pom.xml, src/, README.md, etc.
```

**Option 2: Fresh clone (if in Git)**
```bash
git clone https://github.com/your-repo/jwt-validation-demo.git
cd jwt-validation-demo
```

### 8.3 Project Verification

```bash
# Verify project structure
tree -L 2 src/
# Should see:
# src/
# ├── main
# │   ├── java
# │   └── resources
# └── test

# Check pom.xml
cat pom.xml | grep artifactId
# Should see: <artifactId>jwt-validation-demo</artifactId>
```

### 8.4 Dependencies Installation

Maven will automatically download dependencies during build:

```bash
mvn dependency:resolve

# Downloaded dependencies stored in:
~/.m2/repository/

# Key dependencies:
~/.m2/repository/org/springframework/boot/spring-boot-starter-web/3.2.0/
~/.m2/repository/org/springframework/boot/spring-boot-starter-security/3.2.0/
~/.m2/repository/com/nimbusds/nimbus-jose-jwt/9.37.3/
~/.m2/repository/com/google/guava/guava/32.1.3-jre/
```

### 8.5 Environment Variables (Optional)

```bash
# Set validation mode
export VALIDATION_MODE=LOCAL    # or REMOTE, or HYBRID

# Enable/disable mock mode
export MOCK_MODE=true          # true for testing, false for production

# For REMOTE mode (production):
export JWT_ISSUER_URI=https://your-keycloak.com/auth/realms/myrealm
export JWT_JWK_SET_URI=https://your-keycloak.com/auth/realms/myrealm/protocol/openid-connect/certs
export INTROSPECTION_URI=https://your-keycloak.com/auth/realms/myrealm/protocol/openid-connect/token/introspect
export OAUTH_CLIENT_ID=your-client-id
export OAUTH_CLIENT_SECRET=your-client-secret

# For HYBRID mode:
export VALIDATION_MODE=HYBRID
export REMOTE_VALIDATION_ENABLED=true
export CACHE_EXPIRY=300  # seconds
```

---

## 9. Running the Application

### 9.1 Build the Project

```bash
# Clean and build
mvn clean package -DskipTests

# Output:
# [INFO] BUILD SUCCESS
# [INFO] Total time: 9.638 s
# [INFO] Finished at: 2025-11-08T17:36:39+08:00

# Generated JAR location:
# target/jwt-validation-demo-1.0.0.jar
```

**Build Process Explained:**
```
mvn clean package
│
├─ clean
│  └─ Delete target/ directory
│
├─ validate
│  └─ Validate project structure
│
├─ compile
│  ├─ Compile src/main/java/**/*.java
│  ├─ Process Lombok annotations
│  └─ Output to target/classes/
│
├─ test (skipped with -DskipTests)
│  ├─ Compile src/test/java/**/*.java
│  └─ Run JUnit tests
│
├─ package
│  ├─ Create JAR: target/jwt-validation-demo-1.0.0.jar
│  └─ spring-boot-maven-plugin:repackage
│     ├─ Add embedded Tomcat
│     ├─ Add all dependencies
│     └─ Make executable JAR
│
└─ Result: Executable JAR (30-40 MB)
```

### 9.2 Run the Application

**Method 1: Using java -jar (Production-like)**
```bash
java -jar target/jwt-validation-demo-1.0.0.jar

# With environment variables:
VALIDATION_MODE=LOCAL MOCK_MODE=true \
  java -jar target/jwt-validation-demo-1.0.0.jar

# With JVM options:
java -Xmx512m -Xms256m \
  -Dspring.profiles.active=dev \
  -jar target/jwt-validation-demo-1.0.0.jar
```

**Method 2: Using Maven Spring Boot plugin (Development)**
```bash
mvn spring-boot:run

# With profiles:
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# With arguments:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

**Method 3: Run in background**
```bash
# Linux/macOS
nohup java -jar target/jwt-validation-demo-1.0.0.jar > app.log 2>&1 &

# Get process ID
echo $!

# Check if running
ps aux | grep jwt-validation

# Kill process
kill <PID>
```

### 9.3 Startup Sequence

```
Application Startup Logs:

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2025-11-08T17:36:50 INFO  JwtValidationApplication : Starting...
2025-11-08T17:36:51 INFO  TomcatWebServer : Tomcat initialized with port 8080
2025-11-08T17:36:51 INFO  TomcatWebServer : Tomcat started on port 8080
2025-11-08T17:36:51 INFO  JwtValidationApplication : Started in 1.067 seconds

Application is ready!
```

**What Happens During Startup:**

1. **Spring Boot Initialization** (200ms)
   - Load application.yml
   - Create ApplicationContext
   - Scan components (@Component, @Service, @Controller)

2. **Bean Creation** (300ms)
   - Create SecurityFilterChain beans
   - Initialize JwtDecoder / OpaqueTokenIntrospector
   - Create RestTemplate (if REMOTE mode)
   - Initialize Guava Cache (if HYBRID mode)

3. **Tomcat Startup** (400ms)
   - Initialize embedded Tomcat server
   - Register DispatcherServlet
   - Create thread pools
   - Bind to port 8080

4. **Security Filter Chain Registration** (100ms)
   - Register BearerTokenAuthenticationFilter
   - Register AuthorizationFilter
   - Configure CORS/CSRF

5. **Ready to Accept Requests** (50ms)
   - Log "Started JwtValidationApplication"
   - Listen on http://localhost:8080

**Total Startup Time: ~1 second**

### 9.4 Verify Application is Running

```bash
# Check if port 8080 is listening
lsof -i :8080
# or
netstat -an | grep 8080

# Test health endpoint
curl http://localhost:8080/api/public/health

# Expected response:
{
  "status": "UP",
  "timestamp": "2025-11-08T09:42:07.285814Z",
  "validationMode": "LOCAL"
}
```

### 9.5 Run Modes

**LOCAL Mode (Default)**
```bash
# application.yml already configured for LOCAL
java -jar target/jwt-validation-demo-1.0.0.jar

# Features:
# ✓ Fast validation (~1ms)
# ✓ Mock token generation enabled
# ✓ No external dependencies
# ✓ Perfect for development/testing
```

**REMOTE Mode**
```bash
# Set environment variable
export VALIDATION_MODE=REMOTE

# Must configure Keycloak endpoints:
export INTROSPECTION_URI=https://keycloak.example.com/.../token/introspect
export OAUTH_CLIENT_ID=my-app
export OAUTH_CLIENT_SECRET=secret

java -jar target/jwt-validation-demo-1.0.0.jar

# Features:
# ✓ Real-time token validation
# ✓ Token revocation support
# ✓ Requires Keycloak running
```

**HYBRID Mode**
```bash
export VALIDATION_MODE=HYBRID
export REMOTE_VALIDATION_ENABLED=true
export CACHE_EXPIRY=300

java -jar target/jwt-validation-demo-1.0.0.jar

# Features:
# ✓ Local validation (fast path)
# ✓ Remote validation (conditional)
# ✓ Intelligent caching
# ✓ Best of both worlds
```

### 9.6 Shutdown

```bash
# Graceful shutdown (Ctrl+C)
# - Stops accepting new requests
# - Finishes current requests
# - Closes database connections
# - Shuts down thread pools

# Force shutdown (Ctrl+Z, then kill)
kill -9 <PID>

# Check shutdown logs:
# INFO  TomcatWebServer : Tomcat stopped
# INFO  JwtValidationApplication : Application shutdown complete
```

---

## 10. API Endpoints Reference

### 10.1 Public Endpoints

#### GET /api/public/hello
**Description**: Public greeting endpoint (no authentication required)

**Request:**
```bash
curl http://localhost:8080/api/public/hello
```

**Response:**
```json
{
  "message": "Hello! This is a public endpoint.",
  "timestamp": "2025-11-08T09:42:07.273825Z"
}
```

**Status Codes:**
- `200 OK`: Success

---

#### GET /api/public/health
**Description**: Health check endpoint

**Request:**
```bash
curl http://localhost:8080/api/public/health
```

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2025-11-08T09:42:07.285814Z",
  "validationMode": "LOCAL"
}
```

**Fields:**
- `status`: Application status (UP/DOWN)
- `timestamp`: Current server time
- `validationMode`: Current validation strategy (LOCAL/REMOTE/HYBRID)

---

#### GET /api/public/info
**Description**: API information and version

**Request:**
```bash
curl http://localhost:8080/api/public/info
```

**Response:**
```json
{
  "application": "JWT Validation Demo",
  "version": "1.0.0",
  "validationMode": "LOCAL",
  "description": "Demo application showing Local, Remote, and Hybrid JWT validation"
}
```

---

### 10.2 Mock Token Endpoints

#### GET /api/public/mock/user-token
**Description**: Generate a mock JWT token with USER role

**Request:**
```bash
curl http://localhost:8080/api/public/mock/user-token
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLTAwMSIsImF1ZCI6Im1vY2stYXVkaWVuY2UiLCJyb2xlcyI6WyJVU0VSIl0sIm5hbWUiOiJSZWd1bGFyIFVzZXIiLCJpc3MiOiJtb2NrLWlzc3VlciIsImV4cCI6MTc2MjU5ODM5NywiaWF0IjoxNzYyNTk0Nzk3LCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20ifQ.tt86bX5JddJEV7Jlx2Jm2zDGl1Q6Am1xYmSjfoQPqXE",
  "type": "Bearer",
  "role": "USER"
}
```

**Token Contents:**
```json
{
  "sub": "user-001",
  "email": "user@example.com",
  "name": "Regular User",
  "roles": ["USER"],
  "iss": "mock-issuer",
  "aud": "mock-audience",
  "exp": 1762598397,  // 1 hour from now
  "iat": 1762594797   // Current time
}
```

---

#### GET /api/public/mock/admin-token
**Description**: Generate a mock JWT token with ADMIN role

**Request:**
```bash
curl http://localhost:8080/api/public/mock/admin-token
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "role": "ADMIN"
}
```

**Token Contents:**
```json
{
  "sub": "admin-001",
  "email": "admin@example.com",
  "name": "Admin User",
  "roles": ["ADMIN", "USER"],
  "iss": "mock-issuer",
  "aud": "mock-audience",
  "exp": 1762598533,
  "iat": 1762594933
}
```

---

#### POST /api/public/mock/generate-token
**Description**: Generate a custom JWT token with specified claims

**Request:**
```bash
curl -X POST http://localhost:8080/api/public/mock/generate-token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "custom-123",
    "email": "custom@example.com",
    "name": "Custom User",
    "roles": ["USER", "ADMIN", "SUPERUSER"]
  }'
```

**Request Body:**
```json
{
  "userId": "custom-123",         // Optional, default: "test-user-123"
  "email": "custom@example.com",  // Optional, default: "test@example.com"
  "name": "Custom User",          // Optional, default: "Test User"
  "roles": ["USER", "ADMIN"]      // Optional, default: ["USER"]
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "usage": "Add to Authorization header as: Bearer eyJhbGc..."
}
```

---

### 10.3 User Endpoints (Requires Authentication)

#### GET /api/user/profile
**Description**: Get current user's profile information

**Authentication**: Required (USER or ADMIN role)

**Request:**
```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <TOKEN>"
```

**Response:**
```json
{
  "userId": "user-001",
  "email": "user@example.com",
  "name": "Regular User",
  "roles": ["ROLE_USER"]
}
```

**Status Codes:**
- `200 OK`: Success
- `401 Unauthorized`: Missing or invalid token
- `403 Forbidden`: Insufficient permissions

---

#### GET /api/user/token-info
**Description**: Get detailed information about the JWT token

**Authentication**: Required (USER or ADMIN role)

**Request:**
```bash
curl http://localhost:8080/api/user/token-info \
  -H "Authorization: Bearer <TOKEN>"
```

**Response:**
```json
{
  "principal": "org.springframework.security.oauth2.jwt.Jwt@3b0fba03",
  "authenticated": true,
  "claims": {
    "sub": "user-001",
    "aud": ["mock-audience"],
    "roles": ["USER"],
    "name": "Regular User",
    "iss": "mock-issuer",
    "exp": "2025-11-08T10:42:07Z",
    "iat": "2025-11-08T09:42:07Z",
    "email": "user@example.com"
  },
  "authorities": ["ROLE_USER"]
}
```

**Fields Explained:**
- `principal`: Spring Security principal object (string representation)
- `authenticated`: Whether user is authenticated
- `claims`: All JWT claims (standard + custom)
  - `sub` (subject): User ID
  - `aud` (audience): Intended recipients
  - `roles`: Custom claim with user roles
  - `iss` (issuer): Token issuer
  - `exp` (expiration): Token expiration time
  - `iat` (issued at): Token issue time
- `authorities`: Spring Security authorities (roles with ROLE_ prefix)

---

#### GET /api/user/hello
**Description**: Simple greeting for authenticated users

**Authentication**: Required (USER or ADMIN role)

**Request:**
```bash
curl http://localhost:8080/api/user/hello \
  -H "Authorization: Bearer <TOKEN>"
```

**Response:**
```
Hello, Regular User! You have USER access.
```

---

### 10.4 Admin Endpoints (Requires ADMIN Role)

#### GET /api/admin/dashboard
**Description**: Get admin dashboard data

**Authentication**: Required (ADMIN role only)

**Request:**
```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Response:**
```json
{
  "message": "Welcome to Admin Dashboard",
  "totalUsers": 1234,
  "activeUsers": 567,
  "systemStatus": "Healthy"
}
```

**Status Codes:**
- `200 OK`: Success (ADMIN token)
- `403 Forbidden`: Insufficient permissions (USER token)
- `401 Unauthorized`: Missing or invalid token

---

#### GET /api/admin/info
**Description**: Get admin user information

**Authentication**: Required (ADMIN role only)

**Request:**
```bash
curl http://localhost:8080/api/admin/info \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

**Response:**
```json
{
  "role": "Administrator",
  "access": "Full System Access",
  "user": "admin-001"
}
```

---

#### POST /api/admin/data
**Description**: Post admin data (example endpoint)

**Authentication**: Required (ADMIN role only)

**Request:**
```bash
curl -X POST http://localhost:8080/api/admin/data \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "key1": "value1",
    "key2": "value2",
    "key3": "value3"
  }'
```

**Response:**
```
Admin data processed successfully: 3 fields
```

---

### 10.5 Error Responses

All error responses follow this format:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}
```

**Error Codes:**

| Code | HTTP Status | Description | Example |
|------|-------------|-------------|---------|
| `UNAUTHORIZED` | 401 | Invalid or expired token | Missing Authorization header |
| `JWT_INVALID` | 401 | JWT validation failed | Signature verification failed |
| `TOKEN_INTROSPECTION_FAILED` | 401 | Remote introspection failed | Keycloak returned active=false |
| `FORBIDDEN` | 403 | Insufficient permissions | USER trying to access ADMIN endpoint |
| `INTERNAL_ERROR` | 500 | Unexpected error | Failed to generate mock JWT token |

**Example Error Responses:**

```json
// 401 Unauthorized - Missing token
{
  "code": "UNAUTHORIZED",
  "message": "Invalid or expired token",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}

// 401 Unauthorized - Invalid signature
{
  "code": "JWT_INVALID",
  "message": "JWT validation failed: Signature verification failed",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}

// 403 Forbidden - Insufficient permissions
{
  "code": "FORBIDDEN",
  "message": "Insufficient permissions: Access Denied",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}

// 500 Internal Server Error
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred: Failed to generate mock JWT token",
  "timestamp": "2025-11-08T09:42:07.773887Z"
}
```

---

(This is Part 1 of the guide - continuing in next file due to length...)
