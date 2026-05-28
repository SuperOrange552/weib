# src/main/java/com/weib/exception/GlobalExceptionHandler.java

## 职责
全局异常处理(@RestControllerAdvice)：统一将 Controller 异常转为 ProblemDetail JSON 响应。

## 导出
- `GlobalExceptionHandler` — @RestControllerAdvice

## 异常映射
| 异常类型 | HTTP状态 | 响应title |
|----------|----------|-----------|
| Exception.class | 500 INTERNAL_SERVER_ERROR | "服务器错误" |
| IllegalArgumentException.class | 400 BAD_REQUEST | "参数错误" |

## 依赖
### 外部依赖
- `org.springframework.http.ProblemDetail` (Spring 6+ RFC 7807)
- `org.springframework.web.bind.annotation.ExceptionHandler`
- `org.springframework.web.bind.annotation.RestControllerAdvice`

## 风险标记
- (无)
