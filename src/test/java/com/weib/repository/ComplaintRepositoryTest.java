package com.weib.repository;

import com.weib.entity.Complaint;
import com.weib.entity.UserSanction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ComplaintRepositoryTest {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserSanctionRepository userSanctionRepository;

    @Test
    void pendingComplaintCanBeFoundByReporterAndTarget() {
        Complaint complaint = new Complaint();
        complaint.setReporterId(10L);
        complaint.setTargetType("JOB");
        complaint.setTargetId(20L);
        complaint.setCategory("FAKE_JOB");
        complaint.setDescription("职位信息与实际不符");
        complaint.setStatus("PENDING");
        complaintRepository.saveAndFlush(complaint);

        assertThat(complaintRepository.existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
                10L, "JOB", 20L, "PENDING")).isTrue();
    }

    @Test
    void activePermanentSanctionCanBeFoundSeparatelyFromExpiredTemporarySanction() {
        UserSanction permanent = new UserSanction();
        permanent.setUserId(11L);
        permanent.setSanctionType("MUTE");
        permanent.setReason("测试禁言");
        permanent.setAdminId(1L);
        permanent.setStatus("ACTIVE");
        permanent.setStartsAt(LocalDateTime.now().minusMinutes(1));
        userSanctionRepository.saveAndFlush(permanent);

        assertThat(userSanctionRepository.findActivePermanent(11L, "MUTE", "ACTIVE", LocalDateTime.now())).hasSize(1);
    }
}
