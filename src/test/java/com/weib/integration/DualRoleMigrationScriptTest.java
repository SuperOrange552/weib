package com.weib.integration;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DualRoleMigrationScriptTest {
    @Test
    void migrationContainsIdempotentRulesForExistingAccounts() throws Exception {
        try (var input = getClass().getResourceAsStream("/db/V20260712__dual_role_identity.sql")) {
            assertTrue(input != null, "migration resource must exist");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(sql.contains("seeker_ahua"));
            assertTrue(sql.contains("boss\\_%"));
            assertTrue(sql.contains("NOT EXISTS"));
            assertTrue(sql.contains("UNIQUE KEY uk_user_role"));
            assertTrue(sql.contains("UNIQUE KEY uk_role_profile"));
        }
    }
}
