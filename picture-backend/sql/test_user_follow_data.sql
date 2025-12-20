-- 用户关注功能测试数据
-- 密码统一为：87a312fcfd67ccb7b86aadd364807256

-- 插入测试用户
INSERT INTO `user` (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole, createTime, updateTime, isDelete) VALUES
(1, 'user001', '87a312fcfd67ccb7b86aadd364807256', '张三', 'https://picsum.photos/200/200?random=1', '摄影爱好者，喜欢分享生活中的美好瞬间', 'user', NOW(), NOW(), 0),
(2, 'user002', '87a312fcfd67ccb7b86aadd364807256', '李四', 'https://picsum.photos/200/200?random=2', '设计师，专注于UI/UX设计', 'user', NOW(), NOW(), 0),
(3, 'user003', '87a312fcfd67ccb7b86aadd364807256', '王五', 'https://picsum.photos/200/200?random=3', '旅行博主，记录世界各地的风景', 'user', NOW(), NOW(), 0),
(4, 'user004', '87a312fcfd67ccb7b86aadd364807256', '赵六', 'https://picsum.photos/200/200?random=4', '美食摄影师，用镜头记录美食', 'user', NOW(), NOW(), 0),
(5, 'user005', '87a312fcfd67ccb7b86aadd364807256', '钱七', 'https://picsum.photos/200/200?random=5', '插画师，创作各种风格的插画作品', 'user', NOW(), NOW(), 0),
(6, 'user006', '87a312fcfd67ccb7b86aadd364807256', '孙八', 'https://picsum.photos/200/200?random=6', '产品经理，关注用户体验', 'user', NOW(), NOW(), 0),
(7, 'user007', '87a312fcfd67ccb7b86aadd364807256', '周九', 'https://picsum.photos/200/200?random=7', '前端开发工程师，热爱技术分享', 'user', NOW(), NOW(), 0),
(8, 'user008', '87a312fcfd67ccb7b86aadd364807256', '吴十', 'https://picsum.photos/200/200?random=8', '内容创作者，分享生活点滴', 'user', NOW(), NOW(), 0),
(9, 'user009', '87a312fcfd67ccb7b86aadd364807256', '郑十一', 'https://picsum.photos/200/200?random=9', '艺术爱好者，喜欢各种形式的艺术', 'user', NOW(), NOW(), 0),
(10, 'user010', '87a312fcfd67ccb7b86aadd364807256', '王十二', 'https://picsum.photos/200/200?random=10', '自由职业者，追求生活的多样性', 'user', NOW(), NOW(), 0);

-- 插入用户关注关系
-- 用户1关注用户2,3,4,5
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(1, 3, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(1, 4, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW()),
(1, 5, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW());

-- 用户2关注用户1,3,6
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(2, 1, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW()),
(2, 3, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(2, 6, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

-- 用户3关注用户1,2,4,7,8
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(3, 1, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(3, 2, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(3, 4, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW()),
(3, 7, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(3, 8, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW());

-- 用户4关注用户1,3,5
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(4, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(4, 3, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(4, 5, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW());

-- 用户5关注用户2,4,6,9
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(5, 2, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(5, 4, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(5, 6, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW()),
(5, 9, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW());

-- 用户6关注用户1,3,5,7
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(6, 1, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(6, 3, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(6, 5, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW()),
(6, 7, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW());

-- 用户7关注用户2,4,8,10
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(7, 2, DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
(7, 4, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(7, 8, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW()),
(7, 10, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW());

-- 用户8关注用户1,3,6,9
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(8, 1, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW()),
(8, 3, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW()),
(8, 6, DATE_SUB(NOW(), INTERVAL 11 DAY), NOW()),
(8, 9, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW());

-- 用户9关注用户2,5,7,10
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(9, 2, DATE_SUB(NOW(), INTERVAL 19 DAY), NOW()),
(9, 5, DATE_SUB(NOW(), INTERVAL 16 DAY), NOW()),
(9, 7, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW()),
(9, 10, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW());

-- 用户10关注用户1,4,6,8
INSERT INTO `user_follow` (followerId, followeeId, createTime, updateTime) VALUES
(10, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),
(10, 4, DATE_SUB(NOW(), INTERVAL 17 DAY), NOW()),
(10, 6, DATE_SUB(NOW(), INTERVAL 13 DAY), NOW()),
(10, 8, DATE_SUB(NOW(), INTERVAL 9 DAY), NOW());


