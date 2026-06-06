package com.weib.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI weibOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("微招（Weib）平台 API")
                        .version("1.0.0")
                        .description("微招在线招聘平台接口文档\n\n"
                                + "- 用户端接口：Session + CSRF Token 认证\n"
                                + "- 管理后台接口：JWT Bearer Token 认证\n"
                                + "- WebSocket：SockJS + STOMP 协议"));
    }
}
