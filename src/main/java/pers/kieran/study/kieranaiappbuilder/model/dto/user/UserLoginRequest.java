package pers.kieran.study.kieranaiappbuilder.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/26
 */

/**
 * 用户登录
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
