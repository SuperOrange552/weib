package com.weib.service;

import com.weib.entity.Company;
import com.weib.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CompanyService.class);
    private final CompanyRepository companyRepository;
    private final MapService mapService;

    @Transactional(readOnly = true)
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("公司不存在: " + id));
    }

    @Transactional(readOnly = true)
    public Map<Long, Company> getCompanyMapByIds(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return companyRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Company getCompanyByBossId(Long bossId) {
        return companyRepository.findByBossId(bossId).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("该公司Boss的公司不存在: " + bossId));
    }

    @Transactional
    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    @Transactional
    public Company updateCompany(Company company) {
        // 逐字段更新，避免传入对象字段缺失导致数据被覆盖为 null
        Company existing = getCompanyById(company.getId());
        if (company.getName() != null) existing.setName(company.getName());
        if (company.getIndustry() != null) existing.setIndustry(company.getIndustry());
        if (company.getScale() != null) existing.setScale(company.getScale());
        if (company.getAddress() != null) existing.setAddress(company.getAddress());
        if (company.getDescription() != null) existing.setDescription(company.getDescription());
        if (company.getContactName() != null) existing.setContactName(company.getContactName());
        if (company.getContactPhone() != null) existing.setContactPhone(company.getContactPhone());
        if (company.getContactEmail() != null) existing.setContactEmail(company.getContactEmail());
        if (company.getLongitude() != null) existing.setLongitude(company.getLongitude());
        if (company.getLatitude() != null) existing.setLatitude(company.getLatitude());
        // createdAt 和 bossId 不允许通过此方法修改
        return companyRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public boolean existsByBossId(Long bossId) {
        return companyRepository.existsByBossId(bossId);
    }

    @Transactional(readOnly = true)
    public List<Company> searchCompanies(String keyword) {
        return companyRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Async
    @Transactional
    public void geocodeAndPersistAsync(Company company) {
        if (company.getAddress() == null || company.getAddress().isEmpty()) return;
        try {
            double[] coords = mapService.geocode(company.getAddress(), null);
            if (coords != null) {
                company.setLongitude(coords[0]);
                company.setLatitude(coords[1]);
                updateCompany(company);
            }
        } catch (Exception e) {
            // 地理编码失败不影响主流程，但记录日志便于排查
            log.warn("地理编码异步处理失败, companyId={}", company.getId(), e);
        }
    }
}
