package com.library.web.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.library.web.model.SiteUser;
import com.library.web.repository.SiteUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Turns a Google account into an account of this shop.
 *
 * <p>Google proves who someone is; it says nothing about whether they may see the back
 * office. So this step looks the email up in our own table: an existing reader or librarian
 * signs straight in as themselves, and a brand new email becomes a shop customer, exactly as
 * public sign-up would make one. The role always comes from {@link SiteUser}, never from the
 * identity provider.</p>
 *
 * <p>It extends {@code DefaultOAuth2UserService} because that is the piece Spring Security
 * asks to fetch the profile once Google has said yes. We let the parent do the HTTP call and
 * then rebuild the principal, so the authenticated name is our username and the authority is
 * our own ROLE_.</p>
 */
@Service
public class GoogleLoginService extends DefaultOAuth2UserService {

    private final SiteUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GoogleLoginService(SiteUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User googleUser = super.loadUser(request);

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
            account = userRepository.save(register(email, displayName));
        }

        Map<String, Object> claims = new HashMap<>(googleUser.getAttributes());
        claims.put("username", account.getUsername());
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole())),
                claims,
                "username");
    }

    /** A first-timer from Google: a customer account with no password anyone can guess. */
    private SiteUser register(String email, String displayName) {
        String handle = newHandle(email);
        String name = displayName == null || displayName.isBlank() ? handle : displayName.trim();
        // Password column cannot be null, so it holds the hash of a random value that was
        // never shown to anyone: this account signs in through Google, and a reader who ever
        // wants a password can set one through the forgot-password flow.
        SiteUser user = new SiteUser(handle, passwordEncoder.encode(UUID.randomUUID().toString()),
                "USER", name, email);
        return user;
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

    private static String claim(OAuth2User user, String key) {
        Object value = user.getAttributes().get(key);
        return value == null ? null : value.toString();
    }
}
