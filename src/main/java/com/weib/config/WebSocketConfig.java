package com.weib.config;

import com.weib.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import org.springframework.beans.factory.annotation.Value;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketSessionManager sessionManager;

    @Value("${websocket.allowed-origins:https://localhost:8443,http://localhost:8888,https://127.0.0.1:8443,http://127.0.0.1:8888}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                      WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        Object userId = attributes.get("userId");
                        if (userId != null) {
                            return () -> userId.toString();
                        }
                        return null;
                    }
                })
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            HttpSession session = servletRequest.getServletRequest().getSession(false);
                            if (session != null) {
                                User user = (User) session.getAttribute("user");
                                if (user != null) {
                                    attributes.put("userId", user.getId());
                                    return true;
                                }
                            }
                        }
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                    }
                })
                .withSockJS();
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketConfig.class);

    /**
     * 提取 userId：先取 event.getUser()，再取 STOMP header 中的 simpUser 作为备用
     */
    private Long extractUserId(Principal principal, Map<String, Object> headers) {
        if (principal != null) {
            return Long.valueOf(principal.getName());
        }
        // 备用：有些 Spring 版本 SessionConnectedEvent.getUser() 可能返回 null，
        // 但 simpUser header 中仍然有值
        if (headers != null) {
            Principal headerUser = SimpMessageHeaderAccessor.getUser(headers);
            if (headerUser != null) {
                return Long.valueOf(headerUser.getName());
            }
        }
        return null;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        Long userId = extractUserId(event.getUser(), event.getMessage().getHeaders());
        if (userId != null) {
            try {
                sessionManager.addSession(userId, event.getMessage().getHeaders().get("simpSessionId").toString());
                log.debug("WebSocket 用户上线: userId={}", userId);
            } catch (Exception e) {
                log.warn("WebSocket连接事件处理异常, userId={}", userId, e);
            }
        } else {
            log.warn("WebSocket连接事件：无法获取用户身份（Principal 为空），已跳过在线状态记录");
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = extractUserId(event.getUser(), null);
        if (userId != null) {
            try {
                sessionManager.removeSession(userId, event.getSessionId());
                log.debug("WebSocket 用户下线: userId={}", userId);
            } catch (Exception e) {
                log.warn("WebSocket断开事件处理异常, userId={}", userId, e);
            }
        } else {
            // 使用 close status 中的 sessionId 尝试清理（兜底逻辑）
            String sessionId = event.getSessionId();
            log.warn("WebSocket断开事件：无法获取用户身份，sessionId={}", sessionId);
        }
    }

    /**
     * 用户订阅消息队列时也记录为在线（作为 SessionConnectedEvent 的补充保障）
     * 订阅 /user/{userId}/queue/messages 说明用户已建立完整的 STOMP 连接并准备接收消息
     */
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        Principal principal = SimpMessageHeaderAccessor.getUser(event.getMessage().getHeaders());
        if (principal != null) {
            try {
                Long userId = Long.valueOf(principal.getName());
                String destination = SimpMessageHeaderAccessor.getDestination(event.getMessage().getHeaders());
                if (destination != null && destination.contains("/queue/messages")) {
                    sessionManager.addSession(userId, event.getMessage().getHeaders().get("simpSessionId").toString());
                    log.debug("WebSocket 用户订阅消息队列: userId={}, destination={}", userId, destination);
                }
            } catch (Exception e) {
                log.warn("WebSocket订阅事件处理异常", e);
            }
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        Principal principal = SimpMessageHeaderAccessor.getUser(event.getMessage().getHeaders());
        if (principal != null) {
            try {
                Long userId = Long.valueOf(principal.getName());
                String destination = SimpMessageHeaderAccessor.getDestination(event.getMessage().getHeaders());
                if (destination != null && destination.contains("/queue/messages")) {
                    sessionManager.removeSession(userId, event.getMessage().getHeaders().get("simpSessionId").toString());
                    log.debug("WebSocket 用户取消订阅消息队列: userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("WebSocket取消订阅事件处理异常", e);
            }
        }
    }
}
