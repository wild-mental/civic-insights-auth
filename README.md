# Civic Insights Auth 서비스

Civic Insights 플랫폼을 위한 OAuth2 통합, JWT 토큰 관리, 사용자 프로필 서비스를 제공하는 Spring Boot 인증 마이크로서비스입니다.

## 🚀 기능

- **OAuth2 인증** - Google OAuth2 통합
- **JWT 토큰 관리** - RSA 비대칭키 기반 액세스 토큰 생성, 검증 및 갱신
- **사용자 프로필 관리** - 완전한 사용자 프로필 CRUD 작업
- **마이크로서비스 아키텍처** - 전용 인증 서비스로 설계
- **RESTful API** - 적절한 HTTP 상태 코드를 가진 깔끔한 REST 엔드포인트
- **대화형 API 문서** - Swagger UI를 통한 완전한 OpenAPI 3.0 문서
- **보안** - JWT 기반 인증을 포함한 Spring Security
- **데이터베이스 통합** - JPA/Hibernate를 사용한 MySQL

## 🛠 기술 스택

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Security 6.5.1**
- **Spring Data JPA**
- **MySQL 8.4+**
- **JWT (JJWT 0.12.6)** - RSA 비대칭키 암호화
- **Nimbus JOSE JWT 10.4** - JWK(JSON Web Key) 지원
- **OpenAPI 3.0** - SpringDoc OpenAPI UI for interactive documentation
- **Lombok**
- **Gradle**

## 📋 사전 요구사항

- Java 17 이상
- MySQL 8.4 이상
- Gradle 8.14.3 이상 (wrapper 포함)

## ⚙️ 설치 및 설정

### 1. 저장소 클론
```bash
git clone <repository-url>
cd civic-insights-auth
```

### 2. 데이터베이스 설정
MySQL 데이터베이스 생성:
```sql
CREATE DATABASE civic_insights;
```

### 3. 환경 변수
다음 환경 변수를 설정하거나 `application.properties`를 업데이트하세요:

```bash
# Google OAuth2
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export GOOGLE_REDIRECT_URI=http://localhost:8001/api/v1/auth/login/oauth2/code/google

# JWT 설정 (RSA 비대칭키 사용으로 더 이상 JWT_SECRET_KEY 불필요)
```

### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

서비스는 `http://localhost:8001`에서 시작됩니다.

### 5. Swagger UI 접근
애플리케이션이 실행되면 다음 URL에서 대화형 API 문서에 접근할 수 있습니다:
- **Swagger UI**: http://localhost:8001/swagger-ui.html

## 📚 API 문서

### 🌐 대화형 API 문서 (Swagger UI)
**Swagger UI**: http://localhost:8001/swagger-ui.html
- 모든 API 엔드포인트의 완전한 대화형 문서
- "Try it out" 기능으로 직접 API 테스트 가능
- JWT 인증 통합 (Authorize 버튼 사용)
- 요청/응답 스키마 및 예제 제공
- **실시간 API 테스트**: 브라우저에서 직접 모든 엔드포인트 테스트 가능

### 📋 OpenAPI 스펙
- **JSON 형식**: http://localhost:8001/v3/api-docs
- **YAML 형식**: http://localhost:8001/v3/api-docs.yaml
- **SpringDoc OpenAPI UI**: 최신 OpenAPI 3.0 표준 준수
- **보안 스키마**: JWT Bearer 토큰 인증 지원

### 기본 URL
```
http://localhost:8001/api/v1
```

### JWT 공개키 조회 (JWK)
**엔드포인트:** `GET /.well-known/jwks.json`
- **동작**: JWT 토큰 검증을 위한 공개키를 JWK(JSON Web Key) 형식으로 제공합니다.
- **사용법**: 외부 서비스가 독립적으로 JWT 토큰을 검증할 수 있습니다.
```bash
curl http://localhost:8001/.well-known/jwks.json
```

**응답:**
```json
{
  "keys": [
    {
      "kty": "RSA",
      "n": "...",
      "e": "AQAB",
      "kid": "civic-insights-auth-key"
    }
  ]
}
```

### 인증 엔드포인트

#### Google OAuth2 로그인 시작 (서버 주도)
**엔드포인트:** `GET /auth/google`
- **동작**: 이 엔드포인트로 접속하면, 서버는 사용자를 Google 로그인 및 동의 화면으로 즉시 리디렉션합니다.
- **사용법**: 프론트엔드에서 "Google로 로그인" 버튼 클릭 시 이 엔드포인트로 연결합니다.
  ```html
  <a href="http://localhost:8001/api/v1/auth/google">Google로 로그인</a>
  ```

#### Google OAuth2 토큰 발급 (클라이언트 주도)
**엔드포인트:** `POST /auth/google/token`
- **동작**: 클라이언트(모바일 앱 등)가 직접 얻은 `authorization_code`를 서버에 보내 JWT 토큰을 발급받습니다.
```bash
curl -X POST http://localhost:8001/api/v1/auth/google/token \
  -H "Content-Type: application/json" \
  -d '{"code": "google_auth_code"}'
```

**응답 (성공 시):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "email": "user@example.com",
  "name": "홍길동",
  "role": "USER"
}
```

#### Google OAuth2 콜백 (내부용)
**엔드포인트:** `GET /auth/login/oauth2/code/google?code={auth_code}`
- **동작**: Google 인증 후, Google이 사용자를 이 엔드포인트로 리디렉션합니다. 서버는 `code`를 받아 내부적으로 토큰 교환을 처리합니다.
- **⚠️ 중요**: 사용자가 직접 호출하는 엔드포인트가 아닙니다. 수동 테스트 시 400 오류가 발생하는 것이 정상입니다.

**실제 사용 플로우 (서버 주도)**:
```
1. 사용자: GET /auth/google 클릭
2. 서버: Google 로그인 페이지로 302 리디렉션
3. 사용자: Google 인증 및 동의
4. Google: GET /auth/login/oauth2/code/google 로 code와 함께 리디렉션
5. 서버: code로 JWT 발급 후 클라이언트에 전달 (실제 앱에서는 리디렉션 등으로 전달)
```

#### 토큰 갱신
**엔드포인트:** `POST /auth/refresh`
```bash
curl -X POST http://localhost:8001/api/v1/auth/refresh \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "refreshToken=your_refresh_token"
```

### 프로필 관리 엔드포인트

#### 사용자 프로필 조회
**엔드포인트:** `GET /profile`
```bash
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8001/api/v1/profile
```

**응답:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "name": "홍길동",
  "bio": "소프트웨어 엔지니어",
  "location": "서울, 대한민국",
  "website": "https://johndoe.com",
  "phoneNumber": "+82-10-1234-5678",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

#### 사용자 프로필 업데이트
**엔드포인트:** `PUT /profile`
```bash
curl -X PUT http://localhost:8001/api/v1/profile \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bio": "업데이트된 소개",
    "location": "부산, 대한민국",
    "website": "https://newsite.com",
    "phoneNumber": "+82-10-9876-5432",
    "avatarUrl": "https://example.com/new-avatar.jpg"
  }'
```

## 🗄️ 데이터베이스 스키마

### 사용자 테이블
```sql
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NULL,
  `name` VARCHAR(255) NOT NULL,
  `provider` VARCHAR(50) NOT NULL,
  `provider_id` VARCHAR(255) NULL,
  `role` VARCHAR(50) NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 사용자 프로필 테이블
```sql
CREATE TABLE `user_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `bio` TEXT NULL,
  `location` VARCHAR(255) NULL,
  `website` VARCHAR(255) NULL,
  `phone_number` VARCHAR(50) NULL,
  `avatar_url` VARCHAR(500) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_user_id` (`user_id` ASC),
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 🔧 설정

### Application Properties
```properties
# 서버 설정
server.port=8001

# 데이터베이스 설정
spring.datasource.url=jdbc:mysql://localhost:3312/civic_insights
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# OAuth2 설정
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI}

# JWT 설정 (RSA 비대칭키 사용으로 secret-key 제거됨)
app.jwt.expiration-ms=86400000
app.jwt.refresh-expiration=604800000

# SpringDoc OpenAPI 설정
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.try-it-out-enabled=true
```

## 🏗️ 프로젝트 구조

```
src/
├── main/
│   ├── java/com/makersworld/civic_insights_auth/
│   │   ├── config/
│   │   │   ├── JwtKeyProvider.java
│   │   │   ├── JwtProperties.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebClientConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   ├── JwkController.java
│   │   │   └── UserProfileController.java
│   │   ├── dto/
│   │   │   ├── AuthRequest.java
│   │   │   ├── AuthResponse.java
│   │   │   ├── GoogleTokenResponse.java
│   │   │   ├── GoogleUserInfoResponse.java
│   │   │   ├── UpdateProfileRequest.java
│   │   │   └── UserProfileDto.java
│   │   ├── enums/
│   │   │   ├── Provider.java
│   │   │   └── Role.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   └── UserProfile.java
│   │   ├── repository/
│   │   │   ├── UserProfileRepository.java
│   │   │   └── UserRepository.java
│   │   ├── security/
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── service/
│   │   │   ├── AuthService.java
│   │   │   ├── GoogleOAuth2Service.java
│   │   │   ├── JwtService.java
│   │   │   ├── UserProfileService.java
│   │   │   └── UserService.java
│   │   └── CivicInsightsAuthApplication.java
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/com/makersworld/civic_insights_auth/
        └── CivicInsightsAuthApplicationTests.java
```

## 👤 사용자 프로필 동작

### OAuth2 인증 플로우
사용자가 Google OAuth2로 인증할 때:

1. **사용자 레코드**: `users` 테이블을 다음과 같이 생성 또는 업데이트합니다:
   - 이메일, 이름, 공급자 정보
   - 계정이 이미 존재하는 경우 기존 사용자의 이름을 업데이트

2. **사용자 프로필**: **신규 사용자에 대해 자동으로 생성**
   - OAuth2 인증 중 **즉시** 프로필 레코드가 생성됩니다
   - 사용 가능한 Google 데이터(프로필 사진)로 미리 채워집니다
   - 다른 프로필 필드(소개, 위치, 웹사이트 등)는 `null`로 시작됩니다
   - 기존 사용자: 사용자 커스터마이징을 보존하기 위해 프로필이 수정되지 않습니다

### 프로필 생성 타임라인
```
Google OAuth2 → 사용자 생성/업데이트 → UserProfile 생성 (신규 사용자만) → JWT 발급
                                               ↓
                                    Google에서 아바타가 준비된 프로필
```

### 자동 채워지는 Google 데이터
신규 사용자의 경우, 다음 Google 데이터가 자동으로 프로필에 저장됩니다:
- ✅ **프로필 사진 URL** (avatarUrl 필드)
- ✅ **사용자 이름** (User 테이블에 저장)
- ✅ **이메일** (User 테이블에 저장)

### 수동 프로필 필드
이 필드들은 사용자가 `/api/v1/profile`을 통해 업데이트할 때까지 비어있습니다:
- 소개
- 위치
- 웹사이트
- 전화번호

**참고**: 기존 사용자의 프로필은 커스터마이징을 보존하기 위해 자동으로 수정되지 않습니다. 신규 등록만 자동으로 데이터가 채워집니다.

## 🔐 보안

### JWT 토큰 구조
- **액세스 토큰**: 24시간 유효 (86400000ms)
- **리프레시 토큰**: 7일 유효 (604800000ms)
- **알고리즘**: RSA SHA-256 (RS256) - 비대칭키 암호화
- **공개키 엔드포인트**: `/.well-known/jwks.json`

### 보호된 엔드포인트
모든 `/api/v1/profile/**` 엔드포인트는 Authorization 헤더에 유효한 JWT 토큰이 필요합니다:
```
Authorization: Bearer <access_token>
```

### CORS 설정
- **허용된 Origin**: `http://localhost:3000` (React 프론트엔드)
- **허용된 메서드**: GET, POST, PUT, DELETE, OPTIONS
- **자격 증명**: 활성화됨

## 🧪 테스트

### 테스트 실행
```bash
./gradlew test
```

### 🛠️ 일반적인 문제 해결

#### Q: OAuth2 콜백 엔드포인트가 400 오류를 반환합니다
**A**: 이는 정상적인 동작입니다!
```bash
# ❌ 이렇게 테스트하면 400 오류 (예상됨)
curl http://localhost:8001/api/v1/auth/login/oauth2/code/google

# ✅ 대신 POST 엔드포인트를 사용하세요
curl -X POST http://localhost:8001/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"code": "google_auth_code"}'
```

#### Q: "Required parameter 'code' is not present" 오류가 발생합니다
**A**: OAuth2 콜백 엔드포인트는 Google에서만 호출되어야 합니다.
- 수동 테스트용: `POST /api/v1/auth/google` 사용
- 웹 플로우용: `GET /api/v1/auth/login/oauth2/code/google` (Google 전용)

#### Q: JWT 토큰을 어떻게 테스트하나요?
**A**: Swagger UI를 사용하세요:
1. http://localhost:8001/swagger-ui.html 접속
2. "Authorize" 버튼 클릭
3. `Bearer <your-jwt-token>` 형식으로 토큰 입력
4. 보호된 엔드포인트 테스트

#### Q: "환경 변수가 적용되지 않아요"
**A**: 환경 변수 설정을 다시 확인하세요:
```bash
# 현재 환경 변수 확인
echo $GOOGLE_CLIENT_ID
echo $GOOGLE_CLIENT_SECRET
echo $GOOGLE_REDIRECT_URI

# 올바른 설정 예시
export GOOGLE_CLIENT_ID="실제_Google_클라이언트_ID"
export GOOGLE_CLIENT_SECRET="실제_Google_클라이언트_시크릿"
export GOOGLE_REDIRECT_URI="http://localhost:8001/api/v1/auth/login/oauth2/code/google"

# 환경 변수와 함께 애플리케이션 실행
GOOGLE_CLIENT_ID=your-id GOOGLE_CLIENT_SECRET=your-secret ./gradlew bootRun
```

#### Q: OAuth2 리디렉션이 작동하지 않아요
**A**: Google Cloud Console 설정을 확인하세요:
1. **Google Cloud Console** → APIs & Services → Credentials
2. **OAuth 2.0 Client IDs** 선택
3. **Authorized redirect URIs**에 정확히 추가:
   ```
   http://localhost:8001/api/v1/auth/login/oauth2/code/google
   ```
4. **저장** 후 몇 분 기다린 후 테스트

### 수동 API 테스트
1. **Swagger UI 사용 (권장)**: http://localhost:8001/swagger-ui.html
   - 브라우저에서 직접 API 테스트
   - JWT 토큰으로 인증 후 보호된 엔드포인트 테스트 가능
   
2. **cURL 사용**: 제공된 curl 예제 사용

3. **API 클라이언트**: Postman/Insomnia로 OpenAPI 스펙 가져오기

### 🔍 OAuth2 엔드포인트 테스트 이해하기

#### ✅ 테스트 가능한 엔드포인트
```bash
# Google OAuth2 로그인 (POST 방식)
curl -X POST http://localhost:8001/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"code": "real_google_auth_code"}'

# 토큰 갱신
curl -X POST http://localhost:8001/api/v1/auth/refresh \
  -d "refreshToken=your_refresh_token"

# 사용자 프로필 (JWT 토큰 필요)
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:8001/api/v1/profile
```

#### ⚠️ OAuth2 콜백 엔드포인트 주의사항
```bash
# 이 엔드포인트는 400 오류를 반환합니다 (정상 동작)
curl http://localhost:8001/api/v1/auth/login/oauth2/code/google

# 이유: Google OAuth2 전용 엔드포인트로, 실제 인증 코드가 필요합니다
# 수동 테스트가 아닌 Google OAuth2 플로우에서만 사용됩니다
```

**OAuth2 콜백 엔드포인트가 400 오류를 반환하는 이유**:
- 🔒 **보안 설계**: 유효하지 않은 요청을 거부
- 📋 **OAuth2 사양**: Google OAuth2 코드는 일회용이며 암호화된 보안 코드
- 🛡️ **입력 검증**: 임의의 테스트 문자열은 거부되어야 함

**실제 Google OAuth2 플로우**:
1. 사용자가 "Google로 로그인" 클릭
2. Google 동의 화면으로 리디렉션
3. 사용자가 권한 승인
4. Google이 실제 인증 코드로 콜백 엔드포인트 호출
5. 서비스가 JWT 토큰 반환

## 🚀 배포

### 프로덕션 고려사항
1. **환경 변수**: 필요한 OAuth2 환경 변수를 설정하세요
2. **데이터베이스**: 프로덕션 MySQL 인스턴스를 구성하세요
3. **HTTPS**: 프로덕션에서 SSL/TLS를 활성화하세요
4. **CORS**: 프로덕션 프론트엔드 URL에 대해 허용된 origin을 업데이트하세요
5. **JWT 키 관리**: 프로덕션에서는 외부 키 관리 시스템(KMS)을 사용하여 RSA 키 쌍을 안전하게 관리하세요

### Docker (선택사항)
```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/civic-insights-auth-*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 🤝 다른 서비스와의 통합

이 인증 서비스는 마이크로서비스 아키텍처에서 작동하도록 설계되었습니다:

1. **프론트엔드 애플리케이션**: 사용자 인증을 위해 OAuth2 및 JWT 엔드포인트를 사용
2. **다른 마이크로서비스**: `/.well-known/jwks.json` 엔드포인트에서 공개키를 가져와 JWT 토큰을 독립적으로 검증
3. **API 게이트웨이**: 이 서비스로 인증 요청을 라우팅하거나 공개키를 사용하여 분산 검증 수행

### 통합 예제
```javascript
// 프론트엔드: Google로 로그인
const response = await fetch('/api/v1/auth/google', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ code: googleAuthCode })
});

const { accessToken } = await response.json();

// 후속 API 호출에 토큰 사용
const profileResponse = await fetch('/api/v1/profile', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
```

## 📝 라이선스

이 프로젝트는 Civic Insights 플랫폼의 일부입니다.

## 📞 지원

질문이나 문제가 있으시면 개발팀에 문의하거나 저장소에 이슈를 생성해 주세요.

---

## 🆕 최근 업데이트

### v1.2.0 - RSA 비대칭키 JWT 구현 완료
- ✅ **RSA 비대칭키 JWT** 대칭키에서 비대칭키 방식으로 업그레이드
- ✅ **JWK 엔드포인트** `/.well-known/jwks.json` 공개키 제공
- ✅ **보안 강화** RSA256 알고리즘 사용으로 보안성 향상
- ✅ **마이크로서비스 친화적** 분산 환경에서 독립적 토큰 검증 지원
- ✅ **라이브러리 업데이트** JJWT 0.12.6, Nimbus JOSE JWT 10.4 적용
- ✅ **설정 단순화** JWT secret-key 제거로 설정 단순화

### v1.1.0 - OpenAPI/Swagger 통합 완료
- ✅ **SpringDoc OpenAPI UI** 통합 완료
- ✅ **대화형 API 문서** http://localhost:8001/swagger-ui.html
- ✅ **JWT 인증 통합** Swagger UI에서 직접 토큰 테스트 가능
- ✅ **OAuth2 엔드포인트 최적화** 및 오류 처리 개선
- ✅ **보안 설정 업데이트** SpringDoc 엔드포인트 지원
- ✅ **상세한 문제 해결 가이드** 추가
- ✅ **환경 변수 설정 가이드** 완성

### 주요 개선사항 (v1.2.0)
- **보안 아키텍처**: 대칭키에서 RSA 비대칭키로 JWT 보안 모델 업그레이드
- **분산 검증**: 공개키 배포를 통한 마이크로서비스 독립적 토큰 검증
- **표준 준수**: OAuth2/OpenID Connect JWK 표준 엔드포인트 구현
- **운영 효율성**: 키 관리 단순화 및 설정 복잡도 감소
- **확장성**: 분산 환경에서 토큰 검증 성능 및 보안성 향상

### 주요 개선사항 (v1.1.0)
- **API 문서화**: OpenAPI 3.0 표준 준수하는 완전한 대화형 문서
- **개발자 경험**: Swagger UI에서 직접 API 테스트 및 JWT 인증
- **오류 처리**: OAuth2 콜백 엔드포인트 400 오류에 대한 명확한 설명
- **설정 가이드**: Google Cloud Console 및 환경 변수 설정 상세 가이드
- **보안 강화**: 올바른 엔드포인트만 공개하도록 보안 설정 최적화

---

**Civic Insights 플랫폼을 위해 ❤️로 제작되었습니다** 