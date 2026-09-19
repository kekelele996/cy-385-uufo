package com.babytracker.exception;

import com.babytracker.constants.AllergyErrorCode;
import com.babytracker.constants.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case AllergyErrorCode.FEEDING_NOT_FOUND,
                 AllergyErrorCode.REACTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        log.warn("业务异常[{}]：{}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(status).body(body(ex.getCode(), ex.getMessage(), status.value()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return body(AllergyErrorCode.VALIDATION_FAILED, message, HttpStatus.BAD_REQUEST.value());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handle(Exception ex) {
        log.error("服务器内部错误", ex);
        return body(ErrorCode.INTERNAL_ERROR, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private Map<String, Object> body(String code, String message, int status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", false);
        map.put("code", code);
        map.put("message", message == null ? "" : message);
        map.put("status", status);
        return map;
    }
}
