package com.weib.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ============================================
 * 全局异常处理器
 * ============================================
 *
 * 【@RestControllerAdvice】
 * = @ControllerAdvice + @ResponseBody
 * 
 * 全局处理 Controller 抛出的异常，返回 JSON 格式错误信息
 *
 * 【@ExceptionHandler】
 * 指定处理的异常类型
 * 
 * 常见用法：
 * - @ExceptionHandler(Exception.class)        - 处理所有异常
 * - @ExceptionHandler(RuntimeException.class) - 处理运行时异常
 * - @ExceptionHandler(MyException.class)      - 处理自定义异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("服务器错误");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    /**
     * 处理 IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("参数错误");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
