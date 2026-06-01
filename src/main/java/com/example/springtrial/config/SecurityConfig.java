package com.example.springtrial.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/home",
                                "/applications",
                                "/submit",
                                "/about-us",
                                "/login",
                                "/register",
                                "/employee/login",
                                "/api/loans",
                                "/api/loans/*",
                                // static assets (css/js/images)
                                "/*.css",
                                "/*.js",
                                "/favicon.ico",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/loans/*/approve", "/api/loans/*/deny", "/api/loans/*/review", "/api/loans/*/evaluate").hasRole("EMPLOYEE")
                        .requestMatchers("/api/loans/*/appeal").authenticated()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable()) // disable CSRF so our form POST works
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/keycloak")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
        return logoutSuccessHandler;
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            authorities.forEach(authority -> {
                mappedAuthorities.add(authority); // retain standard scopes/authorities

                String email = null;
                String username = null;

                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    var idToken = oidcUserAuthority.getIdToken();
                    Map<String, Object> realmAccess = idToken.getClaim("realm_access");
                    if (realmAccess == null && oidcUserAuthority.getUserInfo() != null) {
                        realmAccess = oidcUserAuthority.getUserInfo().getClaim("realm_access");
                    }
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                        roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    }

                    email = idToken.getEmail();
                    username = idToken.getPreferredUsername();
                } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    Map<String, Object> attributes = oauth2UserAuthority.getAttributes();
                    Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                        roles.forEach(role -> mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    }

                    email = (String) attributes.get("email");
                    username = (String) attributes.get("preferred_username");
                }

                // Indirect Employee mapping logic:
                if ((email != null && (email.toLowerCase().endsWith("@company.com") || email.toLowerCase().endsWith("@employee.loan.com"))) ||
                    (username != null && (username.toLowerCase().startsWith("employee_") || username.toLowerCase().equals("employee")))) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));
                }
            });
            return mappedAuthorities;
        };
    }
}