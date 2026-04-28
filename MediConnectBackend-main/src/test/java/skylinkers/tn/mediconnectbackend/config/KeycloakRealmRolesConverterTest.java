package skylinkers.tn.mediconnectbackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakRealmRolesConverterTest {

    private final KeycloakRealmRolesConverter converter = new KeycloakRealmRolesConverter();

    @Test
    void mapsLegacyAdminRoleToAdministratorAlias() {
        Set<String> authorities = convertAuthorities(List.of("ROLE_ADMIN"));

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR"), authorities);
    }

    @Test
    void mapsAdministratorRoleBackToLegacyAdminForCompatibility() {
        Set<String> authorities = convertAuthorities(List.of("ROLE_ADMINISTRATOR"));

        assertEquals(Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR"), authorities);
    }

    @Test
    void mapsSuperAdminToAdminAuthorities() {
        Set<String> authorities = convertAuthorities(List.of("ROLE_SUPER_ADMIN"));

        assertEquals(Set.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_ADMINISTRATOR"), authorities);
    }

    @Test
    void returnsEmptyAuthoritiesWhenRealmAccessClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .build();

        assertTrue(converter.convert(jwt).isEmpty());
    }

    private Set<String> convertAuthorities(List<String> roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", roles))
                .build();

        return converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
