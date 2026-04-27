package com.weib.service;

import com.weib.entity.Company;
import com.weib.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================
 * 【Service】公司业务逻辑层
 * ============================================
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 根据ID获取公司
     */
    @Transactional(readOnly = true)
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公司不存在: " + id));
    }

    /**
     * 根据Boss ID获取公司
     */
    @Transactional(readOnly = true)
    public Company getCompanyByBossId(Long bossId) {
        return companyRepository.findByBossId(bossId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("该公司Boss的公司不存在: " + bossId));
    }

    /**
     * 创建公司
     */
    @Transactional
    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    /**
     * 更新公司
     */
    @Transactional
    public Company updateCompany(Company company) {
        Company existing = getCompanyById(company.getId());
        company.setCreatedAt(existing.getCreatedAt());
        return companyRepository.save(company);
    }

    /**
     * Boss是否已创建公司
     */
    @Transactional(readOnly = true)
    public boolean existsByBossId(Long bossId) {
        return companyRepository.existsByBossId(bossId);
    }

    /**
     * 搜索公司（名称模糊匹配）
     */
    @Transactional(readOnly = true)
    public List<Company> searchCompanies(String keyword) {
        return companyRepository.findByNameContainingIgnoreCase(keyword);
    }
}