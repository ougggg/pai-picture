package com.ouguofeng.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户编辑请求
 */
@Data
public class UserEditRequest implements Serializable {

	/**
	 * 用户ID
	 */
	private Long id;

	/**
	 * 用户手机号
	 */
	private String userPhone;

	/**
	 * 用户昵称
	 */
	private String userName;

	/**
	 * 用户头像
	 */
	private String userAvatar;

	/**
	 * 用户简介
	 */
	private String userProfile;

    /**
     * 用户邮箱
     */
    private String userEmail;

    private static final long serialVersionUID = 1L;
}
