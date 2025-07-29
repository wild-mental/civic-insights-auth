package com.makersworld.civic_insights_auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * JWT 키 순환을 자동으로 관리하는 스케줄러
 * 
 * 키 순환 전략:
 * 1. 새로운 키 생성 (기존 키는 유지)
 * 2. 오래된 키를 폐기 상태로 변경 (토큰 만료 시간 고려)
 * 3. 폐기된 키를 완전 삭제 (충분한 시간 경과 후)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jwt.key-rotation.enabled", havingValue = "true", matchIfMissing = false)
public class JwtKeyRotationScheduler {

    private final JwtKeyProvider jwtKeyProvider;
    private final JwtProperties jwtProperties;

    /**
     * 새로운 키를 생성합니다.
     * 기본값: 매월 1일 새벽 2시 (cron: "0 0 2 1 * ?")
     */
    @Scheduled(cron = "${app.jwt.key-rotation.new-key-cron:0 0 2 1 * ?}")
    public void generateNewKey() {
        try {
            String newKeyId = jwtKeyProvider.generateNewKey();
            log.info("자동 키 순환: 새로운 키가 생성되었습니다. 키 ID: {}", newKeyId);
        } catch (Exception e) {
            log.error("새로운 키 생성 중 오류 발생", e);
        }
    }

    /**
     * 오래된 키들을 폐기 상태로 변경합니다.
     * 키 생성 후 일정 시간이 지나면 새로운 토큰 서명에는 사용하지 않고
     * 기존 토큰 검증용으로만 유지합니다.
     * 
     * 기본값: 매일 새벽 3시 (cron: "0 0 3 * * ?")
     */
    @Scheduled(cron = "${app.jwt.key-rotation.deprecate-cron:0 0 3 * * ?}")
    public void deprecateOldKeys() {
        try {
            // 키 생성 후 30일이 지난 키들을 폐기
            LocalDateTime deprecationThreshold = LocalDateTime.now()
                .minusDays(getKeyDeprecationDays());
            
            jwtKeyProvider.deprecateOldKeys(deprecationThreshold);
            log.info("자동 키 순환: {}일 이전의 오래된 키들이 폐기되었습니다", getKeyDeprecationDays());
        } catch (Exception e) {
            log.error("오래된 키 폐기 중 오류 발생", e);
        }
    }

    /**
     * 폐기된 키들을 완전히 제거합니다.
     * 토큰의 최대 수명(refresh token 포함)이 지난 후에 실행됩니다.
     * 
     * 기본값: 매주 일요일 새벽 4시 (cron: "0 0 4 * * SUN")
     */
    @Scheduled(cron = "${app.jwt.key-rotation.cleanup-cron:0 0 4 * * SUN}")
    public void cleanupDeprecatedKeys() {
        try {
            jwtKeyProvider.removeDeprecatedKeys();
            log.info("자동 키 순환: 폐기된 키들이 정리되었습니다");
        } catch (Exception e) {
            log.error("폐기된 키 정리 중 오류 발생", e);
        }
    }

    /**
     * 키 폐기 기준 일수를 계산합니다.
     * 리프레시 토큰의 만료 시간을 고려하여 충분한 여유를 둡니다.
     * 
     * @return 키 폐기 기준 일수
     */
    private long getKeyDeprecationDays() {
        // 리프레시 토큰 만료 시간(밀리초)을 일수로 변환 후 여유를 둠
        long refreshExpirationDays = jwtProperties.getRefreshExpiration() / (1000 * 60 * 60 * 24);
        
        // 리프레시 토큰 만료 시간의 1.5배 정도로 설정 (최소 30일)
        return Math.max(30, refreshExpirationDays + (refreshExpirationDays / 2));
    }

    /**
     * 현재 키 상태를 로깅합니다.
     * 기본값: 매일 오전 9시 (cron: "0 0 9 * * ?")
     */
    @Scheduled(cron = "${app.jwt.key-rotation.status-log-cron:0 0 9 * * ?}")
    public void logKeyStatus() {
        try {
            int activeKeyCount = jwtKeyProvider.getActiveKeys().size();
            String currentKeyId = jwtKeyProvider.getCurrentKeyId();
            
            log.info("JWT 키 상태 - 활성 키 개수: {}, 현재 서명 키: {}", 
                activeKeyCount, currentKeyId);
        } catch (Exception e) {
            log.error("키 상태 로깅 중 오류 발생", e);
        }
    }
} 