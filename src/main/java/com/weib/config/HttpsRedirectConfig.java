package com.weib.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================
 * 【配置类】HTTPS 重定向
 * ============================================
 * 
 * 大白话：用户访问 http://localhost:8080 自动跳到 https://localhost:8443
 * 保证所有流量都走加密通道，密码不会在网络上明文传输
 * 
 * 原理：
 * - 主端口 8443 走 HTTPS（SSL 加密）
 * - 额外开 8080 端口，只做 302 重定向到 8443
 */
@Configuration
public class HttpsRedirectConfig {

    /**
     * 定制 Tomcat 容器，加一个 HTTP 重定向连接器
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpsRedirectCustomizer() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(8888);          // 监听旧的 HTTP 端口
            connector.setSecure(false);
            connector.setRedirectPort(8443);  // 自动 302 跳到 HTTPS 端口
            factory.addAdditionalTomcatConnectors(connector);
        };
    }
}
