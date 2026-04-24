package skylinkers.tn.mediconnectbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@Profile("dev")
@EnableWebSecurity
public class DevSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthConverter()))
                );

        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new RealmRolesConverter());
        return converter;
    }

    private static class RealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Set<String> allRoles = new HashSet<>();

            // 1. Realm Roles
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> realmAccessMap) {
                Object roles = realmAccessMap.get("roles");
                if (roles instanceof List<?> rolesList) {
                    rolesList.stream().filter(String.class::isInstance).map(String.class::cast).forEach(allRoles::add);
                }
            }

            // 2. Client Roles (resource_access -> angular-spa -> roles)
            Object resourceAccess = jwt.getClaims().get("resource_access");
            if (resourceAccess instanceof Map<?, ?> resourceAccessMap) {
                Object clientAccess = resourceAccessMap.get("angular-spa");
                if (clientAccess instanceof Map<?, ?> clientAccessMap) {
                    Object roles = clientAccessMap.get("roles");
                    if (roles instanceof List<?> rolesList) {
                        rolesList.stream().filter(String.class::isInstance).map(String.class::cast).forEach(allRoles::add);
                    }
                }
            }

            Collection<GrantedAuthority> authorities = new HashSet<>();
            for (String r : allRoles) {
                String roleWithPrefix = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                authorities.add(new SimpleGrantedAuthority(roleWithPrefix));

                // Alias specific doctor roles to a generic DOCTOR role
                if (r.equals("DOCTOR_GP") || r.equals("DOCTOR_SPECIALIST") ||
                    r.equals("ROLE_DOCTOR_GP") || r.equals("ROLE_DOCTOR_SPECIALIST")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_DOCTOR"));
                }
            }

            System.out.println("[SECURITY-DEV] Final Authorities: " + authorities);
            return authorities;
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4201", "http://127.0.0.1:4200", "http://127.0.0.1:4201"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
