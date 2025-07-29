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
     * 모든 활성 키들을 JWK Set으로 제공하여 키 순환을 지원합니다.
     * 
     * @return JWK Set JSON 객체
     */
    @GetMapping("/.well-known/jwks.json")
    @Operation(
        summary = "JWT 공개키 조회",
        description = "JWT 토큰 검증을 위한 모든 활성 공개키를 JWK(JSON Web Key) 형식으로 반환합니다."
    )
    public Map<String, Object> getJwks() {
        // 모든 활성 키들을 JWK로 변환
        java.util.List<JWK> jwks = new java.util.ArrayList<>();
        
        jwtKeyProvider.getActiveKeys().forEach((keyId, keyInfo) -> {
            RSAPublicKey publicKey = (RSAPublicKey) keyInfo.getKeyPair().getPublic();
            JWK jwk = new RSAKey.Builder(publicKey)
                    .keyID(keyId) // 각 키의 고유 ID 설정
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE) // 서명용 키임을 명시
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256) // RS256 알고리즘
                    .build();
            jwks.add(jwk);
        });
        
        // JWK Set으로 반환
        JWKSet jwkSet = new JWKSet(jwks);
        return jwkSet.toJSONObject();
    }
} 