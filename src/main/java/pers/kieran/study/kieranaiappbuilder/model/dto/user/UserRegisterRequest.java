package pers.kieran.study.kieranaiappbuilder.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/26
 */

/**
 * 用户注册请求
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
