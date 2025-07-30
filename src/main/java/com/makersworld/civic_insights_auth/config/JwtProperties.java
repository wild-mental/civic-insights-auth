package com.makersworld.civic_insights_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * JWT 관련 설정을 담는 클래스
 * application.properties의 'jwt' 접두사를 가진 설정 값들을 바인딩합니다.
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private long expirationMs;
    private long refreshExpiration;
    private String issuer;
} 