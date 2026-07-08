package com.weib.config;
import com.weib.dto.Result;import org.springframework.http.converter.HttpMessageNotReadableException;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(basePackages="com.weib.controller.admin")
public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) public Result<Void> validation(MethodArgumentNotValidException e){String m=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+"格式不正确").orElse("请求参数不合法");return Result.error(400,m);}
 @ExceptionHandler(HttpMessageNotReadableException.class) public Result<Void> malformed(){return Result.error(400,"请求格式不正确");}
 @ExceptionHandler(IllegalArgumentException.class) public Result<Void> illegal(IllegalArgumentException e){return Result.error(400,e.getMessage());}
}