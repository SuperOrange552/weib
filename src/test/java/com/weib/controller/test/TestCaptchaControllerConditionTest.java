package com.weib.controller.test;

import com.weib.service.CaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TestCaptchaControllerConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CaptchaService.class, () -> mock(CaptchaService.class))
            .withUserConfiguration(TestCaptchaController.class);

    @Test
    void isDisabledByDefault() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(TestCaptchaController.class));
    }

    @Test
    void canBeEnabledExplicitly() {
        contextRunner
                .withPropertyValues(
                        "test-tools.captcha.enabled=true",
                        "test-tools.captcha.access-key=0123456789abcdef-test-key"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(TestCaptchaController.class));
    }
}
