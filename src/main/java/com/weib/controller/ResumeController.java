package com.weib.controller;

import com.weib.entity.Resume;
import com.weib.entity.User;
import com.weib.service.SanctionService;
import com.weib.service.ResumeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================
 * 【Controller】简历控制器 - 求职者简历管理
 * ============================================
 * 
 * 职责：
 * - 查看简历
 * - 编辑/完善简历
 * - 简历预览
 * 
 * ----------------------------------------
 * 【简历的重要性】
 * ----------------------------------------
 * 
 * 简历是求职的核心：
 * - 投递职位必须有简历
 * - Boss 根据简历决定是否录用
 * - 简历质量直接影响求职成功率
 * 
 * ----------------------------------------
 * 【简历信息组成】
 * ----------------------------------------
 * 
 * 1. 基本信息
 *    - 真实姓名、手机、邮箱
 *    - 性别、年龄、学历
 * 
 * 2. 教育背景
 *    - 学校、专业、学历
 *    - 在校时间
 * 
 * 3. 工作经历
 *    - 公司、职位、时间
 *    - 工作内容
 * 
 * 4. 项目经验
 *    - 项目名称、角色
 *    - 项目描述、成果
 * 
 * 5. 技能特长
 *    - 专业技能
 *    - 语言能力
 *    - 证书资质
 * 
 * 6. 自我介绍
 *    - 自我评价
 *    - 职业规划
 */
@Controller
@RequiredArgsConstructor
public class ResumeController {

    /**
     * 【依赖注入】ResumeService
     * 
     * ResumeService 负责：
     * - 创建/更新简历
     * - 获取简历信息
     * - 检查简历状态
     */
    private final ResumeService resumeService;
    private final SanctionService sanctionService;

    /**
     * ========================================
     * 【我的简历】查看/编辑简历
     * ========================================
     * 
     * 显示当前用户的简历
     * 如果没有简历，显示创建页面
     * 如果有简历，显示编辑页面
     */
    @GetMapping("/resume")
    public String myResume(HttpSession session, Model model) {
        
        // 获取当前用户
        User user = (User) session.getAttribute("user");
        
        // 未登录跳转登录页
        if (user == null) {
            return "redirect:/login";
        }
        
        // 非求职者不能访问
        if (!"seeker".equals(user.getRole())) {
            return "redirect:/";
        }
        
        model.addAttribute("user", user);
        
        // 查询简历
        try {
            Resume resume = resumeService.getResumeByUserId(user.getId());
            model.addAttribute("resume", resume);
            model.addAttribute("isNew", false);  // 不是新建
        } catch (Exception e) {
            // 没有简历，返回空简历对象
            Resume emptyResume = new Resume();
            emptyResume.setUserId(user.getId());
            model.addAttribute("resume", emptyResume);
            model.addAttribute("isNew", true);   // 是新建
        }
        
        return "resume-edit";
    }

    /**
     * ========================================
     * 【保存简历】POST 请求
     * ========================================
     * 
     * 保存简历信息（新建或更新）
     * 
     * 【POST vs PUT】
     * - POST：创建新资源
     * - PUT：更新已有资源
     * 
     * 这里用 POST 是因为：
     * - 用户可能没有简历（创建）
     * - 也可能有简历（更新）
     * - 一个方法处理两种情况
     */
    @PostMapping("/resume/save")
    public String saveResume(@RequestParam(required = false) Long id,
                             @RequestParam String realName,
                             @RequestParam(required = false) String gender,
                             @RequestParam String phone,
                             @RequestParam String email,
                             @RequestParam(required = false) String birthday,
                             @RequestParam(required = false) String education,
                             @RequestParam(required = false) String school,
                             @RequestParam(required = false) String major,
                             @RequestParam(required = false) String workExperience,
                             @RequestParam(required = false) String projectExperience,
                             @RequestParam(required = false) String skills,
                             @RequestParam(required = false) String selfIntroduction,
                             HttpSession session,
                             Model model) {
        
        User user = (User) session.getAttribute("user");

        // 未登录
        if (user == null) {
            return "redirect:/login";
        }

        // 非求职者不能创建/编辑简历
        if (!"seeker".equals(user.getRole())) {
            return "redirect:/";
        }

        // 构建简历对象
        Resume resume = new Resume();
        
        // 如果有ID，说明是更新
        if (id != null) {
            try {
                resume = resumeService.getResumeById(id);
                // 【水平越权防护】简历必须是当前用户的，防止篡改ID窃取他人简历
                if (!resume.getUserId().equals(user.getId())) {
                    model.addAttribute("error", "无权修改他人简历");
                    model.addAttribute("user", user);
                    // 不将他人简历放入 model，防止数据泄露
                    Resume safeResume = new Resume();
                    safeResume.setUserId(user.getId());
                    model.addAttribute("resume", safeResume);
                    model.addAttribute("isNew", true);
                    return "resume-edit";
                }
            } catch (Exception e) {
                // 简历不存在，当新建处理：清空 id 防止 JPA 尝试 update 不存在的记录
                resume = new Resume();
                resume.setUserId(user.getId());
            }
        }
        
        // 设置用户ID（必须）
        resume.setUserId(user.getId());
        
        // 必填字段：始终设置
        resume.setRealName(realName);
        resume.setPhone(phone);
        resume.setEmail(email);

        // 可选字段：仅在用户填写时更新，防止空值覆盖已有数据
        if (gender != null && !gender.isBlank()) resume.setGender(gender);
        if (birthday != null && !birthday.isBlank()) resume.setBirthday(birthday);
        if (education != null && !education.isBlank()) resume.setEducation(education);
        if (school != null && !school.isBlank()) resume.setSchool(school);
        if (major != null && !major.isBlank()) resume.setMajor(major);
        if (workExperience != null && !workExperience.isBlank()) resume.setWorkExperience(workExperience);
        if (projectExperience != null && !projectExperience.isBlank()) resume.setProjectExperience(projectExperience);
        if (skills != null && !skills.isBlank()) resume.setSkills(skills);
        if (selfIntroduction != null && !selfIntroduction.isBlank()) resume.setSelfIntroduction(selfIntroduction);
        
        // 保存简历
        try {
            sanctionService.assertAllowed(user.getId(), "PUBLISH_BAN");
            resumeService.saveResume(resume);
            model.addAttribute("success", "简历保存成功！");
        } catch (Exception e) {
            model.addAttribute("error", "保存失败：" + e.getMessage());
        }
        
        model.addAttribute("user", user);
        model.addAttribute("resume", resume);
        model.addAttribute("isNew", false);
        
        return "resume-edit";
    }

    /**
     * ========================================
     * 【预览简历】
     * ========================================
     * 
     * 预览完整简历效果
     */
    @GetMapping("/resume/preview")
    public String previewResume(HttpSession session, Model model) {
        
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        
        try {
            Resume resume = resumeService.getResumeByUserId(user.getId());
            model.addAttribute("resume", resume);
        } catch (Exception e) {
            model.addAttribute("error", "请先完善简历");
            return "redirect:/resume";
        }
        
        return "resume-preview";
    }
}
