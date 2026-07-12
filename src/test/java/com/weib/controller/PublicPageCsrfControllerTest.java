package com.weib.controller;

import com.weib.config.GlobalModelAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.view.InternalResourceView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PublicPageCsrfControllerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(null, null, null, null, null);
        mvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalModelAdvice())
                .setSingleView(new InternalResourceView("/WEB-INF/test-view.jsp"))
                .build();
    }

    @Test
    void loginPageCreatesSessionAndPublishesCsrfToken() throws Exception {
        MvcResult result = mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("csrf_token"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
        assertThat(result.getRequest().getSession(false).getAttribute("csrf_token"))
                .isEqualTo(result.getModelAndView().getModel().get("csrf_token"));
    }

    @Test
    void registerPageCreatesSessionAndPublishesCsrfToken() throws Exception {
        MvcResult result = mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("csrf_token"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }
}
