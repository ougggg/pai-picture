package com.ouguofeng.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.dto.user.UserFollowQueryRequest;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.entity.UserFollow;
import com.ouguofeng.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 20571
 * @description 针对表【user_follow(用户关注)】的数据库操作Service
 * @createDate 2025-01-XX
 */
public interface UserFollowService extends IService<UserFollow> {

    /**
     * 关注用户
     *
     * @param followeeId 被关注者id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean followUser(long followeeId, User loginUser);

    /**
     * 取消关注
     *
     * @param followeeId 被关注者id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean unfollowUser(long followeeId, User loginUser);

    /**
     * 获取用户关注的用户列表
     *
     * @param userFollowQueryRequest 查询请求
     * @param request
     * @return 用户VO分页
     */
    Page<UserVO> listFollowingByPage(UserFollowQueryRequest userFollowQueryRequest,
                                     HttpServletRequest request);

    /**
     * 获取用户的粉丝列表
     *
     * @param userFollowQueryRequest 查询请求
     * @param request
     * @return 用户VO分页
     */
    Page<UserVO> listFollowersByPage(UserFollowQueryRequest userFollowQueryRequest,
                                    HttpServletRequest request);

    /**
     * 判断是否关注了某个用户
     *
     * @param followerId 关注者id
     * @param followeeId 被关注者id
     * @return 是否关注
     */
    boolean isFollowing(long followerId, long followeeId);
}






