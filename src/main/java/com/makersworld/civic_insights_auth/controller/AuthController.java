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
import java.io.IOException;

@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
    public ResponseEntity<AuthResponse> googleCallback(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            AuthResponse response = authService.signInWithGoogle(code);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
} 