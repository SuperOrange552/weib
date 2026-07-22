package com.weib.controller.mobile;

import com.weib.dto.forum.ForumPostCreateRequest;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.ForumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MobileForumControllerTest {
    private ForumService forum;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        forum = mock(ForumService.class);
        ActiveIdentityResolver identities = mock(ActiveIdentityResolver.class);
        when(identities.current(any())).thenReturn(
                new ActiveIdentity(7L, "SEEKER", "测试用户", null));
        mvc = MockMvcBuilders.standaloneSetup(
                new MobileForumController(forum, identities)).build();
    }

    @Test
    void createsPostAsCurrentIdentity() throws Exception {
        mvc.perform(post("/api/mobile/forum/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionId\":1,\"title\":\"自动化标题\",\"content\":\"自动化正文\",\"imageUrls\":[],\"tags\":[\"测试\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(forum).create(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("SEEKER"), any(ForumPostCreateRequest.class));
    }

    @Test
    void deletesOwnPostAsCurrentIdentity() throws Exception {
        mvc.perform(delete("/api/mobile/forum/posts/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(forum).deleteOwnPost(7L, "SEEKER", 88L);
    }
}

