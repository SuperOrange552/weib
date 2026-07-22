package com.weib.controller.mobile;

import com.weib.dto.Result;
import com.weib.dto.forum.ForumPostCreateRequest;
import com.weib.dto.forum.ForumPostResponse;
import com.weib.identity.ActiveIdentity;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.service.ForumService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 移动端论坛写接口。
 *
 * 路径放在/api/mobile/**下，因此使用Bearer Token认证，不要求Web表单CSRF Token。
 * 论坛公开查询仍复用/api/forum/**。
 */
@RestController
@RequestMapping("/api/mobile/forum")
@RequiredArgsConstructor
public class MobileForumController {
    private final ForumService forumService;
    private final ActiveIdentityResolver identityResolver;

    @PostMapping("/posts")
    public Result<ForumPostResponse> create(@RequestBody ForumPostCreateRequest request,
                                            HttpSession session) {
        ActiveIdentity identity = identityResolver.current(session);
        return Result.success(forumService.create(
                identity.userId(), identity.role(), request));
    }

    @DeleteMapping("/posts/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        ActiveIdentity identity = identityResolver.current(session);
        forumService.deleteOwnPost(identity.userId(), identity.role(), id);
        return Result.success();
    }
}

