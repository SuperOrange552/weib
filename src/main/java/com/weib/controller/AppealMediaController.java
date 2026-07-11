package com.weib.controller;

import com.weib.dto.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/appeals")
public class AppealMediaController {
    private final Path uploadRoot;

    public AppealMediaController(@Value("${storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/evidence")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file, HttpSession session)
            throws Exception {
        if (session == null || session.getAttribute("user") == null) throw new IllegalArgumentException("请先登录");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择图片");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("单张图片不能超过 10MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) throw new IllegalArgumentException("申诉证据仅支持图片");
        String ext = extension(file.getOriginalFilename(), contentType);
        Path directory = uploadRoot.resolve("appeals").normalize();
        if (!directory.startsWith(uploadRoot)) throw new IllegalArgumentException("上传目录无效");
        Files.createDirectories(directory);
        String storedName = UUID.randomUUID() + ext;
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) throw new IllegalArgumentException("文件路径无效");
        file.transferTo(target);
        return Result.success(Map.of("url", "/uploads/appeals/" + storedName, "size", file.getSize()));
    }

    private String extension(String originalName, String contentType) {
        String name = originalName == null ? "" : originalName.toLowerCase();
        if (name.endsWith(".png") && contentType.equals("image/png")) return ".png";
        if (name.endsWith(".gif") && contentType.equals("image/gif")) return ".gif";
        if ((name.endsWith(".jpg") || name.endsWith(".jpeg")) && contentType.equals("image/jpeg")) return ".jpg";
        if (contentType.equals("image/webp")) return ".webp";
        throw new IllegalArgumentException("不支持的图片格式");
    }
}