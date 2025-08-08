package com.makersworld.civic_insights_auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.lang.NonNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Gateway 전용 접근 필터
 * 
 * API Gateway를 통한 요청만 허용하고, 직접 접근을 차단합니다.
 * X-Gateway-Internal 헤더의 존재 여부와 값을 검증합니다.
 */
@Component
@Order(1) // 최우선 순위 필터
@Slf4j
public class GatewayOnlyFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;

    public GatewayOnlyFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    // 게이트웨이 검증을 우회할 경로들
    private static final List<String> BYPASS_PATHS = Arrays.asList(
        "/actuator/health",
        "/error",
        "/.well-known/jwks.json" // JWK 공개키 엔드포인트는 우회 허용
    );

    // 허용된 내부 IP 주소들 (옵션)
    private static final List<String> ALLOWED_IPS = Arrays.asList(
        "127.0.0.1", "localhost", "::1"  // 개발 환경이므로 게이트웨이 위치는 로컬 IP로 고정
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String remoteAddr = getClientIpAddress(request);
        
        log.debug("GatewayOnlyFilter: Processing request - URI: {}, IP: {}", requestURI, remoteAddr);
        
        // 게이트웨이 전용 모드가 비활성화된 경우 통과
        if (!securityProperties.isGatewayOnly()) {
            log.debug("Gateway-only mode is disabled, allowing request");
            filterChain.doFilter(request, response);
            return;
        }
        
        // 우회 경로 확인
        if (isBypassPath(requestURI)) {
            log.debug("Bypass path detected, allowing request: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // X-Gateway-Internal 헤더 검증
        String gatewayHeader = request.getHeader("X-Gateway-Internal");
        
        if (gatewayHeader == null) {
            log.warn("Gateway header missing - URI: {}, IP: {}", requestURI, remoteAddr);
            sendForbiddenResponse(response, "Direct access not allowed. Please use the API Gateway.");
            return;
        }
        
        if (!securityProperties.getGatewayToken().equals(gatewayHeader)) {
            log.warn("Invalid gateway token - URI: {}, IP: {}, Token: {}", requestURI, remoteAddr, gatewayHeader);
            sendForbiddenResponse(response, "Invalid gateway token.");
            return;
        }
        
        // IP 주소 추가 검증 (옵션)
        if (!isAllowedIP(remoteAddr)) {
            log.warn("Unauthorized IP access - URI: {}, IP: {}", requestURI, remoteAddr);
            sendForbiddenResponse(response, "Access from this IP address is not allowed.");
            return;
        }
        
        log.debug("Gateway validation passed - URI: {}, IP: {}", requestURI, remoteAddr);
        filterChain.doFilter(request, response);
    }
    
    /**
     * 우회 경로인지 확인
     */
    private boolean isBypassPath(String requestURI) {
        return BYPASS_PATHS.stream().anyMatch(requestURI::startsWith);
    }
    
    /**
     * 허용된 IP 주소인지 확인
     */
    private boolean isAllowedIP(String ipAddress) {
        return ALLOWED_IPS.contains(ipAddress)
            // 개발 환경에서 모든 로컬 IP 허용하려면 아래 주석 해제
            // || ipAddress.startsWith("192.168.")
            // || ipAddress.startsWith("10.")
            // || ipAddress.startsWith("172.")
            // 완전히 모든 IP 허용하려면 아래 주석 해제
            // || ipAddress.startsWith("0.")
            ;
    }
    
    /**
     * 클라이언트 IP 주소 추출 (프록시 고려)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 403 Forbidden 응답 전송
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"error\":\"Forbidden\",\"message\":\"%s\",\"status\":403,\"timestamp\":\"%s\"}",
            message,
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}