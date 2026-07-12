package com.weib.security;

import com.weib.entity.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyApprovalPolicyTest {

    private final CompanyApprovalPolicy policy = new CompanyApprovalPolicy();

    @Test
    void onlyApprovedOrLegacyCompaniesCanOperate() {
        assertThat(policy.isApproved(company("approved"))).isTrue();
        assertThat(policy.isApproved(company(null))).isTrue();
        assertThat(policy.isApproved(company("pending"))).isFalse();
        assertThat(policy.isApproved(company("rejected"))).isFalse();
    }

    @Test
    void newlySubmittedCompanyIsAlwaysPending() {
        Company company = company("approved");
        company.setAuditReason("old rejection");

        policy.markPending(company);

        assertThat(company.getAuditStatus()).isEqualTo("pending");
        assertThat(company.getAuditReason()).isNull();
    }

    private Company company(String status) {
        Company company = new Company();
        company.setAuditStatus(status);
        return company;
    }
}
