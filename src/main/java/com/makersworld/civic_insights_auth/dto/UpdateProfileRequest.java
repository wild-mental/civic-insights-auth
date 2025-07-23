package com.makersworld.civic_insights_auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String bio;
    private String location;
    private String website;
    private String phoneNumber;
    private String avatarUrl;
} 