package com.weib.controller;

import com.weib.dto.ComplaintCreateRequest;
import com.weib.dto.ComplaintResponse;
import com.weib.dto.Result;
import com.weib.entity.User;
import com.weib.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Boss 和求职者共用的投诉 API。 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    public Result<ComplaintResponse> create(@RequestBody ComplaintCreateRequest request,
                                             HttpSession session) {
        User user = currentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(complaintService.create(user.getId(), request));
    }

    @GetMapping("/mine")
    public Result<List<ComplaintResponse>> mine(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(complaintService.mine(user.getId()));
    }

    @GetMapping("/{id}")
    public Result<ComplaintResponse> detail(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return Result.error(401, "请先登录");
        return Result.success(complaintService.getMine(user.getId(), id));
    }

    private User currentUser(HttpSession session) {
        return session == null ? null : (User) session.getAttribute("user");
    }
}
