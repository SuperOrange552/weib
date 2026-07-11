package com.weib.session;

import com.weib.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionSlotInterceptorTest {
    @Test
    void staleMobileSidReturnsStructuredKickoutResponse() throws Exception {
        SessionRegistryService registry = mock(SessionRegistryService.class);
        SessionSlotInterceptor interceptor = new SessionSlotInterceptor(registry);
        MockHttpServletRequest request = authenticated("/api/mobile/jobs", ClientType.MOBILE, "old");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(registry.isCurrent(7L, ClientType.MOBILE, "old")).thenReturn(false);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("KICKED"));
        assertTrue(response.getContentAsString().contains("其他设备登录"));
    }

    @Test
    void staleWebSidRedirectsToKickoutLoginPage() throws Exception {
        SessionRegistryService registry = mock(SessionRegistryService.class);
        SessionSlotInterceptor interceptor = new SessionSlotInterceptor(registry);
        MockHttpServletRequest request = authenticated("/resume", ClientType.WEB, "old");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals("/login?kicked", response.getRedirectedUrl());
    }

    private MockHttpServletRequest authenticated(String uri, ClientType type, String sid) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        User user = new User();
        user.setId(7L);
        request.getSession().setAttribute("user", user);
        request.getSession().setAttribute("sid", sid);
        request.getSession().setAttribute("clientType", type.name());
        return request;
    }
}
