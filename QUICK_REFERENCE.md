# JWT Validation - Quick Reference Card

## 🚀 Quick Start (30 seconds)

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Run
java -jar target/jwt-validation-demo-1.0.0.jar

# 3. Test
curl http://localhost:8080/api/public/health
```

---

## 📋 Common Commands

### Build & Run
```bash
# Build
mvn clean package -DskipTests

# Run (default LOCAL mode)
java -jar target/jwt-validation-demo-1.0.0.jar

# Run with specific mode
VALIDATION_MODE=REMOTE java -jar target/jwt-validation-demo-1.0.0.jar

# Run in background
nohup java -jar target/jwt-validation-demo-1.0.0.jar > app.log 2>&1 &

# Kill running instance
kill -9 $(lsof -ti:8080)
```

### Testing
```bash
# Generate USER token
curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token'

# Generate ADMIN token
curl -s http://localhost:8080/api/public/mock/admin-token | jq -r '.token'

# Test with token
TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/user/profile

# Run test script
./test-api.sh
```

---

## 🔑 API Endpoints

### Public (No Auth)
```bash
GET  /api/public/hello          # Public greeting
GET  /api/public/health         # Health check
GET  /api/public/info           # API info
GET  /api/public/mock/user-token    # Generate USER token
GET  /api/public/mock/admin-token   # Generate ADMIN token
POST /api/public/mock/generate-token # Custom token
```

### User (Authenticated)
```bash
GET  /api/user/profile     # User profile (USER or ADMIN)
GET  /api/user/token-info  # Token details (USER or ADMIN)
GET  /api/user/hello       # User greeting (USER or ADMIN)
```

### Admin (ADMIN Only)
```bash
GET  /api/admin/dashboard  # Admin dashboard (ADMIN only)
GET  /api/admin/info       # Admin info (ADMIN only)
POST /api/admin/data       # Post data (ADMIN only)
```

---

## ⚙️ Configuration

### Validation Modes
```yaml
# LOCAL (default) - Fast, cryptographic
VALIDATION_MODE=LOCAL

# REMOTE - Real-time introspection
VALIDATION_MODE=REMOTE

# HYBRID - Intelligent caching
VALIDATION_MODE=HYBRID
```

### Environment Variables
```bash
# Core settings
export VALIDATION_MODE=LOCAL          # LOCAL, REMOTE, or HYBRID
export MOCK_MODE=true                 # true for dev, false for prod
export SERVER_PORT=8080               # HTTP port

# Production (REMOTE mode)
export JWT_ISSUER_URI=https://keycloak.example.com/auth/realms/myrealm
export JWT_JWK_SET_URI=https://keycloak.example.com/.../certs
export INTROSPECTION_URI=https://keycloak.example.com/.../token/introspect
export OAUTH_CLIENT_ID=my-app
export OAUTH_CLIENT_SECRET=your-secret

# Hybrid mode
export REMOTE_VALIDATION_ENABLED=true
export CACHE_EXPIRY=300  # seconds
```

---

## 🧪 Testing Patterns

### Generate and Use Token
```bash
# Save token to variable
TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')

# Use in request
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/api/user/profile | jq '.'
```

### Test Authorization
```bash
# Generate tokens
USER_TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token')
ADMIN_TOKEN=$(curl -s http://localhost:8080/api/public/mock/admin-token | jq -r '.token')

# USER can access /user endpoints
curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/user/profile
# Expected: 200 OK

# USER cannot access /admin endpoints
curl -H "Authorization: Bearer $USER_TOKEN" \
     http://localhost:8080/api/admin/dashboard
# Expected: 403 Forbidden

# ADMIN can access both
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
     http://localhost:8080/api/admin/dashboard
# Expected: 200 OK
```

### Error Testing
```bash
# No token (401)
curl http://localhost:8080/api/user/profile

# Invalid token (401)
curl -H "Authorization: Bearer invalid.token" \
     http://localhost:8080/api/user/profile

# Malformed header (401)
curl -H "Authorization: eyJhbGc..." \
     http://localhost:8080/api/user/profile
```

---

## 🐛 Troubleshooting

### Application Won't Start
```bash
# Port in use
lsof -ti:8080 | xargs kill -9

# Check logs
tail -f logs/application.log

# Verify Java version
java -version  # Should be 17+
```

### Token Issues
```bash
# Decode JWT (inspect claims)
# Go to https://jwt.io and paste token

# Check token expiration
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq '.exp'
date +%s  # Compare with current time

# Verify secret length (must be 32+ bytes)
echo -n "mySecretKeyForJWT2025mustBe32bytes!" | wc -c
```

### Enable Debug Logging
```yaml
# application.yml
logging:
  level:
    org.springframework.security: DEBUG
    com.example.jwtvalidation: DEBUG
```

---

## 📊 Performance Quick Stats

| Mode | Latency | Throughput | Use Case |
|------|---------|------------|----------|
| **LOCAL** | ~1ms | 8,500 req/s | High-traffic APIs |
| **REMOTE** | ~85ms | 120 req/s | Banking, Security |
| **HYBRID** | ~6ms | 7,200 req/s | SaaS, E-commerce |

---

## 🔒 Security Checklist

```bash
□ Use RS256 in production (not HS256)
□ Disable mock mode (MOCK_MODE=false)
□ Use environment variables for secrets
□ Enable HTTPS
□ Set short token expiration (< 1 hour)
□ Implement token refresh
□ Add rate limiting
□ Enable CORS properly
□ Never store sensitive data in JWT
□ Rotate keys regularly
```

---

## 🐳 Docker Quick Commands

```bash
# Build image
docker build -t jwt-validation-demo:1.0.0 .

# Run container
docker run -d -p 8080:8080 \
  -e VALIDATION_MODE=LOCAL \
  -e MOCK_MODE=false \
  jwt-validation-demo:1.0.0

# View logs
docker logs -f <container-id>

# Stop container
docker stop <container-id>

# Using docker-compose
docker-compose up -d
docker-compose logs -f
docker-compose down
```

---

## ☸️ Kubernetes Quick Commands

```bash
# Deploy
kubectl apply -f deployment.yaml

# Check status
kubectl get deployments
kubectl get pods
kubectl get services

# View logs
kubectl logs -f deployment/jwt-validation-demo

# Scale
kubectl scale deployment jwt-validation-demo --replicas=5

# Delete
kubectl delete -f deployment.yaml
```

---

## 📁 File Locations

```
Project Root:
├── pom.xml                                    # Maven config
├── README.md                                  # Quick start
├── COMPREHENSIVE_GUIDE_INDEX.md               # Full guide index
├── QUICK_REFERENCE.md                         # This file
├── test-api.sh                                # Test script
│
├── src/main/java/com/example/jwtvalidation/
│   ├── JwtValidationApplication.java          # Main class
│   ├── config/
│   │   ├── LocalValidationSecurityConfig.java # LOCAL mode
│   │   ├── RemoteValidationSecurityConfig.java# REMOTE mode
│   │   └── MockJwtConfig.java                 # Mock tokens
│   ├── controller/
│   │   ├── PublicController.java              # Public endpoints
│   │   ├── UserController.java                # User endpoints
│   │   └── AdminController.java               # Admin endpoints
│   └── service/
│       └── HybridTokenValidator.java          # HYBRID mode
│
└── src/main/resources/
    └── application.yml                        # Configuration
```

---

## 🎯 Common Use Cases

### Development/Testing
```yaml
app:
  security:
    validation-mode: LOCAL
    mock-enabled: true
```

### Staging
```yaml
app:
  security:
    validation-mode: HYBRID
    mock-enabled: false
    hybrid:
      remote-validation-enabled: true
```

### Production
```yaml
app:
  security:
    validation-mode: LOCAL
    mock-enabled: false
```

---

## 💡 Tips & Tricks

### Decode JWT in Terminal
```bash
# Extract header
echo $TOKEN | cut -d'.' -f1 | base64 -d | jq '.'

# Extract payload
echo $TOKEN | cut -d'.' -f2 | base64 -d | jq '.'
```

### Check Application Health
```bash
# Simple check
curl -f http://localhost:8080/api/public/health || echo "App is down"

# With jq
curl -s http://localhost:8080/api/public/health | jq '.status'
```

### Load Testing
```bash
# Apache Bench
ab -n 1000 -c 10 http://localhost:8080/api/public/health

# With token
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
   http://localhost:8080/api/user/profile
```

---

## 📚 Documentation Links

- **Full Guide**: `COMPREHENSIVE_GUIDE_INDEX.md`
- **Part 1**: Architecture & Setup
- **Part 2**: Implementation & Security
- **Part 3**: Production & Advanced

---

## 🆘 Emergency Commands

```bash
# Application not responding
kill -9 $(lsof -ti:8080)

# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild from scratch
mvn clean install -DskipTests

# Check Java/Maven versions
java -version
mvn -version

# View all Java processes
jps -lv

# Memory dump (if OOM)
jmap -dump:live,format=b,file=heap.bin <PID>
```

---

## ⚡ One-Liners

```bash
# Complete test flow
TOKEN=$(curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token') && \
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/user/profile | jq '.'

# Build and run
mvn clean package -DskipTests && java -jar target/jwt-validation-demo-1.0.0.jar

# Check if app is running
curl -f http://localhost:8080/api/public/health &>/dev/null && echo "✓ Running" || echo "✗ Down"

# Get token and save to file
curl -s http://localhost:8080/api/public/mock/user-token | jq -r '.token' > token.txt
```

---

**Need more details? Check COMPREHENSIVE_GUIDE_INDEX.md**
