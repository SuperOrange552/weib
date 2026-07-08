package com.weib.config;

import com.weib.security.Idempotent;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI weibOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("微招（Weib）平台 API")
                .version("1.1.0")
                .description("""
                        微招在线招聘平台接口文档。

                        账号规则：用户名 3–32 位，仅允许字母、数字、下划线和中文。
                        密码规则：注册/修改/重置密码时为 8–64 位，必须同时包含大写字母、小写字母和数字，且不能与用户名或手机号相同。
                        登录输入：用户名或手机号最多 32 位，密码最多 64 位；登录失败不返回具体密码规则。
                        手机号：11 位中国大陆手机号；验证码：4 位，有效期 120 秒。

                        用户端：Session + CSRF Token；管理后台：JWT Bearer Token。
                        标注为幂等的写接口必须传 Idempotency-Key，格式为 8–128 位字母、数字、点、下划线、冒号或连字符。
                        """));
    }

    @Bean
    public OperationCustomizer idempotencyOpenApiCustomizer() {
        return (operation, handlerMethod) -> {
            if (handlerMethod.hasMethodAnnotation(Idempotent.class)) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("Idempotency-Key")
                        .required(true)
                        .description("幂等键。同一次操作及其重试必须复用同一个值；建议使用 UUID。")
                        .schema(new StringSchema().minLength(8).maxLength(128)
                                .pattern("^[A-Za-z0-9._:-]{8,128}$")
                                .example("550e8400-e29b-41d4-a716-446655440000")));
                operation.getResponses()
                        .addApiResponse("400", new ApiResponse().description("缺少或非法幂等键/请求参数不合法"))
                        .addApiResponse("409", new ApiResponse().description("相同操作正在处理中，请勿重复提交"));
            }
            return operation;
        };
    }
}