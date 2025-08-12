# JWT 비대칭키 인증방식 구현 완전 가이드 🔐

> **대상**: 대학교 졸업반 개발자 지망생  
> **목표**: JWT의 대칭키에서 비대칭키 방식으로의 전환을 이해하고 실습  
> **소요시간**: 약 2-3시간

## 📊 전체 아키텍처 변화

위의 다이어그램에서 볼 수 있듯이, 우리는 HMAC 기반 대칭키 방식에서 RSA 기반 비대칭키 방식으로 전환했습니다. 이 변화는 단순한 기술적 업그레이드가 아닌, **보안성과 확장성을 크게 향상시키는 패러다임 전환**입니다.

---

## 🎯 학습 목표

이 실습을 통해 여러분은:
1. **JWT의 대칭키와 비대칭키 방식의 차이점**을 이해할 수 있습니다
2. **RSA 암호화의 원리**와 JWT에서의 활용법을 습득할 수 있습니다
3. **Spring Boot에서 JWT 비대칭키 구현**을 직접 해볼 수 있습니다
4. **마이크로서비스 환경에서의 JWT 활용**을 이해할 수 있습니다
5. **보안 모범 사례**를 체득할 수 있습니다

---

## 📚 1. 개념 이해하기

### 🔍 JWT란 무엇인가?

JWT(JSON Web Token)는 웹에서 정보를 안전하게 전송하기 위한 표준입니다. 마치 **디지털 신분증**과 같은 역할을 합니다.

```
JWT = Header.Payload.Signature
예시: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature...
```

### 🆚 대칭키 vs 비대칭키

#### 🔑 대칭키 방식 (HMAC)
```
서명: HMAC(비밀키, 데이터) = 서명값
검증: HMAC(같은 비밀키, 데이터) = 서명값과 비교
```

**장점**: 빠르고 간단  
**단점**: 비밀키 공유 문제, 확장성 제한

#### 🔐 비대칭키 방식 (RSA)
```
서명: RSA_SIGN(개인키, 데이터) = 서명값
검증: RSA_VERIFY(공개키, 데이터, 서명값) = true/false
```

**장점**: 보안성 높음, 확장성 우수  
**단점**: 상대적으로 복잡, 약간의 성능 오버헤드

---

## 🛠️ 2. 핵심 구현 코드 분석

### 📦 2.1 RSA 키 관리자 - JwtKeyProvider

이 클래스는 **RSA 키 쌍의 생성과 관리**를 담당합니다.

```java
package com.makersworld.civic_insights_auth.config;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * JWT 토큰 서명 및 검증을 위한 RSA 키 쌍을 관리하는 컴포넌트
 * 비대칭 암호화 방식을 사용하여 보안성을 향상시킵니다.
 */
@Component
public class JwtKeyProvider {

    private KeyPair keyPair;

    /**
     * 애플리케이션 초기화 시 RSA 키 쌍을 생성합니다.
     * 실제 운영 환경에서는 키를 안전하게 외부에서 주입받아야 합니다.
     */
    @PostConstruct
    public void init() {
        // RSA256 알고리즘을 사용하여 키 쌍 생성 (최신 API 사용)
        this.keyPair = Jwts.SIG.RS256.keyPair().build();
    }

    /**
     * JWT 토큰 서명에 사용할 개인키를 반환합니다.
     * @return RSA 개인키
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * JWT 토큰 검증에 사용할 공개키를 반환합니다.
     * @return RSA 공개키
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
```

#### 🧠 이해하기
- `@PostConstruct`: 스프링이 이 빈을 생성한 후 자동으로 `init()` 메서드를 실행
- `Jwts.SIG.RS256.keyPair().build()`: 최신 JJWT API를 사용한 RSA 키 쌍 생성
- **개인키**: 토큰 서명용 (비밀로 유지)
- **공개키**: 토큰 검증용 (공개 가능)

### 🔧 2.2 JWT 서비스 - JwtService

JWT 토큰의 **생성, 검증, 파싱**을 담당하는 핵심 서비스입니다.

```java
package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.config.JwtKeyProvider;
import com.makersworld.civic_insights_auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 생성, 검증 및 파싱을 담당하는 서비스
 * RSA 비대칭 암호화를 사용하여 보안성을 향상시킵니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private final JwtKeyProvider jwtKeyProvider;

    /**
     * JWT 토큰 서명에 사용할 개인키를 반환합니다.
     * @return RSA 개인키
     */
    private PrivateKey getSigningKey() {
        return jwtKeyProvider.getPrivateKey();
    }

    /**
     * JWT 토큰 검증에 사용할 공개키를 반환합니다.
     * @return RSA 공개키
     */
    private PublicKey getVerifyingKey() {
        return jwtKeyProvider.getPublicKey();
    }

    /**
     * 이메일과 역할 정보로 액세스 토큰을 생성합니다.
     * @param email 사용자 이메일
     * @param role 사용자 역할
     * @return JWT 액세스 토큰
     */
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, email);
    }

    /**
     * 이메일로 리프레시 토큰을 생성합니다.
     * @param email 사용자 이메일
     * @return JWT 리프레시 토큰
     */
    public String generateRefreshToken(String email) {
        return createToken(new HashMap<>(), email, jwtProperties.getRefreshExpiration());
    }

    /**
     * 기본 만료시간으로 토큰을 생성합니다.
     * @param claims 토큰에 포함할 클레임
     * @param subject 토큰 주체 (이메일)
     * @return JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, jwtProperties.getExpirationMs());
    }

    /**
     * 지정된 만료시간으로 토큰을 생성합니다.
     * @param claims 토큰에 포함할 클레임
     * @param subject 토큰 주체 (이메일)
     * @param expiration 만료시간 (밀리초)
     * @return JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date(System.currentTimeMillis());
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey()) // RSA 개인키로 서명
                .compact();
    }

    /**
     * 토큰의 유효성을 검증합니다.
     * @param token JWT 토큰
     * @param email 검증할 이메일
     * @return 토큰 유효 여부
     */
    public Boolean validateToken(String token, String email) {
        try {
            final String username = extractEmail(token);
            return (username.equals(email) && !isTokenExpired(token));
        } catch (Exception e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 이메일을 추출합니다.
     * @param token JWT 토큰
     * @return 이메일
     */
    public String extractEmail(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.debug("토큰에서 이메일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 역할 정보를 추출합니다.
     * @param token JWT 토큰
     * @return 역할
     */
    public String extractRole(String token) {
        try {
            return extractClaim(token, claims -> claims.get("role", String.class));
        } catch (Exception e) {
            log.debug("토큰에서 역할 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 만료시간을 추출합니다.
     * @param token JWT 토큰
     * @return 만료시간
     */
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.debug("토큰에서 만료시간 추출 실패: {}", e.getMessage());
            return new Date(0); // 과거 날짜 반환하여 만료된 것으로 처리
        }
    }

    /**
     * 토큰에서 특정 클레임을 추출합니다.
     * @param token JWT 토큰
     * @param claimsResolver 클레임 추출 함수
     * @return 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            log.debug("토큰에서 클레임 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 모든 클레임을 추출합니다.
     * RSA 공개키를 사용하여 토큰을 검증합니다.
     * @param token JWT 토큰
     * @return 클레임 객체
     * @throws RuntimeException 토큰 파싱 또는 검증 실패 시
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getVerifyingKey()) // RSA 공개키로 검증
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * 토큰이 만료되었는지 확인합니다.
     * @param token JWT 토큰
     * @return 만료 여부
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.debug("토큰 만료 확인 실패: {}", e.getMessage());
            return true; // 오류 시 만료된 것으로 처리
        }
    }
}
```

#### 🧠 핵심 개념
1. **서명**: `signWith(getSigningKey())` - 개인키로 서명
2. **검증**: `verifyWith(getVerifyingKey())` - 공개키로 검증
3. **예외 처리**: 모든 토큰 관련 작업에 try-catch 적용
4. **로깅**: 디버깅을 위한 상세한 로그 기록

### 🌐 2.3 공개키 배포 - JwkController

다른 서비스들이 JWT를 독립적으로 검증할 수 있도록 **공개키를 배포**하는 엔드포인트입니다.

```java
package com.makersworld.civic_insights_auth.controller;

import com.makersworld.civic_insights_auth.config.JwtKeyProvider;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * JWT 공개키를 JWK(JSON Web Key) 형식으로 제공하는 컨트롤러
 * 클라이언트가 JWT 토큰을 독립적으로 검증할 수 있도록 공개키를 노출합니다.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "JWK", description = "JSON Web Key 관련 API")
public class JwkController {

    private final JwtKeyProvider jwtKeyProvider;

    /**
     * JWT 검증을 위한 공개키를 JWK 형식으로 제공합니다.
     * 이 엔드포인트는 OAuth2/OpenID Connect 표준을 따라 구현되었습니다.
     * 
     * @return JWK Set JSON 객체
     */
    @GetMapping("/.well-known/jwks.json")
    @Operation(
        summary = "JWT 공개키 조회",
        description = "JWT 토큰 검증을 위한 공개키를 JWK(JSON Web Key) 형식으로 반환합니다."
    )
    public Map<String, Object> getJwks() {
        // RSA 공개키를 JWK 형식으로 변환
        RSAPublicKey publicKey = (RSAPublicKey) jwtKeyProvider.getPublicKey();
        JWK jwk = new RSAKey.Builder(publicKey)
                .keyID("civic-insights-auth-key") // 키 식별자 설정
                .build();
        
        return new JWKSet(jwk).toJSONObject();
    }
}
```

#### 🧠 JWK의 의미
- **JWK**: JSON Web Key의 약자로, 공개키를 JSON 형태로 표현
- **표준 경로**: `/.well-known/jwks.json`은 OAuth2/OpenID Connect 표준
- **목적**: 다른 서비스들이 이 공개키를 가져가서 JWT를 독립적으로 검증

### 🛡️ 2.4 인증 필터 - JwtAuthenticationFilter

HTTP 요청마다 JWT 토큰을 확인하고 **사용자를 인증**하는 필터입니다.

```java
package com.makersworld.civic_insights_auth.security;

import com.makersworld.civic_insights_auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 토큰 기반 인증을 처리하는 필터
 * RSA 비대칭키를 사용하여 토큰을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // "Bearer " 이후의 토큰 추출
            jwt = authHeader.substring(7);
            
            // 토큰에서 사용자 이메일 추출 (RSA 공개키로 검증)
            userEmail = jwtService.extractEmail(jwt);

            // 이메일이 존재하고 현재 인증 컨텍스트가 없는 경우
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 토큰 유효성 검증
                if (jwtService.validateToken(jwt, userEmail)) {
                    // 인증 토큰 생성 및 설정
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail, null, new ArrayList<>()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT 토큰 인증 성공: {}", userEmail);
                } else {
                    log.debug("JWT 토큰 검증 실패: 유효하지 않은 토큰");
                }
            }
        } catch (Exception e) {
            // JWT 파싱 또는 검증 실패 시 로그 기록하고 인증 없이 진행
            log.debug("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패해도 요청은 계속 진행 (다른 인증 방법이 있을 수 있음)
        }
        
        filterChain.doFilter(request, response);
    }
}
```

#### 🧠 필터의 동작 원리
1. **요청 가로채기**: 모든 HTTP 요청을 가로채서 JWT 토큰 확인
2. **헤더 추출**: `Authorization: Bearer <token>` 형태에서 토큰 추출
3. **토큰 검증**: RSA 공개키로 토큰의 유효성 검증
4. **인증 설정**: 유효한 토큰이면 Spring Security 컨텍스트에 인증 정보 설정
5. **예외 처리**: 토큰이 잘못되어도 애플리케이션이 중단되지 않도록 처리

### ⚙️ 2.5 설정 클래스들

#### JWT 프로퍼티 설정

```java
package com.makersworld.civic_insights_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 관련 설정을 관리하는 프로퍼티 클래스
 * 비대칭키 방식 사용으로 secretKey는 제거되었습니다.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private long expirationMs;
    private long refreshExpiration;
    
    // Getters and setters
    public long getExpirationMs() {
        return expirationMs;
    }
    
    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
    
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
    
    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
```

#### 보안 설정

```java
// SecurityConfig.java에서 JWK 엔드포인트 공개 허용
.requestMatchers(
    "/.well-known/jwks.json",  // 공개키 엔드포인트 - 인증 불필요
    "/api/v1/auth/**", 
    "/error",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
).permitAll()
```

---

## 🔄 3. 변경 전후 비교

### 📋 설정 파일 변화

#### Before (대칭키)
```properties
# JWT 설정
jwt.secret-key=${JWT_SECRET_KEY:mySecretKey123456789012345678901234567890}
jwt.expiration-ms=86400000
jwt.refresh-expiration=604800000
```

#### After (비대칭키)
```properties
# JWT 설정 (RSA 비대칭키 사용으로 secret-key 제거됨)
jwt.expiration-ms=86400000
jwt.refresh-expiration=604800000
```

### 🔐 토큰 서명 방식 변화

#### Before (HMAC)
```java
// 대칭키 방식
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
}

String token = Jwts.builder()
    .claims(claims)
    .subject(subject)
    .signWith(getSigningKey()) // HMAC 서명
    .compact();
```

#### After (RSA)
```java
// 비대칭키 방식
private PrivateKey getSigningKey() {
    return jwtKeyProvider.getPrivateKey();
}

private PublicKey getVerifyingKey() {
    return jwtKeyProvider.getPublicKey();
}

String token = Jwts.builder()
    .claims(claims)
    .subject(subject)
    .signWith(getSigningKey()) // RSA 개인키로 서명
    .compact();

// 검증 시
Jwts.parser()
    .verifyWith(getVerifyingKey()) // RSA 공개키로 검증
    .build()
    .parseSignedClaims(token);
```

---

## 🚀 4. 실습하기

### 🎯 4.1 프로젝트 설정

#### 의존성 추가 (build.gradle)
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
implementation 'com.nimbusds:nimbus-jose-jwt:10.4'
```

#### 애플리케이션 설정 (application.properties)
```properties
# JWT 설정 (RSA 비대칭키 사용으로 secret-key 제거됨)
jwt.expiration-ms=86400000
jwt.refresh-expiration=604800000
```

### 🧪 4.2 테스트해보기

#### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

#### 2. 공개키 확인
```bash
curl http://localhost:8001/.well-known/jwks.json
```

**예상 응답:**
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

#### 3. 로그인 후 토큰 발급
```bash
curl -X POST http://localhost:8001/api/v1/auth/google/token \
  -H "Content-Type: application/json" \
  -d '{"code": "google_auth_code"}'
```

#### 4. JWT 토큰으로 API 호출
```bash
curl -H "Authorization: Bearer <발급받은_토큰>" \
  http://localhost:8001/api/v1/profile
```

### 🔍 4.3 디버깅 팁

#### 로그 레벨 설정
```properties
# application.properties에 추가
logging.level.com.makersworld.civic_insights_auth=DEBUG
```

#### JWT 토큰 디코딩 (jwt.io 사용)
1. https://jwt.io 접속
2. 발급받은 토큰 붙여넣기
3. Header와 Payload 확인

---

## 📊 5. 장점과 실제 활용

### ✅ 비대칭키 방식의 장점

#### 🔒 보안성 향상
- **키 분리**: 서명용 개인키와 검증용 공개키 분리
- **키 노출 위험 감소**: 공개키가 노출되어도 토큰 위조 불가능
- **중앙화된 키 관리**: 개인키만 안전하게 보관하면 됨

#### 🌐 확장성 향상
- **분산 검증**: 각 마이크로서비스가 독립적으로 토큰 검증
- **네트워크 트래픽 감소**: 중앙 인증 서버에 매번 요청할 필요 없음
- **서비스 독립성**: 인증 서버 장애 시에도 토큰 검증 가능

#### 🛠️ 운영 효율성
- **키 배포 용이**: 공개키는 자유롭게 배포 가능
- **설정 단순화**: 각 서비스에서 비밀키 관리 불필요
- **표준 준수**: OAuth2/OpenID Connect 표준 준수

### 🏢 실제 활용 시나리오

#### 마이크로서비스 아키텍처
```
인증 서비스 (Auth Service)
├── 개인키로 JWT 서명
└── /.well-known/jwks.json로 공개키 배포

사용자 서비스 (User Service)
├── 공개키 다운로드
└── JWT 독립 검증

주문 서비스 (Order Service)
├── 공개키 다운로드
└── JWT 독립 검증

결제 서비스 (Payment Service)
├── 공개키 다운로드
└── JWT 독립 검증
```

#### API 게이트웨이 패턴
```
API Gateway
├── 공개키로 JWT 검증
├── 유효한 요청만 백엔드로 전달
└── 인증 서버 부하 감소
```

---

## ⚠️ 6. 주의사항과 모범 사례

### 🚨 보안 주의사항

#### 개인키 보안
```java
// ❌ 나쁜 예: 개인키를 코드에 하드코딩
private static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----...";

// ✅ 좋은 예: 환경변수나 키 관리 시스템 사용
@Value("${jwt.private-key-path}")
private String privateKeyPath;
```

#### 토큰 만료 시간
```properties
# ✅ 적절한 만료 시간 설정
jwt.expiration-ms=900000        # 15분 (액세스 토큰)
jwt.refresh-expiration=604800000 # 7일 (리프레시 토큰)
```

#### 예외 처리
```java
// ✅ 모든 JWT 관련 작업에 예외 처리 적용
try {
    Claims claims = extractAllClaims(token);
    return claims.getSubject();
} catch (Exception e) {
    log.debug("토큰 파싱 실패: {}", e.getMessage());
    return null; // 안전한 기본값 반환
}
```

### 🎯 운영 모범 사례

#### 키 순환 (Key Rotation)
```java
// 정기적인 키 교체를 위한 다중 키 지원
@Component
public class JwtKeyProvider {
    private Map<String, KeyPair> keyPairs = new HashMap<>();
    private String currentKeyId;
    
    @Scheduled(fixedRate = 2592000000L) // 30일마다
    public void rotateKeys() {
        // 새로운 키 생성 및 등록
    }
}
```

#### 모니터링
```java
// 인증 관련 메트릭 수집
@EventListener
public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
    meterRegistry.counter("jwt.authentication.success").increment();
}

@EventListener
public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    meterRegistry.counter("jwt.authentication.failure").increment();
}
```

#### 성능 최적화
```java
// 공개키 캐싱
@Cacheable("jwt-public-keys")
public RSAPublicKey getPublicKey() {
    return (RSAPublicKey) keyPair.getPublic();
}
```

---

## 🎓 7. 마무리 및 학습 점검

### 📝 학습 체크리스트

- [ ] JWT의 구조(Header.Payload.Signature)를 이해했다
- [ ] 대칭키와 비대칭키의 차이점을 설명할 수 있다
- [ ] RSA 키 쌍의 역할(개인키: 서명, 공개키: 검증)을 이해했다
- [ ] JwtKeyProvider의 역할을 설명할 수 있다
- [ ] JwtService의 토큰 생성/검증 과정을 이해했다
- [ ] JwkController의 필요성과 역할을 설명할 수 있다
- [ ] JWT 인증 필터의 동작 과정을 이해했다
- [ ] 예외 처리의 중요성을 이해했다
- [ ] 마이크로서비스에서의 JWT 활용을 설명할 수 있다
- [ ] 보안 모범 사례를 적용할 수 있다

### 🚀 다음 단계 학습 주제

1. **JWT 고급 활용**
   - 토큰 갱신 (Refresh Token) 메커니즘
   - 토큰 무효화 (Token Revocation) 전략
   - 다중 키 지원 및 키 순환

2. **보안 강화**
   - JWE (JSON Web Encryption) 적용
   - 토큰 지문 (Token Fingerprinting)
   - CSRF 및 XSS 방어

3. **성능 최적화**
   - 토큰 캐싱 전략
   - 공개키 CDN 배포
   - 토큰 압축 기법

4. **운영 및 모니터링**
   - JWT 관련 메트릭 수집
   - 보안 이벤트 모니터링
   - 장애 대응 전략

### 💡 실무 팁

1. **코드 리뷰 시 체크포인트**
   - 개인키 하드코딩 여부
   - 예외 처리 적절성
   - 토큰 만료 시간 설정
   - 로깅 레벨 적절성

2. **테스트 전략**
   - 유효한 토큰 테스트
   - 만료된 토큰 테스트
   - 잘못된 서명 토큰 테스트
   - 공개키 엔드포인트 테스트

3. **운영 환경 고려사항**
   - 키 관리 시스템 (KMS) 활용
   - 로드 밸런서 설정
   - 캐시 전략 수립
   - 모니터링 대시보드 구성

---

## 📚 참고 자료

### 📖 공식 문서
- [JWT.io](https://jwt.io/) - JWT 디코더 및 라이브러리 정보
- [RFC 7519](https://tools.ietf.org/html/rfc7519) - JWT 공식 명세
- [RFC 7517](https://tools.ietf.org/html/rfc7517) - JWK 공식 명세
- [JJWT 라이브러리](https://github.com/jwtk/jjwt) - Java JWT 라이브러리

### 🔗 추가 학습 자료
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [OAuth 2.0 Security Best Current Practice](https://tools.ietf.org/html/draft-ietf-oauth-security-topics)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)

---

**🎉 축하합니다!** 

JWT 비대칭키 인증방식의 구현을 완전히 마스터했습니다. 이제 여러분은 보안성과 확장성을 갖춘 현대적인 인증 시스템을 구축할 수 있는 실력을 갖추었습니다. 

계속해서 실무에서 이 지식을 활용하고, 더 나은 보안 시스템을 만들어가세요! 🚀 