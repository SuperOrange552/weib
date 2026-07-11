package com.weib.controller;

import com.weib.annotation.RateLimit;
import com.weib.config.WebSocketSessionManager;
import com.weib.dto.Result;
import com.weib.entity.Application;
import com.weib.entity.Company;
import com.weib.entity.Job;
import com.weib.entity.Message;
import com.weib.entity.User;
import com.weib.service.ApplicationService;
import com.weib.service.CompanyService;
import com.weib.service.JobService;
import com.weib.service.MessageService;
import com.weib.service.UserService;
import com.weib.service.SanctionService;
import com.weib.dto.PublicUserProfile;
import com.weib.repository.MessageRepository;
import com.weib.repository.UserRepository;
import com.weib.util.IdObfuscator;
import com.weib.identity.ActiveIdentityResolver;
import com.weib.identity.ActivePrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private final MessageService messageService;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final CompanyService companyService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SanctionService sanctionService;
    private final WebSocketSessionManager sessionManager;
    private final IdObfuscator idObfuscator;
    private final ActiveIdentityResolver activeIdentityResolver;
    private final com.weib.notification.NotificationEventService notificationEvents;

    @Value("${storage.chat-dir:./weib/uploads/chat}")
    private String chatDir;

    /** 聊天文件保留天数，超期自动清理 */
    @Value("${storage.chat-retention-days:30}")
    private int chatRetentionDays;

    private String sanitizeHtml(String input) {
        if (input == null) return null;
        // 限制消息内容长度，防止超长消息攻击
        if (input.length() > 2000) {
            input = input.substring(0, 2000);
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /**
     * ============================================
     * 【会话列表页 /chat】
     * ============================================
     *
     * 展示用户的所有会话（按角色不同逻辑）：
     * - seeker: 遍历自己的投递 → 每个投递一个会话
     * - boss:   遍历公司所有职位 → 所有投递者 → 每个投递一个会话
     *
     * 新增功能：
     * 1. 每个会话显示未读数（已有）
     * 2. 每个会话显示最后一条消息预览（新增）
     * 3. 按最后消息时间降序排列（新增）
     */
    @GetMapping("/chat")
    public String chatList(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);

        List<Map<String, Object>> conversations = new ArrayList<>();
        if (activeIdentityResolver.hasRole(session, "SEEKER")) {
            List<Application> apps = applicationService.getApplicationsByUser(user.getId());

            // 批量加载职位和公司信息
            List<Long> jobIds = apps.stream().map(Application::getJobId).distinct().collect(java.util.stream.Collectors.toList());
            Map<Long, Job> jobMap = batchLoadJobMap(jobIds);
            Map<Long, Company> companyMap = batchLoadCompanyMap(jobMap);

            for (Application app : apps) {
                conversations.add(buildConversationMap(app, user.getId(), "SEEKER", jobMap, companyMap));
            }
        } else if (activeIdentityResolver.hasRole(session, "BOSS")) {
            try {
                Company company = companyService.getCompanyByBossId(user.getId());
                List<Job> jobs = jobService.getJobsByCompanyId(company.getId());

                // 批量加载所有职位的投递
                List<Long> jobIds = jobs.stream().map(Job::getId).collect(java.util.stream.Collectors.toList());
                List<Application> apps = jobIds.isEmpty() ? List.of() : applicationService.getApplicationsByJobIds(jobIds);

                // 批量加载求职者信息
                List<Long> seekerIds = apps.stream().map(Application::getUserId).distinct().collect(java.util.stream.Collectors.toList());
                Map<Long, User> seekerMap = new HashMap<>();
                if (!seekerIds.isEmpty()) {
                    for (User u : userRepository.findAllById(seekerIds)) seekerMap.put(u.getId(), u);
                }

                // 构建 job/company maps
                Map<Long, Job> jobMap = jobs.stream().collect(java.util.stream.Collectors.toMap(Job::getId, j -> j));
                Map<Long, Company> companyMap = Map.of(company.getId(), company);

                for (Application app : apps) {
                    Map<String, Object> conv = buildConversationMap(app, user.getId(), "BOSS", jobMap, companyMap);
                    User seeker = seekerMap.get(app.getUserId());
                    if (seeker != null) {
                        conv.put("seekerName", seeker.getNickname() != null ? seeker.getNickname() : seeker.getUsername());
                    }
                    conversations.add(conv);
                }
            } catch (Exception e) {
                log.warn("加载Boss会话列表异常: {}", e.getMessage());
            }
        }

        // 按最后消息时间降序排列（有新消息的会话排前面）
        conversations.sort((a, b) -> {
            Long ta = (Long) a.getOrDefault("lastMsgTime", 0L);
            Long tb = (Long) b.getOrDefault("lastMsgTime", 0L);
            return tb.compareTo(ta);
        });

        model.addAttribute("conversations", conversations);
        return "chat-list";
    }

    /**
     * 构建单个会话的展示数据
     * 包含：基本信息、未读数、最后一条消息预览
     */
    private Map<String, Object> buildConversationMap(Application app, Long currentUserId, String currentRole,
                                                      Map<Long, Job> jobMap, Map<Long, Company> companyMap) {
        Map<String, Object> conv = new HashMap<>();
        conv.put("applicationId", app.getId());
        conv.put("encodedApplicationId", idObfuscator.encode(app.getId()));
        conv.put("status", app.getStatus());
        String conversationId = "app_" + app.getId();
        conv.put("conversationId", conversationId);
        conv.put("unread", messageService.getUnreadCount(conversationId, currentUserId, currentRole));

        // 最后一条消息预览
        messageService.getLastMessage(conversationId).ifPresent(lastMsg -> {
            conv.put("lastMsgType", lastMsg.getMessageType());
            if ("text".equals(lastMsg.getMessageType())) {
                String preview = lastMsg.getContent();
                if (preview != null && preview.length() > 30) {
                    preview = preview.substring(0, 30) + "...";
                }
                conv.put("lastMsgText", preview);
            } else if ("file".equals(lastMsg.getMessageType())) {
                conv.put("lastMsgText", "[文件] " + (lastMsg.getFileName() != null ? lastMsg.getFileName() : ""));
            }
            conv.put("lastMsgTime", lastMsg.getCreatedAt() != null
                    ? lastMsg.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : 0L);
            conv.put("lastMsgTimeStr", lastMsg.getCreatedAt() != null
                    ? lastMsg.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    : "");
        });

        Job job = jobMap.get(app.getJobId());
        if (job != null) {
            conv.put("jobTitle", job.getTitle());
            Company company = companyMap.get(job.getCompanyId());
            conv.put("companyName", company != null ? company.getName() : "");
        } else {
            conv.put("jobTitle", "职位已删除");
            conv.put("companyName", "");
        }

        return conv;
    }

    /**
     * ============================================
     * 【聊天详情页 /chat/{applicationId}】
     * ============================================
     *
     * 进入聊天页面时：
     * 1. 加载历史消息（文字+文件）全部展示
     * 2. 标记所有发给当前用户的消息为已读
     * 3. 向消息发送者推送"已读"回执（实时通知对方）
     */
    @GetMapping("/chat/{encodedId}")
    public String chatPage(@PathVariable String encodedId,
                           HttpSession session, Model model) {
        Long applicationId = idObfuscator.decode(encodedId);
        if (applicationId == null) return "redirect:/chat";

        User user = (User) session.getAttribute("user");
        String activeRole = activeIdentityResolver.current(session).role();
        if (user == null) {
            return "redirect:/login";
        }

        Application app = applicationService.getApplicationById(applicationId);
        Job job = jobService.getJobById(app.getJobId());

        Long bossId = null;
        try {
            Company company = companyService.getCompanyById(job.getCompanyId());
            bossId = company.getBossId();
        } catch (Exception e) {
            log.warn("获取公司BossId失败, jobId={}", job.getId(), e);
        }

        // 验证用户身份：投递者 或 该职位的Boss
        boolean isSeeker = user.getId().equals(app.getUserId());
        boolean isBoss = bossId != null && user.getId().equals(bossId);
        if (!isSeeker && !isBoss) {
            return "redirect:/";
        }

        String conversationId = "app_" + applicationId;
        List<Message> messages = messageService.getConversationMessages(conversationId, user.getId(), activeRole);

        // 标记已读并获取发送者ID列表，向每个发送者推送已读回执
        Set<Long> senderIds = messageService.markAsReadAndGetSenders(conversationId, user.getId(), activeRole);
        for (Long senderId : senderIds) {
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("type", "read_receipt");
            receipt.put("conversationId", conversationId);
            receipt.put("readerId", user.getId());
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(), "/queue/read-receipts", receipt);
        }

        Long otherUserId = isSeeker ? bossId : app.getUserId();
        String otherUserName = "未知用户";
        if (otherUserId != null) {
            try {
                PublicUserProfile otherUser = userService.getPublicUserProfile(otherUserId);
                if (otherUser != null) {
                    otherUserName = otherUser.nickname() != null ? otherUser.nickname() : otherUser.username();
                }
            } catch (Exception e) {
                log.warn("获取对话用户信息失败, otherUserId={}", otherUserId, e);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("application", app);
        model.addAttribute("job", job);
        model.addAttribute("messages", messages);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("otherUserId", otherUserId);
        model.addAttribute("otherUserName", otherUserName);
        model.addAttribute("isBoss", isBoss);
        model.addAttribute("encodedApplicationId", encodedId);
        model.addAttribute("encodedJobId", idObfuscator.encode(job.getId()));

        return "chat";
    }

    /**
     * ============================================
     * 【文件上传 /chat/upload】
     * ============================================
     *
     * 支持 PDF 文件上传，验证文件魔数（%PDF 头部），
     * 存储到磁盘并通过 WebSocket 发送文件消息给对方。
     */
    @PostMapping("/chat/upload")
    @ResponseBody
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("conversationId") String conversationId,
                                                   HttpSession session) {
        User user = (User) session.getAttribute("user");
        log.warn("文件上传请求: user={}, fileName={}, size={}, convId={}",
                user != null ? user.getUsername() : "null",
                file.getOriginalFilename(), file.getSize(), conversationId);

        if (user == null) {
            return Result.error("请先登录");
        }

        // 文件大小校验（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        // 验证用户是会话参与者
        if (!isParticipant(conversationId, user.getId())) {
            return Result.error("无权访问该会话");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return Result.error("仅支持PDF文件");
        }

        // 校验真实文件内容是否为PDF（检查文件头魔数%PDF）
        try (var in = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = in.read(header);
            if (read < 4 || header[0] != 0x25 || header[1] != 0x50
                    || header[2] != 0x44 || header[3] != 0x46) {
                return Result.error("仅支持上传真实的PDF文件");
            }
        } catch (IOException e) {
            return Result.error("无法读取文件内容");
        }

        try {
            Path dir = Paths.get(chatDir).toAbsolutePath().normalize();
            log.warn("文件保存目录: {}", dir);
            Files.createDirectories(dir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String storedName = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            Path target = dir.resolve(storedName);
            file.transferTo(target.toFile());
            log.warn("文件保存成功: {}", target.toAbsolutePath());

            Map<String, Object> data = new HashMap<>();
            data.put("fileName", originalName);
            data.put("filePath", "/chat/" + storedName);
            data.put("fileSize", file.getSize());
            return Result.success(data);
        } catch (IOException e) {
            log.error("文件保存磁盘失败: {}", e.getMessage(), e);
            return Result.error("文件上传失败");
        }
    }

    /**
     * 定时清理过期聊天文件（每天凌晨3点执行，默认保留30天）
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredChatFiles() {
        try {
            Path dir = Paths.get(chatDir).toAbsolutePath().normalize();
            if (!Files.exists(dir)) return;

            java.time.Instant cutoff = java.time.Instant.now().minus(java.time.Duration.ofDays(chatRetentionDays));
            Files.list(dir).forEach(file -> {
                try {
                    if (Files.isRegularFile(file)
                            && Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        Files.delete(file);
                        log.info("清理过期聊天文件: {}", file.getFileName());
                    }
                } catch (IOException e) {
                    log.warn("清理文件失败: {}", file, e);
                }
            });
        } catch (IOException e) {
            log.warn("扫描聊天文件目录失败: {}", e.getMessage());
        }
    }

    /**
     * ============================================
     * 【文件下载 /chat/file/{storedName}】
     * ============================================
     *
     * 安全措施：
     * 1. 路径穿越防护：normalize + startsWith 校验
     * 2. 权限校验：只有消息的收发双方才能下载文件
     */
    @GetMapping("/chat/file/{storedName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String storedName,
                                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(302).header("Location", "/login").build();
        }

        Path chatDirPath = Paths.get(chatDir).toAbsolutePath().normalize();
        Path filePath = chatDirPath.resolve(storedName).normalize();
        if (!filePath.startsWith(chatDirPath)) {
            return ResponseEntity.status(403).build();
        }
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String fullPath = "/chat/" + storedName;
        var msgOpt = messageRepository.findByFilePath(fullPath);
        if (msgOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        var msg = msgOpt.get();
        if (!msg.getSenderId().equals(user.getId()) && !msg.getReceiverId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        FileSystemResource resource = new FileSystemResource(filePath);
        String encodedName = URLEncoder.encode(storedName.replaceFirst("^\\d+_", ""), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    /**
     * ============================================
     * 【WebSocket 消息处理 /app/chat】
     * ============================================
     *
     * 收到消息后：
     * 1. 持久化到数据库（文字+文件都存储）
     * 2. 推送给接收者（实时送达）
     *
     * 消息的 isRead 默认为 false（见 Message 实体），
     * 接收者打开聊天页或调用 mark-read API 时变为 true。
     */
    @MessageMapping("/chat")
    public void handleMessage(@Payload Map<String, Object> payload, java.security.Principal principal) {
        if (principal == null) {
            log.warn("WebSocket消息处理：principal为null，拒绝处理");
            return;
        }
        String conversationId = (String) payload.get("conversationId");
        Long senderId = Long.valueOf(principal.getName());
        String senderRole = principal instanceof ActivePrincipal active ? active.activeRole() : "LEGACY";
        try {
            sanctionService.assertAllowed(senderId, "MUTE");
        } catch (com.weib.exception.SanctionDeniedException e) {
            log.info("WebSocket message rejected by mute sanction, userId={}", senderId);
            return;
        }

        // 解析会话参与者（一次 DB 查询验证双方身份）
        Long[] participantIds = resolveParticipantIds(conversationId);
        if (participantIds == null) {
            return;
        }
        if (!isParticipant(participantIds, senderId, senderRole)) {
            return;
        }

        // 安全提取 receiverId，防止 NPE
        Object receiverIdObj = payload.get("receiverId");
        if (receiverIdObj == null) {
            return;
        }
        Long receiverId = Long.valueOf(receiverIdObj.toString());

        // 验证接收者是对方（不能发给自己）
        if (receiverId.equals(senderId)) {
            return;
        }
        if (!receiverId.equals(participantIds[0]) && !receiverId.equals(participantIds[1])) {
            return;
        }

        String content = sanitizeHtml((String) payload.get("content"));
        String messageType = (String) payload.getOrDefault("messageType", "text");
        // 修复：消息类型白名单校验，防止注入非法类型
        if (!List.of("text", "file").contains(messageType)) {
            log.warn("WebSocket消息：不支持的消息类型, messageType={}", messageType);
            return;
        }
        // 文本消息不允许空内容
        if ("text".equals(messageType) && (content == null || content.isBlank())) {
            return;
        }
        String fileName = (String) payload.getOrDefault("fileName", null);
        String filePath = (String) payload.getOrDefault("filePath", null);
        Object fileSizeObj = payload.get("fileSize");
        Long fileSize = fileSizeObj != null ? Long.valueOf(fileSizeObj.toString()) : null;

        String receiverRole = "SEEKER".equalsIgnoreCase(senderRole) ? "BOSS" : "SEEKER";
        Message saved = messageService.saveMessage(conversationId, senderId, senderRole, receiverId, receiverRole,
                content, messageType, fileName, filePath, fileSize, null);
        notificationEvents.create("chat:" + saved.getId(), receiverId, receiverRole, "CHAT_MESSAGE", saved.getId(),
                "收到一条新消息", "{\"conversationId\":\"" + conversationId + "\"}");

        // 实时推送给接收者（在线时即时送达，离线时下次上线通过历史记录查看）
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", saved);

        // 回显给发送者，使其无需刷新即可看到自己发的消息
        messagingTemplate.convertAndSendToUser(
                senderId.toString(), "/queue/messages", saved);
    }

    /**
     * ============================================
     * 【REST 消息发送 API】 POST /api/chat/send
     * ============================================
     *
     * 当 WebSocket 不可用时（发送者断连等），可通过此 REST 接口发送消息。
     * 消息持久化到数据库，接收者在线则实时推送，离线则下次上线查看。
     * 同时回显给发送者（在线时通过 WebSocket 推送）。
     *
     * 请求体 JSON：
     * {
     *   "conversationId": "app_123",
     *   "receiverId": 2,
     *   "content": "你好",
     *   "messageType": "text",
     *   "fileName": null,
     *   "filePath": null,
     *   "fileSize": null
     * }
     */
    @RateLimit(maxRequests = 30, windowSeconds = 60, key = "user")
    @PostMapping("/api/chat/send")
    @com.weib.security.Idempotent
    @ResponseBody
    public Result<Message> sendMessage(@RequestBody Map<String, Object> payload, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        try {
            sanctionService.assertAllowed(user.getId(), "MUTE");
        } catch (com.weib.exception.SanctionDeniedException e) {
            return Result.error("当前账号暂时不能发言");
        }

        String conversationId = (String) payload.get("conversationId");
        Long senderId = user.getId();
        String senderRole = activeIdentityResolver.current(session).role();

        Long[] participantIds = resolveParticipantIds(conversationId);
        if (participantIds == null) return Result.error("无效的会话");
        if (!isParticipant(participantIds, senderId, senderRole)) {
            return Result.error("无权在此会话发言");
        }

        Object receiverIdObj = payload.get("receiverId");
        if (receiverIdObj == null) return Result.error("缺少接收者ID");
        Long receiverId = Long.valueOf(receiverIdObj.toString());
        if (receiverId.equals(senderId)) return Result.error("不能给自己发消息");
        if (!receiverId.equals(participantIds[0]) && !receiverId.equals(participantIds[1])) {
            return Result.error("接收者不属于此会话");
        }

        String content = sanitizeHtml((String) payload.get("content"));
        String messageType = (String) payload.getOrDefault("messageType", "text");
        // 修复：消息类型白名单校验，防止注入非法类型
        if (!List.of("text", "file").contains(messageType)) {
            return Result.error("不支持的消息类型: " + messageType);
        }
        // 文本消息不允许空内容
        if ("text".equals(messageType) && (content == null || content.isBlank())) {
            return Result.error("消息内容不能为空");
        }
        String fileName = (String) payload.getOrDefault("fileName", null);
        String filePath = (String) payload.getOrDefault("filePath", null);
        Object fileSizeObj = payload.get("fileSize");
        Long fileSize = fileSizeObj != null ? Long.valueOf(fileSizeObj.toString()) : null;

        String clientMessageId = payload.get("clientMessageId") == null ? null : payload.get("clientMessageId").toString();
        String receiverRole = "SEEKER".equals(senderRole) ? "BOSS" : "SEEKER";
        Message saved = messageService.saveMessage(conversationId, senderId, senderRole, receiverId, receiverRole,
                content, messageType, fileName, filePath, fileSize, clientMessageId);
        notificationEvents.create("chat:" + saved.getId(), receiverId, receiverRole, "CHAT_MESSAGE", saved.getId(),
                "收到一条新消息", "{\"conversationId\":\"" + conversationId + "\"}");

        // 接收者在线则实时推送
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", saved);

        // 回显给发送者（在线时可见）
        messagingTemplate.convertAndSendToUser(
                senderId.toString(), "/queue/messages", saved);

        return Result.success(saved);
    }

    /**
     * ============================================
     * 【消息同步 API】 GET /api/chat/messages/{conversationId}
     * ============================================
     *
     * WebSocket 订阅建立后调用，补拉窗口期内可能错过的消息。
     * sinceId=0 返回全部，sinceId>0 只返回更新的消息。
     */
    @GetMapping("/api/chat/messages/{conversationId}")
    @ResponseBody
    public Result<List<Message>> syncMessages(@PathVariable String conversationId,
                                               @RequestParam(defaultValue = "0") Long sinceId,
                                               HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        String activeRole = activeIdentityResolver.current(session).role();
        if (!isParticipant(conversationId, user.getId(), activeRole)) return Result.error("无权访问该会话");

        List<Message> allMessages = messageService.getConversationMessages(conversationId, user.getId(), activeRole);
        if (sinceId <= 0) {
            return Result.success(allMessages);
        }
        List<Message> newer = allMessages.stream()
                .filter(m -> m.getId() > sinceId)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(newer);
    }

    /**
     * ============================================
     * 【在线状态查询 API】 GET /api/chat/online-status/{userId}
     * ============================================
     *
     * 返回指定用户是否在线（已连接 WebSocket）。
     * 响应示例：{"code":200, "data":{"userId":2, "online":true}}
     */
    @GetMapping("/api/chat/online-status/{userId}")
    @ResponseBody
    public Result<Map<String, Object>> onlineStatus(@PathVariable Long userId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("online", sessionManager.isOnline(userId));
        return Result.success(data);
    }

    /**
     * ============================================
     * 【标记已读 API】 POST /api/chat/mark-read
     * ============================================
     *
     * 当前端（接收者）正在聊天页面时，收到新消息后调用此接口标记已读。
     * 触发时机：
     * 1. 用户在聊天页且页面可见时，收到 WebSocket 新消息 → 自动调此接口
     * 2. 不限于页面加载时（页面加载时的标记已读在 chatPage() 中处理）
     *
     * 标记完成后，向消息发送者推送已读回执。
     *
     * 请求体 JSON：{"conversationId": "app_123"}
     */
    @PostMapping("/api/chat/mark-read")
    @ResponseBody
    public Result<?> apiMarkRead(@RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error("请先登录");
        String activeRole = activeIdentityResolver.current(session).role();

        String conversationId = body.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            return Result.error("缺少 conversationId");
        }

        // 验证用户是会话参与者
        if (!isParticipant(conversationId, user.getId(), activeRole)) {
            return Result.error("无权访问该会话");
        }

        Set<Long> senderIds = messageService.markAsReadAndGetSenders(conversationId, user.getId(), activeRole);
        for (Long senderId : senderIds) {
            Map<String, Object> receipt = new HashMap<>();
            receipt.put("type", "read_receipt");
            receipt.put("conversationId", conversationId);
            receipt.put("readerId", user.getId());
            messagingTemplate.convertAndSendToUser(
                    senderId.toString(), "/queue/read-receipts", receipt);
        }

        return Result.success();
    }

    /**
     * 验证用户是否是会话参与者
     * 会话ID格式: "app_{applicationId}"
     */
    private boolean isParticipant(String conversationId, Long userId) {
        Long[] ids = resolveParticipantIds(conversationId);
        return ids != null && (userId.equals(ids[0]) || userId.equals(ids[1]));
    }

    private boolean isParticipant(String conversationId, Long userId, String role) {
        return isParticipant(resolveParticipantIds(conversationId), userId, role);
    }

    private boolean isParticipant(Long[] ids, Long userId, String role) {
        if (ids == null || role == null) return false;
        return ("SEEKER".equalsIgnoreCase(role) && userId.equals(ids[0]))
                || ("BOSS".equalsIgnoreCase(role) && userId.equals(ids[1]));
    }

    private Map<Long, Job> batchLoadJobMap(List<Long> jobIds) {
        if (jobIds.isEmpty()) return Map.of();
        return jobService.getJobsByIds(jobIds).stream()
                .collect(java.util.stream.Collectors.toMap(Job::getId, j -> j));
    }

    private Map<Long, Company> batchLoadCompanyMap(Map<Long, Job> jobMap) {
        List<Long> companyIds = jobMap.values().stream().map(Job::getCompanyId).distinct()
                .collect(java.util.stream.Collectors.toList());
        return companyService.getCompanyMapByIds(companyIds);
    }

    /**
     * 解析会话的两个参与者ID: [seekerId, bossId]，会话无效返回 null
     */
    private Long[] resolveParticipantIds(String conversationId) {
        if (conversationId == null || !conversationId.startsWith("app_")) {
            return null;
        }
        try {
            Long appId = Long.parseLong(conversationId.substring(4));
            Application app = applicationService.getApplicationById(appId);
            Long seekerId = app.getUserId();
            Job job = jobService.getJobById(app.getJobId());
            Company company = companyService.getCompanyById(job.getCompanyId());
            return new Long[]{seekerId, company.getBossId()};
        } catch (Exception e) {
            log.warn("解析会话参与者ID失败, conversationId={}", conversationId, e);
            return null;
        }
    }
}
