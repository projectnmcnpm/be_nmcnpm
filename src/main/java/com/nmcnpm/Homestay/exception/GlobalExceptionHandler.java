package com.nmcnpm.Homestay.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Lỗi nghiệp vụ (AppException)
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(
            AppException ex, HttpServletRequest request) {
        return buildError(
                ex.getErrorCode().getHttpStatus().value(),
                ex.getErrorCode().getHttpStatus().getReasonPhrase(),
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // Lỗi @Valid validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        return buildError(400, "Bad Request", "VALIDATION_ERROR",
                "Validation failed", request.getRequestURI(), details);
    }

    // 403 từ @PreAuthorize
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildError(403, "Forbidden", "FORBIDDEN",
                "You don't have permission", request.getRequestURI(), null);
    }

    // Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {
        return buildError(500, "Internal Server Error", "INTERNAL_ERROR",
                ex.getMessage(), request.getRequestURI(), null);
    }

    private ResponseEntity<Map<String, Object>> buildError(
            int status, String error, String code,
            String message, String path, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("code", code);
        body.put("message", message);
        body.put("path", path);
        if (details != null) body.put("details", details);
        return ResponseEntity.status(status).body(body);
    }
}
