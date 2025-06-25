package healthwebapp.example.restapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO representing the payload for user verification
 */
public class UserVerificationPayload {

    @NotNull
    private Long userId;

    @NotNull
    @Email
    private String email;

    @NotNull
    private String verificationToken;

    @NotNull
    @FutureOrPresent
    private LocalDateTime tokenExpirationTime;

    // Constructor
    public UserVerificationPayload(Long userId, String email, String verificationToken, LocalDateTime tokenExpirationTime) {
        this.userId = userId;
        this.email = email;
        this.verificationToken = verificationToken;
        this.tokenExpirationTime = tokenExpirationTime;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getTokenExpirationTime() {
        return tokenExpirationTime;
    }

    public void setTokenExpirationTime(LocalDateTime tokenExpirationTime) {
        this.tokenExpirationTime = tokenExpirationTime;
    }

    @Override
    public String toString() {
        return "UserVerificationPayload{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", verificationToken='" + verificationToken + '\'' +
                ", tokenExpirationTime=" + tokenExpirationTime +
                '}';
    }
}
