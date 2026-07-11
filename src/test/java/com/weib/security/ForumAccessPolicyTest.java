package com.weib.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForumAccessPolicyTest {
    @Test
    void onlyForumReadRoutesArePublic() {
        assertTrue(ForumAccessPolicy.isPublicRead("GET", "/forum"));
        assertTrue(ForumAccessPolicy.isPublicRead("GET", "/forum/post/42"));
        assertTrue(ForumAccessPolicy.isPublicRead("GET", "/api/forum/posts/42/comments"));
        assertFalse(ForumAccessPolicy.isPublicRead("GET", "/forum/post/new"));
        assertFalse(ForumAccessPolicy.isPublicRead("POST", "/api/forum/posts"));
        assertFalse(ForumAccessPolicy.isPublicRead("POST", "/api/forum/media"));
    }
}
