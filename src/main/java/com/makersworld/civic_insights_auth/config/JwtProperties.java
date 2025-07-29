package com.makersworld.civic_insights_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 관련 설정을 관리하는 프로퍼티 클래스
 * 비대칭키 방식 사용으로 secretKey는 제거되었습니다.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
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