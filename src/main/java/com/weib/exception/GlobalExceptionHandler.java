package com.weib.exception;

import com.weib.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

/**
 * 全局异常处理器
 *
 * 设计：
 *   API 请求 → 直接写 JSON 到 HttpServletResponse（不走视图解析）
 *   浏览器   → 返回 ModelAndView 或视图名，由 Thymeleaf 渲染
 *
 * @ExceptionHandler 不支持 produces 属性，靠 isApiRequest() 内部判断分发
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------- 静态资源 404 ← 永远 JSON ---------- */

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public Result handleNoResourceFound() {
        return Result.error(404, "资源不存在");
    }

    /* ---------- 通用异常 ---------- */

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.error("未处理异常, URI={}", request.getRequestURI(), ex);
        return dispatch(ex, request, response, 500, "服务器内部错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex,
                                         HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return dispatch(ex, request, response, 400, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                      HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.warn("权限不足, URI={}", request.getRequestURI());
        return dispatch(ex, request, response, 403, "权限不足");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthFail(AuthenticationException ex,
                                  HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.warn("认证失败, URI={}", request.getRequestURI());
        if (isApiRequest(request)) {
            writeJson(response, 401, "未登录或Token已过期");
            return null;
        }
        return "redirect:/login";
    }

    /* ---------- 核心分发方法 ---------- */

    /**
     * API 请求 → 写 JSON 到 response → 返回 null
     * 浏览器  → 返回 ModelAndView → Thymeleaf 渲染 error/500.html
     */
    private Object dispatch(Exception ex, HttpServletRequest request,
                            HttpServletResponse response, int status, String msg)
            throws IOException {
        if (isApiRequest(request)) {
            writeJson(response, status, msg);
            return null; // null + 已写 response = Spring 不再处理
        }
        return new ModelAndView("error/500");
    }

    private void writeJson(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":" + status + ",\"msg\":\"" + msg + "\",\"data\":null}"
        );
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
            || uri.startsWith("/api/");
    }
}
