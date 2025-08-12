# Civic Insights Auth Service

A Spring Boot authentication microservice providing OAuth2 (Google), JWT, and user profile features. Since v1.3.0, the service assumes API Gateway traversal (Gateway Only Security) by default.

## 🚀 Key Features
- OAuth2 (Google): Server-driven and client-driven flows
- JWT (RS256): Issue/validate access and refresh tokens with RSA asymmetric keys
- JWK Public Keys: Provide JWK Set at `/.well-known/jwks.json` (for distributed verification)
- User Profile: Authenticated user profile read/update APIs
- Gateway Only Security: Enforce internal gateway-only access via `X-Gateway-Internal` header
- Swagger UI: Interactive OpenAPI documentation

## 🛠 Tech Stack
- Java 17
- Spring Boot 3.5.4
- Spring Web, Spring Security, Spring Data JPA
- MySQL 8.4+
- JJWT 0.12.6, Nimbus JOSE JWT 10.4
- SpringDoc OpenAPI UI, Lombok, Gradle

## 📋 Prerequisites
- Java 17+
- MySQL 8.4+
- Gradle (wrapper included)

## ⚙️ Installation & Run
### 1) Clone Repository
```bash
git clone <repository-url>
cd civic-insights-auth
```

### 2) Prepare Database
```sql
CREATE DATABASE civic_insights;
```

### 3) Environment Variables
```bash
# Google OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
# Gateway-based callback by default (can be overridden)
export GOOGLE_REDIRECT_URI=http://localhost:8000/api/auth/login/oauth2/code/google

# Gateway Only Security
export GATEWAY_SECRET_TOKEN=your-production-gateway-token

# Frontend integration (server-driven flow hand-off target)
export FRONTEND_BASE_URL=http://localhost:9002
export FRONTEND_SESSION_POST_URL=http://localhost:9002/api/session
```

### 4) Run
```bash
./gradlew bootRun
```
Service starts at `http://localhost:8001`.

### 5) Swagger UI
`http://localhost:8001/swagger-ui.html`

## 🔧 Configuration Summary (`application.properties`)
```properties
# Server
server.port=8001

# DB
spring.datasource.url=jdbc:mysql://localhost:3312/civic_insights
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# OAuth2 (Gateway callback default)
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:your-google-client-id}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:your-google-client-secret}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:8000/api/auth/login/oauth2/code/google}

# JWT (RS256)
jwt.expiration-ms=86400000
jwt.refresh-expiration=604800000

# Gateway Only Security
app.security.gateway-only=true
app.security.gateway-token=${GATEWAY_SECRET_TOKEN:civic-insights-gateway-v1}

# Frontend redirect/hand-off (server-driven flow)
frontend.redirect-base=${FRONTEND_BASE_URL:http://localhost:9002}
frontend.session-post-url=${FRONTEND_SESSION_POST_URL:http://localhost:9002/api/session}

# OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.try-it-out-enabled=true
```

## 🔐 Security Overview
### JWT
- Algorithm: RS256
- Public key: `GET /.well-known/jwks.json`

### Gateway Only Security (v1.3.0)
- All external traffic must go through the API Gateway.
- Gateway injects `X-Gateway-Internal: <secret>` header into requests.
- Auth service validates the header; direct access is rejected with 403 (Forbidden).
- Env var: Externalize secret via `GATEWAY_SECRET_TOKEN`.
- Default allowed CORS origin: `http://localhost:8000` (Gateway).

Nginx example:
```nginx
location /api/auth/ {
  proxy_pass http://localhost:8001/api/v1/auth/;
  proxy_set_header X-Gateway-Internal "${GATEWAY_SECRET_TOKEN}";
}
```

## 🌐 Endpoint Overview
Base URL: `http://localhost:8001/api/v1`

### JWK
- `GET /.well-known/jwks.json` JWK Set

### Auth
- `GET  /auth/google` Redirect to Google login (server-driven)
- `POST /auth/google/token` Issue JWT with auth code (client-driven)
- `GET  /auth/login/oauth2/code/google` Google callback (internal)
  - On success, returns auto-submitting HTML form posting tokens to `FRONTEND_SESSION_POST_URL` (prevents URL exposure)
- `POST /auth/refresh` Refresh tokens

### Profile
- `GET  /profile` Get my profile (requires auth)
- `PUT  /profile` Update my profile (requires auth)

## 🧪 Testing
### Gateway header requirement
```bash
# Direct access (expected to be blocked)
curl http://localhost:8001/api/v1/profile

# With Gateway header (passes filter; may 401/403 without JWT)
curl -H "X-Gateway-Internal: ${GATEWAY_SECRET_TOKEN}" http://localhost:8001/api/v1/profile
```

### Swagger UI
Test protected APIs with Bearer tokens at `http://localhost:8001/swagger-ui.html`.

## 🏗️ Project Structure
```
src/
├── main/
│   ├── java/com/makersworld/civic_insights_auth/
│   │   ├── config/
│   │   │   ├── GatewayOnlyFilter.java
│   │   │   ├── JwtKeyProvider.java
│   │   │   ├── JwtProperties.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   ├── SecurityProperties.java
│   │   │   └── WebClientConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── JwkController.java
│   │   │   └── UserProfileController.java
│   │   ├── dto/ ...
│   │   ├── enums/ ...
│   │   ├── model/ ...
│   │   ├── repository/ ...
│   │   ├── security/JwtAuthenticationFilter.java
│   │   ├── service/ ...
│   │   └── CivicInsightsAuthApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/ ...
```

## 📞 Support
Please file an issue in the repository for questions or problems.

## 🆕 Versions
- v1.3.0: Introduced Gateway Only Security, type-safe settings (SecurityProperties), and secure token hand-off for server-driven flow
- v1.2.0: Introduced RSA-based JWT and JWK endpoint

—

**Built with ❤️ for the Civic Insights Platform** 