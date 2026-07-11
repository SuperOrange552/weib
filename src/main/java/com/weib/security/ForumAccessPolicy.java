package com.weib.security;

/** Public forum routes are read-only; all compose and interaction routes require a session. */
public final class ForumAccessPolicy {
    private ForumAccessPolicy() {}

    public static boolean isPublicRead(String method, String uri) {
        if (!"GET".equalsIgnoreCase(method) || uri == null) return false;
        if ("/forum".equals(uri) || "/api/forum/sections".equals(uri) || "/api/forum/posts".equals(uri)) {
            return true;
        }
        if (uri.matches("/forum/post/[0-9]+")) return true;
        if (uri.matches("/api/forum/posts/[0-9]+")) return true;
        return uri.matches("/api/forum/posts/[0-9]+/comments");
    }
}
