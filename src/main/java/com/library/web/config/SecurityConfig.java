package com.library.web.config;

import java.io.IOException;
import java.util.Optional;

import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import com.library.web.service.GoogleLoginService;
import com.library.web.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
public class SecurityConfig {

    /** How this app refers to Google in the URL people click: /oauth2/authorization/google. */
    public static final String GOOGLE = "google";

    /**
     * A second door to the same Google app, hung on the Sign up page only.
     *
     * <p>Google hands back an identical profile whichever button was pressed, so the page a
     * reader came from has to travel with the request and come back with the answer. Nothing in
     * the URL Google sees may be added to without asking Google first - but Spring Security
     * already keeps the whole authorization request, including the registration it was built
     * from, in the reader's session, and reads the door back out of that on the way in. So two
     * names for one client is enough to tell "create my account" from "log me in", and both can
     * return to the single address Google was told about.</p>
     */
    public static final String GOOGLE_SIGNUP = "google-signup";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, PresenceService presence,
            ClientRegistrationRepository clientRegistrations, GoogleLoginService googleUsers) throws Exception {
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

        // Google is a bonus door, not a requirement. Only wire it in once there are
        // credentials, so the app still boots and signs in by password with none.
        // The reader hook is oidcUserService because our scopes ask for "openid"; with that
        // scope Spring Security ignores the plain user-info hook entirely.
        if (clientRegistrations.findByRegistrationId(GOOGLE) != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .userInfoEndpoint(user -> user.oidcUserService(googleUsers))
                    .failureHandler(new GoogleDoorFailureHandler())
                    .permitAll());
        }

        return http.build();
    }

    /**
     * Sends a refused Google sign-in to the right place.
     *
     * <p>The login page's {@code ?error} means "wrong username or password", which would be
     * a lie about a reader who never typed anything. So the one refusal this app raises on
     * purpose - a Google address with no account behind it - gets its own address that the
     * page can answer with a pointer to Sign up. Everything Google itself rejects (a bad
     * code, a revoked consent) keeps the plain error page.</p>
     */
    static class GoogleDoorFailureHandler implements AuthenticationFailureHandler {

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                AuthenticationException failed) throws IOException {
            boolean newReader = failed instanceof OAuth2AuthenticationException oauth
                    && GoogleLoginService.NO_ACCOUNT_HERE.equals(oauth.getError().getErrorCode());
            response.sendRedirect(request.getContextPath()
                    + (newReader ? "/login?google=unregistered" : "/login?error"));
        }
    }

    /**
     * The list of outside identity providers this app can sign people in with.
     *
     * <p>Boot would normally build this from {@code spring.security.oauth2.client.*}
     * properties, but those must all be filled in or none at all, and we want the third
     * possibility: Google configured on some machines and not on others. So the bean is
     * declared here and answers {@code null} for every provider when no client id was
     * given. Spring Security reads that as "no Google button", and nothing else notices.</p>
     *
     * <p>The secret is never written in this repository. It arrives from the environment,
     * so each machine supplies its own.</p>
     *
     * <p>Both Google doors are added together or neither is, so a machine can never offer
     * "create an account with Google" without also offering the plain sign-in next door.</p>
     */
    @Bean
    ClientRegistrationRepository clientRegistrations(
            @Value("${eshelf.google.client-id:}") String clientId,
            @Value("${eshelf.google.client-secret:}") String clientSecret) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return registrationId -> null;
        }
        return new InMemoryClientRegistrationRepository(
                googleRegistration(GOOGLE, clientId, clientSecret),
                googleRegistration(GOOGLE_SIGNUP, clientId, clientSecret));
    }

    /**
     * One Google app, spelled out twice because a name is the only thing that tells the two
     * doors apart. Both come back to the same address: that is the one Google has been told
     * about, and the door the reader pressed is remembered in the session, not in the URL.
     */
    private static ClientRegistration googleRegistration(String registrationId, String clientId,
            String clientSecret) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/" + GOOGLE)
                .scope(OidcScopes.OPENID, OidcScopes.EMAIL, OidcScopes.PROFILE)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
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
