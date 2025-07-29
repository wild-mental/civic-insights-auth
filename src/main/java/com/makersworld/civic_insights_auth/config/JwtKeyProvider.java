package com.makersworld.civic_insights_auth.config;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 토큰 서명 및 검증을 위한 RSA 키 쌍을 관리하는 컴포넌트
 * 다중 키 지원으로 키 순환 시에도 기존 토큰의 유효성을 보장합니다.
 */
@Slf4j
@Component
public class JwtKeyProvider {

    // 다중 키 저장소: keyId -> KeyInfo
    private final Map<String, KeyInfo> keyStore = new ConcurrentHashMap<>();
    
    // 현재 서명에 사용할 키 ID
    private volatile String currentSigningKeyId;

    /**
     * 애플리케이션 초기화 시 초기 키 쌍을 생성합니다.
     */
    @PostConstruct
    public void init() {
        // 초기 키 생성
        generateNewKey();
        log.info("JWT 키 제공자가 초기화되었습니다. 현재 서명 키 ID: {}", currentSigningKeyId);
    }

    /**
     * 새로운 키를 생성하고 현재 서명 키로 설정합니다.
     * 기존 키들은 검증용으로 유지됩니다.
     * 
     * @return 새로 생성된 키 ID
     */
    public String generateNewKey() {
        String keyId = "key-" + System.currentTimeMillis();
        KeyPair keyPair = Jwts.SIG.RS256.keyPair().build();
        
        KeyInfo keyInfo = new KeyInfo(
            keyId,
            keyPair,
            LocalDateTime.now(),
            KeyStatus.ACTIVE
        );
        
        keyStore.put(keyId, keyInfo);
        currentSigningKeyId = keyId;
        
        log.info("새로운 JWT 키가 생성되었습니다. 키 ID: {}", keyId);
        return keyId;
    }

    /**
     * 현재 서명에 사용할 개인키를 반환합니다.
     * 
     * @return 현재 활성 키의 RSA 개인키
     */
    public PrivateKey getPrivateKey() {
        return getCurrentKeyInfo().getKeyPair().getPrivate();
    }

    /**
     * 현재 서명에 사용할 공개키를 반환합니다.
     * 
     * @return 현재 활성 키의 RSA 공개키
     */
    public PublicKey getPublicKey() {
        return getCurrentKeyInfo().getKeyPair().getPublic();
    }

    /**
     * 특정 키 ID의 공개키를 반환합니다.
     * JWT 검증 시 토큰의 kid 헤더를 기반으로 적절한 키를 선택합니다.
     * 
     * @param keyId 키 식별자
     * @return 해당 키의 RSA 공개키, 존재하지 않으면 null
     */
    public PublicKey getPublicKey(String keyId) {
        KeyInfo keyInfo = keyStore.get(keyId);
        if (keyInfo == null) {
            log.warn("요청된 키 ID를 찾을 수 없습니다: {}", keyId);
            return null;
        }
        return keyInfo.getKeyPair().getPublic();
    }

    /**
     * 현재 서명 키의 ID를 반환합니다.
     * 
     * @return 현재 활성 키 ID
     */
    public String getCurrentKeyId() {
        return currentSigningKeyId;
    }

    /**
     * 모든 활성 키들의 정보를 반환합니다.
     * JWK 엔드포인트에서 모든 유효한 키를 노출하는 데 사용됩니다.
     * 
     * @return 활성 키들의 맵 (키ID -> KeyInfo)
     */
    public Map<String, KeyInfo> getActiveKeys() {
        return keyStore.entrySet().stream()
            .filter(entry -> entry.getValue().getStatus() == KeyStatus.ACTIVE)
            .collect(ConcurrentHashMap::new, 
                    (map, entry) -> map.put(entry.getKey(), entry.getValue()), 
                    ConcurrentHashMap::putAll);
    }

    /**
     * 지정된 시간보다 오래된 키들을 폐기 상태로 변경합니다.
     * 폐기된 키는 더 이상 토큰 검증에 사용되지 않습니다.
     * 
     * @param olderThan 이 시간보다 오래된 키들을 폐기
     */
    public void deprecateOldKeys(LocalDateTime olderThan) {
        keyStore.entrySet().stream()
            .filter(entry -> entry.getValue().getCreatedAt().isBefore(olderThan))
            .filter(entry -> !entry.getKey().equals(currentSigningKeyId)) // 현재 키는 제외
            .forEach(entry -> {
                entry.getValue().setStatus(KeyStatus.DEPRECATED);
                log.info("키가 폐기되었습니다: {}", entry.getKey());
            });
    }

    /**
     * 폐기된 키들을 완전히 제거합니다.
     * 일반적으로 토큰의 최대 수명이 지난 후에 실행합니다.
     */
    public void removeDeprecatedKeys() {
        keyStore.entrySet().removeIf(entry -> {
            if (entry.getValue().getStatus() == KeyStatus.DEPRECATED) {
                log.info("폐기된 키가 제거되었습니다: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 현재 활성 키 정보를 반환합니다.
     * 
     * @return 현재 키 정보
     * @throws IllegalStateException 현재 키가 없는 경우
     */
    private KeyInfo getCurrentKeyInfo() {
        KeyInfo keyInfo = keyStore.get(currentSigningKeyId);
        if (keyInfo == null) {
            throw new IllegalStateException("현재 서명 키를 찾을 수 없습니다: " + currentSigningKeyId);
        }
        return keyInfo;
    }

    /**
     * 키 정보를 담는 내부 클래스
     */
    public static class KeyInfo {
        private final String keyId;
        private final KeyPair keyPair;
        private final LocalDateTime createdAt;
        private volatile KeyStatus status;

        public KeyInfo(String keyId, KeyPair keyPair, LocalDateTime createdAt, KeyStatus status) {
            this.keyId = keyId;
            this.keyPair = keyPair;
            this.createdAt = createdAt;
            this.status = status;
        }

        // Getters
        public String getKeyId() { return keyId; }
        public KeyPair getKeyPair() { return keyPair; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public KeyStatus getStatus() { return status; }
        
        // Setter for status
        public void setStatus(KeyStatus status) { this.status = status; }
    }

    /**
     * 키 상태를 나타내는 열거형
     */
    public enum KeyStatus {
        ACTIVE,      // 활성: 서명 및 검증 가능
        DEPRECATED   // 폐기: 검증만 가능, 서명 불가
    }
} 