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
import com.ouguofeng.service.PictureLikeService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 图片点赞接口
 */
@Slf4j
@RestController
@RequestMapping("/picture/like")
public class PictureLikeController {

    @Resource
    private UserService userService;

    @Resource
    private PictureLikeService pictureLikeService;

    /**
     * 点赞图片
     */
    @PostMapping("/do")
    public BaseResponse<Boolean> likePicture(@RequestBody PictureInteractionRequest pictureInteractionRequest,
                                            HttpServletRequest request) {
        if (pictureInteractionRequest == null || pictureInteractionRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long pictureId = pictureInteractionRequest.getPictureId();
        boolean result = pictureLikeService.likePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 取消点赞
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> unlikePicture(@RequestBody PictureInteractionRequest pictureInteractionRequest,
                                              HttpServletRequest request) {
        if (pictureInteractionRequest == null || pictureInteractionRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long pictureId = pictureInteractionRequest.getPictureId();
        boolean result = pictureLikeService.unlikePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户点赞的图片列表
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<PictureVO>> listLikedPictureByPage(@RequestBody PictureInteractionQueryRequest pictureInteractionQueryRequest,
                                                                HttpServletRequest request) {
        if (pictureInteractionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果没有传userId，则查询当前登录用户的点赞列表
        if (pictureInteractionQueryRequest.getUserId() == null) {
            User loginUser = userService.getLoginUser(request);
            pictureInteractionQueryRequest.setUserId(loginUser.getId());
        }
        Page<PictureVO> pictureVOPage = pictureLikeService.listLikedPictureByPage(pictureInteractionQueryRequest, request);
        return ResultUtils.success(pictureVOPage);
    }
}

