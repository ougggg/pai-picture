package com.ouguofeng.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.annotation.AuthCheck;
import com.ouguofeng.common.BaseResponse;
import com.ouguofeng.common.DeleteRequest;
import com.ouguofeng.common.ResultUtils;
import com.ouguofeng.constant.UserConstant;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.model.dto.user.*;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.LoginUserVO;
import com.ouguofeng.model.vo.UserVO;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求体
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        //判断参数是否合法
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        //判断参数是否合法
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        //获取用户注册信息
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 用户注销(退出)
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        //设置默认密码
        final String DEFAULT_PASSWORD = "12345678a";
        //密码加密
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取包装类（脱敏用户信息）
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> baseResponse = getUserById(id);
        User data = baseResponse.getData();
        return ResultUtils.success(userService.getUserVO(data));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户（管理员）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(userService.updateById(user));
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        // 拼接查询条件
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 修改密码
     */
    @PostMapping("/update/password")
    public BaseResponse<Boolean> updateUserPassword(@RequestBody UserEditPasswordRequest userEditPasswordRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userEditPasswordRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateUserPassword(userEditPasswordRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取个人中心信息
     */
    @GetMapping("/profile")
    public BaseResponse<UserVO> getUserProfile(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        // 直接复用现有的getUserVO方法
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 更新个人信息
     */
    @PostMapping("/profile/update")
    public BaseResponse<Boolean> updateUserProfile(@RequestBody UserEditRequest userEditRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        String userPhone = userEditRequest.getUserPhone();
        // 手机号格式校验
        if (StrUtil.isNotBlank(userPhone)) {
            if (!userPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "手机号格式错误（应为11位中国大陆手机号）");
            }
        }
        String userEmail = userEditRequest.getUserEmail();
        //邮箱格式校验
        if (StrUtil.isNotBlank(userEmail)) {
            if (!userEmail.matches("^[\\w.-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "邮箱格式错误");
            }
        }
        String userProfile = userEditRequest.getUserProfile();
        if (StrUtil.isNotEmpty(userProfile) && userProfile.length() > 500) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户简介过长");
        }
        // 设置当前用户ID，确保只能修改自己的信息
        userEditRequest.setId(loginUser.getId());
        //操作数据库
        boolean result = userService.updateUserProfile(userEditRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 上传头像（临时上传，不更新用户信息）
     */
    @PostMapping("/avatar/upload")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "头像文件不能为空");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 只上传文件到COS，返回URL，不更新数据库
        String avatarUrl = userService.uploadAvatarToCos(file, loginUser);
        return ResultUtils.success(avatarUrl);
    }


}
