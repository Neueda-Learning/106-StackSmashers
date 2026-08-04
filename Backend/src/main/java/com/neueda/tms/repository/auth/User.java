package com.neueda.tms.repository.auth;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public User() {
        this.role = UserRole.ANALYST;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(Long id, String username, String passwordHash,
            UserRole role, Boolean isActive, LocalDateTime createdAt) {

        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = (role != null) ? role : UserRole.ANALYST;
        this.isActive = (isActive != null) ? isActive : true;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User)) {
            return false;
        }

        User user = (User) obj;

        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public enum UserRole {
        ADMIN,
        ANALYST
    }
}