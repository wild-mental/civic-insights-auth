package com.makersworld.civic_insights_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String email;
    private String name;
    private String bio;
    private String location;
    private String website;
    private String phoneNumber;
    private String avatarUrl;
} 