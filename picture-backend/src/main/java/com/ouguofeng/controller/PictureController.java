package com.ouguofeng.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.annotation.AuthCheck;
import com.ouguofeng.api.aliyunai.AliYunAiApi;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskResponse;
import com.ouguofeng.api.aliyunai.model.GetPortraitStyleRedrawTaskResponse;
import com.ouguofeng.api.imagesearch.ImageSearchApiFacade;
import com.ouguofeng.api.imagesearch.model.ImageSearchResult;
import com.ouguofeng.common.BaseResponse;
import com.ouguofeng.common.DeleteRequest;
import com.ouguofeng.common.ResultUtils;
import com.ouguofeng.constant.UserConstant;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.model.dto.picture.*;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.Space;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.enums.PictureReviewStatusEnum;
import com.ouguofeng.model.vo.PictureTagCategory;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceService spaceService;

    @Resource
    private CacheService cacheService;

    /**
     * 清除图片列表缓存
     * 在图片增删改操作后调用，确保缓存数据的一致性
     * 使用统一的缓存服务
     */
    private void clearPictureCache() {
        cacheService.clearPictureCache();
    }

    /**
     * 清除单个图片缓存
     */
    private void clearPictureCache(Long pictureId) {
        String cacheKey = String.format("picture:vo:%d", pictureId);
        cacheService.delete(cacheKey);
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        // 清除缓存
        clearPictureCache();
        if (pictureVO != null && pictureVO.getId() != null) {
            clearPictureCache(pictureVO.getId());
        }
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取并创建图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        // 清除缓存
        clearPictureCache();
        return ResultUtils.success(uploadCount);
    }

    /**
     * 通过url上传图片
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        // 清除缓存
        clearPictureCache();
        if (pictureVO != null && pictureVO.getId() != null) {
            clearPictureCache(pictureVO.getId());
        }
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long pictureId = deleteRequest.getId();

        pictureService.deletePicture(pictureId, loginUser);
        // 清除缓存
        clearPictureCache();
        clearPictureCache(pictureId);

        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     *
     * @param pictureUpdateRequest 修改的图片信息
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
            HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 清除缓存
        clearPictureCache();
        clearPictureCache(id);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
            HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        // 清除缓存
        clearPictureCache();
        clearPictureCache(pictureEditRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // //限制爬虫
        // ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 普通用户默认智能查看已过审的数据
        if (spaceId == null) {
            // 公共图库
            // 普通用户只能看到审核通过的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类，有缓存）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 100, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();

        // 私有空间不使用缓存，直接查询（因为涉及权限校验）
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
            // 私有空间直接查询数据库，不使用缓存
            Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                    pictureService.getQueryWrapper(pictureQueryRequest));
            return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
        }

        // 公共图库使用缓存
        // 普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        pictureQueryRequest.setNullSpaceId(true);

        // 查询缓存，缓存中没有，再查询数据库
        // 构建缓存的 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("listPictureVOByPage:%s", hashKey);
        // 1. 先从本地缓存中查询
        String cachedValue = cacheService.getLocalCache(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            @SuppressWarnings("unchecked")
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2. 本地缓存未命中，查询 Redis 分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，更新本地缓存，返回结果
            cacheService.putLocalCache(cacheKey, cachedValue);
            @SuppressWarnings("unchecked")
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 3. 查询数据库（使用分布式锁防止缓存击穿）
        String lockKey = "query:" + cacheKey;
        boolean locked = false;
        try {
            // 尝试获取锁，超时时间5秒
            locked = cacheService.tryLock(lockKey, 5);
            if (locked) {
                // 获取到锁，再次检查缓存（双重检查，防止其他线程已经写入缓存）
                cachedValue = opsForValue.get(cacheKey);
                if (cachedValue != null) {
                    cacheService.putLocalCache(cacheKey, cachedValue);
                    @SuppressWarnings("unchecked")
                    Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
                    return ResultUtils.success(cachedPage);
                }

                // 查询数据库
                Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                        pictureService.getQueryWrapper(pictureQueryRequest));
                Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

                // 4. 更新缓存
                String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
                // 设置缓存的过期时间，5 - 10 分钟过期，防止缓存雪崩
                int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
                opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
                // 写入本地缓存
                cacheService.putLocalCache(cacheKey, cacheValue);
                return ResultUtils.success(pictureVOPage);
            } else {
                // 未获取到锁，说明其他线程正在查询数据库，等待一小段时间后重试从缓存获取
                Thread.sleep(50); // 等待50ms
                cachedValue = opsForValue.get(cacheKey);
                if (cachedValue != null) {
                    cacheService.putLocalCache(cacheKey, cachedValue);
                    @SuppressWarnings("unchecked")
                    Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
                    return ResultUtils.success(cachedPage);
                }
                // 如果还是没有，直接查询数据库（降级策略）
                Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                        pictureService.getQueryWrapper(pictureQueryRequest));
                Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
                return ResultUtils.success(pictureVOPage);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 中断异常，直接查询数据库
            Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                    pictureService.getQueryWrapper(pictureQueryRequest));
            Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
            return ResultUtils.success(pictureVOPage);
        } finally {
            if (locked) {
                cacheService.releaseLock(lockKey);
            }
        }
    }

    /**
     * 根据id获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片(封装类)
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 构建缓存键
        String cacheKey = String.format("picture:vo:%d", id);

        // 1. 先从缓存中查询
        String cachedValue = cacheService.get(cacheKey);
        if (cachedValue != null) {
            @SuppressWarnings("unchecked")
            PictureVO cachedVO = JSONUtil.toBean(cachedValue, PictureVO.class);
            // 空间权限校验（即使有缓存也需要校验）
            Long spaceId = cachedVO.getSpaceId();
            if (spaceId != null) {
                User loginUser = userService.getLoginUser(request);
                Picture picture = pictureService.getById(id);
                if (picture != null) {
                    pictureService.checkPictureAuth(loginUser, picture);
                }
            }
            return ResultUtils.success(cachedVO);
        }

        // 2. 缓存未命中，查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            User loginUser = userService.getLoginUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }
        // 获取封装类
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);

        // 3. 写入缓存（5分钟过期）
        String cacheValue = JSONUtil.toJsonStr(pictureVO);
        cacheService.set(cacheKey, cacheValue, 300);

        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片审核
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        // 清除缓存（审核状态改变会影响公共图库显示）
        clearPictureCache();
        if (pictureReviewRequest.getId() != null) {
            clearPictureCache(pictureReviewRequest.getId());
        }
        return ResultUtils.success(true);
    }

    /**
     * 预定义的图片标签列表（精简版）
     * 用于前端展示和AI识别时的参考
     */
    public static final List<String> PICTURE_TAGS = Arrays.asList(
            // 风格类
            "高清", "创意", "艺术", "简约", "卡通",
            // 场景类
            "风景", "自然", "城市", "建筑", "夜景",
            // 人物类
            "人物", "美女", "儿童", "情侣",
            // 生活类
            "生活", "美食", "旅游", "运动", "宠物",
            // 情感类
            "搞笑", "温馨", "浪漫", "治愈",
            // 主题类
            "科技", "动物", "植物", "节日",
            // 用途类
            "壁纸", "海报", "头像", "背景");

    /**
     * 预定义的图片分类列表
     * 用于前端展示和AI识别时的选择
     */
    public static final List<String> PICTURE_CATEGORIES = Arrays.asList(
            "模板", "电商", "表情包", "素材", "海报", "壁纸",
            "图标", "插画", "背景图");

    /**
     * 获取图片标签和分类
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        // 构建缓存键（静态数据，使用固定键）
        String cacheKey = "picture:tag_category";

        // 1. 先从缓存中查询
        String cachedValue = cacheService.get(cacheKey);
        if (cachedValue != null) {
            @SuppressWarnings("unchecked")
            PictureTagCategory cachedCategory = JSONUtil.toBean(cachedValue, PictureTagCategory.class);
            return ResultUtils.success(cachedCategory);
        }

        // 2. 缓存未命中，构建数据
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(PICTURE_TAGS);
        pictureTagCategory.setCategoryList(PICTURE_CATEGORIES);

        // 3. 写入缓存（24小时过期，静态数据）
        String cacheValue = JSONUtil.toJsonStr(pictureTagCategory);
        cacheService.set(cacheKey, cacheValue, 86400);

        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        // 通过图片id进行以图搜图
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 以色搜图
     */
    @PostMapping("/search//color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(
            @RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        // 通过图片颜色进行以色搜图
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        // 清除缓存
        clearPictureCache();
        // 清除批量编辑的图片缓存
        if (pictureEditByBatchRequest.getPictureIdList() != null) {
            for (Long pictureId : pictureEditByBatchRequest.getPictureIdList()) {
                clearPictureCache(pictureId);
            }
        }
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService
                .createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * 创建人物风格重绘任务
     */
    @PostMapping("/portrait_style_redraw/create_task")
    public BaseResponse<CreatePortraitStyleRedrawTaskResponse> createPicturePortraitStyleRedrawTask(
            @RequestBody CreatePicturePortraitStyleRedrawTaskRequest createPicturePortraitStyleRedrawTaskRequest,
            HttpServletRequest request) {
        if (createPicturePortraitStyleRedrawTaskRequest == null
                || createPicturePortraitStyleRedrawTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreatePortraitStyleRedrawTaskResponse response = pictureService
                .createPicturePortraitStyleRedrawTask(createPicturePortraitStyleRedrawTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询人物风格重绘任务
     */
    @GetMapping("/portrait_style_redraw/get_task")
    public BaseResponse<GetPortraitStyleRedrawTaskResponse> getPicturePortraitStyleRedrawTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetPortraitStyleRedrawTaskResponse task = aliYunAiApi.getPortraitStyleRedrawTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * AI 生成图片
     */
    @PostMapping("/ai_picture")
    public BaseResponse<String> AiCreatePicture(String text, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        ThrowUtils.throwIf(text == null, ErrorCode.PARAMS_ERROR, "提示词为空");
        String pictureUrl;
        try {
            // 调用生图方法
            pictureUrl = aliYunAiApi.AiCreatePicture(text);

        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            // 捕获并打印所有可能的异常（避免程序崩溃，便于排查问题）
            log.error("AI生成图片失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI生成图片失败");
        }
        return ResultUtils.success(pictureUrl);

    }

    /**
     * 保存图片到私人空间
     */
    @PostMapping("/save/private")
    public BaseResponse<Boolean> savePictureToPrivate(
            @RequestBody PictureSaveToPrivateRequest pictureSaveToPrivateRequest,
            HttpServletRequest request) {
        if (pictureSaveToPrivateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.savePictureToPrivate(pictureSaveToPrivateRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 发布图片到公共空间
     */
    @PostMapping("/connect/public")
    public BaseResponse<Boolean> publishPictureToPublic(
            @RequestBody PicturePublishToPublicRequest picturePublishToPublicRequest,
            HttpServletRequest request) {
        if (picturePublishToPublicRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.publishPictureToPublic(picturePublishToPublicRequest, loginUser);
        return ResultUtils.success(true);
    }

}
