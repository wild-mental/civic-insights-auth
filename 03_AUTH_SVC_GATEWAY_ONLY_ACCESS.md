# 🔐 인증 서비스 Gateway Only Security 구현 가이드

> **전제조건**: v1.2.0 RSA 비대칭키 JWT 구현 완료  
> **목표**: API Gateway를 통한 보안 접근 제어 구현  

## 📚 학습 목표

이 교육을 통해 다음을 이해하고 구현할 수 있습니다:
- ✅ **마이크로서비스 보안 패턴** 이해
- ✅ **Gateway Only Security** 개념과 필요성
- ✅ **Spring Boot Filter Chain** 동작 원리
- ✅ **@ConfigurationProperties** 패턴 활용
- ✅ **환경 변수 기반 설정 관리**

---

## v1.2.0 → v1.3.0 업그레이드 개요
- 새로 도입: Gateway Only Security (외부 직접 접근 차단, API Gateway 경유 강제)
- 새로 도입: @ConfigurationProperties 기반 타입 세이프 설정 관리(SecurityProperties)
- 장점: 경계 보안 강화, 설정 오타/누락 방지, 환경변수로 보안 토큰 외부화, IDE 자동완성/검증

---

## 🎯 1단계: Gateway Only Security란?

### 💡 문제 상황
마이크로서비스 환경에서 각 서비스가 **직접 외부 접근**을 허용하면:
```
❌ 브라우저 → 인증 서비스 (직접 접근)
❌ 모바일 앱 → 인증 서비스 (직접 접근)
❌ 해커 → 인증 서비스 (보안 위험!)
```

### ✅ 해결 방안
**API Gateway만을 통한 접근**을 강제합니다:
```
✅ 브라우저 → API Gateway → 인증 서비스
✅ 모바일 앱 → API Gateway → 인증 서비스  
❌ 해커 → 인증 서비스 (차단!)
```

### 🔍 동작 원리
1. **API Gateway**가 모든 외부 요청을 받음
2. **특별한 헤더** (`X-Gateway-Internal`)를 추가하여 서비스에 전달
3. **인증 서비스**는 이 헤더가 있는 요청만 처리
4. **직접 접근 시도**는 403 Forbidden으로 차단

---

## 🛠️ 2단계: 프로젝트 준비

### 📋 현재 상태 확인
v1.2.0에서 다음이 구현되어 있어야 합니다:
```bash
# 필수 파일들 확인
ls src/main/java/com/makersworld/civic_insights_auth/config/
# 있어야 할 파일: JwtKeyProvider.java, SecurityConfig.java 등

# RSA JWT 동작 확인
curl http://localhost:8001/.well-known/jwks.json
```

### 🎯 구현할 목표
```
인증 서비스에 Gateway Only Filter 추가
→ API Gateway 헤더 검증
→ 직접 접근 차단
→ 환경 변수로 보안 토큰 관리
```

---

## 🔧 3단계: SecurityProperties 구현

### 💭 @ConfigurationProperties를 사용할 때의 장점

**타입 세이프(type-safe)** 하고 **중앙화된 방식**으로 관리하기 위해 `@ConfigurationProperties` 패턴을 사용할 수 있습니다.

**코드 예제 (✅)**:
```java
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private boolean gatewayOnly;
    private String gatewayToken;
}
```
**장점**:
- 타입 안전성 보장
- IDE 자동완성 지원
- 중앙화된 설정 관리
- 환경 변수와 자연스러운 연동 (`app.security.gateway-token=${GATEWAY_SECRET_TOKEN:...}`)

### 📝 실습: SecurityProperties.java 생성

```bash
# 1. 파일 생성
touch src/main/java/com/makersworld/civic_insights_auth/config/SecurityProperties.java
```

**파일 내용 작성**:
```java
package com.makersworld.civic_insights_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

/**
 * Security 관련 설정 프로퍼티
 * 
 * application.properties의 app.security.* 설정을 매핑합니다.
 */
@Data  // Lombok: getter, setter, toString 자동 생성
@Component  // Spring Bean으로 등록
@ConfigurationProperties(prefix = "app.security")  // app.security로 시작하는 설정 매핑
public class SecurityProperties {
    
    /**
     * API Gateway 전용 모드 활성화 여부
     * application.properties: app.security.gateway-only
     */
    private boolean gatewayOnly;
    
    /**
     * API Gateway 전용 모드 토큰 (환경변수 GATEWAY_SECRET_TOKEN으로 설정)
     * application.properties: app.security.gateway-token
     */
    private String gatewayToken;
}
```

### 🧪 테스트해보기
```bash
# 애플리케이션 실행하여 Bean이 정상 등록되는지 확인
./gradlew bootRun

# 로그에서 SecurityProperties 관련 오류가 없는지 확인
```

---

## 🛡️ 4단계: GatewayOnlyFilter 구현

### 💡 Spring Filter Chain 이해

Spring Security는 **Filter Chain** 패턴을 사용합니다:
```
HTTP 요청 → Filter1 → Filter2 → Filter3 → Controller
```

우리가 만들 `GatewayOnlyFilter`는:
- **최우선 순위**로 실행 (`@Order(1)`)
- **모든 요청**을 검사 (`OncePerRequestFilter`)
- **Gateway 헤더**가 없으면 즉시 차단

### 📝 실습: GatewayOnlyFilter.java 생성

```bash
# 파일 생성
touch src/main/java/com/makersworld/civic_insights_auth/config/GatewayOnlyFilter.java
```

**파일 내용 작성**:
```java
package com.makersworld.civic_insights_auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Gateway 전용 접근 필터
 * 
 * API Gateway를 통한 요청만 허용하고, 직접 접근을 차단합니다.
 * X-Gateway-Internal 헤더의 존재 여부와 값을 검증합니다.
 */
@Component
@Order(1) // 최우선 순위 필터 (숫자가 낮을수록 먼저 실행)
@Slf4j    // 로깅을 위한 Lombok 어노테이션
public class GatewayOnlyFilter extends OncePerRequestFilter {

    // SecurityProperties 의존성 주입 (Constructor Injection)
    private final SecurityProperties securityProperties;

    public GatewayOnlyFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    // 게이트웨이 검증을 우회할 경로들
    private static final List<String> BYPASS_PATHS = Arrays.asList(
        "/actuator/health",     // 헬스체크
        "/error",              // 에러 페이지
        "/.well-known/jwks.json" // JWK 공개키 엔드포인트
    );

    // 허용된 내부 IP 주소들 (개발환경용)
    private static final List<String> ALLOWED_IPS = Arrays.asList(
        "127.0.0.1", "localhost", "::1"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String remoteAddr = getClientIpAddress(request);
        
        log.debug("GatewayOnlyFilter: Processing request - URI: {}, IP: {}", requestURI, remoteAddr);
        
        // 1. 게이트웨이 전용 모드가 비활성화된 경우 통과
        if (!securityProperties.isGatewayOnly()) {
            log.debug("Gateway-only mode is disabled, allowing request");
            filterChain.doFilter(request, response);
            return;
        }
        
        // 2. 우회 경로 확인
        if (shouldBypassFilter(requestURI)) {
            log.debug("Bypassing gateway filter for path: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 3. Gateway 헤더 검증
        String gatewayHeader = request.getHeader("X-Gateway-Internal");
        if (gatewayHeader == null) {
            log.warn("Gateway header missing - URI: {}, IP: {}", requestURI, remoteAddr);
            sendForbiddenResponse(response, "Direct access not allowed. Please use the API Gateway.");
            return;
        }
        
        // 4. Gateway 토큰 검증
        if (!securityProperties.getGatewayToken().equals(gatewayHeader)) {
            log.warn("Invalid gateway token - URI: {}, IP: {}, Token: {}", requestURI, remoteAddr, gatewayHeader);
            sendForbiddenResponse(response, "Invalid gateway token.");
            return;
        }
        
        // 5. 모든 검증 통과 - 요청 처리 계속
        log.debug("Gateway validation passed for URI: {}", requestURI);
        filterChain.doFilter(request, response);
    }

    /**
     * 필터를 우회할 경로인지 확인
     */
    private boolean shouldBypassFilter(String requestURI) {
        return BYPASS_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 403 Forbidden 응답 전송
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\": \"Forbidden\", \"message\": \"%s\", \"status\": 403}", 
            message
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
```

---

## ⚙️ 5단계: application.properties 설정

### 📝 설정 추가

`src/main/resources/application.properties`에 다음 설정을 추가합니다:

```properties
# Gateway Only Security 설정
app.security.gateway-only=true
app.security.gateway-token=${GATEWAY_SECRET_TOKEN:civic-insights-gateway-v1}

# Frontend redirect base (AuthController에서 사용)
frontend.redirect-base=${FRONTEND_BASE_URL:http://localhost:9002}
# Frontend session POST URL (현재 구현: 토큰 폼 POST 방식 사용)
frontend.session-post-url=${FRONTEND_SESSION_POST_URL:http://localhost:9002/api/session}
```

### 💡 설정 설명

- **`app.security.gateway-only=true`**: Gateway Only 모드 활성화
- **`${GATEWAY_SECRET_TOKEN:civic-insights-gateway-v1}`**: 
  - 환경 변수 `GATEWAY_SECRET_TOKEN`이 있으면 사용
  - 없으면 기본값 `civic-insights-gateway-v1` 사용

### 🌍 환경 변수 설정

**개발환경**:
```bash
# 터미널에서 설정
export GATEWAY_SECRET_TOKEN=my-dev-secret-token

# 또는 .env 파일 사용 (선택사항)
echo "GATEWAY_SECRET_TOKEN=my-dev-secret-token" > .env
```

**프로덕션 환경**:
```bash
# 강력한 보안 토큰 사용
export GATEWAY_SECRET_TOKEN=super-secure-production-token-2024
```

---

## 🧪 6단계: 테스트 및 검증

### 🚀 애플리케이션 실행

```bash
# 환경 변수와 함께 실행
export GATEWAY_SECRET_TOKEN=my-test-token
./gradlew bootRun
```

### ✅ 정상 동작 테스트

**1. 우회 경로 테스트 (성공해야 함)**:
```bash
# JWK 엔드포인트 - 우회 허용
curl http://localhost:8001/.well-known/jwks.json
# ✅ 응답: {"keys": [...]} 

# 헬스체크 - 우회 허용  
curl http://localhost:8001/actuator/health
# ✅ 응답: {"status": "UP"}
```

**2. 직접 접근 차단 테스트 (실패해야 함)**:
```bash
# Gateway 헤더 없이 API 접근
curl http://localhost:8001/api/v1/profile
# ❌ 응답: {"error": "Forbidden", "message": "Direct access not allowed. Please use the API Gateway.", "status": 403}
```

**3. Gateway 헤더로 접근 테스트 (성공해야 함)**:
```bash
# 올바른 Gateway 헤더로 접근
curl -H "X-Gateway-Internal: my-test-token" http://localhost:8001/api/v1/profile
# ✅ 응답: JWT 토큰이 필요하다는 인증 오류 (정상 - 다음 단계로 진행됨)
```

**4. 잘못된 토큰 테스트 (실패해야 함)**:
```bash
# 잘못된 Gateway 토큰
curl -H "X-Gateway-Internal: wrong-token" http://localhost:8001/api/v1/profile  
# ❌ 응답: {"error": "Forbidden", "message": "Invalid gateway token.", "status": 403}
```

### 📊 로그 확인

애플리케이션 실행 중 다음과 같은 로그가 보여야 합니다:

```
DEBUG - GatewayOnlyFilter: Processing request - URI: /api/v1/profile, IP: 127.0.0.1
WARN  - Gateway header missing - URI: /api/v1/profile, IP: 127.0.0.1

DEBUG - GatewayOnlyFilter: Processing request - URI: /.well-known/jwks.json, IP: 127.0.0.1  
DEBUG - Bypassing gateway filter for path: /.well-known/jwks.json

DEBUG - Gateway validation passed for URI: /api/v1/profile
```

---

## 🔧 7단계: 개발 편의성 설정 (선택사항)

### 💻 개발 중 Gateway 모드 비활성화

개발 중에는 Gateway 없이 테스트하고 싶을 수 있습니다.

**방법 1**: application-dev.properties 생성
```bash
# 개발용 프로파일 생성
echo "app.security.gateway-only=false" > src/main/resources/application-dev.properties

# 개발 모드로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**방법 2**: 환경 변수로 임시 비활성화
```bash
# Gateway 모드 비활성화하고 실행
./gradlew bootRun --args='--app.security.gateway-only=false'
```

---

## 🚀 8단계: API Gateway 연동 예제

### 🌐 API Gateway 설정 예제

실제 API Gateway에서는 다음과 같이 헤더를 추가해야 합니다:

**Nginx 예제**:
```nginx
location /api/v1/auth/ {
    proxy_pass http://civic-insights-auth:8001;
    proxy_set_header X-Gateway-Internal "super-secure-production-token-2024";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

**Spring Cloud Gateway 예제**:
```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: auth-service
        uri: http://civic-insights-auth:8001
        predicates:
        - Path=/api/v1/auth/**
        filters:
        - AddRequestHeader=X-Gateway-Internal, ${GATEWAY_SECRET_TOKEN}
```

### 🔄 전체 플로우

```
1. 클라이언트 요청 → API Gateway
2. API Gateway → X-Gateway-Internal 헤더 추가 → 인증 서비스
3. GatewayOnlyFilter → 헤더 검증 → 다음 필터로 전달
4. Spring Security → JWT 검증 → Controller 실행
5. 응답 반환
```

---

## 🎯 9단계: 보안 고려사항

### 🔒 프로덕션 보안 체크리스트

**✅ 강력한 토큰 사용**:
```bash
# 랜덤 토큰 생성 예제 (Linux/Mac)
openssl rand -base64 32
# 예: K8zP2wX9mN5vQ7rT1yU4eR6bF3sA8dC0

export GATEWAY_SECRET_TOKEN="K8zP2wX9mN5vQ7rT1yU4eR6bF3sA8dC0"
```

**✅ 토큰 정기 교체**:
- 월 1회 또는 분기 1회 토큰 변경
- 무중단 배포 시 점진적 토큰 교체

**✅ 로깅 및 모니터링**:
```java
// GatewayOnlyFilter에서 보안 이벤트 로깅
log.warn("Security Alert: Multiple failed gateway attempts from IP: {}", remoteAddr);
```

**✅ IP 화이트리스트 (선택사항)**:
```java
// 특정 IP 대역만 허용하는 추가 검증
private boolean isAllowedIP(String clientIP) {
    // API Gateway의 IP 대역만 허용
    return clientIP.startsWith("10.0.") || clientIP.startsWith("192.168.");
}
```

---

## 🏆 10단계: 완료 확인 및 다음 단계

### ✅ 완료 체크리스트

- [ ] `SecurityProperties.java` 생성 및 동작 확인
- [ ] `GatewayOnlyFilter.java` 구현 및 테스트
- [ ] `application.properties` 설정 추가
- [ ] 환경 변수 `GATEWAY_SECRET_TOKEN` 설정
- [ ] 직접 접근 차단 동작 확인
- [ ] Gateway 헤더로 접근 성공 확인
- [ ] 우회 경로 정상 동작 확인

### 🎓 학습 성과

이제 여러분은 다음을 이해하고 구현할 수 있습니다:

1. **마이크로서비스 보안 패턴**: Gateway Only Security의 개념과 필요성
2. **Spring Boot Filter Chain**: 요청 처리 과정과 Filter 우선순위
3. **@ConfigurationProperties**: Type-safe한 설정 관리 방법
4. **환경 변수 관리**: 코드와 설정의 분리, 보안 토큰 관리
5. **실전 보안 구현**: 헤더 검증, IP 추적, 로깅

### 🚀 다음 단계 제안

1. **JWT 토큰과 Gateway Security 통합**: JWT 검증과 Gateway 검증의 조합
2. **Rate Limiting 구현**: API 호출 빈도 제한
3. **CORS 설정**: 브라우저 보안 정책 설정
4. **API 문서 보안**: Swagger UI에 Gateway 헤더 설정 방법

---

## 🤔 FAQ (자주 묻는 질문)

### Q1: Gateway 헤더가 누락되었는데 로그에 안 보여요
**A**: 로깅 레벨을 DEBUG로 설정하세요:
```properties
logging.level.com.makersworld.civic_insights_auth.config.GatewayOnlyFilter=DEBUG
```

### Q2: 개발 중에 매번 헤더 추가하기 번거로워요
**A**: 개발용 프로파일에서 Gateway 모드를 비활성화하세요:
```properties
# application-dev.properties
app.security.gateway-only=false
```

### Q3: 환경 변수가 적용되지 않아요
**A**: 환경 변수 설정을 확인하고 애플리케이션을 재시작하세요:
```bash
echo $GATEWAY_SECRET_TOKEN  # 값이 출력되는지 확인
```

### Q4: API Gateway 없이 테스트하려면 어떻게 하나요?
**A**: curl에서 직접 헤더를 추가하세요:
```bash
curl -H "X-Gateway-Internal: your-token" http://localhost:8001/api/v1/profile
```

### Q5: 프로덕션에서 토큰이 노출되면 어떻게 하나요?
**A**: 즉시 새 토큰으로 교체하고 API Gateway와 서비스 모두 업데이트하세요.

---

## 📚 참고 자료

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)
- [Spring Security Filter Chain](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-security-the-big-picture)
- [Microservice Security Patterns](https://microservices.io/patterns/security/access-token.html)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

---

**🎉 축하합니다! Gateway Only Security 구현을 완료했습니다!**

이제 여러분의 인증 서비스는 API Gateway를 통한 안전한 접근만을 허용하며, 직접적인 외부 접근을 차단할 수 있습니다. 마이크로서비스 보안의 첫 걸음을 성공적으로 내디뎠습니다! 🚀