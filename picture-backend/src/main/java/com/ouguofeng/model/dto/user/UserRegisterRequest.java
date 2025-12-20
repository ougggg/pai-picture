package com.ouguofeng.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = -6208026469796142904L;

//    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 用户手机号码
     */
    private String userPhone;

    /**
     * 用户邮箱
     */
    private String userEmail;

}
