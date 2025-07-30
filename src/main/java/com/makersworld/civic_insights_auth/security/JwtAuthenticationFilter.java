package com.makersworld.civic_insights_auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * API 게이트웨이를 통과한 요청을 처리하는 인증 필터.
 * <p>
 * 게이트웨이가 JWT 검증 후 추가한 HTTP 헤더(X-User-Id, X-User-Roles)를 읽어
 * Spring Security의 인증 컨텍스트(SecurityContext)를 설정합니다.
 * 이 서비스는 더 이상 JWT를 직접 검증하지 않고, 게이트웨이를 신뢰하는 것을 전제로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JwtService는 게이트웨이가 토큰 처리를 전담하므로 더 이상 필요하지 않습니다.

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 게이트웨이가 주입한 사용자 정보 헤더 추출
        final String userId = request.getHeader(USER_ID_HEADER);
        final String userRoles = request.getHeader(USER_ROLES_HEADER);

        // 2. 사용자 ID 헤더가 없거나, 이미 인증된 상태라면 추가 처리 없이 다음 필터로 진행
        if (!StringUtils.hasText(userId) || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Request received with trusted header '{}': {}", USER_ID_HEADER, userId);

        try {
            // 3. Roles 헤더를 파싱하여 Spring Security의 GrantedAuthority 컬렉션으로 변환
            var authorities = StringUtils.hasText(userRoles) ?
                    Arrays.stream(userRoles.split(","))
                            .map(String::trim)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList())
                    : Collections.<SimpleGrantedAuthority>emptyList();

            // 4. 인증 토큰 생성
            // Principal: userId (사용자 식별자), Credentials: null (이미 게이트웨이에서 자격증명 완료)
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            // 5. 현재 요청에 대한 상세 정보 설정
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // 6. SecurityContext에 최종 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("Authentication successful for user: {}, roles: {}", userId, authorities);

        } catch (Exception e) {
            log.error("Failed to process authentication headers. Clearing security context.", e);
            // 헤더 처리 중 예외 발생 시, 안전을 위해 SecurityContext를 초기화
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
} 