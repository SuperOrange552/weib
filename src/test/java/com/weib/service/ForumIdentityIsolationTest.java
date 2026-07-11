package com.weib.service;

import com.weib.entity.ForumPost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForumIdentityIsolationTest {
    @Test
    void forumPostRetainsBusinessAuthorRole() {
        ForumPost post = new ForumPost();
        post.setAuthorId(7L);
        post.setAuthorRole("BOSS");
        assertEquals("BOSS", post.getAuthorRole());
    }
}
