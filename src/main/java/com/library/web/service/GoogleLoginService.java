package com.library.web.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.library.web.config.SecurityConfig;
import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Turns a Google account into an account of this shop.
 *
 * <p>Google proves who someone is; it says nothing about whether they may see the back
 * office. So this step looks the email up in our own table and the role always comes from
 * {@link SiteUser}, never from the identity provider. What it does with an email it has never
 * seen depends on which door the person came through: the Sign up door adopts them as a new
 * customer, the Log in door sends them back to choose a password or sign up.</p>
 *
 * <p>It extends {@code OidcUserService} rather than the plain "OAuth2" one because our scopes
 * include {@code openid}, and Spring Security sends every such login down the OIDC path. Ask
 * for the wrong one and the login still succeeds, but with Google's own principal: a numeric
 * subject instead of a username, and no ROLE_ at all, which the shop reads as a stranger.</p>
 */
@Service
public class GoogleLoginService extends OidcUserService {

    /**
     * Marks the one refusal this app raises itself, so the failure handler can tell it from a
     * genuine Google problem and show a helpful page rather than "wrong password".
     */
    public static final String NO_ACCOUNT_HERE = "no_e_shelf_account";

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleLoginService(SiteUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser googleUser = super.loadUser(userRequest);

        String email = claim(googleUser, "email");
        if (email == null || email.isBlank()) {
            // Without an email there is nothing to tie this person to a cart or an order,
            // and no way to find the account they already have here.
            throw new OAuth2AuthenticationException("Google did not share an email address");
        }
        email = email.trim().toLowerCase();

        String displayName = claim(googleUser, "name");
        SiteUser account = userRepository.findByEmail(email).orElse(null);
        if (account == null) {
            if (!SecurityConfig.GOOGLE_SIGNUP.equals(userRequest.getClientRegistration().getRegistrationId())) {
                // They asked to log in, and there is nothing here to log in to. Making an
                // account anyway would quietly invent a shelf for a stranger.
                throw new OAuth2AuthenticationException(new OAuth2Error(NO_ACCOUNT_HERE,
                        "No E-Shelf account uses " + email, null),
                        "New Google reader at the login door");
            }
            account = userRepository.save(register(email, displayName));
        }
        return new ShopAccount(googleUser, account);
    }

    /** A first-timer from Google: a customer account with no password anyone can guess. */
    private SiteUser register(String email, String displayName) {
        String handle = newHandle(email);
        String name = displayName == null || displayName.isBlank() ? handle : displayName.trim();
        // Password column cannot be null, so it holds the hash of a random value that was
        // never shown to anyone: this account signs in through Google, and a reader who ever
        // wants a password can set one through the forgot-password flow.
        return new SiteUser(handle, passwordEncoder.encode(UUID.randomUUID().toString()),
                "USER", name, email);
    }

    /**
     * The part before the {@code @}, cleaned to characters the username column allows and
     * given a number when someone else already claimed it.
     */
    private String newHandle(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9]", "");
        if (base.length() < 3) {
            base = "reader" + base;
        }
        if (base.length() > 16) {
            base = base.substring(0, 16);
        }
        String candidate = base;
        for (int suffix = 2; userRepository.findByUsername(candidate).isPresent(); suffix++) {
            candidate = base + suffix;
        }
        return candidate;
    }

    private static String claim(OidcUser user, String key) {
        Object value = user.getAttributes().get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Google's signed-in person, wearing this shop's name and role.
     *
     * <p>The rest of the app only ever asks an authenticated user two things: their name,
     * which must be a row in {@code site_user}, and their authority, which decides which
     * portal they may open. Both come from our account here, while every Google claim is kept
     * as it was so nothing about the verified ID token is lost.</p>
     */
    private static final class ShopAccount implements OidcUser {

        private final OidcUser googleUser;
        private final String username;
        private final Collection<? extends GrantedAuthority> authorities;
        private final Map<String, Object> attributes;

        ShopAccount(OidcUser googleUser, SiteUser account) {
            this.googleUser = googleUser;
            this.username = account.getUsername();
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole()));
            Map<String, Object> claims = new HashMap<>(googleUser.getAttributes());
            claims.put("username", this.username);
            this.attributes = claims;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return this.authorities;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return this.attributes;
        }

        /** What {@code SecurityContext} reports as the logged-in username. */
        @Override
        public String getName() {
            return this.username;
        }

        @Override
        public Map<String, Object> getClaims() {
            return this.googleUser.getClaims();
        }

        @Override
        public OidcIdToken getIdToken() {
            return this.googleUser.getIdToken();
        }

        @Override
        public OidcUserInfo getUserInfo() {
            return this.googleUser.getUserInfo();
        }
    }
}
