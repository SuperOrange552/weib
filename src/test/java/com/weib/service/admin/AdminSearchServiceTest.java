package com.weib.service.admin;

import com.weib.entity.User;
import com.weib.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminSearchServiceTest {
    @Test
    void userSearchReturnsSafeSummaryWithoutSecrets() {
        UserRepository users = mock(UserRepository.class);
        User user = new User();
        user.setId(7L); user.setUsername("alice"); user.setNickname("Alice"); user.setPhone("13800138000");
        user.setRole("seeker"); user.setStatus("active"); user.setPassword("$2a$10$secret");
        user.setRememberToken("token"); user.setCreatedAt(LocalDateTime.now());
        when(users.findByUsernameContainingIgnoreCase(eq("alice"), any())).thenReturn(new PageImpl<>(List.of(user)));

        AdminSearchService service = new AdminSearchService(users, mock(com.weib.repository.CompanyRepository.class),
                mock(com.weib.repository.JobRepository.class), mock(com.weib.repository.ResumeRepository.class));
        var page = service.search("USER", "alice", PageRequest.of(0, 20), false);

        assertThat(page.getContent()).hasSize(1);
        var json = page.getContent().get(0).toString();
        assertThat(json).doesNotContain("secret").doesNotContain("token");
        assertThat(page.getContent().get(0).title()).isEqualTo("Alice");
    }
}