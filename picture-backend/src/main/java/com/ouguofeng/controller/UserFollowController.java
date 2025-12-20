package com.ouguofeng.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.common.BaseResponse;
import com.ouguofeng.common.ResultUtils;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.model.dto.user.UserFollowQueryRequest;
import com.ouguofeng.model.dto.user.UserFollowRequest;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.UserVO;
import com.ouguofeng.service.UserFollowService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 用户关注接口
 */
@Slf4j
@RestController
@RequestMapping("/user/follow")
public class UserFollowController {

    @Resource
    private UserService userService;

    @Resource
    private UserFollowService userFollowService;

    /**
     * 关注用户
     */
    @PostMapping("/do")
    public BaseResponse<Boolean> followUser(@RequestBody UserFollowRequest userFollowRequest,
                                            HttpServletRequest request) {
        if (userFollowRequest == null || userFollowRequest.getFolloweeId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long followeeId = userFollowRequest.getFolloweeId();
        boolean result = userFollowService.followUser(followeeId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 取消关注
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> unfollowUser(@RequestBody UserFollowRequest userFollowRequest,
                                             HttpServletRequest request) {
        if (userFollowRequest == null || userFollowRequest.getFolloweeId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long followeeId = userFollowRequest.getFolloweeId();
        boolean result = userFollowService.unfollowUser(followeeId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户关注的用户列表
     */
    @PostMapping("/following/list/page")
    public BaseResponse<Page<UserVO>> listFollowingByPage(@RequestBody UserFollowQueryRequest userFollowQueryRequest,
                                                          HttpServletRequest request) {
        if (userFollowQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果没有传userId，则查询当前登录用户的关注列表
        if (userFollowQueryRequest.getUserId() == null) {
            User loginUser = userService.getLoginUser(request);
            userFollowQueryRequest.setUserId(loginUser.getId());
        }
        Page<UserVO> userVOPage = userFollowService.listFollowingByPage(userFollowQueryRequest, request);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 获取用户的粉丝列表
     */
    @PostMapping("/followers/list/page")
    public BaseResponse<Page<UserVO>> listFollowersByPage(@RequestBody UserFollowQueryRequest userFollowQueryRequest,
                                                         HttpServletRequest request) {
        if (userFollowQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果没有传userId，则查询当前登录用户的粉丝列表
        if (userFollowQueryRequest.getUserId() == null) {
            User loginUser = userService.getLoginUser(request);
            userFollowQueryRequest.setUserId(loginUser.getId());
        }
        Page<UserVO> userVOPage = userFollowService.listFollowersByPage(userFollowQueryRequest, request);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 判断是否关注了某个用户
     */
    @GetMapping("/isFollowing")
    public BaseResponse<Boolean> isFollowing(@RequestParam Long followeeId,
                                           HttpServletRequest request) {
        if (followeeId == null || followeeId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = userFollowService.isFollowing(loginUser.getId(), followeeId);
        return ResultUtils.success(result);
    }
}


