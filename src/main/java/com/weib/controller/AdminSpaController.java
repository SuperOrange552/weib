package com.weib.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 管理后台 SPA 回退控制器
 *
 * React SPA 使用客户端路由（react-router-dom），所有 /admin 路径
 * 在前端处理。当用户直接访问 /admin/companies 等路径或刷新页面时，
 * Spring Boot 需要返回 index.html 让 React Router 接管。
 *
 * 注意：Spring Boot 3.x 默认不匹配尾斜杠（/admin 与 /admin/ 视为不同），
 * 因此每个映射都同时声明带/不带尾斜杠两个变体。
 */
@Controller
public class AdminSpaController {

    /**
     * 管理后台入口页
     * 匹配 /admin 和 /admin/
     */
    @GetMapping({"/admin", "/admin/"})
    public String adminIndex() {
        return "forward:/admin/index.html";
    }

    /**
     * 管理后台子路由回退（仅匹配不含文件扩展名的路径）
     * 例如 /admin/companies、/admin/jobs  → 转发到 index.html
     * /admin/assets/main.js 不匹配（含 . 扩展名）
     */
    @GetMapping({"/admin/{path:[^.]+}", "/admin/{path:[^.]+}/"})
    public String adminForward() {
        return "forward:/admin/index.html";
    }

    /**
     * 管理后台多级子路由回退
     * 例如 /admin/companies/123
     */
    @GetMapping({"/admin/{path1:[^.]+}/{path2:[^.]+}", "/admin/{path1:[^.]+}/{path2:[^.]+}/"})
    public String adminDeepForward() {
        return "forward:/admin/index.html";
    }
}
