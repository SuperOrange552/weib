package com.weib.service;

import com.weib.entity.Company;
import com.weib.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
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
        Company existing = getCompanyById(company.getId());
        company.setCreatedAt(existing.getCreatedAt());
        return companyRepository.save(company);
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
    public void geocodeAndPersistAsync(Company company) {
        if (company.getAddress() == null || company.getAddress().isEmpty()) return;
        double[] coords = mapService.geocode(company.getAddress(), null);
        if (coords != null) {
            company.setLongitude(coords[0]);
            company.setLatitude(coords[1]);
            updateCompany(company);
        }
    }
}
