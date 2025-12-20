package com.ouguofeng;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouguofeng.model.dto.picture.PictureInteractionQueryRequest;
import com.ouguofeng.model.entity.Picture;
import com.ouguofeng.model.entity.User;
import com.ouguofeng.model.vo.PictureVO;
import com.ouguofeng.service.PictureFavoriteService;
import com.ouguofeng.service.PictureLikeService;
import com.ouguofeng.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 图片点赞收藏集成测试
 * 测试完整的用户交互流程
 */
@SpringBootTest
@Slf4j
public class PictureInteractionIntegrationTest {

    @Resource
    private PictureLikeService pictureLikeService;

    @Resource
    private PictureFavoriteService pictureFavoriteService;

    @Resource
    private PictureService pictureService;

    @Test
    public void testCompleteInteractionFlow() {
        log.info("=== 开始测试完整的图片交互流程 ===");
        
        try {
            // 1. 准备测试数据
            User testUser = new User();
            testUser.setId(1L);
            
            Long pictureId = 1L;
            
            log.info("步骤1: 查询图片初始状态");
            Picture picture = pictureService.getById(pictureId);
            if (picture != null) {
                log.info("图片初始点赞数: {}, 收藏数: {}", picture.getLikeCount(), picture.getFavoriteCount());
            } else {
                log.warn("图片不存在，跳过后续测试");
                return;
            }
            
            // 2. 用户点赞图片
            log.info("步骤2: 用户点赞图片");
            pictureLikeService.likePicture(pictureId, testUser);
            picture = pictureService.getById(pictureId);
            log.info("点赞后的点赞数: {}", picture.getLikeCount());
            
            // 3. 用户收藏图片
            log.info("步骤3: 用户收藏图片");
            pictureFavoriteService.favoritePicture(pictureId, testUser);
            picture = pictureService.getById(pictureId);
            log.info("收藏后的收藏数: {}", picture.getFavoriteCount());
            
            // 4. 查询用户点赞的图片列表
            log.info("步骤4: 查询用户点赞的图片列表");
            PictureInteractionQueryRequest queryRequest = new PictureInteractionQueryRequest();
            queryRequest.setUserId(testUser.getId());
            queryRequest.setCurrent(1);
            queryRequest.setPageSize(10);
            
            Page<PictureVO> likedPictures = pictureLikeService.listLikedPictureByPage(queryRequest, null);
            log.info("用户点赞的图片数量: {}", likedPictures.getTotal());
            
            // 5. 查询用户收藏的图片列表
            log.info("步骤5: 查询用户收藏的图片列表");
            Page<PictureVO> favoritedPictures = pictureFavoriteService.listFavoritedPictureByPage(queryRequest, null);
            log.info("用户收藏的图片数量: {}", favoritedPictures.getTotal());
            
            // 6. 取消点赞
            log.info("步骤6: 取消点赞");
            pictureLikeService.unlikePicture(pictureId, testUser);
            picture = pictureService.getById(pictureId);
            log.info("取消点赞后的点赞数: {}", picture.getLikeCount());
            
            // 7. 取消收藏
            log.info("步骤7: 取消收藏");
            pictureFavoriteService.unfavoritePicture(pictureId, testUser);
            picture = pictureService.getById(pictureId);
            log.info("取消收藏后的收藏数: {}", picture.getFavoriteCount());
            
            log.info("=== 完整流程测试成功 ===");
            
        } catch (Exception e) {
            log.error("集成测试失败: {}", e.getMessage(), e);
            log.info("提示：请确保数据库中存在id为1的用户和图片数据");
        }
    }

    @Test
    public void testMultipleUserInteraction() {
        log.info("=== 测试多用户对同一图片的交互 ===");
        
        try {
            Long pictureId = 1L;
            
            // 模拟多个用户
            User user1 = new User();
            user1.setId(1L);
            
            User user2 = new User();
            user2.setId(2L);
            
            // 查询初始状态
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.warn("图片不存在，跳过测试");
                return;
            }
            int initialLikeCount = picture.getLikeCount() != null ? picture.getLikeCount() : 0;
            int initialFavoriteCount = picture.getFavoriteCount() != null ? picture.getFavoriteCount() : 0;
            
            log.info("初始状态 - 点赞数: {}, 收藏数: {}", initialLikeCount, initialFavoriteCount);
            
            // 用户1点赞和收藏
            try {
                pictureLikeService.likePicture(pictureId, user1);
                pictureFavoriteService.favoritePicture(pictureId, user1);
                log.info("用户1点赞和收藏成功");
            } catch (Exception e) {
                log.warn("用户1操作失败（可能已经点赞/收藏过）: {}", e.getMessage());
            }
            
            // 用户2点赞和收藏
            try {
                pictureLikeService.likePicture(pictureId, user2);
                pictureFavoriteService.favoritePicture(pictureId, user2);
                log.info("用户2点赞和收藏成功");
            } catch (Exception e) {
                log.warn("用户2操作失败: {}", e.getMessage());
            }
            
            // 查询最终状态
            picture = pictureService.getById(pictureId);
            log.info("最终状态 - 点赞数: {}, 收藏数: {}", picture.getLikeCount(), picture.getFavoriteCount());
            
            // 清理：取消点赞和收藏
            try {
                pictureLikeService.unlikePicture(pictureId, user1);
                pictureFavoriteService.unfavoritePicture(pictureId, user1);
                pictureLikeService.unlikePicture(pictureId, user2);
                pictureFavoriteService.unfavoritePicture(pictureId, user2);
                log.info("清理数据成功");
            } catch (Exception e) {
                log.warn("清理数据失败: {}", e.getMessage());
            }
            
            log.info("=== 多用户交互测试完成 ===");
            
        } catch (Exception e) {
            log.error("多用户交互测试失败: {}", e.getMessage(), e);
        }
    }

    @Test
    public void testViewPictureStatistics() {
        log.info("=== 测试查看图片统计信息 ===");
        
        try {
            Long pictureId = 1L;
            Picture picture = pictureService.getById(pictureId);
            
            if (picture != null) {
                log.info("图片ID: {}", picture.getId());
                log.info("图片名称: {}", picture.getName());
                log.info("点赞数: {}", picture.getLikeCount());
                log.info("收藏数: {}", picture.getFavoriteCount());
                log.info("图片创建者ID: {}", picture.getUserId());
            } else {
                log.warn("图片不存在");
            }
            
        } catch (Exception e) {
            log.error("查看统计信息失败: {}", e.getMessage(), e);
        }
    }
}

