package com.library.web.controller;

import java.util.List;
import java.util.Optional;

import com.library.web.config.SecurityConfig;
import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    /**
     * The questions a reader may pick at sign-up. Fixed on purpose: a free-text question
     * invites people to write one their own answer makes obvious, and the list is short
     * enough that a dropdown reads better than a form field.
     */
    private static final List<String> SECURITY_QUESTIONS = List.of(
            "What is your favourite book?",
            "What was the name of your first school?",
            "What city were you born in?",
            "What is your favourite film?",
            "What was your childhood nickname?",
            "What is the name of your favourite teacher?");

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRegistrationRepository clientRegistrations;

    public AuthController(SiteUserRepository userRepository, PasswordEncoder passwordEncoder,
                          ClientRegistrationRepository clientRegistrations) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clientRegistrations = clientRegistrations;
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        @RequestParam(value = "reset", required = false) String reset,
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
        if (reset != null) {
            model.addAttribute("passwordReset", true);
        }
        // Only true on a machine that was given Google credentials, so the page can hide
        // the button instead of showing one that leads to a dead end.
        model.addAttribute("googleSignIn",
                clientRegistrations.findByRegistrationId(SecurityConfig.GOOGLE) != null);
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("questions", SECURITY_QUESTIONS);
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
                         @RequestParam(required = false) String securityQuestion,
                         @RequestParam(required = false) String securityAnswer,
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

        // The security question is optional at sign-up, but a reader who skips it cannot
        // reset their own password later, so it has to be a complete pair when present.
        String question = securityQuestion == null ? "" : securityQuestion.trim();
        String answer = normaliseAnswer(securityAnswer);
        if (!question.isEmpty() && answer.length() < 2) {
            model.addAttribute("securityAnswerError", "Please give an answer of at least 2 characters.");
            valid = false;
        }
        if (!question.isEmpty() && !SECURITY_QUESTIONS.contains(question)) {
            model.addAttribute("securityAnswerError", "Please choose one of the listed questions.");
            valid = false;
        }

        model.addAttribute("username", name);
        model.addAttribute("fullName", display);
        model.addAttribute("email", mail);
        model.addAttribute("securityQuestion", question);
        model.addAttribute("securityAnswer", securityAnswer == null ? "" : securityAnswer);
        model.addAttribute("questions", SECURITY_QUESTIONS);

        if (!valid) {
            return "signup";
        }

        SiteUser user = new SiteUser(name, passwordEncoder.encode(password), "USER", display, mail);
        if (!question.isEmpty()) {
            user.setSecurityQuestion(question);
            user.setSecurityAnswer(answer);
        }
        userRepository.save(user);
        redirect.addFlashAttribute("registeredUsername", name);
        return "redirect:/login?registered";
    }

    /**
     * Step one of a reset: the reader names their account and the page shows the question
     * they chose at sign-up, so an answer means something. The account is not revealed to
     * exist or not - a missing question and a missing account get the same quiet page.
     */
    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotIdentify(@RequestParam String username, Model model) {
        String name = username == null ? "" : username.trim();
        model.addAttribute("username", name);

        SiteUser user = findAccount(name).orElse(null);
        if (user == null || !user.isSecurityQuestionSet()) {
            model.addAttribute("noQuestion", true);
            return "forgot-password";
        }

        model.addAttribute("question", user.getSecurityQuestion());
        model.addAttribute("askAnswer", true);
        return "forgot-password";
    }

    /** Step two: the answer unlocks a new password, saved encoded, then back to the login page. */
    @PostMapping("/forgot-password/reset")
    public String forgotReset(@RequestParam String username,
                              @RequestParam String answer,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              Model model) {
        String name = username == null ? "" : username.trim();
        model.addAttribute("username", name);

        SiteUser user = findAccount(name).orElse(null);
        if (user == null || !user.isSecurityQuestionSet()
                || !normaliseAnswer(answer).equals(user.getSecurityAnswer())) {
            model.addAttribute("question", user == null ? null : user.getSecurityQuestion());
            model.addAttribute("askAnswer", true);
            model.addAttribute("answerError", "That answer does not match our records.");
            return "forgot-password";
        }

        if (password == null || password.length() < 6) {
            model.addAttribute("question", user.getSecurityQuestion());
            model.addAttribute("askAnswer", true);
            model.addAttribute("passwordError", "Password must be at least 6 characters.");
            return "forgot-password";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("question", user.getSecurityQuestion());
            model.addAttribute("askAnswer", true);
            model.addAttribute("confirmError", "Passwords do not match.");
            return "forgot-password";
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return "redirect:/login?reset";
    }

    /** An account by username, or by email when the reader typed an address instead. */
    private Optional<SiteUser> findAccount(String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        Optional<SiteUser> byName = userRepository.findByUsername(typed);
        return byName.isPresent() ? byName : userRepository.findByEmail(typed.toLowerCase());
    }

    /** Answers are compared loosely: trimmed, lower cased, and any run of spaces counts as one. */
    private static String normaliseAnswer(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
