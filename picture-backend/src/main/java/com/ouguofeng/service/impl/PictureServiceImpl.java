package com.ouguofeng.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouguofeng.api.aliyunai.AliYunAiApi;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.ouguofeng.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskRequest;
import com.ouguofeng.api.aliyunai.model.CreatePortraitStyleRedrawTaskResponse;
import com.ouguofeng.exception.BusinessException;
import com.ouguofeng.exception.ErrorCode;
import com.ouguofeng.exception.ThrowUtils;
import com.ouguofeng.manager.CosManager;
import com.ouguofeng.manager.upload.FilePictureUpload;
import com.ouguofeng.manager.upload.PictureUploadTemplate;
import com.ouguofeng.manager.upload.UrlPictureUpload;
import com.ouguofeng.mapper.PictureMapper;
import com.ouguofeng.model.dto.file.UploadPictureResult;
import com.ouguofeng.model.dto.picture.*;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.Space;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.enums.PictureReviewStatusEnum;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.model.vo.UserVO;
import com.ouguofeng.service.PictureService;
import com.ouguofeng.service.SpaceService;
import com.ouguofeng.service.UserService;
import com.ouguofeng.utils.ColorSimilarUtils;
import com.ouguofeng.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author 20571
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-08-20 15:11:31
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {


    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private com.ouguofeng.mapper.PictureLikeMapper pictureLikeMapper;

    @Resource
    private com.ouguofeng.mapper.PictureFavoriteMapper pictureFavoriteMapper;

    @Resource
    private com.ouguofeng.service.CacheService cacheService;

    // 批量上传专用线程池
    private final ExecutorService batchUploadExecutor = Executors.newFixedThreadPool(10);

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能为空");
        }
        //校验登录用户
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            //通过传入的空间id查询是否存在空间
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //还需判断登录用户是否是空间创建者
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        //判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //判断是否传入了id，有则是根据传入的id值更新图片
        //如果是更新，判断图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            //仅本人或者管理员可以编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            //校验空间是否一致
            //没传入spaceId，则复用原图片的spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                //表示有传入spaceId参数，判断是否跟原图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不一致");
                }
            }
        }
        //上传图片，得到图片信息
        //按照用户id划分目录还要判断是公共空间还是私有空间
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public_space/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("private_space/%s", spaceId);
        }
        //根据inoutSource判断类型是文件上传还是url上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造需要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 转换为标准颜色
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        //如果pictureId不为空，表示更新，否则是新增
        if (pictureId != null) {
            //如果是更新，需要补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId).setSql("totalSize = totalSize + " + picture.getPicSize()).setSql("totalCount = totalCount + 1").update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        
        // AI 自动识别图片标签和分类（仅在新增图片时才进行AI识别，异步执行不阻塞保存流程）
        if (pictureId == null && picture.getId() != null) {
            batchUploadExecutor.submit(() -> {
                asyncRecognizeImageTagsAndCategory(picture);
            });
        }
        
        return PictureVO.objToVo(picture);
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();//简介

        //修改数据时，id不能为空，有参数则检验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        //对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        //关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        //查询当前登录用户是否已点赞和收藏
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户，忽略
        }
        if (loginUser != null) {
            Long loginUserId = loginUser.getId();
            Long pictureId = picture.getId();
            //查询是否已点赞
            QueryWrapper<com.ouguofeng.model.entity.PictureLike> likeQueryWrapper = new QueryWrapper<>();
            likeQueryWrapper.eq("pictureId", pictureId);
            likeQueryWrapper.eq("userId", loginUserId);
            long likeCount = pictureLikeMapper.selectCount(likeQueryWrapper);
            pictureVO.setHasLiked(likeCount > 0);
            //查询是否已收藏
            QueryWrapper<com.ouguofeng.model.entity.PictureFavorite> favoriteQueryWrapper = new QueryWrapper<>();
            favoriteQueryWrapper.eq("pictureId", pictureId);
            favoriteQueryWrapper.eq("userId", loginUserId);
            long favoriteCount = pictureFavoriteMapper.selectCount(favoriteQueryWrapper);
            pictureVO.setHasFavorited(favoriteCount > 0);
        } else {
            pictureVO.setHasLiked(false);
            pictureVO.setHasFavorited(false);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        //对象列表转vo对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //1、关联查询用户信息（优化：使用toMap避免分组后再取第一个元素的开销）
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 优化：直接使用toMap，避免groupingBy后再get(0)的开销
        Map<Long, User> userIdUserMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (existing, replacement) -> existing));
        //2、填充信息（优化：批量查询UserVO，减少重复查询）
        // 先批量获取所有UserVO
        Map<Long, UserVO> userIdUserVOMap = userIdUserMap.values().stream()
                .collect(Collectors.toMap(User::getId, user -> userService.getUserVO(user)));
        // 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            UserVO userVO = userIdUserVOMap.get(userId);
            pictureVO.setUser(userVO);
        });
        //3、查询当前登录用户是否已点赞和收藏（批量查询优化）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户，忽略
        }
        if (loginUser != null) {
            Long loginUserId = loginUser.getId();
            Set<Long> pictureIdSet = pictureList.stream().map(Picture::getId).collect(Collectors.toSet());
            //批量查询当前用户对这些图片的点赞记录
            QueryWrapper<com.ouguofeng.model.entity.PictureLike> likeQueryWrapper = new QueryWrapper<>();
            likeQueryWrapper.eq("userId", loginUserId);
            likeQueryWrapper.in("pictureId", pictureIdSet);
            List<com.ouguofeng.model.entity.PictureLike> pictureLikeList = pictureLikeMapper.selectList(likeQueryWrapper);
            Set<Long> likedPictureIdSet = pictureLikeList.stream()
                    .map(com.ouguofeng.model.entity.PictureLike::getPictureId)
                    .collect(Collectors.toSet());
            //批量查询当前用户对这些图片的收藏记录
            QueryWrapper<com.ouguofeng.model.entity.PictureFavorite> favoriteQueryWrapper = new QueryWrapper<>();
            favoriteQueryWrapper.eq("userId", loginUserId);
            favoriteQueryWrapper.in("pictureId", pictureIdSet);
            List<com.ouguofeng.model.entity.PictureFavorite> pictureFavoriteList = pictureFavoriteMapper.selectList(favoriteQueryWrapper);
            Set<Long> favoritedPictureIdSet = pictureFavoriteList.stream()
                    .map(com.ouguofeng.model.entity.PictureFavorite::getPictureId)
                    .collect(Collectors.toSet());
            //填充是否已点赞和收藏
            pictureVOList.forEach(pictureVO -> {
                pictureVO.setHasLiked(likedPictureIdSet.contains(pictureVO.getId()));
                pictureVO.setHasFavorited(favoritedPictureIdSet.contains(pictureVO.getId()));
            });
        } else {
            //未登录用户，设置为false
            pictureVOList.forEach(pictureVO -> {
                pictureVO.setHasLiked(false);
                pictureVO.setHasFavorited(false);
            });
        }
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // and (name like "%xxx%" or introduction like "%xxx%")
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // >= startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // < endEditTime
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            /* and (tag like ("%\"Java\"%" and like "%\"Python\"%") */
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        //•必须有有效的id
        //•必须有有效地审核状态值
        //•且当前状态不能是"待审核"的状态
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //判断老图片跟请求更新的状态是不是同一状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复审核");
        }
        //更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        //后端填充审核信息
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        //操作数据库进行更新
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            //管理员自动审核
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动审核");
            picture.setReviewTime(new Date());
        } else {
            //非管理员，创建或者编辑都要改成待审核状态
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断改图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUserId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUserId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 只在图片属于个人私有空间时才更新额度
            if (oldPicture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        //在此处将实体类跟dto类进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        //注意将list转换成string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //设置编辑时间
        picture.setEditTime(new Date());
        //数据校验
        this.validPicture(picture);

        //判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验
        this.checkPictureAuth(loginUser, oldPicture);

        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        //操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        //1、校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        //3、查询该空间下所有图片，必须有主色调
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        //如果没有查询到图片直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {return Collections.emptyList();}

        //将目标颜色转换成color对象
        Color color = Color.decode(picColor);
        //4、计算相似度并排序
        // 4. 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 计算相似度
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(color, pictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());

        // 5. 返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        // 1. 获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询指定图片（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return;
        }
        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);
        // 5. 操作数据库进行批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量编辑失败");
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限，已经改为使用注解鉴权
        checkPictureAuth(loginUser, picture);
        // 创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }

    @Override
    public CreatePortraitStyleRedrawTaskResponse createPicturePortraitStyleRedrawTask(CreatePicturePortraitStyleRedrawTaskRequest createPicturePortraitStyleRedrawTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPicturePortraitStyleRedrawTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限
        checkPictureAuth(loginUser, picture);
        // 校验必选参数
        ThrowUtils.throwIf(createPicturePortraitStyleRedrawTaskRequest.getStyleIndex() == null, 
                ErrorCode.PARAMS_ERROR, "风格索引不能为空");
        // 当styleIndex=-1时，必须提供styleRefUrl
        if (createPicturePortraitStyleRedrawTaskRequest.getStyleIndex() == -1) {
            ThrowUtils.throwIf(StrUtil.isBlank(createPicturePortraitStyleRedrawTaskRequest.getStyleRefUrl()),
                    ErrorCode.PARAMS_ERROR, "使用参考图像风格时，风格参考图像URL不能为空");
        }
        // 创建风格重绘任务
        CreatePortraitStyleRedrawTaskRequest createPortraitStyleRedrawTaskRequest = new CreatePortraitStyleRedrawTaskRequest();
        CreatePortraitStyleRedrawTaskRequest.Input input = new CreatePortraitStyleRedrawTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        input.setStyleIndex(createPicturePortraitStyleRedrawTaskRequest.getStyleIndex());
        if (StrUtil.isNotBlank(createPicturePortraitStyleRedrawTaskRequest.getStyleRefUrl())) {
            input.setStyleRefUrl(createPicturePortraitStyleRedrawTaskRequest.getStyleRefUrl());
        }
        createPortraitStyleRedrawTaskRequest.setInput(input);
        // 创建任务
        return aliYunAiApi.createPortraitStyleRedrawTask(createPortraitStyleRedrawTaskRequest);
    }

    /**
     * 批量命名规则
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();//搜索关键词
        Integer count = pictureUploadByBatchRequest.getCount();//搜索条数
        //对搜索条数进行校验，最多不超过30条
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多上传30张图片");
        //名称前缀默认等于搜索关键词
        String namePrefixTemp = pictureUploadByBatchRequest.getNamePrefix();//图片名称前缀
        if (StrUtil.isBlank(namePrefixTemp)) {
            namePrefixTemp = searchText;//如果没有传递名称前缀，则默认等于搜索关键词
        }
        final String namePrefix = namePrefixTemp;
        //抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        //解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img[src^='http']");
        
        // 收集所有图片URL
        List<String> imageUrls = new ArrayList<>();
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前连接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            int indexOf = fileUrl.indexOf("?");
            if (indexOf > -1) {
                fileUrl = fileUrl.substring(0, indexOf);
            }
            imageUrls.add(fileUrl);
            if (imageUrls.size() >= count) {
                break;
            }
        }
        
        if (imageUrls.isEmpty()) {
            log.warn("未找到可用的图片URL");
            return 0;
        }
        
        // 并发上传图片（跳过AI识别）
        List<CompletableFuture<Picture>> futures = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(1);
        
        for (String fileUrl : imageUrls) {
            CompletableFuture<Picture> future = CompletableFuture.supplyAsync(() -> {
                try {
                    PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                    pictureUploadRequest.setFileUrl(fileUrl);
                    pictureUploadRequest.setPicName(namePrefix + index.getAndIncrement());
                    return uploadPictureWithoutAI(fileUrl, pictureUploadRequest, loginUser);
                } catch (Exception e) {
                    log.error("上传图片失败，URL: {}", fileUrl, e);
                    return null;
                }
            }, batchUploadExecutor);
            futures.add(future);
        }
        
        // 等待所有任务完成，收集成功上传的图片
        List<Picture> successPictures = new ArrayList<>();
        for (CompletableFuture<Picture> future : futures) {
            try {
                Picture picture = future.get(30, TimeUnit.SECONDS);
                if (picture != null) {
                    successPictures.add(picture);
                }
            } catch (Exception e) {
                log.error("获取上传结果失败", e);
            }
        }
        
        if (successPictures.isEmpty()) {
            log.warn("批量上传失败，没有成功上传的图片");
            return 0;
        }
        
        // 从成功上传的图片中获取spaceId（所有图片应该有相同的spaceId）
        Long spaceId = successPictures.get(0).getSpaceId();
        Long totalSize = successPictures.stream()
                .mapToLong(Picture::getPicSize)
                .sum();
        
        transactionTemplate.execute(status -> {
            // 批量保存图片
            boolean result = this.saveBatch(successPictures);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量保存图片失败");
            
            // 更新空间额度（只有私有空间才需要更新）
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize + " + totalSize)
                        .setSql("totalCount = totalCount + " + successPictures.size())
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        
        // 异步进行AI识别（使用线程池，不阻塞主流程）
        for (Picture picture : successPictures) {
            batchUploadExecutor.submit(() -> {
                asyncRecognizeImageTagsAndCategory(picture);
            });
        }
        
        log.info("批量上传完成，成功上传 {} 张图片，AI识别任务已提交到后台执行", successPictures.size());
        return successPictures.size();
    }
    
    /**
     * 上传图片但不进行AI识别（用于批量上传优化）
     */
    private Picture uploadPictureWithoutAI(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片不能为空");
        }
        //校验登录用户
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            if (!space.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验额度（批量上传时在最后统一更新，这里只做校验）
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        
        //上传图片，得到图片信息
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public_space/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("private_space/%s", spaceId);
        }
        
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        //构造需要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()));
        
        // 跳过AI识别，批量上传时异步处理
        
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        
        return picture;
    }
    
    /**
     * 异步识别图片标签和分类（内部方法，通过线程池调用）
     */
    private void asyncRecognizeImageTagsAndCategory(Picture picture) {
        try {
            log.info("开始异步AI识别图片标签和分类，图片ID: {}, URL: {}", picture.getId(), picture.getUrl());
            AliYunAiApi.ImageRecognitionResult recognitionResult = 
                    aliYunAiApi.recognizeImageTagsAndCategory(picture.getUrl());
            
            Picture updatePicture = new Picture();
            updatePicture.setId(picture.getId());
            boolean needUpdate = false;
            
            // 设置AI识别的标签
            if (recognitionResult.getTags() != null && !recognitionResult.getTags().isEmpty()) {
                updatePicture.setTags(JSONUtil.toJsonStr(recognitionResult.getTags()));
                needUpdate = true;
                log.info("AI识别标签成功，图片ID: {}, 标签: {}", picture.getId(), recognitionResult.getTags());
            }
            
            // 设置AI识别的分类
            if (StrUtil.isNotBlank(recognitionResult.getCategory())) {
                updatePicture.setCategory(recognitionResult.getCategory());
                needUpdate = true;
                log.info("AI识别分类成功，图片ID: {}, 分类: {}", picture.getId(), recognitionResult.getCategory());
            }
            
            // 更新数据库
            if (needUpdate) {
                this.updateById(updatePicture);
                log.info("异步AI识别完成，已更新图片ID: {}", picture.getId());
                
                // AI识别完成后清除缓存，确保前端能获取到最新的标签信息
                cacheService.clearPictureCache();
            }
        } catch (Exception e) {
            log.error("异步AI识别图片标签和分类失败，图片ID: {}", picture.getId(), e);
        }
    }

    /**
     * 应用关闭时清理线程池
     */
    @PreDestroy
    public void shutdownBatchUploadExecutor() {
        batchUploadExecutor.shutdown();
        try {
            if (!batchUploadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchUploadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchUploadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}





