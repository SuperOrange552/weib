package com.weib.controller;

import com.weib.dto.Result;
import com.weib.identity.ActiveIdentityResolver;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/seeker/resume/media")
@RequiredArgsConstructor
public class ResumeMediaController {
    private final ActiveIdentityResolver identities;
    @Value("${storage.upload-dir}") private String uploadDir;

    @PostMapping
    public Result<Map<String,Object>> upload(@RequestParam("file") MultipartFile file,
                                             @RequestParam("kind") String kind, HttpSession session) throws Exception {
        identities.require(session, "SEEKER");
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("文件无效或超过10MB");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        boolean avatar = "avatar".equalsIgnoreCase(kind);
        if (avatar && !Set.of("image/jpeg","image/png","image/webp").contains(contentType)) throw new IllegalArgumentException("头像仅支持 JPG、PNG、WebP");
        if (!avatar && !Set.of("application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document").contains(contentType)) throw new IllegalArgumentException("附件仅支持 PDF、DOC、DOCX");
        String ext = extension(file.getOriginalFilename());
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve("resume").normalize();
        if (!dir.startsWith(root)) throw new IllegalArgumentException("上传路径无效");
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + ext;
        file.transferTo(dir.resolve(name));
        return Result.success(Map.of("url", "/uploads/resume/" + name, "name", Optional.ofNullable(file.getOriginalFilename()).orElse(name), "size", file.getSize()));
    }

    private String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
        if (!Set.of(".jpg",".jpeg",".png",".webp",".pdf",".doc",".docx").contains(ext)) throw new IllegalArgumentException("文件扩展名无效");
        return ext;
    }
}
