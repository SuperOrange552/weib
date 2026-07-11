package com.weib.session;

import com.weib.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SessionSlotInterceptor implements HandlerInterceptor {
    private final SessionRegistryService registry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("user") instanceof User user)) {
            return true;
        }
        String sid = (String) session.getAttribute("sid");
        String clientTypeValue = (String) session.getAttribute("clientType");
        if (sid == null || clientTypeValue == null) {
            return true;
        }
        ClientType clientType;
        try {
            clientType = ClientType.valueOf(clientTypeValue);
        } catch (IllegalArgumentException ex) {
            session.invalidate();
            return reject(request, response);
        }
        if (registry.isCurrent(user.getId(), clientType, sid)) {
            return true;
        }
        session.invalidate();
        return reject(request, response);
    }

    private boolean reject(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"你的账号已在其他设备登录，请检查密码是否泄露\",\"data\":{\"reason\":\"KICKED\"}}");
        } else {
            response.sendRedirect("/login?kicked");
        }
        return false;
    }
}
