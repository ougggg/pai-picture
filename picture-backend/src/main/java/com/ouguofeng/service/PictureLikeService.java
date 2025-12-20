package com.ouguofeng.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.PictureLike;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 20571
 * @description 针对表【picture_like(图片点赞)】的数据库操作Service
 * @createDate 2025-11-04
 */
public interface PictureLikeService extends IService<PictureLike> {

    /**
     * 点赞图片
     *
     * @param pictureId 图片id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean likePicture(long pictureId, User loginUser);

    /**
     * 取消点赞
     *
     * @param pictureId 图片id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean unlikePicture(long pictureId, User loginUser);

    /**
     * 获取用户点赞的图片列表
     *
     * @param pictureInteractionQueryRequest 查询请求
     * @param request
     * @return 图片VO分页
     */
    Page<PictureVO> listLikedPictureByPage(PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                           HttpServletRequest request);
}

