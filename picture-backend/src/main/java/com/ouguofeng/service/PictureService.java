package com.ouguofeng.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskResponse;
import com.ouguofeng.model.dto.picture.*;
import com.ouguofeng.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 20571
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-08-20 15:11:31
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *文件输入源
     * @param pictureUploadRequest 图片上传请求类
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 通过批量上传创建图片请求类
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                 User loginUser);

    /**
     * 校验图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片分页封装类
     *
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询对象，转换对象信息
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 设置审核状态参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建ai扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    /**
     * 创建人物风格重绘任务
     *
     * @param createPicturePortraitStyleRedrawTaskRequest
     * @param loginUser
     */
    CreatePortraitStyleRedrawTaskResponse createPicturePortraitStyleRedrawTask(CreatePicturePortraitStyleRedrawTaskRequest createPicturePortraitStyleRedrawTaskRequest, User loginUser);

}
