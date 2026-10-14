package com.library.web.config;

import java.util.Optional;

import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import com.library.web.service.PresenceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, PresenceService presence) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/favicon.svg",
                                "/login", "/signup", "/forgot-password", "/forgot-password/reset", "/").permitAll()
                        .requestMatchers("/h2-console/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/shop/**").hasRole("USER")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        // Signing out should read as offline straight away, not five minutes later.
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication != null) {
                                presence.signOut(authentication.getName());
                            }
                        }))
                .exceptionHandling(errors -> errors
                        .accessDeniedPage("/403"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    /**
     * Spring calls this with whatever was typed in the login form's username box. Readers
     * often remember their email better than their handle, so an unknown username gets one
     * more try as an email address. The account's real username is what goes into the
     * authenticated principal either way, so the rest of the app never has to care which
     * of the two was typed.
     */
    @Bean
    UserDetailsService users(SiteUserRepository repository) {
        return typed -> lookup(repository, typed).map(user -> User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user: " + typed));
    }

    private static Optional<SiteUser> lookup(SiteUserRepository repository, String typed) {
        if (typed == null || typed.isBlank()) {
            return Optional.empty();
        }
        String name = typed.trim();
        Optional<SiteUser> byUsername = repository.findByUsername(name);
        return byUsername.isPresent() ? byUsername : repository.findByEmail(name.toLowerCase());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
