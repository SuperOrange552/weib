package com.weib.controller.mobile;

import com.weib.dto.Result;
import com.weib.dto.mobile.MobileProfileResponse;
import com.weib.dto.mobile.MobileProfileUpdateRequest;
import com.weib.entity.RoleProfile;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.MobileProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Token认证后的移动端个人资料接口。 */
@RestController
@RequestMapping("/api/mobile/profile")
@RequiredArgsConstructor
public class MobileProfileController {
    private final ActiveIdentityResolver identityResolver;
    private final MobileProfileService profileService;

    @GetMapping
    public Result<MobileProfileResponse> current(HttpSession session) {
        return Result.success(toResponse(identityResolver.current(session)));
    }

    @PutMapping
    public Result<MobileProfileResponse> update(@RequestBody MobileProfileUpdateRequest request,
                                                HttpSession session) {
        ActiveIdentity identity = identityResolver.current(session);
        RoleProfile saved = profileService.updateNickname(
                identity.userId(), identity.role(), request == null ? null : request.nickname());
        String avatar = saved.getAvatar() != null ? saved.getAvatar() : identity.avatar();
        return Result.success(new MobileProfileResponse(
                identity.userId(), identity.role(), saved.getNickname(), avatar));
    }

    private MobileProfileResponse toResponse(ActiveIdentity identity) {
        return new MobileProfileResponse(
                identity.userId(), identity.role(), identity.nickname(), identity.avatar());
    }
}

