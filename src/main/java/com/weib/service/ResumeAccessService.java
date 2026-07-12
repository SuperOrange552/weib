package com.weib.service;

import com.weib.entity.ResumeAccessRequest;
import com.weib.repository.ResumeAccessRequestRepository;
import com.weib.notification.NotificationEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class ResumeAccessService {
    private final ResumeAccessRequestRepository repository;
    private final NotificationEventService notifications;

    @Transactional
    public ResumeAccessRequest request(Long seekerId, Long bossId, Long companyId) {
        ResumeAccessRequest item = repository.findBySeekerIdAndBossId(seekerId, bossId).orElseGet(ResumeAccessRequest::new);
        item.setSeekerId(seekerId); item.setBossId(bossId); item.setCompanyId(companyId);
        item.setStatus("PENDING"); item.setDecidedAt(null); item.setRequestedAt(LocalDateTime.now());
        ResumeAccessRequest saved = repository.save(item);
        notifications.create("resume-request-" + saved.getId() + "-" + saved.getRequestedAt(), seekerId, "SEEKER",
                "RESUME_ACCESS_REQUEST", saved.getId(), "招聘者申请查看你的完整简历", null);
        return saved;
    }

    @Transactional
    public ResumeAccessRequest decide(Long id, Long seekerId, boolean approved) {
        ResumeAccessRequest item = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        if (!item.getSeekerId().equals(seekerId)) throw new org.springframework.security.access.AccessDeniedException("无权处理该申请");
        item.setStatus(approved ? "APPROVED" : "REJECTED"); item.setDecidedAt(LocalDateTime.now());
        ResumeAccessRequest saved = repository.save(item);
        notifications.create("resume-decision-" + saved.getId() + "-" + saved.getStatus(), saved.getBossId(), "BOSS",
                "RESUME_ACCESS_DECISION", saved.getId(), approved ? "求职者已同意完整简历授权" : "求职者已拒绝完整简历授权", null);
        return saved;
    }

    public List<ResumeAccessRequest> seekerRequests(Long seekerId) { return repository.findBySeekerIdOrderByRequestedAtDesc(seekerId); }
    public List<ResumeAccessRequest> bossRequests(Long bossId) { return repository.findByBossIdOrderByRequestedAtDesc(bossId); }
    public ResumeAccessRequest approved(Long id, Long bossId) {
        ResumeAccessRequest item = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        if (!item.getBossId().equals(bossId) || !"APPROVED".equals(item.getStatus())) throw new org.springframework.security.access.AccessDeniedException("尚未获得完整简历授权");
        return item;
    }
}
