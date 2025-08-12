
# Spring Boot와 OAuth2.0, JWT를 이용한 인증 마이크로서비스 구축 (핸즈온)

**[실전 포트폴리오 적용 예제 프로젝트]**

Spring Boot를 사용하여 Google OAuth 2.0 기반의 인증 마이크로서비스를 처음부터 끝까지 구축하는 과정을 상세하게 알아봅시다.

> - [참고] 본 단계 구현 후, 후속 단계인 분산 인증을 위한 추가 개발 단계에서 API Gateway(8000) 경유를 기본으로 설정합니다. 따라서 OAuth2 redirect-uri는 `http://localhost:8000/api/auth/login/oauth2/code/google`(8000포트, 게이트웨이) → (8001포트, 인증서버) `/api/v1/auth/login/oauth2/code/google`로 프록시합니다.

이 프로젝트를 통해 여러분은 최신 기술 스택을 경험하고, 실제 현업에서 사용되는 아키텍처와 구현 방식을 익혀 멋진 포트폴리오를 완성할 수 있습니다.

---

## 🎯 학습 목표

1.  **Spring Boot 3.x** 기반의 RESTful API 서버 구축
2.  **Google OAuth 2.0**을 이용한 소셜 로그인 기능 구현
3.  **JWT (JSON Web Token)**를 이용한 상태 비저장(Stateless) 인증 시스템 구현
4.  **Spring Security 6.x**를 이용한 강력한 보안 체계 구축
5.  **Spring Data JPA**와 **MySQL**을 이용한 데이터베이스 연동 및 관리
6.  **사용자 프로필** 관리 기능 구현 (CRUD)
7.  **Swagger (OpenAPI 3.0)**를 이용한 API 문서 자동화 및 테스트
8.  **실무적인 프로젝트 구조**와 **클린 코드** 원칙 이해

---

## 🛠️ 기술 스택

-   **Java 17**: 최신 LTS 버전의 자바
-   **Spring Boot 3.5.3**: 강력하고 빠른 웹 애플리케이션 프레임워크
-   **Spring Security 6.5.1**: 인증 및 권한 부여를 위한 표준 프레임워크
-   **Spring Data JPA**: 데이터베이스 상호작용을 위한 추상화 라이브러리
-   **MySQL 8.4+**: 안정적인 관계형 데이터베이스
-   **JWT (JJWT 0.12.3)**: 토큰 기반 인증을 위한 라이브러리
-   **SpringDoc OpenAPI UI 2.5.0**: API 문서 자동화를 위한 라이브러리
-   **Gradle**: 유연한 빌드 자동화 도구
-   **Lombok**: 보일러플레이트 코드를 줄여주는 라이브러리

---

## 🚀 1단계: 프로젝트 초기 설정

가장 먼저, Spring Boot 프로젝트를 생성하고 필요한 의존성을 설정합니다.

### 1.1. `build.gradle` - 의존성 관리

`build.gradle` 파일은 프로젝트의 모든 의존성을 관리하는 심장과도 같습니다. 우리는 웹, 보안, 데이터베이스, JWT, OpenAPI 등 다양한 기능을 구현하기 위해 아래와 같은 라이브러리들을 추가합니다.

```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.5.3'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.makersworld'
version = '0.0.1-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom annotationProcessor
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters: 핵심 기능들을 모아놓은 의존성 패키지
	implementation 'org.springframework.boot:spring-boot-starter-web' // RESTful API 개발
	implementation 'org.springframework.boot:spring-boot-starter-webflux' // 비동기 통신을 위한 WebClient 사용
	implementation 'org.springframework.boot:spring-boot-starter-security' // 인증 및 보안
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa' // 데이터베이스 연동 (JPA)
	implementation 'org.springframework.boot:spring-boot-starter-validation' // 유효성 검증

	// Database
	runtimeOnly 'com.mysql:mysql-connector-j' // MySQL 데이터베이스 드라이버

	// Lombok: 반복적인 코드를 어노테이션으로 대체하여 생산성 향상
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'

	// JWT: 토큰 생성 및 검증을 위한 라이브러리
	implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'

	// OpenAPI (Swagger): API 문서 자동화 및 테스트 UI 제공
	implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'

	// DevTools & Configuration
	developmentOnly 'org.springframework.boot:spring-boot-devtools' // 개발 편의성 (자동 재시작 등)
	annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor' // application.properties 자동 완성

	// Testing
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
	useJUnitPlatform()
}
```

**🔍 주요 의존성 설명:**
-   `spring-boot-starter-web`: Spring MVC를 사용하여 REST 컨트롤러를 만듭니다.
-   `spring-boot-starter-security`: 필터 체인을 기반으로 API 접근 제어와 인증 로직을 구현합니다.
-   `spring-boot-starter-data-jpa`: `User`와 `UserProfile` 엔티티를 데이터베이스 테이블과 매핑합니다.
-   `jjwt`: JWT 토큰을 생성하고, 서명을 검증하며, 토큰에서 정보를 추출합니다.
-   `springdoc-openapi-starter-webmvc-ui`: 컨트롤러에 작성된 어노테이션을 분석하여 자동으로 멋진 API 테스트 UI를 생성합니다.

### 1.2. `src/main/resources/application.properties` - 애플리케이션 설정

이 파일은 애플리케이션의 동작을 제어하는 모든 설정을 담고 있습니다. 데이터베이스 연결 정보, OAuth2 클라이언트 정보, JWT 비밀 키 등을 여기서 관리합니다.

```properties
# --- Server Configuration ---
spring.application.name=civic-insights-auth
server.port=8001

# --- Database Configuration ---
# 로컬 MySQL 데이터베이스 연결 정보
spring.datasource.url=jdbc:mysql://localhost:3312/civic_insights
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# --- JPA/Hibernate Configuration ---
# 애플리케이션 실행 시 엔티티를 보고 DDL 자동 실행 (개발 시 유용)
spring.jpa.hibernate.ddl-auto=update
# 실행되는 SQL 쿼리를 로그로 출력
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# --- Database Initialization ---
# 애플리케이션 시작 시 schema.sql 실행
spring.sql.init.mode=always
spring.sql.init.continue-on-error=true

# --- Google OAuth2 Configuration ---
# ${ENV_VAR:default_value} 형식: 환경 변수가 없으면 기본값을 사용
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:your-google-client-id}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET:your-google-client-secret}
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:8001/api/v1/auth/login/oauth2/code/google}

# --- JWT Configuration ---
jwt.secret-key=${JWT_SECRET_KEY:mySecretKey123456789012345678901234567890}
jwt.expiration-ms=86400000 # 24 hours
jwt.refresh-expiration=604800000 # 7 days

# --- SpringDoc OpenAPI (Swagger) Configuration ---
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.try-it-out-enabled=true
```

**🔍 주요 설정 설명:**
-   `server.port`: 내장 톰캣 서버가 실행될 포트 번호입니다.
-   `spring.datasource.*`: 로컬에 설치된 MySQL 데이터베이스에 연결하기 위한 정보입니다.
-   `spring.jpa.hibernate.ddl-auto=update`: `User` 같은 엔티티 클래스가 변경되면, 서버가 재시작될 때 데이터베이스 테이블 구조를 자동으로 업데이트해줍니다.
-   `spring.security.oauth2.client.registration.google.*`: Google Cloud Console에서 발급받은 OAuth2 클라이언트 정보를 설정합니다. 이 정보가 있어야 Google과 통신할 수 있습니다.
-   `jwt.*`: JWT 토큰을 암호화하고 서명하는 데 사용될 비밀 키와 만료 시간을 설정합니다.
-   `springdoc.swagger-ui.*`: Swagger UI의 접속 경로와 동작 방식을 설정합니다.

### 1.3. `CivicInsightsAuthApplication.java` - 애플리케이션 시작점

모든 Spring Boot 애플리케이션은 `@SpringBootApplication` 어노테이션이 붙은 메인 클래스에서 시작됩니다.

```java
package com.makersworld.civic_insights_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CivicInsightsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(CivicInsightsAuthApplication.class, args);
	}

}
```
**🔍 어노테이션 설명:**
-   `@SpringBootApplication`: 이 어노테이션 하나가 `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` 세 가지를 합친 역할을 합니다. 즉, 이 클래스가 설정 파일임을 알리고, 필요한 빈(Bean)들을 자동으로 구성하며, 현재 패키지 하위의 컴포넌트들을 스캔하여 등록합니다.

---

## 🚀 2단계: 데이터베이스 설계 및 엔티티 생성

사용자 정보와 프로필 정보를 저장할 데이터베이스 테이블을 설계하고, JPA 엔티티 클래스를 만듭니다.

### 2.1. `src/main/resources/schema.sql` - 데이터베이스 테이블 구조 (DDL)

이 파일은 애플리케이션이 시작될 때 실행되어 `users`와 `user_profiles` 테이블을 생성합니다.

```sql
-- 사용자 계정 정보를 저장하는 테이블
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `email` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NULL, -- 소셜 로그인은 비밀번호가 없음
  `name` VARCHAR(255) NOT NULL,
  `provider` VARCHAR(50) NOT NULL, -- 예: GOOGLE, LOCAL
  `provider_id` VARCHAR(255) NULL, -- 소셜 로그인 제공자의 사용자 고유 ID
  `role` VARCHAR(50) NOT NULL, -- 예: USER, ADMIN
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_email` (`email` ASC) -- 이메일은 고유해야 함
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 프로필 정보를 저장하는 테이블
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
  UNIQUE INDEX `uk_user_id` (`user_id` ASC), -- 한 명의 유저는 하나의 프로필만 가짐
  -- users 테이블의 id를 참조하는 외래키. 사용자가 삭제되면 프로필도 함께 삭제됨 (CASCADE)
  CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2. `src/main/java/com/makersworld/civic_insights_auth/model/User.java` - 사용자 엔티티

`users` 테이블과 1:1로 매핑되는 JPA 엔티티 클래스입니다. 사용자의 핵심 인증 정보를 담습니다.

```java
package com.makersworld.civic_insights_auth.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String provider;

    @Column
    private String providerId;

    @Column(nullable = false)
    private String role;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Builder
    public User(String email, String password, String name, String provider, String providerId, String role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
```
**🔍 어노테이션 및 코드 설명:**
-   `@Entity`: 이 클래스가 JPA 엔티티임을 나타냅니다.
-   `@Table(name = "users")`: `users` 테이블과 매핑됨을 명시합니다.
-   `@Id`, `@GeneratedValue`: `id` 필드가 기본 키(Primary Key)이며, 데이터베이스가 자동으로 값을 생성(Auto Increment)함을 나타냅니다.
-   `@Column`: 각 필드가 테이블의 컬럼과 매핑됨을 나타냅니다. `nullable`, `unique` 등의 제약 조건을 설정할 수 있습니다.
-   `@CreationTimestamp`, `@UpdateTimestamp`: 데이터가 생성되거나 업데이트될 때 자동으로 타임스탬프를 기록합니다.
-   `@Builder`: 빌더 패턴을 사용하여 객체를 안전하고 편리하게 생성할 수 있게 해줍니다.

### 2.3. `src/main/java/com/makersworld/civic_insights_auth/model/UserProfile.java` - 프로필 엔티티

`user_profiles` 테이블과 매핑되며, 사용자의 부가적인 프로필 정보를 관리합니다. `User` 엔티티와 1:1 관계를 가집니다.

```java
package com.makersworld.civic_insights_auth.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String bio;

    @Column
    private String location;

    @Column
    private String website;

    @Column
    private String phoneNumber;

    @Column
    private String avatarUrl;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Builder
    public UserProfile(User user, String bio, String location, String website, String phoneNumber, String avatarUrl) {
        this.user = user;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }

    public void updateProfile(String bio, String location, String website, String phoneNumber, String avatarUrl) {
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }
}
```
**🔍 어노테이션 설명:**
-   `@OneToOne`: `User` 엔티티와 1:1 관계임을 나타냅니다. `fetch = FetchType.LAZY`는 프로필을 조회할 때 즉시 관련 유저 정보를 가져오지 않고, 실제로 `user` 필드에 접근할 때 가져오도록 하여 성능을 최적화합니다.
-   `@JoinColumn(name = "user_id")`: 외래 키 컬럼이 `user_id`임을 명시합니다.

### 2.4. `Enums` - 역할 및 제공자 열거형

`Role`과 `Provider` 같이 정해진 값만 사용해야 하는 경우, 문자열 대신 `Enum`을 사용하여 타입 안정성을 높이고 실수를 방지합니다.

**`src/main/java/com/makersworld/civic_insights_auth/enums/Provider.java`**
```java
package com.makersworld.civic_insights_auth.enums;

public enum Provider {
    GOOGLE, FACEBOOK, LOCAL
}
```

**`src/main/java/com/makersworld/civic_insights_auth/enums/Role.java`**
```java
package com.makersworld.civic_insights_auth.enums;

public enum Role {
    USER, ADMIN, MODERATOR
}
```

---

## 🚀 3단계: 데이터 접근 계층 - Repository

`Repository`는 데이터베이스와 직접 통신하는 인터페이스입니다. Spring Data JPA를 사용하면 복잡한 SQL 쿼리 없이도 기본적인 CRUD(Create, Read, Update, Delete) 작업을 쉽게 수행할 수 있습니다.

### 3.1. `src/main/java/com/makersworld/civic_insights_auth/repository/UserRepository.java`

`JpaRepository<User, Long>`을 상속받는 것만으로 `User` 엔티티에 대한 `save()`, `findById()`, `findAll()`, `delete()` 등의 메소드를 자동으로 사용할 수 있습니다.

```java
package com.makersworld.civic_insights_auth.repository;

import com.makersworld.civic_insights_auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 메소드 이름 규칙에 따라 자동으로 쿼리 생성:
    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);
}
```

### 3.2. `src/main/java/com/makersworld/civic_insights_auth/repository/UserProfileRepository.java`

마찬가지로 `UserProfile` 엔티티에 대한 Repository를 생성합니다.

```java
package com.makersworld.civic_insights_auth.repository;

import com.makersworld.civic_insights_auth.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
    Optional<UserProfile> findByUserEmail(String email);
}
```
**🔍 Spring Data JPA의 마법:**
-   `findBy` 뒤에 엔티티의 필드 이름을 붙이면, Spring Data JPA가 메소드 이름을 분석하여 자동으로 해당 필드를 조건으로 하는 `SELECT` 쿼리를 만들어줍니다. 예를 들어, `findByEmail(String email)`은 `WHERE email = :email` 조건의 쿼리를 생성합니다.

---

## 🚀 4단계: 비즈니스 로직 - Service

`Service` 계층은 애플리케이션의 핵심 비즈니스 로직을 처리합니다. OAuth2 인증, JWT 생성, 사용자 정보 관리 등의 복잡한 작업들이 여기서 이루어집니다.

### 4.1. `DTO (Data Transfer Object)` - 계층 간 데이터 전송 객체

Service와 Controller, 외부 API 등 계층 간에 데이터를 주고받을 때, 엔티티를 직접 사용하기보다 필요한 데이터만 담은 DTO를 사용하는 것이 좋습니다. 이는 엔티티의 불필요한 정보 노출을 막고, 각 계층의 역할을 명확히 분리하여 유연성을 높입니다.

**Google 통신용 DTO:**
-   `GoogleTokenResponse.java`: Google로부터 액세스 토큰 응답을 받을 때 사용합니다.
-   `GoogleUserInfoResponse.java`: Google로부터 사용자 정보를 받을 때 사용합니다.

**API 요청/응답용 DTO:**
-   `AuthRequest.java`: 클라이언트가 인증 코드를 보낼 때 사용합니다.
-   `AuthResponse.java`: 서버가 클라이언트에게 JWT 토큰과 사용자 정보를 응답할 때 사용합니다.
-   `UserProfileDto.java`, `UpdateProfileRequest.java`: 프로필 조회 및 업데이트 시 사용합니다.

**`src/main/java/com/makersworld/civic_insights_auth/dto/GoogleTokenResponse.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("scope")
    private String scope;
}
```

**`src/main/java/com/makersworld/civic_insights_auth/dto/GoogleUserInfoResponse.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleUserInfoResponse {
    @JsonProperty("id")
    private String id;
    @JsonProperty("email")
    private String email;
    @JsonProperty("verified_email")
    private boolean verifiedEmail;
    @JsonProperty("name")
    private String name;
    @JsonProperty("given_name")
    private String givenName;
    @JsonProperty("family_name")
    private String familyName;
    @JsonProperty("picture")
    private String picture;
    @JsonProperty("locale")
    private String locale;
}
```

**`src/main/java/com/makersworld/civic_insights_auth/dto/AuthRequest.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String code;
}
```

**`src/main/java/com/makersworld/civic_insights_auth/dto/AuthResponse.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String email;
    private String name;
    private String role;
}
```

**`src/main/java/com/makersworld/civic_insights_auth/dto/UserProfileDto.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String email;
    private String name;
    private String bio;
    private String location;
    private String website;
    private String phoneNumber;
    private String avatarUrl;
}
```

**`src/main/java/com/makersworld/civic_insights_auth/dto/UpdateProfileRequest.java`**
```java
package com.makersworld.civic_insights_auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String bio;
    private String location;
    private String website;
    private String phoneNumber;
    private String avatarUrl;
}
```

### 4.2. `src/main/java/com/makersworld/civic_insights_auth/service/GoogleOAuth2Service.java`

이 서비스는 Google의 OAuth2 서버와 직접 통신하는 역할을 담당합니다. 클라이언트로부터 받은 인증 코드를 사용하여 Google에 액세스 토큰을 요청하고, 그 토큰으로 다시 사용자 정보를 요청합니다.

```java
package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.GoogleTokenResponse;
import com.makersworld.civic_insights_auth.dto.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    // application.properties에 설정된 값들을 주입받음
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final WebClient webClient; // 비동기 HTTP 통신을 위한 클라이언트

    // 1. 인증 코드로 Google에 액세스 토큰 요청
    public String getAccessToken(String code) {
        String tokenUri = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        GoogleTokenResponse response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve() // 응답을 받아옴
                .bodyToMono(GoogleTokenResponse.class) // 응답 본문을 GoogleTokenResponse 객체로 변환
                .block(); // 비동기 작업이 끝날 때까지 대기

        if (response == null) {
            throw new RuntimeException("Failed to get access token from Google");
        }
        return response.getAccessToken();
    }

    // 2. 액세스 토큰으로 Google에 사용자 정보 요청
    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        String userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";

        GoogleUserInfoResponse response = webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken)) // 헤더에 Bearer 토큰 추가
                .retrieve()
                .bodyToMono(GoogleUserInfoResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to get user info from Google");
        }
        return response;
    }
}
```

### 4.3. `src/main/java/com/makersworld/civic_insights_auth/service/JwtService.java`

JWT 토큰의 생성, 검증, 정보 추출을 담당하는 서비스입니다. 토큰의 암호화, 서명, 만료 시간 관리 등 JWT와 관련된 모든 로직이 여기에 집중되어 있습니다.

```java
package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties; // JWT 설정을 담는 객체

    // 서명 키 생성 (HMAC-SHA 알고리즘 사용)
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes());
    }

    // 액세스 토큰 생성
    public String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // 페이로드에 역할 정보 추가
        return createToken(claims, email);
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(String email) {
        return createToken(new HashMap<>(), email, jwtProperties.getRefreshExpiration());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, jwtProperties.getExpirationMs());
    }

    // JWT 생성 로직
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims) // 페이로드(데이터)
                .subject(subject) // 토큰 주체(사용자 이메일)
                .issuedAt(new Date(System.currentTimeMillis())) // 발급 시간
                .expiration(new Date(System.currentTimeMillis() + expiration)) // 만료 시간
                .signWith(getSigningKey()) // 서명
                .compact();
    }

    // 토큰 유효성 검증
    public Boolean validateToken(String token, String email) {
        final String username = extractEmail(token);
        return (username.equals(email) && !isTokenExpired(token));
    }

    // 토큰에서 이메일 추출
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // ... 기타 정보 추출 메소드 ...
}
```
*전체 코드는 프로젝트 파일에서 확인하세요.*

### 4.4. `src/main/java/com/makersworld/civic_insights_auth/service/AuthService.java`

인증 플로우 전체를 총괄하는 핵심 서비스입니다. `GoogleOAuth2Service`와 `JwtService`를 사용하여 다음 과정을 순서대로 처리합니다.

1.  인증 코드를 받아 Google로부터 사용자 정보 획득
2.  받아온 사용자 정보를 우리 데이터베이스와 동기화 (신규 유저 저장 또는 기존 유저 정보 업데이트)
3.  신규 유저일 경우, 프로필 자동 생성
4.  JWT 액세스 토큰과 리프레시 토큰 발급

```java
package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.AuthResponse;
import com.makersworld.civic_insights_auth.dto.GoogleUserInfoResponse;
import com.makersworld.civic_insights_auth.model.User;
import com.makersworld.civic_insights_auth.model.UserProfile;
import com.makersworld.civic_insights_auth.repository.UserRepository;
import com.makersworld.civic_insights_auth.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signInWithGoogle(String code) {
        // 1. Google 사용자 정보 가져오기
        String accessToken = googleOAuth2Service.getAccessToken(code);
        GoogleUserInfoResponse userInfo = googleOAuth2Service.getUserInfo(accessToken);
        
        // 2. 사용자 정보 DB와 동기화
        boolean isNewUser = userRepository.findByEmail(userInfo.getEmail()).isEmpty();
        
        User user = userRepository.findByEmail(userInfo.getEmail())
               .map(existingUser -> {
                    // 기존 유저: 이름 업데이트
                    existingUser.updateName(userInfo.getName());
                    return userRepository.save(existingUser);
                })
               .orElseGet(() -> {
                    // 신규 유저: DB에 저장
                    return userRepository.save(User.builder()
                           .email(userInfo.getEmail())
                           .name(userInfo.getName())
                           .provider("GOOGLE")
                           .providerId(userInfo.getId())
                           .role("USER")
                           .build());
                });

        // 3. 신규 유저일 경우, Google 프로필 사진으로 프로필 자동 생성
        if (isNewUser) {
            createUserProfileFromGoogle(user, userInfo);
        }

        // 4. JWT 토큰 생성 및 응답
        String accessTokenJwt = jwtService.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        
        return new AuthResponse(
                accessTokenJwt, refreshToken, "Bearer", 86400L,
                user.getEmail(), user.getName(), user.getRole()
        );
    }
    
    // ... refreshToken, createUserProfileFromGoogle 메소드 ...
}
```
*전체 코드는 프로젝트 파일에서 확인하세요.*

### 4.5. `UserProfileService` 및 `UserService`

`UserProfileService`는 프로필 조회 및 업데이트를, `UserService`는 사용자 정보 관련 로직을 담당하며, `AuthService`의 책임을 분산시켜줍니다.

---

## 🚀 5단계: 보안 설정 - Spring Security

Spring Security는 애플리케이션의 보안을 책임지는 가장 중요한 부분입니다. 여기서는 JWT 인증 필터를 등록하고, API 엔드포인트별로 접근 권한을 설정합니다.

### 5.1. `src/main/java/com/makersworld/civic_insights_auth/security/JwtAuthenticationFilter.java`

클라이언트가 보내는 모든 요청의 `Authorization` 헤더에서 JWT 토큰을 확인하는 커스텀 필터입니다. 유효한 토큰이 있으면, Spring Security의 `SecurityContextHolder`에 인증 정보를 등록하여 해당 요청이 인증되었음을 시스템에 알립니다.

```java
package com.makersworld.civic_insights_auth.security;

import com.makersworld.civic_insights_auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;

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

        // "Bearer "로 시작하는 헤더가 없으면 필터를 통과시킴
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // "Bearer " 다음의 토큰 부분 추출
        userEmail = jwtService.extractEmail(jwt);

        // 토큰에서 추출한 이메일이 있고, 아직 인증 정보가 등록되지 않았다면
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtService.validateToken(jwt, userEmail)) { // 토큰 유효성 검증
                // 인증 토큰 생성
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail, null, new ArrayList<>()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // SecurityContext에 인증 정보 등록
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### 5.2. `src/main/java/com/makersworld/civic_insights_auth/config/SecurityConfig.java`

애플리케이션의 전체 보안 설정을 정의하는 클래스입니다. `JwtAuthenticationFilter`를 필터 체인에 추가하고, 각 API 경로에 대한 접근 권한(permitAll, authenticated)을 설정합니다.

```java
package com.makersworld.civic_insights_auth.config;

import com.makersworld.civic_insights_auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
           .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정
           .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (Stateless API)
           .httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic 인증 비활성화
           .formLogin(formLogin -> formLogin.disable()) // 폼 로그인 비활성화
           // 세션을 사용하지 않는 Stateless 정책 설정
           .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
           // 경로별 접근 권한 설정
           .authorizeHttpRequests(auth -> auth
               .requestMatchers(
                   "/api/v1/auth/**", // 인증 관련 API는 모두 허용
                   "/error",
                   "/swagger-ui/**", // Swagger UI 접근 허용
                   "/swagger-ui.html",
                   "/v3/api-docs/**"
               ).permitAll()
               .requestMatchers("/api/v1/profile/**").authenticated() // 프로필 API는 인증 필요
               .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
            )
           // UsernamePasswordAuthenticationFilter 앞에 우리 커스텀 필터 추가
           .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    // CORS(Cross-Origin Resource Sharing) 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 프론트엔드 개발 서버(React) 주소 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

## 🚀 6단계: API 엔드포인트 - Controller 및 OpenAPI 문서화

Controller는 클라이언트의 HTTP 요청을 직접 받아 처리하는 계층입니다. 여기서는 `@RestController`를 사용하여 JSON 형식의 데이터를 반환하는 API 엔드포인트를 만듭니다.

### 6.1. `src/main/java/com/makersworld/civic_insights_auth/controller/AuthController.java`

인증 관련 모든 엔드포인트를 담당합니다.

```java
package com.makersworld.civic_insights_auth.controller;

import com.makersworld.civic_insights_auth.dto.AuthRequest;
import com.makersworld.civic_insights_auth.dto.AuthResponse;
import com.makersworld.civic_insights_auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "사용자 인증 및 토큰 관리 엔드포인트")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Google OAuth2 로그인", description = "인증 코드를 JWT 토큰으로 교환합니다.",
            responses = {
                @ApiResponse(responseCode = "200", description = "인증 성공", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                @ApiResponse(responseCode = "400", description = "잘못된 인증 코드")
            })
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> signInWithGoogle(@RequestBody AuthRequest request) {
        // ... 로직 생략 ...
    }

    @Operation(summary = "Google OAuth2 콜백", description = "Google의 OAuth2 플로우에서 사용되는 콜백 엔드포인트입니다.")
    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<AuthResponse> googleCallback(@RequestParam(value = "code", required = false) String code) {
        // ... 로직 생략 ...
    }
    
    @Operation(summary = "JWT 토큰 갱신", description = "유효한 리프레시 토큰으로 새 토큰을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        // ... 로직 생략 ...
    }
}
```
*전체 코드는 프로젝트 파일에서 확인하세요.*

### 6.2. `src/main/java/com/makersworld/civic_insights_auth/controller/UserProfileController.java`

사용자 프로필 관련 엔드포인트를 담당합니다. 모든 엔드포인트는 인증이 필요합니다.

```java
package com.makersworld.civic_insights_auth.controller;

// ... import 생략 ...

@Tag(name = "User Profile", description = "사용자 프로필 관리 엔드포인트")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "사용자 프로필 조회", description = "인증된 사용자의 프로필을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileDto profile = userProfileService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "사용자 프로필 업데이트", description = "인증된 사용자의 프로필을 업데이트합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        UserProfileDto updatedProfile = userProfileService.updateUserProfile(email, request);
        return ResponseEntity.ok(updatedProfile);
    }
}
```
**🔍 OpenAPI(Swagger) 어노테이션 설명:**
-   `@Tag`: Swagger UI에서 API들을 그룹화합니다.
-   `@Operation`: 각 API 엔드포인트의 제목과 설명을 추가합니다.
-   `@ApiResponse`: 가능한 HTTP 응답 코드와 설명을 명시합니다.
-   `@SecurityRequirement(name = "bearerAuth")`: 이 엔드포인트가 JWT Bearer 토큰 인증을 필요로 함을 나타냅니다.

### 6.3. `src/main/java/com/makersworld/civic_insights_auth/config/OpenApiConfig.java`

Swagger UI에서 JWT 인증을 테스트할 수 있도록 "Authorize" 버튼을 추가하는 설정입니다.

```java
package com.makersworld.civic_insights_auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Civic Insights Auth API")
                .version("v1.0.0")
                .description("API for authentication, token management, and user profiles.");

        // JWT Bearer 토큰 인증 스키마 설정
        String securitySchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);
        Components components = new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
```

---

## 🚀 7단계: 실행 및 테스트

이제 모든 준비가 끝났습니다! 애플리케이션을 실행하고 API를 테스트해봅시다.

### 7.1. Google Cloud Console 설정

1.  **Google Cloud Console** 접속
2.  새 프로젝트 생성 또는 기존 프로젝트 선택
3.  **API 및 서비스 > 사용자 인증 정보**로 이동
4.  **사용자 인증 정보 만들기 > OAuth 2.0 클라이언트 ID** 선택
5.  애플리케이션 유형: **웹 애플리케이션**
6.  **승인된 리디렉션 URI**에 다음을 정확히 추가:
    `http://localhost:8001/api/v1/auth/login/oauth2/code/google`
7.  생성된 **클라이언트 ID**와 **클라이언트 보안 비밀**을 복사합니다.

### 7.2. 환경 변수 설정

보안을 위해 민감한 정보는 코드에 직접 넣지 않고 환경 변수로 설정합니다.

```bash
# 터미널에서 아래 명령 실행 (실제 값으로 대체)
export GOOGLE_CLIENT_ID="your-actual-google-client-id"
export GOOGLE_CLIENT_SECRET="your-actual-google-client-secret"
```

### 7.3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 7.4. Swagger UI로 API 테스트

1.  브라우저에서 **http://localhost:8001/swagger-ui.html** 접속
2.  `Authentication` 태그의 API들을 확인합니다.
3.  `User Profile` 태그의 API를 테스트하기 전에, 먼저 Google 로그인을 통해 JWT 토큰을 받아야 합니다.

### 7.5. Google OAuth2 로그인 테스트

서버 주도 방식의 테스트는 매우 간단합니다.

1.  **Google 로그인 페이지로 이동**: 브라우저에서 아래의 새로운 엔드포인트로 접속합니다.
    ```
    http://localhost:8001/api/v1/auth/google
    ```
2.  **Google 인증**: 서버가 자동으로 Google 로그인 페이지로 리디렉션합니다. Google 계정으로 로그인하고 권한을 허용합니다.

3.  **인증 코드 획득**: 인증이 완료되면, Google은 우리 서버의 콜백 URL(`GET /api/v1/auth/login/oauth2/code/google`)로 리디렉션하면서 `code` 파라미터를 전달합니다. 이 과정에서 브라우저에는 400 오류 페이지가 표시될 수 있지만, 이는 서버가 성공적으로 코드를 받아 처리했음을 의미하므로 **정상적인 동작**입니다. 실제 프론트엔드 애플리케이션에서는 이 응답을 받아 JWT 토큰을 저장하고 다음 페이지로 넘어갑니다.

4.  **JWT 토큰 확인 (서버 로그)**: 애플리케이션 로그를 확인하면 Google로부터 받은 사용자 정보로 JWT 토큰이 생성된 것을 볼 수 있습니다. (실제 운영 시에는 클라이언트에 바로 토큰을 전달합니다.)

**참고: 클라이언트 주도 방식 테스트 (선택 사항)**

만약 클라이언트(예: 모바일 앱)에서 직접 인증 코드를 받아 서버에 전달하는 방식을 테스트하고 싶다면, 변경된 `POST` 엔드포인트를 사용하면 됩니다.

1.  **새로운 인증 코드 받기**: 수동으로 Google 인증 URL을 만들어 접속 후 코드를 복사합니다.
    ```
    https://accounts.google.com/o/oauth2/v2/auth?client_id=YOUR_GOOGLE_CLIENT_ID&redirect_uri=http://localhost:8001/api/v1/auth/login/oauth2/code/google&response_type=code&scope=openid%20profile%20email
    ```
2.  **Swagger UI**에서 `POST /api/v1/auth/google/token` 엔드포인트를 사용하여 복사한 코드로 토큰을 요청합니다.
    ```json
    {
      "code": "복사한_실제_인증_코드"
    }
    ```

### 7.6. 보호된 API 테스트

1.  위 과정에서 서버 로그나 응답을 통해 얻은 `accessToken`을 복사합니다.
2.  Swagger UI 우측 상단의 **"Authorize"** 버튼을 클릭합니다.
3.  `Value` 필드에 `Bearer 복사한_accessToken` 형식으로 붙여넣고 **"Authorize"**를 클릭합니다. (예: `Bearer eyJhbGciOi...`)
4.  이제 자물쇠 아이콘이 잠긴 것을 확인하고, `User Profile` 태그의 `GET /api/v1/profile` 엔드포인트를 **Execute**하면 정상적으로 200 응답과 함께 프로필 정보가 조회됩니다.

---

## 🎉 Finished!

여러분은 이제 Spring Boot, OAuth2.0, JWT를 이용한 현대적인 인증 마이크로서비스를 성공적으로 구축했습니다.
이 프로젝트는 여러분의 3차 프로젝트에 활용해서, 멋진 포트폴리오를 만드는 데 도움이 되길 바랍니다!

앞으로 기능을 확장해보세요!
-   다른 소셜 로그인(Facebook, Kakao) 추가
-   JWT 핸들링을 MSA 분산인증 방식으로 변경
-   이메일/비밀번호 기반의 로컬 회원가입 기능
-   비밀번호 암호화 (BCrypt)
-   더 세분화된 역할 기반 접근 제어 (RBAC)
-   Docker를 이용한 컨테이너화