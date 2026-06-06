package com.weib.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which users are currently connected via WebSocket.
 * Used for online status checks and conditional message push.
 *
 * Includes TTL-based automatic cleanup to prevent memory leaks
 * from ghost sessions (browser crash, network loss without close frame).
 */
@Component
public class WebSocketSessionManager {

    /** Session data: sessionId → lastActivityTime */
    private static class SessionInfo {
        final String sessionId;
        volatile Instant lastActivity;

        SessionInfo(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = Instant.now();
        }
    }

    private final Map<Long, Set<SessionInfo>> userSessions = new ConcurrentHashMap<>();

    /** Sessions idle longer than this are eligible for cleanup */
    private static final long SESSION_TTL_SECONDS = 300; // 5 minutes

    public void addSession(Long userId, String sessionId) {
        userSessions.compute(userId, (k, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }
            // Update timestamp if session already exists, otherwise add new
            SessionInfo existing = findSession(sessions, sessionId);
            if (existing != null) {
                existing.lastActivity = Instant.now();
            } else {
                sessions.add(new SessionInfo(sessionId));
            }
            return sessions;
        });
    }

    public void removeSession(Long userId, String sessionId) {
        Set<SessionInfo> sessions = userSessions.get(userId);
        if (sessions != null) {
            SessionInfo target = findSession(sessions, sessionId);
            if (target != null) {
                sessions.remove(target);
            }
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    public boolean isOnline(Long userId) {
        Set<SessionInfo> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        // Also check: at least one session is still within TTL
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        return sessions.stream().anyMatch(s -> s.lastActivity.isAfter(cutoff));
    }

    public int getOnlineCount() {
        return userSessions.size();
    }

    /**
     * Periodic cleanup: remove sessions that have been idle longer than TTL.
     * Runs every 2 minutes. This handles browser crashes and network losses
     * where the normal SessionDisconnectEvent is never fired.
     */
    @Scheduled(fixedRate = 120_000)
    public void cleanupStaleSessions() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_SECONDS);
        for (Map.Entry<Long, Set<SessionInfo>> entry : userSessions.entrySet()) {
            Set<SessionInfo> sessions = entry.getValue();
            sessions.removeIf(s -> s.lastActivity.isBefore(cutoff));
            if (sessions.isEmpty()) {
                userSessions.remove(entry.getKey());
            }
        }
    }

    private SessionInfo findSession(Set<SessionInfo> sessions, String sessionId) {
        for (SessionInfo s : sessions) {
            if (s.sessionId.equals(sessionId)) {
                return s;
            }
        }
        return null;
    }
}
