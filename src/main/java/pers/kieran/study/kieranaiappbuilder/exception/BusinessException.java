package pers.kieran.study.kieranaiappbuilder.exception;

import lombok.Getter;
/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/21
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
