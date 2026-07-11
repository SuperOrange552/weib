package com.weib.repository;

import com.weib.entity.SanctionAppeal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SanctionAppealRepositoryTest {
    @Autowired
    private SanctionAppealRepository repository;

    @Test
    void pendingAppealCanBeFoundBySanctionAndUser() {
        SanctionAppeal appeal = new SanctionAppeal();
        appeal.setSanctionId(12L);
        appeal.setUserId(8L);
        appeal.setReason("我已补充真实材料，请重新审核");
        appeal.setEvidenceUrls("[\"/uploads/appeals/proof.png\"]");
        appeal.setStatus("PENDING");
        repository.saveAndFlush(appeal);

        assertThat(repository.findFirstBySanctionIdAndUserIdAndStatus(12L, 8L, "PENDING"))
                .isPresent();
    }
}