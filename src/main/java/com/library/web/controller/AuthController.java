package com.library.web.controller;

import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(SiteUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("loggedOut", true);
        }
        if (registered != null) {
            model.addAttribute("accountCreated", true);
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    /**
     * Public sign-up always creates a shop customer. The ADMIN account is seeded
     * in DataSeeder, so nobody can grant themselves the back office by typing a role.
     */
    @PostMapping("/signup")
    public String signUp(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         @RequestParam String fullName,
                         @RequestParam String email,
                         Model model,
                         RedirectAttributes redirect) {

        String name = username == null ? "" : username.trim();
        String display = fullName == null ? "" : fullName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase();
        boolean valid = true;

        if (name.length() < 3 || name.length() > 20) {
            model.addAttribute("usernameError", "Username must be 3-20 characters.");
            valid = false;
        } else if (userRepository.existsByUsername(name)) {
            model.addAttribute("usernameError", "That username is already taken.");
            valid = false;
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("passwordError", "Password must be at least 6 characters.");
            valid = false;
        }

        if (password != null && !password.equals(confirmPassword)) {
            model.addAttribute("confirmError", "Passwords do not match.");
            valid = false;
        }

        if (display.length() < 2 || display.length() > 60) {
            model.addAttribute("fullNameError", "Please tell us your full name.");
            valid = false;
        }

        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")) {
            model.addAttribute("emailError", "That does not look like an email address.");
            valid = false;
        } else if (userRepository.existsByEmail(mail)) {
            model.addAttribute("emailError", "That email is already registered.");
            valid = false;
        }

        model.addAttribute("username", name);
        model.addAttribute("fullName", display);
        model.addAttribute("email", mail);

        if (!valid) {
            return "signup";
        }

        userRepository.save(new SiteUser(name, passwordEncoder.encode(password), "USER", display, mail));
        redirect.addFlashAttribute("registeredUsername", name);
        return "redirect:/login?registered";
    }
}
