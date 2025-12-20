package com.ouguofeng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.mapper.PictureLikeMapper;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.PictureLike;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureLikeService;
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
 * @description 针对表【picture_like(图片点赞)】的数据库操作Service实现
 * @createDate 2025-11-04
 */
@Service
@Slf4j
public class PictureLikeServiceImpl extends ServiceImpl<PictureLikeMapper, PictureLike>
        implements PictureLikeService {

    @Resource
    private PictureService pictureService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likePicture(long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 判断是否已经点赞
        long userId = loginUser.getId();
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureId);
        queryWrapper.eq("userId", userId);
        PictureLike existLike = this.getOne(queryWrapper);
        if (existLike != null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "已经点赞过了");
        }

        // 创建点赞记录
        PictureLike pictureLike = new PictureLike();
        pictureLike.setPictureId(pictureId);
        pictureLike.setUserId(userId);
        boolean saveResult = this.save(pictureLike);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "点赞失败");
        }

        // 更新图片点赞数
        picture.setLikeCount(picture.getLikeCount() == null ? 1 : picture.getLikeCount() + 1);
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新点赞数失败");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikePicture(long pictureId, User loginUser) {
        // 判断图片是否存在
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 判断是否已经点赞
        long userId = loginUser.getId();
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pictureId", pictureId);
        queryWrapper.eq("userId", userId);
        PictureLike existLike = this.getOne(queryWrapper);
        if (existLike == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "还未点赞");
        }

        // 删除点赞记录
        boolean removeResult = this.removeById(existLike.getId());
        if (!removeResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "取消点赞失败");
        }

        // 更新图片点赞数
        int newLikeCount = picture.getLikeCount() - 1;
        if (newLikeCount < 0) {
            newLikeCount = 0;
        }
        picture.setLikeCount(newLikeCount);
        boolean updateResult = pictureService.updateById(picture);
        if (!updateResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新点赞数失败");
        }

        return true;
    }

    @Override
    public Page<PictureVO> listLikedPictureByPage(PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                                  HttpServletRequest request) {
        long current = pictureInteractionQueryRequest.getCurrent();
        long size = pictureInteractionQueryRequest.getPageSize();
        Long userId = pictureInteractionQueryRequest.getUserId();

        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        // 查询用户点赞的图片id列表
        QueryWrapper<PictureLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<PictureLike> pictureLikePage = this.page(new Page<>(current, size), queryWrapper);

        List<PictureLike> pictureLikeList = pictureLikePage.getRecords();
        if (pictureLikeList.isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 获取图片id列表
        List<Long> pictureIdList = pictureLikeList.stream()
                .map(PictureLike::getPictureId)
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
        Page<PictureVO> pictureVOPage = new Page<>(current, size, pictureLikePage.getTotal());
        pictureVOPage.setRecords(pictureVOList);

        return pictureVOPage;
    }
}

