package com.makersworld.civic_insights_auth.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "user_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column
    private String bio;

    @Column
    private String location;

    @Column
    private String website;

    @Column
    private String phoneNumber;

    @Column
    private String avatarUrl;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @Builder
    public UserProfile(User user, String bio, String location, String website, String phoneNumber, String avatarUrl) {
        this.user = user;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }

    public void updateProfile(String bio, String location, String website, String phoneNumber, String avatarUrl) {
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }
} 