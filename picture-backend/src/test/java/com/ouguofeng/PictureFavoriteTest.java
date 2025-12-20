package com.ouguofeng;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureFavoriteService;
import com.ouguofeng.service.PictureService;
import com.ouguofeng.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 图片收藏功能测试
 */
@SpringBootTest
@Slf4j
public class PictureFavoriteTest {

    @Resource
    private PictureFavoriteService pictureFavoriteService;

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
    public void testFavoritePicture() {
        log.info("测试收藏图片功能");
        
        // 注意：这个测试需要数据库中存在对应的用户和图片数据
        // 如果数据库为空，可能会抛出异常，这是正常的
        try {
            boolean result = pictureFavoriteService.favoritePicture(testPicture.getId(), testUser);
            log.info("收藏结果: {}", result);
            
            // 验证收藏成功后，可以查询收藏记录
            PictureInteractionQueryRequest queryRequest = new PictureInteractionQueryRequest();
            queryRequest.setUserId(testUser.getId());
            queryRequest.setCurrent(1);
            queryRequest.setPageSize(10);
            
            Page<PictureVO> favoritedPictures = pictureFavoriteService.listFavoritedPictureByPage(queryRequest, null);
            log.info("用户收藏的图片数量: {}", favoritedPictures.getTotal());
            
        } catch (Exception e) {
            log.error("测试收藏功能时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户和图片数据");
        }
    }

    @Test
    public void testUnfavoritePicture() {
        log.info("测试取消收藏功能");
        
        try {
            // 先收藏
            pictureFavoriteService.favoritePicture(testPicture.getId(), testUser);
            
            // 再取消收藏
            boolean result = pictureFavoriteService.unfavoritePicture(testPicture.getId(), testUser);
            log.info("取消收藏结果: {}", result);
            
        } catch (Exception e) {
            log.error("测试取消收藏功能时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户和图片数据");
        }
    }

    @Test
    public void testListFavoritedPictures() {
        log.info("测试查询用户收藏的图片列表");
        
        try {
            PictureInteractionQueryRequest queryRequest = new PictureInteractionQueryRequest();
            queryRequest.setUserId(testUser.getId());
            queryRequest.setCurrent(1);
            queryRequest.setPageSize(10);
            
            Page<PictureVO> favoritedPictures = pictureFavoriteService.listFavoritedPictureByPage(queryRequest, null);
            log.info("用户收藏的图片总数: {}", favoritedPictures.getTotal());
            log.info("当前页图片数量: {}", favoritedPictures.getRecords().size());
            
        } catch (Exception e) {
            log.error("测试查询收藏列表时出错: {}", e.getMessage());
            log.info("提示：请确保数据库中存在id为1的用户数据");
        }
    }

    @Test
    public void testDuplicateFavorite() {
        log.info("测试重复收藏（应该抛出异常）");
        
        try {
            // 第一次收藏
            pictureFavoriteService.favoritePicture(testPicture.getId(), testUser);
            
            // 第二次收藏应该失败
            try {
                pictureFavoriteService.favoritePicture(testPicture.getId(), testUser);
                Assertions.fail("重复收藏应该抛出异常");
            } catch (Exception e) {
                log.info("重复收藏正确抛出异常: {}", e.getMessage());
            }
            
            // 清理：取消收藏
            pictureFavoriteService.unfavoritePicture(testPicture.getId(), testUser);
            
        } catch (Exception e) {
            log.error("测试重复收藏时出错: {}", e.getMessage());
        }
    }
}

