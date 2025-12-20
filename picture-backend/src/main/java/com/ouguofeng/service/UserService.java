package com.ouguofeng.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ouguofeng.model.dto.user.UserEditPasswordRequest;
import com.ouguofeng.model.dto.user.UserEditRequest;
import com.ouguofeng.model.dto.user.UserQueryRequest;
import com.ouguofeng.model.dto.user.UserRegisterRequest;
import com.ouguofeng.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.vo.LoginUserVO;
import com.ouguofeng.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 20571
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-08-19 11:31:50
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 获取加密密码
     *
     * @param userPassword 密码
     * @return 加密密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 将查询到的用户对象脱敏封装返回
     *
     * @param user 查询到的用户对象
     * @return 脱敏后的用户对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return 脱敏后的用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销（退出）
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户对象
     *
     * @param user 查询
     * @return 脱敏后的用户对象
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 用户查询条件
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 修改密码
     *
     * @param userEditPasswordRequest
     * @param request
     * @return
     */
    boolean updateUserPassword(UserEditPasswordRequest userEditPasswordRequest, HttpServletRequest request);

    /**
     * 用户更新个人信息
     *
     * @param userEditRequest
     * @param loginUser
     * @return
     */
    boolean updateUserProfile(UserEditRequest userEditRequest, User loginUser);


    /**
     * 上传头像到COS（不更新数据库）
     *
     * @param file      头像文件
     * @param loginUser 当前登录用户
     * @return 头像URL
     */
    String uploadAvatarToCos(MultipartFile file, User loginUser);


}
