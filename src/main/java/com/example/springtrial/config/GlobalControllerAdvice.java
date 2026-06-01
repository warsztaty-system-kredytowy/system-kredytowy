package com.example.springtrial.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addSecurityAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            model.addAttribute("isAuthenticated", true);
            
            boolean isEmployee = auth.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_EMPLOYEE"));
            model.addAttribute("isEmployee", isEmployee);

            String name = auth.getName();
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                if (oidcUser.getFullName() != null) {
                    name = oidcUser.getFullName();
                } else if (oidcUser.getEmail() != null) {
                    name = oidcUser.getEmail();
                }
            } else if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
                String fullName = (String) oauth2User.getAttribute("name");
                String email = (String) oauth2User.getAttribute("email");
                if (fullName != null) {
                    name = fullName;
                } else if (email != null) {
                    name = email;
                }
            }

            String ownerKey = auth.getName();
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                ownerKey = oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getName();
            } else if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
                String email = (String) oauth2User.getAttribute("email");
                ownerKey = email != null ? email : oauth2User.getName();
            }
            model.addAttribute("currentUserOwnerKey", ownerKey);
            model.addAttribute("username", name);
        } else {
            model.addAttribute("isAuthenticated", false);
            model.addAttribute("isEmployee", false);
            model.addAttribute("username", null);
            model.addAttribute("currentUserOwnerKey", null);
        }
    }
}
