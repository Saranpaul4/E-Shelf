package com.library.web.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Remembers the last moment each signed-in username asked for a page. Someone counts as
 * online while that stamp is still fresh, which gives a live-looking status board without
 * needing websockets or a database column. The map lives in memory, so a restart clears it.
 */
@Service
public class PresenceService {

    /** How long a "last seen" stamp keeps a person marked online. */
    public static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public void touch(String username) {
        lastSeen.put(username, Instant.now());
    }

    public void signOut(String username) {
        lastSeen.remove(username);
    }

    public Instant lastSeenOf(String username) {
        return lastSeen.get(username);
    }

    public boolean isOnline(String username) {
        Instant seen = lastSeen.get(username);
        return seen != null && Duration.between(seen, Instant.now()).compareTo(ONLINE_WINDOW) < 0;
    }
}
