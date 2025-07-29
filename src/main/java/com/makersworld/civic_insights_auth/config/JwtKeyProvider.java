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