package com.weib.controller.mobile;

import com.weib.entity.RoleProfile;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.MobileProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MobileProfileControllerTest {
    private ActiveIdentityResolver identities;
    private MobileProfileService profiles;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        identities = mock(ActiveIdentityResolver.class);
        profiles = mock(MobileProfileService.class);
        when(identities.current(any())).thenReturn(
                new ActiveIdentity(7L, "SEEKER", "旧昵称", null));
        mvc = MockMvcBuilders.standaloneSetup(
                new MobileProfileController(identities, profiles)).build();
    }

    @Test
    void updatesNicknameForCurrentIdentity() throws Exception {
        RoleProfile saved = new RoleProfile();
        saved.setUserId(7L);
        saved.setRoleType("SEEKER");
        saved.setNickname("自动化昵称");
        when(profiles.updateNickname(7L, "SEEKER", "自动化昵称")).thenReturn(saved);

        mvc.perform(put("/api/mobile/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"自动化昵称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("自动化昵称"));

        verify(profiles).updateNickname(7L, "SEEKER", "自动化昵称");
    }
}

