package skylinkers.tn.mediconnectbackend.entities;

import skylinkers.tn.mediconnectbackend.security.converter.AES256Converter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persists the Google OAuth2 access/refresh tokens obtained after
 * the Keycloak Identity Broker flow (Authorization Code + PKCE).
 *
 * Both tokens are AES-256-GCM encrypted at rest with a KEY DISTINCT
 * from the one used for medical data (RGPD principle of isolation).
 *
 * Lifecycle:
 *   1. Created when user authorizes Google Calendar scope.
 *   2. accessToken refreshed automatically when isExpired() == true.
 *   3. Revoked (row deleted) on MediConnect logout or user account deletion.
 *
 * SOLID — ISP: OAuthTokenService only exposes methods relevant to token
 *              management — no user-profile methods pollute the interface.
 */
@Entity
@Table(name = "mc_oauth_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    /** OAuth2 provider identifier — currently only "google" is supported. */
    @Column(nullable = false, length = 20)
    private String provider;

    /** Short-lived Google access token. AES-256-GCM encrypted. */
    @Convert(converter = AES256Converter.class)
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    /** Long-lived Google refresh token. AES-256-GCM encrypted. */
    @Convert(converter = AES256Converter.class)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Comma-separated list of authorized scopes.
     * Only "calendar.events" is requested — never the broader "calendar" scope.
     */
    @Column(length = 500)
    private String scopes;

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean hasCalendarScope() {
        return scopes != null && scopes.contains("calendar.events");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }
}
