package com.weib.security;

import com.weib.entity.Company;
import org.springframework.stereotype.Component;

/** Central gate for company onboarding and recruiting operations. */
@Component
public class CompanyApprovalPolicy {

    public boolean isApproved(Company company) {
        if (company == null) return false;
        String status = company.getAuditStatus();
        // Existing production rows created before the audit column are legacy-approved.
        return status == null || "approved".equalsIgnoreCase(status);
    }

    public void markPending(Company company) {
        company.setAuditStatus("pending");
        company.setAuditReason(null);
    }
}
