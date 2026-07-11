package com.weib.controller.mobile;

import com.weib.dto.Result;
import com.weib.entity.Application;
import com.weib.entity.User;
import com.weib.security.Idempotent;
import com.weib.service.ApplicationService;
import com.weib.service.FavoriteJobService;
import com.weib.util.IdObfuscator;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mobile/seeker")
@RequiredArgsConstructor
public class MobileSeekerActionController {
    private final ApplicationService applicationService;
    private final FavoriteJobService favoriteJobService;
    private final IdObfuscator idObfuscator;
    private final MobileAccessPolicy accessPolicy;

    @PostMapping("/jobs/{jobId}/apply")
    @Idempotent
    public Result<?> apply(@PathVariable String jobId, HttpSession session) {
        User user = currentSeeker(session);
        if (user == null) return Result.error(403, "仅求职者可以投递职位");
        Long decoded = idObfuscator.decode(jobId);
        if (decoded == null) return Result.error("职位参数无效");
        try {
            Application application = applicationService.apply(decoded, user.getId());
            return Result.success(Map.of(
                    "applicationId", idObfuscator.encode(application.getId()),
                    "status", application.getStatus()));
        } catch (RuntimeException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/jobs/{jobId}/favorite")
    @Idempotent
    public Result<?> favorite(@PathVariable String jobId, HttpSession session) {
        User user = currentSeeker(session);
        if (user == null) return Result.error(403, "仅求职者可以收藏职位");
        Long decoded = idObfuscator.decode(jobId);
        if (decoded == null) return Result.error("职位参数无效");
        favoriteJobService.toggleFavorite(decoded, user.getId());
        return Result.success(Map.of("favorited", favoriteJobService.isFavorited(decoded, user.getId())));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    @Idempotent
    public Result<?> withdraw(@PathVariable String applicationId, HttpSession session) {
        User user = currentSeeker(session);
        if (user == null) return Result.error(403, "仅求职者可以撤回投递");
        Long decoded = idObfuscator.decode(applicationId);
        if (decoded == null) return Result.error("投递参数无效");
        try {
            Application application = applicationService.getApplicationById(decoded);
            if (!user.getId().equals(application.getUserId())) {
                return Result.error(403, "无权操作该投递记录");
            }
            if (!java.util.Set.of("pending", "viewed").contains(application.getStatus())) {
                return Result.error("当前状态不能撤回");
            }
            application.setStatus("withdrawn");
            applicationService.updateStatus(application);
            return Result.success(Map.of("status", "withdrawn"));
        } catch (RuntimeException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private User currentSeeker(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return accessPolicy.hasRole(session, "seeker") ? user : null;
    }
}
