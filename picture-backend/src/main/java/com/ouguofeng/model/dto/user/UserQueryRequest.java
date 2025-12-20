package com.ouguofeng.model.dto.user;

import com.ouguofeng.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

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

    /**
     * 用户手机号码
     */
    private String userPhone;

    /**
     * 用户邮箱
     */
    private String userEmail;


    private static final long serialVersionUID = 1L;
}