package com.weib.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weib.dto.PublicUserProfile;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConsistencyIntegrationTest {

    @Test
    void publicUserCachePayloadContainsNoCredentialFields() throws Exception {
        PublicUserProfile profile = new PublicUserProfile(1L, "seeker", "user", "求职者", "/avatar.png", "active");
        String json = new ObjectMapper().writeValueAsString(profile);
        assertThat(json).doesNotContain("password", "rememberToken", "captcha", "csrf");
    }
}
