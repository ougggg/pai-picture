package com.ouguofeng.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改密码请求
 */
@Data
public class UserEditPasswordRequest implements Serializable {

	/**
	 * 原密码
	 */
	private String oldPassword;
	/**
	 * 新密码
	 */
	private String newPassword;
	/**
	 * 确认密码
	 */
	private String confirmPassword;

	private static final long serialVersionUID = 1L;
}
