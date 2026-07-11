package com.weib.controller;

import com.weib.dto.AppealCreateRequest;
import com.weib.dto.AppealResponse;
import com.weib.dto.Result;
import com.weib.entity.User;
import com.weib.service.AppealService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
public class AppealController {
    private final AppealService appealService;

    @PostMapping
    public Result<AppealResponse> create(@RequestBody AppealCreateRequest request, HttpSession session) {
        return Result.success(appealService.create(userId(session), request));
    }

    @GetMapping("/mine")
    public Result<com.weib.dto.admin.PageResponse<AppealResponse>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpSession session) {
        int bounded = Math.min(Math.max(size, 1), 50);
        return Result.success(com.weib.dto.admin.PageResponse.of(appealService.mine(userId(session),
                PageRequest.of(Math.max(page, 0), bounded, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/{id}")
    public Result<AppealResponse> detail(@PathVariable Long id, HttpSession session) {
        return Result.success(appealService.detailForUser(userId(session), id));
    }

    private Long userId(HttpSession session) {
        User user = session == null ? null : (User) session.getAttribute("user");
        if (user == null) throw new IllegalArgumentException("请先登录");
        return user.getId();
    }
}