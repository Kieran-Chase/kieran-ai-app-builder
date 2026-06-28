package pers.kieran.study.kieranaiappbuilder.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pers.kieran.study.kieranaiappbuilder.common.PageRequest;

import java.io.Serializable;

/**
 * @author Kieran_Chase
 * @project kieran-ai-app-builder
 * @date 2026/6/28
 */

/**
 * 用户查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
