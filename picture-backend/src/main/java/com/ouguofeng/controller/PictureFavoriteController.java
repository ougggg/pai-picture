package com.ouguofeng.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.common.BaseResponse;
import com.ouguofeng.common.ResultUtils;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.dto.picture.PictureInteractionRequest;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureFavoriteService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 图片收藏接口
 */
@Slf4j
@RestController
@RequestMapping("/picture/favorite")
public class PictureFavoriteController {

    @Resource
    private UserService userService;

    @Resource
    private PictureFavoriteService pictureFavoriteService;

    /**
     * 收藏图片
     */
    @PostMapping("/do")
    public BaseResponse<Boolean> favoritePicture(@RequestBody PictureInteractionRequest pictureInteractionRequest,
                                                HttpServletRequest request) {
        if (pictureInteractionRequest == null || pictureInteractionRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long pictureId = pictureInteractionRequest.getPictureId();
        boolean result = pictureFavoriteService.favoritePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 取消收藏
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> unfavoritePicture(@RequestBody PictureInteractionRequest pictureInteractionRequest,
                                                  HttpServletRequest request) {
        if (pictureInteractionRequest == null || pictureInteractionRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long pictureId = pictureInteractionRequest.getPictureId();
        boolean result = pictureFavoriteService.unfavoritePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户收藏的图片列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PictureVO>> listFavoritedPictureByPage(@RequestBody PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                                                    HttpServletRequest request) {
        if (pictureInteractionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果没有传userId，则查询当前登录用户的收藏列表
        if (pictureInteractionQueryRequest.getUserId() == null) {
            User loginUser = userService.getLoginUser(request);
            pictureInteractionQueryRequest.setUserId(loginUser.getId());
        }
        Page<PictureVO> pictureVOPage = pictureFavoriteService.listFavoritedPictureByPage(pictureInteractionQueryRequest, request);
        return ResultUtils.success(pictureVOPage);
    }
}

