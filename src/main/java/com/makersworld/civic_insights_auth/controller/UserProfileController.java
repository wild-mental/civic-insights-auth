package com.makersworld.civic_insights_auth.controller;

import com.makersworld.civic_insights_auth.dto.UpdateProfileRequest;
import com.makersworld.civic_insights_auth.dto.UserProfileDto;
import com.makersworld.civic_insights_auth.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Profile", description = "Endpoints for managing user profiles")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get User Profile", description = "Retrieves the profile of the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        String email = authentication.getName();
        UserProfileDto profile = userProfileService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update User Profile", description = "Updates the profile of the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserProfileDto.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        UserProfileDto updatedProfile = userProfileService.updateUserProfile(email, request);
        return ResponseEntity.ok(updatedProfile);
    }
} 