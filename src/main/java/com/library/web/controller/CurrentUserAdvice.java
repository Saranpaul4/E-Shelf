package com.library.web.controller;

import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import com.library.web.service.Cart;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every template can see, so no controller has to repeat this work. */
@ControllerAdvice
public class CurrentUserAdvice {

    private final SiteUserRepository userRepository;

    public CurrentUserAdvice(SiteUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUser")
    public String currentUser(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }

    /**
     * The signed in person as a whole, so any page can show their name and picture
     * without every controller loading it again. Null on the login and signup pages.
     */
    @ModelAttribute("me")
    public SiteUser profileOfCurrentUser(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @ModelAttribute("isAdminUser")
    public boolean isAdminUser(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
    }

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        Object cart = session.getAttribute(ShopController.CART_SESSION_KEY);
        return cart instanceof Cart live ? live.totalItems() : 0;
    }
}
