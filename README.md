# Civic Insights Auth 서비스

OAuth2(Google), JWT, 사용자 프로필을 제공하는 Spring Boot 기반 인증 마이크로서비스입니다. v1.3.0부터는 API Gateway 경유(Gateway Only Security)를 기본 가정으로 합니다.

## 🚀 핵심 기능
- OAuth2 인증(Google): 서버 주도/클라이언트 주도 플로우 지원
- JWT(RS256): RSA 비대칭 키로 액세스/리프레시 토큰 발급·검증
- JWK 공개키: `/.well-known/jwks.json`에서 JWK Set 제공(분산 검증)
- 사용자 프로필: 인증 사용자 프로필 조회/수정 API
- Gateway Only Security: `X-Gateway-Internal` 헤더 기반 내부 게이트웨이 전용 접근 강제
- Swagger UI: 대화형 OpenAPI 문서 제공

## 🛠 기술 스택
- Java 17
- Spring Boot 3.5.4
- Spring Web, Spring Security, Spring Data JPA
- MySQL 8.4+
- JJWT 0.12.6, Nimbus JOSE JWT 10.4
- SpringDoc OpenAPI UI, Lombok, Gradle

## 📋 사전 요구사항
- Java 17+
- MySQL 8.4+
- Gradle(wrapper 포함)

## ⚙️ 설치 및 실행
### 1) 저장소 클론
```bash
git clone <repository-url>
cd civic-insights-auth
```

### 2) 데이터베이스 준비
```sql
CREATE DATABASE civic_insights;
```

### 3) 환경 변수 설정
```bash
# Google OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
# Gateway 경유 콜백 기본값(환경변수로 변경 가능)
export GOOGLE_REDIRECT_URI=http://localhost:8000/api/auth/login/oauth2/code/google

# Gateway Only Security
export GATEWAY_SECRET_TOKEN=your-production-gateway-token

# Frontend 연동 (서버 주도 플로우 콜백 이후 자동 POST 대상)
export FRONTEND_BASE_URL=http://localhost:9002
export FRONTEND_SESSION_POST_URL=http://localhost:9002/api/session
```

### 4) 애플리케이션 실행
```bash
./gradlew bootRun
```
서비스는 `http://localhost:8001`에서 시작됩니다.

### 5) Swagger UI
`http://localhost:8001/swagger-ui.html`

## 🔧 설정 요약(`application.properties`)
```properties
# Server
server.port=8001

# DB
spring.datasource.url=jdbc:mysql://localhost:3312/civic_insights
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
auth.spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# OAuth2 (Gateway 경유 콜백 기본값)
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

# Frontend redirect/hand-off (서버 주도 플로우용)
frontend.redirect-base=${FRONTEND_BASE_URL:http://localhost:9002}
frontend.session-post-url=${FRONTEND_SESSION_POST_URL:http://localhost:9002/api/session}

# OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.try-it-out-enabled=true
```

## 🔐 보안 개요
### JWT
- 알고리즘: RS256
- 공개키: `GET /.well-known/jwks.json`

### Gateway Only Security (v1.3.0)
- 모든 외부 트래픽은 반드시 API Gateway를 경유해야 합니다.
- Gateway는 요청에 `X-Gateway-Internal: <secret>` 헤더를 주입합니다.
- 인증 서비스는 헤더를 검증하고, 직접 접근은 403(Forbidden)으로 차단합니다.
- 환경변수: `GATEWAY_SECRET_TOKEN`으로 시크릿 외부화
- CORS 기본 허용 출처: `http://localhost:8000` (Gateway)

Nginx 예시:
```nginx
location /api/auth/ {
  proxy_pass http://localhost:8001/api/v1/auth/;
  proxy_set_header X-Gateway-Internal "${GATEWAY_SECRET_TOKEN}";
}
```

## 🌐 엔드포인트 개요
Base URL: `http://localhost:8001/api/v1`

### JWK
- `GET /.well-known/jwks.json` 공개키(JWK Set)

### 인증(Auth)
- `GET  /auth/google` Google 로그인 페이지로 리디렉션(서버 주도)
- `POST /auth/google/token` code로 JWT 발급(클라이언트 주도)
- `GET  /auth/login/oauth2/code/google` Google 콜백(내부용)
  - 콜백 성공 시 HTML 자동 제출 폼으로 `FRONTEND_SESSION_POST_URL`로 토큰을 안전하게 POST (URL 노출 방지)
- `POST /auth/refresh` 리프레시 토큰으로 갱신

### 프로필(Profile)
- `GET  /profile` 내 프로필 조회(인증 필요)
- `PUT  /profile` 내 프로필 수정(인증 필요)

## 🧪 테스트
### Gateway 헤더 필수 확인
```bash
# 직접 접근(차단 예상)
curl http://localhost:8001/api/v1/profile

# Gateway 헤더 포함(인증 토큰 없으면 401/403, 필터 통과 확인용)
curl -H "X-Gateway-Internal: ${GATEWAY_SECRET_TOKEN}" http://localhost:8001/api/v1/profile
```

### Swagger UI
`http://localhost:8001/swagger-ui.html`에서 Bearer 토큰으로 보호 API 테스트

## 🏗️ 프로젝트 구조
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

## 📞 지원
문제나 질문은 저장소 이슈로 등록해 주세요.

## 🆕 버전
- v1.3.0: Gateway Only Security 도입, 타입 세이프 설정(SecurityProperties), 서버 주도 플로우의 안전한 토큰 POST 연계 추가
- v1.2.0: RSA 비대칭키 기반 JWT 및 JWK 엔드포인트 도입

—

**Civic Insights 플랫폼을 위해 ❤️로 제작되었습니다** 