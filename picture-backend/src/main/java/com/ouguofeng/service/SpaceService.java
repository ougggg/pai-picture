package com.ouguofeng.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.model.dto.space.SpaceAddRequest;
import com.ouguofeng.model.dto.space.SpaceQueryRequest;
import com.ouguofeng.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 20571
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-08-24 10:14:53
*/
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     *
     * @param space
     * @param add   是否为创建时检验
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间包装类（单条）
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 将Space分页对象转换为SpaceVO分页对象
     *
     * @param spacePage Space实体的分页对象
     * @param request HTTP请求对象
     * @return SpaceVO的分页对象，包含用户信息等关联数据
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
