package com.makersworld.civic_insights_auth.service;

import com.makersworld.civic_insights_auth.dto.GoogleUserInfoResponse;
import com.makersworld.civic_insights_auth.enums.Provider;
import com.makersworld.civic_insights_auth.enums.Role;
import com.makersworld.civic_insights_auth.model.User;
import com.makersworld.civic_insights_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createOrUpdateGoogleUser(GoogleUserInfoResponse googleUser) {
        Optional<User> existingUser = findByEmail(googleUser.getEmail());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.updateName(googleUser.getName());
            return userRepository.save(user);
        } else {
            User newUser = User.builder()
                    .email(googleUser.getEmail())
                    .name(googleUser.getName())
                    .provider(Provider.GOOGLE.name())
                    .providerId(googleUser.getId())
                    .role(Role.USER.name())
                    .build();
            return userRepository.save(newUser);
        }
    }

    public User save(User user) {
        return userRepository.save(user);
    }
} 