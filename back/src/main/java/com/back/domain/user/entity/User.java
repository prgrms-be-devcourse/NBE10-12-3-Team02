package com.back.domain.user.entity;

import com.back.global.jpa.converter.EncryptedStringConverter;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name = "id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    private LocalDate deletedAt;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String oauthRefreshToken;

    private User(String loginId, String email, String password, String name, LoginType loginType, String oauthRefreshToken) {
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.loginType = loginType;
        this.oauthRefreshToken = oauthRefreshToken;
    }

    public static User create(String loginId, String email, String password, String name, LoginType loginType) {
        return new User(loginId, email, password, name, loginType, null);
    }

    public static User createOAuth(String loginId, String email, String password, String name, LoginType loginType, String oauthRefreshToken) {
        return new User(loginId, email, password, name, loginType, oauthRefreshToken);
    }

    public void withdraw() {
        String uuid = UUID.randomUUID().toString();
        this.deletedAt = LocalDate.now();
        this.loginId = uuid;
        this.email = uuid + "@deleted.local";
        this.oauthRefreshToken = null;
    }

    public void updateOauthRefreshToken(String oauthRefreshToken) {
        this.oauthRefreshToken = oauthRefreshToken;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updatePassword(String password) {
        this.password = password;
    }
}