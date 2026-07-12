package com.weib.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebTemplateRegressionTest {

    @Test
    void dualRoleTemplatesUseTheActiveSessionRole() throws IOException {
        for (String template : new String[]{"index.html", "chat-list.html", "job-detail.html"}) {
            String html = resource("templates/" + template);
            assertThat(html).as(template).doesNotContain("user.role");
            assertThat(html).as(template).contains("activeRole");
        }
    }

    @Test
    void companySelectOptionsHaveReadableLightTheme() throws IOException {
        String html = resource("templates/boss-register.html");
        assertThat(html).contains("select.form-input option { background: #fff; color: #0f172a;");
    }

    @Test
    void companyAddressSearchUsesAuthenticatedServerGeocoding() throws IOException {
        String html = resource("templates/boss-register.html");
        assertThat(html).contains("fetch('/api/geocode?address='");
        assertThat(html).contains("placeMarker(result.data.lng, result.data.lat)");
    }

    @Test
    void forumComposeUsesThePublishedCsrfModelAttribute() throws IOException {
        String html = resource("templates/forum-compose.html");
        assertThat(html).contains("th:content=\"${csrf_token}\"");
        assertThat(html).doesNotContain("${session.csrf_token}");
    }

    @Test
    void pendingCompanyIsPresentedAsAwaitingAuditInsteadOfOperational() throws IOException {
        String html = resource("templates/boss-home.html");
        assertThat(html).contains("companyApproved");
        assertThat(html).contains("等待管理员审核");
    }

    private String resource(String path) throws IOException {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(stream).as(path).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
