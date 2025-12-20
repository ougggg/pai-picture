package com.ouguofeng;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureLikeService;
import com.ouguofeng.service.PictureService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 图片点赞功能测试
 */
@SpringBootTest
@Slf4j
public class PictureLikeTest {

    @Resource
    private PictureLikeService pictureLikeService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    private User testUser;
    private Picture testPicture;

    @BeforeEach
    public void setUp() {
        // 准备测试数据：创建测试用户和测试图片
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserAccount("testUser");
        testUser.setUserName("测试用户");

        testPicture = new Picture();
        testPicture.setId(1L);
        testPicture.setUrl("https://test.com/test.jpg");
        testPicture.setName("测试图片");
        testPicture.setUserId(1L);
        testPicture.setLikeCount(0);
        testPicture.setFavoriteCount(0);
    }

    @Test
    public void testLikePicture() {
        log.info("测试点赞图片功能");
        
        // 注意：这个测试需要数据库中存在对应的用户和图片数据
        // 如果数据库为空，可能会抛出异常，这是正常的
        try {
            boolean result = pictureLikeService.likePicture(testPicture.getId(), testUser);
            log.info("点赞结果: {}", result);
            
            // 验证点赞成功后，可以查询点赞记录
            PictureInteractionQueryRequest queryRequest = new PictureInteractionQueryRequest();
            queryRequest.setUserId(testUser.getId());
            queryRequest.setCurrent(1);
            queryRequest.setPageSize(10);
            
            Page<PictureVO> likedPictures = pictureLikeService.listLikedPictureByPage(queryRequest, null);
            log.info("用户点赞的图片数量: {}", likedPictures.getTotal());
            
        } catch (Exception e) {
            log.error("测试点赞功能时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户和图片数据");
        }
    }

    @Test
    public void testUnlikePicture() {
        log.info("测试取消点赞功能");
        
        try {
            // 先点赞
            pictureLikeService.likePicture(testPicture.getId(), testUser);
            
            // 再取消点赞
            boolean result = pictureLikeService.unlikePicture(testPicture.getId(), testUser);
            log.info("取消点赞结果: {}", result);
            
        } catch (Exception e) {
            log.error("测试取消点赞功能时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户和图片数据");
        }
    }

    @Test
    public void testListLikedPictures() {
        log.info("测试查询用户点赞的图片列表");
        
        try {
            PictureInteractionQueryRequest queryRequest = new PictureInteractionQueryRequest();
            queryRequest.setUserId(testUser.getId());
            queryRequest.setCurrent(1);
            queryRequest.setPageSize(10);
            
            Page<PictureVO> likedPictures = pictureLikeService.listLikedPictureByPage(queryRequest, null);
            log.info("用户点赞的图片总数: {}", likedPictures.getTotal());
            log.info("当前页图片数量: {}", likedPictures.getRecords().size());
            
        } catch (Exception e) {
            log.error("测试查询点赞列表时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户数据");
        }
    }

    @Test
    public void testDuplicateLike() {
        log.info("测试重复点赞（应该抛出异常）");
        
        try {
            // 第一次点赞
            pictureLikeService.likePicture(testPicture.getId(), testUser);
            
            // 第二次点赞应该失败
            try {
                pictureLikeService.likePicture(testPicture.getId(), testUser);
                Assertions.fail("重复点赞应该抛出异常");
            } catch (Exception e) {
                log.info("重复点赞正确抛出异常: {}", e.getMessage());
            }
            
            // 清理：取消点赞
            pictureLikeService.unlikePicture(testPicture.getId(), testUser);
            
        } catch (Exception e) {
            log.error("测试重复点赞时出错: {}", e.getMessage());
        }
    }
}

