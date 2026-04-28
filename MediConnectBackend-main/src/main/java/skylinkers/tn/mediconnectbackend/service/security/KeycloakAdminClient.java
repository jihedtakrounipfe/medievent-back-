package skylinkers.tn.mediconnectbackend.service.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminClient {

    private record AdminTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {}

    private record RoleRepresentation(
            String id,
            String name
    ) {}

    private record UserRepresentation(
            String id,
            String email,
            String username,
            java.util.List<String> requiredActions
    ) {}

    private record CredentialRepresentation(
            String id,
            String type,
            String userLabel
    ) {}

    private final RestClient restClient;
    private final String serverUrl;
    private final String realm;
    private final String adminRealm;
    private final String adminClientId;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakAdminClient(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin.realm:master}") String adminRealm,
            @Value("${keycloak.admin.client-id:admin-cli}") String adminClientId,
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword
    ) {
        this.restClient = RestClient.builder().build();
        this.serverUrl = normalizeBaseUrl(serverUrl);
        this.realm = realm == null ? "" : realm.trim();
        this.adminRealm = adminRealm == null ? "master" : adminRealm.trim();
        this.adminClientId = adminClientId == null ? "admin-cli" : adminClientId.trim();
        this.adminUsername = adminUsername == null ? "" : adminUsername.trim();
        this.adminPassword = adminPassword == null ? "" : adminPassword.trim();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    public String createPatientUser(String email, String password, String firstName, String lastName) {
        return createUser(email, password, firstName, lastName, "ROLE_PATIENT", Map.of("user_type", List.of("PATIENT")));
    }

    public String createDoctorUser(String email, String password, String firstName, String lastName, String rppsNumber) {
        return createUser(
                email,
                password,
                firstName,
                lastName,
                "ROLE_DOCTOR_GP",
                Map.of(
                        "user_type", List.of("DOCTOR"),
                        "rpps_number", List.of(rppsNumber)
                )
        );
    }

    /**
     * Create a user who registered via Google Sign-In.
     * Sets emailVerified=true (Google already verified the email).
     * No CONFIGURE_TOTP required action — Google users don't need it.
     */
    public String createGoogleUser(String email, String internalPassword, String firstName, String lastName, String realmRole, Map<String, List<String>> attributes) {
        String token = getAdminAccessToken();

        Map<String, Object> userPayload = Map.of(
                "username", email,
                "email", email,
                "enabled", true,
                "emailVerified", true,
                "firstName", firstName,
                "lastName", lastName,
                "attributes", attributes,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", internalPassword,
                        "temporary", false
                ))
        );

        String createUrl = serverUrl + "/admin/realms/" + realm + "/users";
        ResponseEntity<Void> res = restClient.post()
                .uri(createUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userPayload)
                .retrieve()
                .toBodilessEntity();

        URI location = res.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header for created user.");
        }

        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        assignRealmRole(token, userId, realmRole);
        return userId;
    }

    public String createAdministratorUser(String email, String password, String firstName, String lastName) {
        return createUser(
                email,
                password,
                firstName,
                lastName,
                "ROLE_ADMIN",
                Map.of("user_type", List.of("ADMINISTRATOR"))
        );
    }

    // ── 2FA / OTP Management ──────────────────────────────────────────────────

    /**
     * Enable TOTP for a user by adding CONFIGURE_TOTP as a required action.
     * The next time the user logs in, Keycloak will prompt them to set up
     * their authenticator app.
     */
    public void enableOtp(String keycloakId) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId;
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requiredActions", List.of("CONFIGURE_TOTP")))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Disable 2FA for a user by removing all OTP credentials and
     * clearing the CONFIGURE_TOTP required action.
     */
    public void disableOtp(String keycloakId) {
        String token = getAdminAccessToken();

        // Remove OTP credentials
        CredentialRepresentation[] credentials = getCredentials(token, keycloakId);
        if (credentials != null) {
            for (CredentialRepresentation cred : credentials) {
                if ("otp".equals(cred.type())) {
                    String deleteUrl = serverUrl + "/admin/realms/" + realm
                            + "/users/" + keycloakId + "/credentials/" + cred.id();
                    restClient.delete()
                            .uri(deleteUrl)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .retrieve()
                            .toBodilessEntity();
                }
            }
        }

        // Clear the required action
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId;
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requiredActions", List.of()))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Check whether an OTP credential is currently configured for a user.
     *
     * @return true if at least one OTP credential exists in Keycloak
     */
    public boolean isOtpConfigured(String keycloakId) {
        String token = getAdminAccessToken();
        CredentialRepresentation[] credentials = getCredentials(token, keycloakId);
        if (credentials == null) return false;
        for (CredentialRepresentation cred : credentials) {
            if ("otp".equals(cred.type())) return true;
        }
        return false;
    }

    /**
     * Clear all required actions for a user (e.g. CONFIGURE_TOTP).
     * Called during password reset to unblock users who were stuck
     * because CONFIGURE_TOTP prevented ROPC login.
     */
    public void clearRequiredActions(String keycloakId) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId;
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("requiredActions", List.of()))
                .retrieve()
                .toBodilessEntity();
    }

    private CredentialRepresentation[] getCredentials(String token, String keycloakId) {
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId + "/credentials";
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(CredentialRepresentation[].class);
    }

    // ─────────────────────────────────────────────────────────────────────────

    public void logoutUserSessions(String userId) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout";
        restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Reset a user's password in Keycloak by their Keycloak UUID.
     * Uses the Admin API PUT /users/{id}/reset-password endpoint.
     * Preferred over resetPasswordByEmail when keycloakId is available
     * (avoids an extra GET /users?email= lookup).
     */
    public void resetPasswordById(String keycloakId, String newPassword) {
        String token = getAdminAccessToken();
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + keycloakId + "/reset-password";
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", newPassword, "temporary", false))
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Reset a user's password in Keycloak by their email address.
     * Uses the Admin API PUT /users/{id}/reset-password endpoint.
     */
    public void resetPasswordByEmail(String email, String newPassword) {
        String token = getAdminAccessToken();
        String userId = findUserIdByEmail(token, email);
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", newPassword, "temporary", false))
                .retrieve()
                .toBodilessEntity();
    }

    public void setEmailVerifiedByEmail(String email, boolean verified) {
        String token = getAdminAccessToken();
        String userId = findUserIdByEmail(token, email);
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId;
        restClient.put()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("emailVerified", verified))
                .retrieve()
                .toBodilessEntity();
    }

    private String findUserIdByEmail(String token, String email) {
        String normalized = email == null ? "" : email.trim();
        String url = serverUrl + "/admin/realms/" + realm + "/users?email=" + normalized + "&exact=true";
        UserRepresentation[] users = restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(UserRepresentation[].class);
        if (users == null || users.length == 0 || users[0].id() == null || users[0].id().isBlank()) {
            throw new IllegalArgumentException("Keycloak user not found for email: " + normalized);
        }
        return users[0].id();
    }

    private String createUser(
            String email,
            String password,
            String firstName,
            String lastName,
            String realmRole,
            Map<String, List<String>> attributes
    ) {
        String token = getAdminAccessToken();

        Map<String, Object> userPayload = Map.of(
                "username", email,
                "email", email,
                "enabled", true,
                "emailVerified", false,
                "firstName", firstName,
                "lastName", lastName,
                "attributes", attributes,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                ))
        );

        String createUrl = serverUrl + "/admin/realms/" + realm + "/users";
        ResponseEntity<Void> res = restClient.post()
                .uri(createUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userPayload)
                .retrieve()
                .toBodilessEntity();

        URI location = res.getHeaders().getLocation();
        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header for created user.");
        }

        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        assignRealmRole(token, userId, realmRole);
        return userId;
    }

    private void assignRealmRole(String token, String userId, String roleName) {
        RoleRepresentation role = getRealmRole(token, roleName);
        String url = serverUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private RoleRepresentation getRealmRole(String token, String roleName) {
        String url = serverUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        return restClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(RoleRepresentation.class);
    }

    private String getAdminAccessToken() {
        String tokenUrl = serverUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", adminClientId);
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        AdminTokenResponse res = restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(AdminTokenResponse.class);

        if (res == null || res.accessToken() == null || res.accessToken().isBlank()) {
            throw new IllegalStateException("Unable to obtain Keycloak admin access token.");
        }
        return res.accessToken();
    }
}
