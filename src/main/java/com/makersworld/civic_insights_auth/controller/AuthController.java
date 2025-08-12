package com.makersworld.civic_insights_auth.controller;

import com.makersworld.civic_insights_auth.dto.AuthRequest;
import com.makersworld.civic_insights_auth.dto.AuthResponse;
import com.makersworld.civic_insights_auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 프런트엔드 최종 리다이렉트 목적지(base). 기본값은 application.properties의 frontend.redirect-base
    @Value("${frontend.redirect-base:http://localhost:9002}")
    private String frontendBaseUrl;

    // 콜백에서 교환한 토큰을 프런트의 Next API Route로 안전하게 전달하기 위한 POST 대상 URL
    // 기본값: http://localhost:9002/api/session (FRONTEND_SESSION_POST_URL 로 재정의 가능)
    @Value("${frontend.session-post-url:http://localhost:9002/api/session}")
    private String frontendSessionPostUrl;

    @Operation(summary = "Google OAuth2 로그인 페이지로 리디렉션", description = "사용자를 Google OAuth2 인증 페이지로 보냅니다. 인증 후에는 설정된 redirect-uri로 돌아옵니다.")
    @GetMapping("/google")
    public void redirectToGoogle(HttpServletResponse response) throws IOException {
        String url = authService.getGoogleAuthorizationUrl();
        response.sendRedirect(url);
    }


    @Operation(summary = "Google OAuth2 로그인", description = "Handles Google OAuth2 authentication by exchanging an auth code for JWT tokens.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid authentication code")
            })
    @PostMapping("/google/token")
    public ResponseEntity<AuthResponse> signInWithGoogle(@RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.signInWithGoogle(request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Google OAuth2 Callback", description = "Callback endpoint for Google's OAuth2 flow. This is typically used in web-based OAuth flows.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid authentication code")
            })
    @GetMapping("/login/oauth2/code/google")
    public void googleCallback(@RequestParam(value = "code", required = false) String code, HttpServletResponse response) throws IOException {
        // 서버 주도 OAuth2: 콜백에서 Google 코드로 토큰 교환을 완료한 뒤,
        // 브라우저가 프런트(Next API Route)로 cross-origin POST 하도록 자동 제출 폼(HTML) 반환.
        // URL(쿼리/해시)에 토큰을 노출하지 않으며, 프런트 서버가 HttpOnly 쿠키로 보관 가능.
        if (code == null || code.trim().isEmpty()) {
            response.sendRedirect(frontendBaseUrl + "/auth/callback?error=missing_code");
            return;
        }
        try {
            AuthResponse tokens = authService.signInWithGoogle(code);

            String html = "<!DOCTYPE html>" +
                    "<html><head><meta charset='utf-8'><title>Signing in...</title></head><body>" +
                    "<form id='f' method='POST' action='" + escapeHtml(frontendSessionPostUrl) + "'>" +
                    "<input type='hidden' name='access_token' value='" + escapeHtml(tokens.getAccessToken()) + "'/>" +
                    "<input type='hidden' name='refresh_token' value='" + escapeHtml(tokens.getRefreshToken()) + "'/>" +
                    "<input type='hidden' name='token_type' value='" + escapeHtml(tokens.getTokenType()) + "'/>" +
                    "<input type='hidden' name='expires_in' value='" + tokens.getExpiresIn() + "'/>" +
                    "</form>" +
                    "<script>document.getElementById('f').submit();</script>" +
                    "</body></html>";

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            response.setStatus(200);
            response.setContentType("text/html; charset=UTF-8");
            response.setContentLength(bytes.length);
            response.getOutputStream().write(bytes);
        } catch (Exception e) {
            response.sendRedirect(frontendBaseUrl + "/auth/callback?error=auth_failed");
        }
    }

    @Operation(summary = "Refresh JWT Token", description = "Generates new access and refresh tokens using a valid refresh token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refresh successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid refresh token")
            })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        try {
            AuthResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 단순 HTML 이스케이프(속성 값/텍스트에 안전하게 삽입)
    private static String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
} 