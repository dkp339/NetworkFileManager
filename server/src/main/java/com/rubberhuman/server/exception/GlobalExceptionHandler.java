package com.rubberhuman.server.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 拦截所有 Controller 抛出的异常，统一返回格式
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理参数校验异常 (如 IllegalArgumentException)
    // 返回 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildErrorMap(e.getMessage()));
    }

    // 处理业务逻辑错误 (我们自定义的 BusinessException)
    // 返回 400 Bad Request
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        // 返回 400 状态码，前端根据这个状态码弹窗提示错误信息
        return ResponseEntity.badRequest().body(buildErrorMap(e.getMessage()));
    }

    // 处理所有未知的 Exception (兜底)
    // 返回 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("系统未知异常", e); // 打印堆栈，方便你排查 bug
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorMap("系统繁忙，请稍后重试"));
    }

    // 辅助方法：构建统一的 JSON 返回格式 { "error": "xxx" }
    private Map<String, String> buildErrorMap(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }
}