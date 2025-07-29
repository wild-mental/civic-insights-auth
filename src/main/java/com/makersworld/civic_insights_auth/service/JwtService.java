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
        
        // 헤더에 키 ID 추가
        Map<String, Object> headers = new HashMap<>();
        headers.put("kid", jwtKeyProvider.getCurrentKeyId());
        
        return Jwts.builder()
                .header()
                    .add(headers)
                    .and()
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
            return (username != null && username.equals(email) && !isTokenExpired(token));
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
     * 토큰의 kid 헤더를 기반으로 적절한 키를 선택합니다.
     * @param token JWT 토큰
     * @return 클레임 객체
     * @throws RuntimeException 토큰 파싱 또는 검증 실패 시
     */
    private Claims extractAllClaims(String token) {
        try {
            // 먼저 헤더에서 키 ID 추출 (검증 없이)
            String keyId = extractKeyIdFromHeader(token);
            
            // 키 ID가 있으면 해당 키 사용, 없으면 현재 키 사용
            PublicKey verifyingKey;
            if (keyId != null) {
                verifyingKey = jwtKeyProvider.getPublicKey(keyId);
                if (verifyingKey == null) {
                    // 해당 키가 없으면 현재 키로 시도
                    log.debug("키 ID {}에 해당하는 키를 찾을 수 없음, 현재 키로 검증 시도", keyId);
                    verifyingKey = getVerifyingKey();
                }
            } else {
                verifyingKey = getVerifyingKey();
            }
            
            return Jwts.parser()
                    .verifyWith(verifyingKey) // 선택된 공개키로 검증
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT 토큰 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * JWT 토큰 헤더에서 키 ID를 추출합니다.
     * 이 메서드는 토큰을 검증하지 않고 헤더만 파싱합니다.
     * 
     * @param token JWT 토큰
     * @return 키 ID, 없으면 null
     */
    private String extractKeyIdFromHeader(String token) {
        try {
            // 토큰을 점(.)으로 분할하여 헤더 부분 추출
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                return null;
            }
            
            // Base64 디코딩하여 헤더 파싱
            String headerJson = new String(
                java.util.Base64.getUrlDecoder().decode(chunks[0])
            );
            
            // 간단한 JSON 파싱으로 kid 추출
            if (headerJson.contains("\"kid\"")) {
                String kidPattern = "\"kid\"\\s*:\\s*\"([^\"]+)\"";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(kidPattern);
                java.util.regex.Matcher matcher = pattern.matcher(headerJson);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("헤더에서 키 ID 추출 실패: {}", e.getMessage());
            return null;
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