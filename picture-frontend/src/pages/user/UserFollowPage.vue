<template>
  <div id="userFollowPage">
    <div class="follow-container">
      <div class="page-header">
        <h2>我的关注</h2>
        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-label">关注</span>
            <span class="stat-value">{{ followingTotal }}</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-label">粉丝</span>
            <span class="stat-value">{{ followersTotal }}</span>
          </div>
        </div>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="onTabChange" class="custom-tabs">
        <a-tab-pane key="following" tab="关注列表">
          <a-spin :spinning="followingLoading">
            <a-list
              :data-source="followingList"
              :pagination="{
                current: followingPage,
                pageSize: followingPageSize,
                total: followingTotal,
                showTotal: (total: number) => `共 ${total} 人`,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '30', '50'],
                onChange: onFollowingPageChange,
                onShowSizeChange: onFollowingPageSizeChange,
              }"
              class="user-list"
            >
              <template #renderItem="{ item }">
                <a-list-item class="user-list-item">
                  <a-list-item-meta>
                    <template #avatar>
                      <div class="avatar-wrapper" @click="goToUserProfile(item)">
                        <a-avatar :size="72" :src="item.userAvatar" class="user-avatar">
                          <template #icon><UserOutlined /></template>
                        </a-avatar>
                        <div class="avatar-badge" v-if="item.userRole === 'admin'">
                          <CrownOutlined />
                        </div>
                      </div>
                    </template>
                    <template #title>
                      <div class="user-title-section" @click="goToUserProfile(item)">
                        <a-space>
                          <span class="user-name">{{ item.userName || item.userAccount }}</span>
                          <a-tag v-if="item.userRole === 'admin'" color="gold">管理员</a-tag>
                        </a-space>
                      </div>
                    </template>
                    <template #description>
                      <div class="user-info">
                        <div class="user-profile">{{ item.userProfile || '这个人很懒，什么都没有留下...' }}</div>
                        <div class="user-meta">
                          <span class="meta-item">
                            <UserOutlined /> {{ item.userAccount }}
                          </span>
                          <span v-if="item.userPhone" class="meta-item">
                            <PhoneOutlined /> {{ item.userPhone }}
                          </span>
                          <span v-if="item.userEmail" class="meta-item">
                            <MailOutlined /> {{ item.userEmail }}
                          </span>
                        </div>
                      </div>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button
                      type="primary"
                      danger
                      @click="handleUnfollow(item)"
                      class="action-btn"
                    >
                      <UserDeleteOutlined /> 取消关注
                    </a-button>
                  </template>
                </a-list-item>
              </template>
              <template #empty>
                <a-empty description="还没有关注任何人" class="empty-state">
                  <template #image>
                    <div class="empty-icon">
                      <UserAddOutlined />
                    </div>
                  </template>
                  <a-button type="primary" @click="goToHome">去发现用户</a-button>
                </a-empty>
              </template>
            </a-list>
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="followers" tab="粉丝列表">
          <a-spin :spinning="followersLoading">
            <a-list
              :data-source="followersList"
              :pagination="{
                current: followersPage,
                pageSize: followersPageSize,
                total: followersTotal,
                showTotal: (total: number) => `共 ${total} 人`,
                showSizeChanger: true,
                pageSizeOptions: ['10', '20', '30', '50'],
                onChange: onFollowersPageChange,
                onShowSizeChange: onFollowersPageSizeChange,
              }"
              class="user-list"
            >
              <template #renderItem="{ item }">
                <a-list-item class="user-list-item">
                  <a-list-item-meta>
                    <template #avatar>
                      <div class="avatar-wrapper" @click="goToUserProfile(item)">
                        <a-avatar :size="72" :src="item.userAvatar" class="user-avatar">
                          <template #icon><UserOutlined /></template>
                        </a-avatar>
                        <div class="avatar-badge" v-if="item.userRole === 'admin'">
                          <CrownOutlined />
                        </div>
                      </div>
                    </template>
                    <template #title>
                      <div class="user-title-section" @click="goToUserProfile(item)">
                        <a-space>
                          <span class="user-name">{{ item.userName || item.userAccount }}</span>
                          <a-tag v-if="item.userRole === 'admin'" color="gold">管理员</a-tag>
                        </a-space>
                      </div>
                    </template>
                    <template #description>
                      <div class="user-info">
                        <div class="user-profile">{{ item.userProfile || '这个人很懒，什么都没有留下...' }}</div>
                        <div class="user-meta">
                          <span class="meta-item">
                            <UserOutlined /> {{ item.userAccount }}
                          </span>
                          <span v-if="item.userPhone" class="meta-item">
                            <PhoneOutlined /> {{ item.userPhone }}
                          </span>
                          <span v-if="item.userEmail" class="meta-item">
                            <MailOutlined /> {{ item.userEmail }}
                          </span>
                        </div>
                      </div>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button
                      v-if="!isFollowingMap[item.id!]"
                      type="primary"
                      @click="handleFollow(item)"
                      class="action-btn"
                    >
                      <UserAddOutlined /> 关注
                    </a-button>
                    <a-button
                      v-else
                      type="default"
                      danger
                      @click="handleUnfollow(item)"
                      class="action-btn"
                    >
                      <CheckOutlined /> 已关注
                    </a-button>
                  </template>
                </a-list-item>
              </template>
              <template #empty>
                <a-empty description="还没有粉丝" class="empty-state">
                  <template #image>
                    <div class="empty-icon">
                      <TeamOutlined />
                    </div>
                  </template>
                  <a-button type="primary" @click="goToHome">去发布图片</a-button>
                </a-empty>
              </template>
            </a-list>
          </a-spin>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  UserOutlined,
  UserAddOutlined,
  UserDeleteOutlined,
  PhoneOutlined,
  MailOutlined,
  CrownOutlined,
  CheckOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import {
  listFollowingByPageUsingPost,
  listFollowersByPageUsingPost,
  followUserUsingPost,
  unfollowUserUsingPost,
  isFollowingUsingGet,
} from '@/api/userFollowController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const activeTab = ref('following')

// 关注列表相关
const followingList = ref<API.UserVO[]>([])
const followingTotal = ref(0)
const followingLoading = ref(false)
const followingPage = ref(1)
const followingPageSize = ref(10)

// 粉丝列表相关
const followersList = ref<API.UserVO[]>([])
const followersTotal = ref(0)
const followersLoading = ref(false)
const followersPage = ref(1)
const followersPageSize = ref(10)

// 关注状态映射（用于粉丝列表中显示是否已关注）
const isFollowingMap = ref<Record<number, boolean>>({})

// 获取关注列表
const fetchFollowingList = async () => {
  followingLoading.value = true
  try {
    const res = await listFollowingByPageUsingPost({
      current: followingPage.value,
      pageSize: followingPageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      followingList.value = res.data.data.records ?? []
      followingTotal.value = res.data.data.total ?? 0
    } else {
      message.error('获取关注列表失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取关注列表失败:', error)
    message.error('获取关注列表失败')
  } finally {
    followingLoading.value = false
  }
}

// 获取粉丝列表
const fetchFollowersList = async () => {
  followersLoading.value = true
  try {
    const res = await listFollowersByPageUsingPost({
      current: followersPage.value,
      pageSize: followersPageSize.value,
    })
    if (res.data.code === 0 && res.data.data) {
      followersList.value = res.data.data.records ?? []
      followersTotal.value = res.data.data.total ?? 0

      // 检查每个粉丝是否已关注
      if (loginUserStore.loginUser.id) {
        const checkPromises = followersList.value.map(async (user) => {
          if (user.id) {
            try {
              const checkRes = await isFollowingUsingGet({ followeeId: user.id })
              if (checkRes.data.code === 0 && checkRes.data.data !== undefined) {
                isFollowingMap.value[user.id] = checkRes.data.data
              }
            } catch (error) {
              console.error('检查关注状态失败:', error)
            }
          }
        })
        await Promise.all(checkPromises)
      }
    } else {
      message.error('获取粉丝列表失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取粉丝列表失败:', error)
    message.error('获取粉丝列表失败')
  } finally {
    followersLoading.value = false
  }
}

// 关注用户
const handleFollow = async (user: API.UserVO) => {
  if (!user.id) {
    return
  }
  try {
    const res = await followUserUsingPost({ followeeId: user.id })
    if (res.data.code === 0) {
      message.success('关注成功')
      isFollowingMap.value[user.id] = true
      // 如果在关注列表tab，刷新列表
      if (activeTab.value === 'following') {
        fetchFollowingList()
      }
      // 更新统计数据
      fetchFollowStats()
    } else {
      message.error('关注失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('关注失败：' + error.message)
  }
}

// 取消关注
const handleUnfollow = async (user: API.UserVO) => {
  if (!user.id) {
    return
  }
  Modal.confirm({
    title: '确认取消关注',
    content: `确定要取消关注"${user.userName || user.userAccount}"吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      try {
        const res = await unfollowUserUsingPost({ followeeId: user.id })
        if (res.data.code === 0) {
          message.success('取消关注成功')
          if (user.id) {
            isFollowingMap.value[user.id] = false
          }
          // 刷新当前列表
          if (activeTab.value === 'following') {
            fetchFollowingList()
          } else {
            fetchFollowersList()
          }
          // 更新统计数据
          fetchFollowStats()
        } else {
          message.error('取消关注失败：' + res.data.message)
        }
      } catch (error: any) {
        message.error('取消关注失败：' + error.message)
      }
    },
  })
}

// Tab切换
const onTabChange = (key: string) => {
  if (key === 'following' && followingList.value.length === 0) {
    fetchFollowingList()
  } else if (key === 'followers') {
    if (followersList.value.length === 0) {
      fetchFollowersList()
    } else {
      // 刷新统计数据
      fetchFollowStats()
    }
  }
}

// 获取关注和粉丝统计
const fetchFollowStats = async () => {
  try {
    const [followingRes, followersRes] = await Promise.all([
      listFollowingByPageUsingPost({ current: 1, pageSize: 1 }),
      listFollowersByPageUsingPost({ current: 1, pageSize: 1 }),
    ])

    if (followingRes.data.code === 0 && followingRes.data.data) {
      followingTotal.value = followingRes.data.data.total ?? 0
    }

    if (followersRes.data.code === 0 && followersRes.data.data) {
      followersTotal.value = followersRes.data.data.total ?? 0
    }
  } catch (error) {
    console.error('获取关注统计失败:', error)
  }
}

// 关注列表分页
const onFollowingPageChange = (page: number, pageSize: number) => {
  followingPage.value = page
  followingPageSize.value = pageSize
  fetchFollowingList()
}

const onFollowingPageSizeChange = (current: number, size: number) => {
  followingPage.value = 1
  followingPageSize.value = size
  fetchFollowingList()
}

// 粉丝列表分页
const onFollowersPageChange = (page: number, pageSize: number) => {
  followersPage.value = page
  followersPageSize.value = pageSize
  fetchFollowersList()
}

const onFollowersPageSizeChange = (current: number, size: number) => {
  followersPage.value = 1
  followersPageSize.value = size
  fetchFollowersList()
}

const goToHome = () => {
  router.push('/')
}

const goToUserProfile = (user: API.UserVO) => {
  if (user.id) {
    router.push(`/user/${user.id}`)
  }
}

onMounted(() => {
  fetchFollowingList()
  fetchFollowStats()
})
</script>

<style scoped>
#userFollowPage {
  padding: 24px;
  min-height: 100vh;
  background: linear-gradient(to right, #e6f6f6, #fff);
}

.follow-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  color: #262626;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1890ff;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: #e8e8e8;
}

.custom-tabs {
  background: transparent;
}

.user-list {
  background: transparent;
}

.user-list-item {
  background: transparent;
  border-radius: 12px;
  margin-bottom: 16px;
  padding: 20px 24px;
  border: none;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.user-list-item:hover {
  transform: translateX(4px);
  background: rgba(255, 255, 255, 0.1);
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.avatar-wrapper:hover {
  transform: scale(1.05);
}

.user-avatar {
  border: 3px solid #e8f4ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
  transition: all 0.3s ease;
}

.user-list-item:hover .user-avatar {
  border-color: #1890ff;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
}

.avatar-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  border: 2px solid white;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.user-title-section {
  margin-bottom: 4px;
  cursor: pointer;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  cursor: pointer;
  transition: color 0.3s ease;
}

.user-name:hover {
  color: #1890ff;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.user-profile {
  color: #595959;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8c8c8c;
}

.meta-item :deep(.anticon) {
  color: #1890ff;
  font-size: 14px;
}

.action-btn {
  height: 36px;
  border-radius: 8px;
  font-weight: 500;
  padding: 0 20px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

:deep(.ant-list-item-meta-title) {
  margin-bottom: 8px;
}

:deep(.ant-list-item-meta-description) {
  margin-top: 8px;
}

:deep(.ant-list-item-action) {
  margin-left: 24px;
}

:deep(.ant-pagination) {
  margin-top: 24px;
  text-align: center;
}

.empty-state {
  padding: 60px 20px;
}

.empty-icon {
  font-size: 80px;
  color: #d9d9d9;
  margin-bottom: 16px;
}

:deep(.ant-tabs-tab) {
  font-size: 16px;
  font-weight: 500;
  padding: 12px 24px;
}

:deep(.ant-tabs-tab-active) {
  color: #1890ff;
}

:deep(.ant-tabs-ink-bar) {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  height: 3px;
}

:deep(.ant-empty-description) {
  color: #8c8c8c;
  font-size: 14px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-bar {
    width: 100%;
    justify-content: space-around;
  }

  .user-list-item {
    padding: 16px;
  }

  :deep(.ant-list-item-action) {
    margin-left: 0;
    margin-top: 12px;
  }

  .action-btn {
    width: 100%;
  }
}
</style>








