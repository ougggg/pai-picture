package com.ouguofeng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.mapper.UserFollowMapper;
import com.ouguofeng.model.dto.user.UserFollowQueryRequest;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.entity.UserFollow;
import com.ouguofeng.model.vo.UserVO;
import com.ouguofeng.service.UserFollowService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 20571
 * @description 针对表【user_follow(用户关注)】的数据库操作Service实现
 * @createDate 2025-01-XX
 */
@Service
@Slf4j
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
        implements UserFollowService {

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean followUser(long followeeId, User loginUser) {
        // 判断被关注者是否存在
        User followee = userService.getById(followeeId);
        ThrowUtils.throwIf(followee == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 不能关注自己
        long followerId = loginUser.getId();
        ThrowUtils.throwIf(followerId == followeeId, ErrorCode.OPERATION_ERROR, "不能关注自己");

        // 判断是否已经关注
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", followerId);
        queryWrapper.eq("followeeId", followeeId);
        UserFollow existFollow = this.getOne(queryWrapper);
        if (existFollow != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经关注过了");
        }

        // 创建关注记录
        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(followerId);
        userFollow.setFolloweeId(followeeId);
        boolean saveResult = this.save(userFollow);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "关注失败");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfollowUser(long followeeId, User loginUser) {
        // 判断被关注者是否存在
        User followee = userService.getById(followeeId);
        ThrowUtils.throwIf(followee == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        // 判断是否已经关注
        long followerId = loginUser.getId();
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", followerId);
        queryWrapper.eq("followeeId", followeeId);
        UserFollow existFollow = this.getOne(queryWrapper);
        if (existFollow == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "还未关注");
        }

        // 删除关注记录
        boolean removeResult = this.removeById(existFollow.getId());
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消关注失败");
        }

        return true;
    }

    @Override
    public Page<UserVO> listFollowingByPage(UserFollowQueryRequest userFollowQueryRequest,
                                            HttpServletRequest request) {
        long current = userFollowQueryRequest.getCurrent();
        long size = userFollowQueryRequest.getPageSize();
        Long userId = userFollowQueryRequest.getUserId();

        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        // 查询用户关注的用户id列表
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<UserFollow> userFollowPage = this.page(new Page<>(current, size), queryWrapper);

        List<UserFollow> userFollowList = userFollowPage.getRecords();
        if (userFollowList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 获取被关注者id列表
        List<Long> followeeIdList = userFollowList.stream()
                .map(UserFollow::getFolloweeId)
                .collect(Collectors.toList());

        // 查询用户信息
        List<User> userList = userService.listByIds(followeeIdList);
        if (userList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 转换为UserVO
        List<UserVO> userVOList = userService.getUserVOList(userList);

        // 构造分页结果
        Page<UserVO> userVOPage = new Page<>(current, size, userFollowPage.getTotal());
        userVOPage.setRecords(userVOList);

        return userVOPage;
    }

    @Override
    public Page<UserVO> listFollowersByPage(UserFollowQueryRequest userFollowQueryRequest,
                                           HttpServletRequest request) {
        long current = userFollowQueryRequest.getCurrent();
        long size = userFollowQueryRequest.getPageSize();
        Long userId = userFollowQueryRequest.getUserId();

        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        // 查询关注该用户的用户id列表
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followeeId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<UserFollow> userFollowPage = this.page(new Page<>(current, size), queryWrapper);

        List<UserFollow> userFollowList = userFollowPage.getRecords();
        if (userFollowList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 获取关注者id列表
        List<Long> followerIdList = userFollowList.stream()
                .map(UserFollow::getFollowerId)
                .collect(Collectors.toList());

        // 查询用户信息
        List<User> userList = userService.listByIds(followerIdList);
        if (userList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 转换为UserVO
        List<UserVO> userVOList = userService.getUserVOList(userList);

        // 构造分页结果
        Page<UserVO> userVOPage = new Page<>(current, size, userFollowPage.getTotal());
        userVOPage.setRecords(userVOList);

        return userVOPage;
    }

    @Override
    public boolean isFollowing(long followerId, long followeeId) {
        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("followerId", followerId);
        queryWrapper.eq("followeeId", followeeId);
        return this.count(queryWrapper) > 0;
    }
}






