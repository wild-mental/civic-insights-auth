package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.UpdateProfileRequest;
import com.makersworld.civic_insights_auth.dto.UserProfileDto;
import com.makersworld.civic_insights_auth.model.User;
import com.makersworld.civic_insights_auth.model.UserProfile;
import com.makersworld.civic_insights_auth.repository.UserProfileRepository;
import com.makersworld.civic_insights_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    public UserProfileDto getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("User profile not found - this should not happen"));

        return UserProfileDto.builder()
                .id(profile.getId())
                .email(user.getEmail())
                .name(user.getName())
                .bio(profile.getBio())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }

    public UserProfileDto updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("User profile not found - this should not happen"));

        profile.updateProfile(
                request.getBio(),
                request.getLocation(),
                request.getWebsite(),
                request.getPhoneNumber(),
                request.getAvatarUrl()
        );

        userProfileRepository.save(profile);

        return UserProfileDto.builder()
                .id(profile.getId())
                .email(user.getEmail())
                .name(user.getName())
                .bio(profile.getBio())
                .location(profile.getLocation())
                .website(profile.getWebsite())
                .phoneNumber(profile.getPhoneNumber())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }
} 