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
    void forumUploadReportsGatewaySizeErrorsInsteadOfClaimingSessionExpired() throws IOException {
        String javascript = resource("static/js/forum-compose.js");
        assertThat(javascript).contains("response.status === 413");
        assertThat(javascript).contains("图片超过服务器允许的上传大小（单张最大 10MB）");
        assertThat(javascript).contains("response.status === 401 || response.status === 403");
        assertThat(javascript).doesNotContain("if (!type.includes('application/json')) return { code: response.status, msg: '请先登录后再进行此操作' }");
    }

    @Test
    void multipartRequestLimitAllowsTenMegabyteFilePlusFormOverhead() throws IOException {
        String yaml = resource("application.yml");
        assertThat(yaml).contains("max-file-size: 10MB");
        assertThat(yaml).contains("max-request-size: 12MB");
    }

    @Test
    void everyForumPageProvidesAnExplicitSystemReturnEntry() throws IOException {
        for (String template : new String[]{"forum.html", "forum-detail.html", "forum-compose.html"}) {
            String html = resource("templates/" + template);
            assertThat(html).as(template).contains("class=\"forum-system-back\"");
            assertThat(html).as(template).contains("activeRole == 'BOSS'");
            assertThat(html).as(template).contains("返回Boss工作台", "返回职位大厅");
        }
    }

    @Test
    void forumComposeAcceptsChineseAndEnglishCommaTagSeparators() throws IOException {
        String javascript = resource("static/js/forum-compose.js");
        assertThat(javascript).contains(".split(/[,，]/)");
        String html = resource("templates/forum-compose.html");
        assertThat(html).contains("中文或英文逗号分隔");
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
