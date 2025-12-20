package com.ouguofeng.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户权限更新请求（管理员）
 */
@Data
public class UserUpdateRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户角色：user/admin
     */
    private String userRole;


    private static final long serialVersionUID = 1L;
}