package com.ouguofeng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.mapper.PictureFavoriteMapper;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.PictureFavorite;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureFavoriteService;
import com.ouguofeng.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 20571
 * @description 针对表【picture_favorite(图片收藏)】的数据库操作Service实现
 * @createDate 2025-11-04
 */
@Service
@Slf4j
public class PictureFavoriteServiceImpl extends ServiceImpl<PictureFavoriteMapper, PictureFavorite>
        implements PictureFavoriteService {

    @Resource
    private PictureService pictureService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean favoritePicture(long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 判断是否已经收藏
        long userId = loginUser.getId();
        QueryWrapper<PictureFavorite> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureId);
        queryWrapper.eq("userId", userId);
        PictureFavorite existFavorite = this.getOne(queryWrapper);
        if (existFavorite != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经收藏过了");
        }

        // 创建收藏记录
        PictureFavorite pictureFavorite = new PictureFavorite();
        pictureFavorite.setPictureId(pictureId);
        pictureFavorite.setUserId(userId);
        boolean saveResult = this.save(pictureFavorite);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "收藏失败");
        }

        // 更新图片收藏数
        picture.setFavoriteCount(picture.getFavoriteCount() == null ? 1 : picture.getFavoriteCount() + 1);
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新收藏数失败");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unfavoritePicture(long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 判断是否已经收藏
        long userId = loginUser.getId();
        QueryWrapper<PictureFavorite> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureId);
        queryWrapper.eq("userId", userId);
        PictureFavorite existFavorite = this.getOne(queryWrapper);
        if (existFavorite == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "还未收藏");
        }

        // 删除收藏记录
        boolean removeResult = this.removeById(existFavorite.getId());
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消收藏失败");
        }

        // 更新图片收藏数
        int newFavoriteCount = picture.getFavoriteCount() - 1;
        if (newFavoriteCount < 0) {
            newFavoriteCount = 0;
        }
        picture.setFavoriteCount(newFavoriteCount);
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新收藏数失败");
        }

        return true;
    }

    @Override
    public Page<PictureVO> listFavoritedPictureByPage(PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                                      HttpServletRequest request) {
        long current = pictureInteractionQueryRequest.getCurrent();
        long size = pictureInteractionQueryRequest.getPageSize();
        Long userId = pictureInteractionQueryRequest.getUserId();

        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        // 查询用户收藏的图片id列表
        QueryWrapper<PictureFavorite> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<PictureFavorite> pictureFavoritePage = this.page(new Page<>(current, size), queryWrapper);

        List<PictureFavorite> pictureFavoriteList = pictureFavoritePage.getRecords();
        if (pictureFavoriteList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 获取图片id列表
        List<Long> pictureIdList = pictureFavoriteList.stream()
                .map(PictureFavorite::getPictureId)
                .collect(Collectors.toList());

        // 查询图片信息
        List<Picture> pictureList = pictureService.listByIds(pictureIdList);
        if (pictureList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 转换为PictureVO
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> pictureService.getPictureVO(picture, request))
                .collect(Collectors.toList());

        // 构造分页结果
        Page<PictureVO> pictureVOPage = new Page<>(current, size, pictureFavoritePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }
}

