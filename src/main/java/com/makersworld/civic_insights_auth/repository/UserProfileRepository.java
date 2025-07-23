package com.makersworld.civic_insights_auth.repository;

import com.makersworld.civic_insights_auth.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
    Optional<UserProfile> findByUserEmail(String email);
} 