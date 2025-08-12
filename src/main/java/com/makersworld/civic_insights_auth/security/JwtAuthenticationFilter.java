package com.makersworld.civic_insights_auth.security;

import com.makersworld.civic_insights_auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 토큰 기반 인증을 처리하는 필터
 * RSA 비대칭키를 사용하여 토큰을 검증합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Authorization 헤더가 없거나 Bearer로 시작하지 않으면 다음 필터로 진행
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // "Bearer " 이후의 토큰 추출
            jwt = authHeader.substring(7);
            
            // 토큰에서 사용자 이메일 추출 (RSA 공개키로 검증)
            userEmail = jwtService.extractEmail(jwt);

            // 이메일이 존재하고 현재 인증 컨텍스트가 없는 경우
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 토큰 유효성 검증
                if (jwtService.validateToken(jwt, userEmail)) {
                    // 인증 토큰 생성 및 설정
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail, null, new ArrayList<>()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT 토큰 인증 성공: {}", userEmail);
                } else {
                    log.debug("JWT 토큰 검증 실패: 유효하지 않은 토큰");
                }
            }
        } catch (Exception e) {
            // JWT 파싱 또는 검증 실패 시 로그 기록하고 인증 없이 진행
            log.debug("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
            // 인증 실패해도 요청은 계속 진행 (다른 인증 방법이 있을 수 있음)
        }
        
        filterChain.doFilter(request, response);
    }
} 