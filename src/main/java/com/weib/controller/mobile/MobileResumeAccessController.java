package com.weib.controller.mobile;

import com.weib.dto.Result;
import com.weib.entity.Company;
import com.weib.entity.ResumeAccessRequest;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.CompanyService;
import com.weib.service.ResumeAccessService;
import com.weib.service.ResumeService;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/mobile/resume-access") @RequiredArgsConstructor
public class MobileResumeAccessController {
    private final ActiveIdentityResolver identities; private final ResumeAccessService access;
    private final ResumeService resumes; private final CompanyService companies; private final IdObfuscator ids;

    @PostMapping("/requests")
    public Result<?> request(@RequestBody Map<String,String> body, HttpSession session) {
        ActiveIdentity boss=identities.require(session,"BOSS"); Long seekerId=ids.decode(body.get("seekerId"));
        if(seekerId==null) return Result.error(400,"求职者参数无效");
        Company company=companies.getCompanyByBossId(boss.userId());
        return Result.success(access.request(seekerId,boss.userId(),company.getId()));
    }
    @GetMapping("/requests") public Result<?> list(HttpSession session){ActiveIdentity i=identities.current(session);return Result.success("SEEKER".equalsIgnoreCase(i.role())?access.seekerRequests(i.userId()):access.bossRequests(i.userId()));}
    @PostMapping("/requests/{id}/decision") public Result<?> decide(@PathVariable Long id,@RequestBody Map<String,Boolean> body,HttpSession session){ActiveIdentity seeker=identities.require(session,"SEEKER");return Result.success(access.decide(id,seeker.userId(),Boolean.TRUE.equals(body.get("approved"))));}
    @GetMapping("/requests/{id}/resume") public Result<?> resume(@PathVariable Long id,HttpSession session){ActiveIdentity boss=identities.require(session,"BOSS");ResumeAccessRequest grant=access.approved(id,boss.userId());return Result.success(resumes.getResumeByUserId(grant.getSeekerId()));}
}
