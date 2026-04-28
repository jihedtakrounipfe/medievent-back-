package skylinkers.tn.mediconnectbackend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class KeycloakRealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Map<String, Set<String>> ROLE_ALIASES = Map.of(
            "ROLE_ADMIN", Set.of("ROLE_ADMINISTRATOR"),
            "ROLE_ADMINISTRATOR", Set.of("ROLE_ADMIN"),
            "ROLE_SUPER_ADMIN", Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR")
    );

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return List.of();
        }

        Object roles = realmAccessMap.get("roles");
        if (!(roles instanceof List<?> rolesList)) {
            return List.of();
        }

        Set<String> authorities = new LinkedHashSet<>();
        for (Object role : rolesList) {
            if (!(role instanceof String roleName) || roleName.isBlank()) {
                continue;
            }
            authorities.add(roleName);
            authorities.addAll(ROLE_ALIASES.getOrDefault(roleName, Set.of()));
        }

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
