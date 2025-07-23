package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.AuthResponse;
import com.makersworld.civic_insights_auth.dto.GoogleUserInfoResponse;
import com.makersworld.civic_insights_auth.model.User;
import com.makersworld.civic_insights_auth.model.UserProfile;
import com.makersworld.civic_insights_auth.repository.UserRepository;
import com.makersworld.civic_insights_auth.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuth2Service googleOAuth2Service;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signInWithGoogle(String code) {
        // 1. Get Google user info
        String accessToken = googleOAuth2Service.getAccessToken(code);
        GoogleUserInfoResponse userInfo = googleOAuth2Service.getUserInfo(accessToken);
        
        // 2. Sync Google user info to our DB
        boolean isNewUser = userRepository.findByEmail(userInfo.getEmail()).isEmpty();
        
        User user = userRepository.findByEmail(userInfo.getEmail())
               .map(existingUser -> {
                    // Update name for existing user
                    existingUser.updateName(userInfo.getName());
                    return userRepository.save(existingUser);
                })
               .orElseGet(() -> {
                    // Save new user to DB
                    return userRepository.save(User.builder()
                           .email(userInfo.getEmail())
                           .name(userInfo.getName())
                           .provider("GOOGLE")
                           .providerId(userInfo.getId())
                           .role("USER")
                           .build());
                });

        // 3. Create user profile for new users with Google data
        if (isNewUser) {
            createUserProfileFromGoogle(user, userInfo);
        }

        // 4. Generate JWT tokens and return response
        String accessTokenJwt = jwtService.generateToken(user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        
        return new AuthResponse(
                accessTokenJwt,
                refreshToken,
                "Bearer",
                86400L, // 24 hours in seconds
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    private void createUserProfileFromGoogle(User user, GoogleUserInfoResponse googleInfo) {
        // Create profile with available Google data
        UserProfile profile = UserProfile.builder()
                .user(user)
                .avatarUrl(googleInfo.getPicture()) // Google profile picture
                .build();
        
        userProfileRepository.save(profile);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.validateToken(refreshToken, email)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                86400L,
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
} 