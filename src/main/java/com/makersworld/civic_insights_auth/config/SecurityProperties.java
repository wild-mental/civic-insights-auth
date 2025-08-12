package com.makersworld.civic_insights_auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Security 관련 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    
    /**
     * API Gateway 전용 모드 활성화 여부
     */
    private boolean gatewayOnly;
    
    /**
     * API Gateway 전용 모드 토큰 (환경변수 GATEWAY_SECRET_TOKEN으로 설정)
     */
    private String gatewayToken;
}