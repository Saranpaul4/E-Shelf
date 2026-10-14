package com.library.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * One entry point for everyone: an ADMIN lands in the back office,
     * a USER lands in the shop.
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin/books" : "redirect:/shop";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "forbidden";
    }
}
