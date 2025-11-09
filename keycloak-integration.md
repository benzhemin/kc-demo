# Keycloak Integration Example for Spring Boot Microservices

## Client Authentication Flow

```
[Client App] --login--> [Keycloak] --JWT Token--> [Client App]
```

## Microservice Architecture Flow

```
[Client] --(JWT in Auth Header)--> [API Gateway] --(propagates JWT)--> [Microservice A]
                                   |
                                   v
                             [Microservice B] --(possible call to)--> [Microservice C]
```

## JWT Token Structure (Keycloak JWT)

```json
{
  "jti": "b37e5f3d-6239-46f0-8dd9-b3959b5f7c2f",
  "exp": 1698457453,
  "nbf": 0,
  "iat": 1698456853,
  "iss": "http://keycloak:8080/auth/realms/myrealm",
  "aud": "my-client",
  "sub": "c2a1c7c5-f9a8-402e-a741-8e57b3935c47",
  "typ": "ID",
  "azp": "my-client",
  "session_state": "e4d0943d-4383-47b9-9745-3a9560b1b6c2",
  "acr": "1",
  "name": "John Doe",
  "preferred_username": "jdoe",
  "email": "john.doe@example.com",
  "realm_access": {
    "roles": ["user", "admin", "editor"]
  },
  "scope": "openid email profile",
  "email_verified": true
}
```