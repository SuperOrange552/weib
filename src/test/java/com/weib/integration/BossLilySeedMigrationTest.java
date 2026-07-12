package com.weib.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BossLilySeedMigrationTest {
    @Test
    void seedIsIdempotentAndCreatesBossIdentityWithStandardPasswordHash() throws Exception {
        try (var input = getClass().getResourceAsStream("/db/V20260712_06__boss_lily_test_account.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("boss_lily", "Weib@123456", "NOT EXISTS", "'BOSS'", "'ACTIVE'");
            assertThat(sql).contains("role_profiles");
            assertThat(sql).contains("UPDATE users", "login_fail_count = 0", "lock_until = NULL");
            assertThat(sql).contains("'boss_li'", "'boss_zhang'", "'boss_wang'", "'boss_zhao'", "'boss_chen'");

            var matcher = Pattern.compile("\\$2[aby]\\$10\\$[./A-Za-z0-9]{53}").matcher(sql);
            assertThat(matcher.find()).isTrue();
            assertThat(new BCryptPasswordEncoder().matches("Weib@123456", matcher.group())).isTrue();
        }
    }
}
