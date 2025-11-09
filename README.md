# JWT Validation Demo - Spring Boot

This project demonstrates **three different approaches** to JWT token validation in Spring Boot:

1. **LOCAL Validation** - Fast, cryptographic verification using public keys
2. **REMOTE Validation** - Real-time token introspection via Keycloak
3. **HYBRID Validation** - Best of both worlds with intelligent caching

## Features

- ✅ Multiple JWT validation strategies
- ✅ Role-based access control (USER, ADMIN)
- ✅ Mock JWT generation for testing (no Keycloak required)
- ✅ Comprehensive error handling
- ✅ Production-ready security configuration
- ✅ Detailed logging and debugging

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Running the Application

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run with LOCAL validation (default):**
   ```bash
   mvn spring-boot:run
   ```

3. **Run with REMOTE validation:**
   ```bash
   VALIDATION_MODE=REMOTE mvn spring-boot:run
   ```

4. **Run with HYBRID validation:**
   ```bash
   VALIDATION_MODE=HYBRID mvn spring-boot:run
   ```

## Testing the Application

### 1. Check Application Health

```bash
curl http://localhost:8080/api/public/health
```

### 2. Generate Mock Tokens

**Generate a USER token:**
```bash
curl http://localhost:8080/api/public/mock/user-token
```

**Generate an ADMIN token:**
```bash
curl http://localhost:8080/api/public/mock/admin-token
```

**Generate a custom token:**
```bash
curl -X POST http://localhost:8080/api/public/mock/generate-token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "custom-123",
    "email": "custom@example.com",
    "name": "Custom User",
    "roles": ["USER", "ADMIN"]
  }'
```

### 3. Test Authenticated Endpoints

**Save the token:**
```bash
export TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')
```

**Access user profile:**
```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"
```

**Access token info:**
```bash
curl http://localhost:8080/api/user/token-info \
  -H "Authorization: Bearer $TOKEN"
```

**Access user hello:**
```bash
curl http://localhost:8080/api/user/hello \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Test Admin Endpoints

**Get admin token:**
```bash
export ADMIN_TOKEN=$(curl -s http://localhost:8080/api/public/mock/admin-token | jq -r '.token')
```

**Access admin dashboard:**
```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Access admin info:**
```bash
curl http://localhost:8080/api/admin/info \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Try admin endpoint with USER token (should fail):**
```bash
curl http://localhost:8080/api/admin/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

## API Endpoints

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/public/hello` | Public greeting |
| GET | `/api/public/health` | Health check |
| GET | `/api/public/info` | API information |
| GET | `/api/public/mock/user-token` | Generate USER token |
| GET | `/api/public/mock/admin-token` | Generate ADMIN token |
| POST | `/api/public/mock/generate-token` | Generate custom token |

### User Endpoints (Requires Authentication)

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/user/profile` | Get user profile | USER, ADMIN |
| GET | `/api/user/token-info` | Get token details | USER, ADMIN |
| GET | `/api/user/hello` | User greeting | USER, ADMIN |

### Admin Endpoints (Requires ADMIN Role)

| Method | Endpoint | Description | Required Role |
|--------|----------|-------------|---------------|
| GET | `/api/admin/dashboard` | Admin dashboard | ADMIN |
| GET | `/api/admin/info` | Admin information | ADMIN |
| POST | `/api/admin/data` | Post admin data | ADMIN |

## Validation Modes

### LOCAL Validation (Default)

**Pros:**
- ⚡ Fast (~1ms)
- 📈 Highly scalable
- 🔌 Works offline
- 💾 Minimal load on Keycloak

**Cons:**
- ❌ No real-time revocation
- ⏰ Relies on token expiration

**Configuration:**
```yaml
app:
  security:
    validation-mode: LOCAL
    mock-enabled: true
```

### REMOTE Validation

**Pros:**
- ✅ Real-time revocation
- 🔄 Immediate permission changes
- 🔒 Server-verified

**Cons:**
- 🐌 Slower (~50-200ms)
- 🌐 Requires network
- 📊 Higher Keycloak load

**Configuration:**
```yaml
app:
  security:
    validation-mode: REMOTE
spring:
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          introspection-uri: https://keycloak.example.com/auth/realms/myrealm/protocol/openid-connect/token/introspect
          client-id: my-app
          client-secret: your-secret
```

### HYBRID Validation

**Pros:**
- 🚀 Fast for cached tokens
- ✅ Remote validation when needed
- 💡 Intelligent caching
- 🎯 Best of both worlds

**Configuration:**
```yaml
app:
  security:
    validation-mode: HYBRID
    hybrid:
      remote-validation-enabled: true
      cache-expiry-seconds: 300
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VALIDATION_MODE` | Validation mode (LOCAL, REMOTE, HYBRID) | LOCAL |
| `MOCK_MODE` | Enable mock JWT generation | true |
| `MOCK_SECRET` | Secret for mock JWT signing | mySecretKey... |
| `JWT_ISSUER_URI` | Keycloak issuer URI | https://keycloak... |
| `JWT_JWK_SET_URI` | Keycloak JWKS URI | https://keycloak... |
| `INTROSPECTION_URI` | Token introspection endpoint | https://keycloak... |
| `OAUTH_CLIENT_ID` | OAuth2 client ID | my-app |
| `OAUTH_CLIENT_SECRET` | OAuth2 client secret | your-secret |

## Project Structure

```
src/main/java/com/example/jwtvalidation/
├── config/
│   ├── LocalValidationSecurityConfig.java     # Local validation config
│   ├── RemoteValidationSecurityConfig.java    # Remote validation config
│   ├── MockJwtConfig.java                     # Mock JWT for testing
│   └── RestTemplateConfig.java                # RestTemplate bean
├── controller/
│   ├── PublicController.java                  # Public endpoints
│   ├── UserController.java                    # User endpoints
│   ├── AdminController.java                   # Admin endpoints
│   └── MockTokenController.java               # Token generation
├── service/
│   ├── CustomOpaqueTokenIntrospector.java     # Remote validation
│   └── HybridTokenValidator.java              # Hybrid validation
├── model/
│   ├── UserProfile.java                       # User profile model
│   └── ErrorResponse.java                     # Error response model
├── exception/
│   └── SecurityExceptionHandler.java          # Global exception handler
└── JwtValidationApplication.java              # Main application
```

## Troubleshooting

### Issue: "Unable to find key with kid..."

**Solution:** Make sure mock mode is enabled or configure proper Keycloak URLs.

### Issue: "Token introspection failed"

**Solution:** Check that Keycloak is running and introspection URI is correct.

### Issue: "Access Denied"

**Solution:** Verify that your token has the required role (USER or ADMIN).

## Security Considerations

1. **Production Use:**
   - Disable mock mode: `MOCK_MODE=false`
   - Use strong secrets for token signing
   - Configure proper Keycloak URLs
   - Enable HTTPS in production

2. **Token Expiration:**
   - Set appropriate token expiration times
   - Implement token refresh mechanisms
   - Handle expired tokens gracefully

3. **Role Management:**
   - Use fine-grained role definitions
   - Implement proper RBAC policies
   - Regular security audits

## License

MIT License - Feel free to use this for learning and development!

## References

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)
- [JWT.io](https://jwt.io/)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
