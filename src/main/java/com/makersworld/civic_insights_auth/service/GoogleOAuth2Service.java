package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.GoogleTokenResponse;
import com.makersworld.civic_insights_auth.dto.GoogleUserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.oauth2.client.registration.google.scope}")
    private String scope;

    private final WebClient webClient;

    public String buildGoogleAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope.replace(",", " "); // application.properties의 scope를 공백으로 구분
    }

    public String getAccessToken(String code) {
        String tokenUri = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        GoogleTokenResponse response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to get access token from Google");
        }
        return response.getAccessToken();
    }

    public GoogleUserInfoResponse getUserInfo(String accessToken) {
        String userInfoUri = "https://www.googleapis.com/oauth2/v2/userinfo";

        GoogleUserInfoResponse response = webClient.get()
                .uri(userInfoUri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GoogleUserInfoResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Failed to get user info from Google");
        }
        return response;
    }
}