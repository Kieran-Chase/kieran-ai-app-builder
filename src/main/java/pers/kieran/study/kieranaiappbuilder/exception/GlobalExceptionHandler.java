package pers.kieran.study.kieranaiappbuilder.exception;

import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pers.kieran.study.kieranaiappbuilder.common.ResultUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/23
 */
@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);

        if (isSseRequest()) {
            return buildSseErrorResponse(e.getCode(), e.getMessage());
        }

        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Object runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);

        if (isSseRequest()) {
            return buildSseErrorResponse(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误");
        }

        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    private boolean isSseRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return false;
        }

        HttpServletRequest request = attributes.getRequest();
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();

        return (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
                || uri.contains("/chat/gen/code");
    }

    private ResponseEntity<String> buildSseErrorResponse(int errorCode, String errorMessage) {
        Map<String, Object> errorData = Map.of(
                "error", true,
                "code", errorCode,
                "message", errorMessage
        );

        String errorJson = JSONUtil.toJsonStr(errorData);
        String sseData = "event: business-error\ndata: " + errorJson + "\n\n";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .body(sseData);
    }
}
