package com.weib.security;

import com.weib.util.CookieUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieUtilTest {
    @Test
    void cookiesFollowTheActualRequestTransport() {
        MockHttpServletRequest http = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();
        CookieUtil.addJwtCookie(httpResponse, "token", http);
        assertFalse(httpResponse.getCookie("jwt_token").getSecure());

        MockHttpServletRequest https = new MockHttpServletRequest();
        https.setSecure(true);
        MockHttpServletResponse httpsResponse = new MockHttpServletResponse();
        CookieUtil.addJwtCookie(httpsResponse, "token", https);
        assertTrue(httpsResponse.getCookie("jwt_token").getSecure());
    }

    @Test
    void forwardedHttpsIsAlsoTreatedAsSecure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieUtil.addRememberTokenCookie(response, "token", request);
        assertTrue(response.getCookie("remember_token").getSecure());
    }
}
