package com.ouguofeng.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouguofeng.config.CosClientConfig;
import com.ouguofeng.constant.UserConstant;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.manager.CosManager;
import com.ouguofeng.manager.upload.FilePictureUpload;
import com.ouguofeng.manager.upload.PictureUploadTemplate;
import com.ouguofeng.model.dto.file.UploadPictureResult;
import com.ouguofeng.model.dto.user.UserEditPasswordRequest;
import com.ouguofeng.model.dto.user.UserEditRequest;
import com.ouguofeng.model.dto.user.UserQueryRequest;
import com.ouguofeng.model.dto.user.UserRegisterRequest;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.enums.UserRoleEnum;
import com.ouguofeng.model.vo.LoginUserVO;
import com.ouguofeng.model.vo.UserVO;
import com.ouguofeng.service.UserService;
import com.ouguofeng.mapper.UserMapper;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author 20571
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-08-19 11:31:50
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    @Resource
    private FilePictureUpload filePictureUpload;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {

        //获取参数
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userPhone = userRegisterRequest.getUserPhone();
        String userEmail = userRegisterRequest.getUserEmail();
        String userName = userRegisterRequest.getUserName();

        //1,检验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword, userPhone, userEmail, userName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        if (!userPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "密码需包含字母和数字，且长度至少8位");
        }
        // 手机号格式校验
        if (StrUtil.isNotBlank(userPhone)) {
            if (!userPhone.matches("^1[3-9]\\d{9}$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "手机号格式错误（应为11位中国大陆手机号）");
            }
        }
        //邮箱格式校验
        if (StrUtil.isNotBlank(userEmail)) {
            if (!userEmail.matches("^[\\w.-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "邮箱格式错误");
            }
        }


        //2,检查是否重复
        // 2.1 创建查询条件包装器
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 2.2 设置查询条件：userAccount字段等于参数值
        queryWrapper.eq("userAccount", userAccount);//前面是说要查询的数据库字段，后面是参数值
        // 2.3 执行查询并返回匹配的记录数
        long count = this.baseMapper.selectCount(queryWrapper);
        // 2.4 判断是否存在重复账号
        if (count > 0) {
            // 2.5 如果存在重复，抛出业务异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }

        //3,加密
        String encryptPassword = getEncryptPassword(userPassword);

        //4,插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userName);
        user.setUserRole(UserRoleEnum.USER.getValue());//设置默认角色为普通用户
        user.setUserPhone(userPhone);
        user.setUserEmail(userEmail);
        user.setUserAvatar("https://youke1.picui.cn/s1/2025/08/31/68b3c102ad49e.jpg");
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败,数据库异常");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "paidaxing";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1、校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        //2、对用户的传递密码进行加密
        String encryptPassword = getEncryptPassword(userPassword);

        //3、查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        //不存在抛异常
        if (user == null) {
            log.info("用户不存在");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        //4、保存用户的登录状态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);//将查询到的用户对象脱敏封装返回
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断是否已经登录
        Object object = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) object;
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //再次查询，确保数据一致性
        Long currentUserId = user.getId();
        user = this.getById(currentUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        //判断是否已经登录
        Object object = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (object == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        //移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String userPhone = userQueryRequest.getUserPhone();
        String userEmail = userQueryRequest.getUserEmail();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        // 新增 userPhone 和 userEmail 查询条件
        queryWrapper.like(StrUtil.isNotBlank(userPhone), "userPhone", userPhone);
        queryWrapper.like(StrUtil.isNotBlank(userEmail), "userEmail", userEmail);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public boolean updateUserPassword(UserEditPasswordRequest userEditPasswordRequest, HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = this.getLoginUser(request);
        // 2. 参数校验
        if (StrUtil.hasBlank(userEditPasswordRequest.getOldPassword(), userEditPasswordRequest.getNewPassword(), userEditPasswordRequest.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }
        if (!userEditPasswordRequest.getNewPassword().equals(userEditPasswordRequest.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }
        // 增加密码复杂度校验
        if (!userEditPasswordRequest.getNewPassword().matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码需包含字母和数字，且不小与8位");
        }
        // 3. 验证旧密码
        //旧密码加密值
        String oldEncrypt = getEncryptPassword(userEditPasswordRequest.getOldPassword());
        if (!oldEncrypt.equals(loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }
        // 4. 更新密码
        String newEncrypt = getEncryptPassword(userEditPasswordRequest.getNewPassword());
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserPassword(newEncrypt);

        return this.updateById(updateUser);
    }

    @Override
    public boolean updateUserProfile(UserEditRequest userEditRequest, User loginUser) {
        if (!userEditRequest.getId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 创建User对象并复制属性
        User user = new User();
        BeanUtils.copyProperties(userEditRequest, user);

        // 调用现有的updateById方法
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return true;
    }

    @Override
    public String uploadAvatarToCos(MultipartFile file, User loginUser) {
        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能为空");
        }
        //校验登录用户
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        String uploadPathPrefix = String.format("userAvatar/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(file, uploadPathPrefix);
        return uploadPictureResult.getUrl();
    }

}




