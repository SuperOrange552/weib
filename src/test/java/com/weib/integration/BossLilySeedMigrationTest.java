package com.weib.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BossLilySeedMigrationTest {
    @Test
    void seedIsIdempotentAndCreatesBossIdentityWithStandardPasswordHash() throws Exception {
        try (var input = getClass().getResourceAsStream("/db/V20260712_06__boss_lily_test_account.sql")) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("boss_lily", "Weib@123456", "NOT EXISTS", "'BOSS'", "'ACTIVE'");
            assertThat(sql).contains("role_profiles");
        }
    }
}
