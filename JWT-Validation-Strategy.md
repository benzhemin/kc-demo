# JWT Validation Strategies for Microservices

## The Key Question: Should every downstream request validate JWT via Keycloak?

**Short Answer:** No, every microservice should validate JWT tokens, but NOT all need to call Keycloak directly for validation.

## Two Main Validation Approaches

### 1. Remote Validation (Calling Keycloak Introspection Endpoint)

Every request makes a network call to Keycloak:

```
[Service] --token introspection--> [Keycloak] --token validation result--> [Service]
```

**Pros:**
- Always up-to-date validation (immediate revocation detection)
- Simpler setup (no key management needed)

**Cons:**
- Performance impact (additional network call for every request)
- Single point of failure (if Keycloak is down, all services fail)
- High load on Keycloak servers
- Higher latency

### 2. Local Validation Using JWKS (JSON Web Key Set)

Services download public keys from Keycloak once and validate tokens locally:

```
[Service] --startup download--> [Keycloak JWKS endpoint]
[Service] --local validation--> [JWT Token]
```

**Pros:**
- Much faster validation (no network call per request)
- Resilient to Keycloak downtime (after initial key download)
- Reduced load on Keycloak servers
- Lower latency
- Better for high-throughput microservice architectures

**Cons:**
- Slightly more complex setup
- Revocation detection might be delayed until keys are refreshed

## Recommended Architecture for Microservices

### For Most Production Scenarios: Use Local Validation

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8080/auth/realms/myrealm/protocol/openid-connect/certs
```

### Why Local Validation is Recommended:

1. **Performance**: Critical for microservice-to-service communication
2. **Resilience**: If Keycloak is temporarily unavailable, your services can still validate tokens
3. **Scalability**: Avoids overwhelming Keycloak with validation requests
4. **Latency**: Eliminates network roundtrip for every internal service call

### Edge Cases for Remote Validation:

1. **High-security environments** where immediate token revocation is critical
2. **Token introspection** scenarios where you need more than just validation
3. **Blacklisting scenarios** where tokens need to be invalidated immediately

## Best Practice Implementation

### 1. Hybrid Approach for External vs Internal Traffic

```
[External Client] --> [API Gateway] --Remote Validation--> [Keycloak]
                      |
                      v
              [Validated Request]
                      |
                      v
           [Microservice A] --Local Validation--> [JWT Token]
                      |
                      v
           [Microservice B] --Local Validation--> [JWT Token]
```

### 2. Key Rotation Handling

Spring Boot's `NimbusJwtDecoder` handles this automatically by:
- Caching JWKS keys
- Refreshing keys periodically
- Falling back to Keycloak when needed

### 3. Security Considerations

- Always use HTTPS for JWKS endpoint
- Implement proper key validation (issuer, audience, expiration)
- Cache JWKS response with appropriate TTL
- Monitor key rotation events

### 4. API Gateway Pattern

Using an API Gateway is highly recommended:

```
[Client] --> [API Gateway] --> Keycloak validation
               |
               v
         [Internal network]
               |
     +---------+---------+
     v         v         v
 [Service A] [Service B] [Service C]
```

**Benefits:**
- Centralized authentication/authorization
- Simplified downstream services (they can trust tokens already validated at gateway)
- Reduced attack surface
- Unified rate limiting and security policies

## Final Recommendation

For a Spring Boot microservice architecture with Keycloak:

1. **Use local JWT validation** in all microservices
2. **Implement an API Gateway** that validates all external requests
3. **Enable short-lived token expiry** to balance security and performance
4. **Use refresh tokens** for maintaining sessions without re-authentication
5. **Monitor key rotation** and ensure your services handle it properly

This approach provides the best balance of security, performance, and resilience for microservice architectures.