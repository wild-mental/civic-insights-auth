package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.config.JwtKeyProvider;
import com.makersworld.civic_insights_auth.config.JwtProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;


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
     * nimbus-jose-jwt 라이브러리를 사용하여 액세스 토큰을 생성합니다.
     *
     * <p><b>Why nimbus-jose-jwt?</b></p>
     * <p>
     * 기존 io.jsonwebtoken 라이브러리 대신 nimbus-jose-jwt를 사용하는 이유는 MSA(분산 환경)에서의 표준 호환성과 Spring Security와의 네이티브 통합성 때문입니다.
     * </p>
     * <ol>
     *   <li><b>JWKS(JSON Web Key Set) 표준 지원</b>:
     *       이 라이브러리는 JWS, JWE, JWK, JWA를 포함하는 JOSE 스펙의 완전한 구현체입니다.
     *       이를 통해 JwkController에서 표준 형식의 JWK Set 엔드포인트를 쉽게 구현할 수 있으며,
     *       API 게이트웨이나 다른 리소스 서버들이 이 엔드포인트를 통해 공개키를 동적으로 가져가 토큰을 검증할 수 있습니다.
     *   </li>
     *   <li><b>Spring Security와의 네이티브 통합</b>:
     *       Spring Security 5.x의 OAuth2 리소스 서버 모듈은 내부적으로 nimbus-jose-jwt를 사용합니다.
     *       인증 서버에서 동일한 라이브러리를 사용함으로써, 토큰의 생성과 검증 로직이 완벽하게 호환되어 안정성을 높입니다.
     *   </li>
     *   <li><b>명시적인 헤더 제어 ('kid')</b>:
     *       JWSHeader.Builder를 통해 'kid'(Key ID)를 명시적으로 헤더에 포함시킬 수 있습니다.
     *       'kid'는 여러 공개키가 존재할 때, 토큰 검증 측에서 어떤 키를 사용해야 할지 정확히 알려주는 중요한 역할을 합니다.
     *   </li>
     * </ol>
     *
     * @param email 사용자의 고유 식별자 (토큰의 'sub' 클레임)
     * @param role 사용자의 권한 정보 (커스텀 클레임 'roles')
     * @return 서명된 JWT 액세스 토큰 문자열
     */
    public String generateToken(String email, String role) {
        try {
            // 1. 서명자(Signer) 준비: RSA 개인키를 사용하여 RSASSA-SIGNER 생성
            JWSSigner signer = new RSASSASigner(jwtKeyProvider.getPrivateKey());

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtProperties.getExpirationMs());

            // 2. JWT 페이로드(Payload)에 포함될 클레임(Claims) 설정
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email) // 'sub' (Subject): 토큰의 주체, 사용자를 식별하는 값
                    .issueTime(now) // 'iat' (Issued At): 토큰 발급 시간
                    .expirationTime(expiryDate) // 'exp' (Expiration Time): 토큰 만료 시간
                    .claim("roles", role) // 비표준 클레임: 사용자 역할 정보
                    .issuer(jwtProperties.getIssuer()) // 'iss' (Issuer): 토큰 발급자
                    .build();

            // 3. JWS 헤더 설정: 서명 알고리즘과 Key ID를 명시
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID("civic-insights-auth-key") // 'kid' (Key ID): JwkController에서 제공하는 공개키의 ID와 일치
                    .build();
            
            // 4. 헤더와 클레임셋을 사용하여 서명된 JWT 객체 생성
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            // 5. 서명 실행
            signedJWT.sign(signer);

            // 6. JWT를 직렬화하여 문자열 형태로 반환
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("JWT 토큰 생성 중 오류 발생", e);
            throw new RuntimeException("JWT 토큰 생성에 실패했습니다.", e);
        }
    }

    /**
     * nimbus-jose-jwt 라이브러리를 사용하여 리프레시 토큰을 생성합니다.
     * 리프레시 토큰은 액세스 토큰보다 긴 만료 시간을 가지며, 추가적인 사용자 정보(claim) 없이 최소한의 정보만 담습니다.
     *
     * @param email 사용자 이메일 (토큰의 'sub' 클레임)
     * @return 서명된 JWT 리프레시 토큰 문자열
     */
    public String generateRefreshToken(String email) {
        try {
            // 1. 서명자(Signer) 준비: RSA 개인키 사용
            JWSSigner signer = new RSASSASigner(jwtKeyProvider.getPrivateKey());
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

            // 2. 리프레시 토큰에 필요한 최소한의 클레임 설정
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .issuer(jwtProperties.getIssuer())
                    .build();
            
            // 3. JWS 헤더 설정
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID("civic-insights-auth-key") // 액세스 토큰과 동일한 키 ID 사용
                    .build();

            // 4. 헤더와 클레임셋으로 서명된 JWT 객체 생성
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);

            // 5. 서명
            signedJWT.sign(signer);
            
            // 6. 직렬화하여 반환
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("리프레시 토큰 생성 중 오류 발생", e);
            throw new RuntimeException("리프레시 토큰 생성에 실패했습니다.", e);
        }
    }

    /**
     * JWT 토큰에서 이메일(subject)을 추출합니다.
     * 이 과정에서 토큰의 서명, 만료 시간, 발급자도 함께 검증됩니다.
     *
     * @param token JWT 토큰 문자열
     * @return 추출된 이메일 (토큰의 subject)
     * @throws RuntimeException 토큰이 유효하지 않은 경우
     */
    public String extractEmail(String token) {
        try {
            JWTClaimsSet claimsSet = parseAndValidate(token);
            return claimsSet.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid Token: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰이 유효한지, 그리고 토큰의 주체(subject)가 주어진 이메일과 일치하는지 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @param email 비교할 이메일
     * @return 토큰이 유효하고 이메일이 일치하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return email.equals(extractedEmail);
        } catch (Exception e) {
            // extractEmail에서 예외가 발생하면 토큰이 유효하지 않은 것이므로 false를 반환
            log.warn("Token validation failed for email [{}]: {}", email, e.getMessage());
            return false;
        }
    }
    
    /**
     * 토큰 문자열을 파싱하고 서명, 만료 시간, 발급자를 검증합니다.
     * 이 메서드는 다른 public 검증 메서드들의 핵심 로직을 담당합니다.
     *
     * @param token 검증할 JWT 문자열
     * @return 유효한 경우 JWTClaimsSet 반환
     * @throws Exception 파싱 또는 검증 실패 시
     */
    private JWTClaimsSet parseAndValidate(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // 공개키를 사용하여 서명 검증기(Verifier) 생성
        RSAPublicKey publicKey = (RSAPublicKey) jwtKeyProvider.getPublicKey();
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey);

        // 서명 검증
        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("JWT signature verification failed.");
        }

        JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

        // 발급자(issuer) 검증
        if (!jwtProperties.getIssuer().equals(claimsSet.getIssuer())) {
            throw new RuntimeException("Invalid JWT issuer.");
        }

        // 만료 시간 검증
        if (new Date().after(claimsSet.getExpirationTime())) {
            throw new RuntimeException("Expired JWT.");
        }

        return claimsSet;
    }
} 