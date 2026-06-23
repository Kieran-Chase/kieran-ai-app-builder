package pers.kieran.study.kieranaiappbuilder.common;

import lombok.Data;
import pers.kieran.study.kieranaiappbuilder.exception.ErrorCode;

import java.io.Serializable;
/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/23
 */

/**
 * 通用响应类
 * @param <T>
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
