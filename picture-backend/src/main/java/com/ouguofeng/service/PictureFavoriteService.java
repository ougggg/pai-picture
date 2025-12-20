package com.ouguofeng.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.PictureFavorite;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 20571
 * @description 针对表【picture_favorite(图片收藏)】的数据库操作Service
 * @createDate 2025-11-04
 */
public interface PictureFavoriteService extends IService<PictureFavorite> {

    /**
     * 收藏图片
     *
     * @param pictureId 图片id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean favoritePicture(long pictureId, User loginUser);

    /**
     * 取消收藏
     *
     * @param pictureId 图片id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    boolean unfavoritePicture(long pictureId, User loginUser);

    /**
     * 获取用户收藏的图片列表
     *
     * @param pictureInteractionQueryRequest 查询请求
     * @param request
     * @return 图片VO分页
     */
    Page<PictureVO> listFavoritedPictureByPage(PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                               HttpServletRequest request);
}

