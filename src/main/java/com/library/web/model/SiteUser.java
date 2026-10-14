package com.library.web.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false)
    private String password;

    /** Either ADMIN (runs the shop) or USER (buys and rents from it). */
    @Column(nullable = false)
    private String role;

    @Column(length = 60)
    private String fullName;

    @Column(length = 80)
    private String email;

    /** A story character picked from the shelf on the profile page. */
    @Enumerated(EnumType.STRING)
    private ProfileAvatar avatar;

    /** Web path of a picture the reader uploaded themselves. Wins over {@link #avatar}. */
    @Column(length = 120)
    private String photoPath;

    @Column(length = 40)
    private String phone;

    @Column(length = 160)
    private String address;

    /** Set when the account is created, so the profile can say "reader since". */
    private LocalDateTime joinedAt = LocalDateTime.now();

    public SiteUser() {
    }

    public SiteUser(String username, String password, String role, String fullName, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ProfileAvatar getAvatar() {
        return avatar;
    }

    public void setAvatar(ProfileAvatar avatar) {
        this.avatar = avatar;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    /** Whatever the page should show as this person's picture, or null for the letter tile. */
    public String getAvatarUrl() {
        if (photoPath != null && !photoPath.isBlank()) {
            return photoPath;
        }
        return avatar == null ? null : avatar.getImageUrl();
    }

    /** The name we print in headings: what they told us, or the username if they never said. */
    public String getDisplayName() {
        return fullName == null || fullName.isBlank() ? username : fullName.trim();
    }

    /** First letters of the name, for the round letter tile. */
    public String getInitials() {
        String source = getDisplayName();
        StringBuilder letters = new StringBuilder();
        for (String word : source.split("\\s+")) {
            if (!word.isBlank()) {
                letters.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        String initials = letters.length() <= 2 ? letters.toString() : letters.substring(0, 2);
        return initials.isEmpty() ? "?" : initials;
    }
}
