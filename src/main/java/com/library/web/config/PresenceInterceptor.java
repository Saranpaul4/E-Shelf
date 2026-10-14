package com.library.web.config;

import com.library.web.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs just before every controller method and stamps the visitor as "seen now".
 * Spring Security has already finished by this point, so getUserPrincipal() is the
 * logged-in person (or null for the anonymous visitor on the login page).
 */
@Component
public class PresenceInterceptor implements HandlerInterceptor {

    private final PresenceService presence;

    public PresenceInterceptor(PresenceService presence) {
        this.presence = presence;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var visitor = request.getUserPrincipal();
        if (visitor != null) {
            presence.touch(visitor.getName());
        }
        return true;
    }
}
