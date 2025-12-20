<template>
  <div id="userProfilePage">
    <div class="user-profile-container">
      <div class="user-header">
        <a-avatar :size="80" :src="userInfo.userAvatar" class="user-avatar">
          <template #icon><UserOutlined /></template>
        </a-avatar>
        <div class="user-info">
          <div class="user-name-row">
            <h2>{{ userInfo.userName || userInfo.userAccount }}</h2>
            <a-button
              v-if="showFollowButton"
              :type="isFollowing ? 'default' : 'primary'"
              :loading="followLoading"
              @click="handleFollow"
              class="follow-btn"
            >
              <UserAddOutlined v-if="!isFollowing" />
              <CheckOutlined v-else />
              {{ isFollowing ? '已关注' : '关注' }}
            </a-button>
          </div>
          <div class="user-meta">
            <span v-if="userInfo.userProfile">{{ userInfo.userProfile }}</span>
            <span v-else class="empty-profile">这个人很懒，什么都没有留下...</span>
          </div>
        </div>
      </div>
      
      <a-divider />
      
      <div class="pictures-section">
        <h3>发布的图片</h3>
        <a-spin :spinning="loading">
          <a-row :gutter="[16, 16]" v-if="pictureList.length > 0">
            <a-col 
              v-for="picture in pictureList" 
              :key="picture.id"
              :xs="24" 
              :sm="12" 
              :md="8" 
              :lg="6" 
              :xl="4"
            >
              <div class="picture-card">
                <div class="picture-image" @click="goToPictureDetail(picture.id)">
                  <img :alt="picture.name" :src="picture.thumbnailUrl ?? picture.url" />
                  <div class="picture-hover-overlay">
                    <div class="picture-info">
                      <div class="picture-name">{{ picture.name }}</div>
                      <div class="picture-stats">
                        <span><HeartOutlined /> {{ picture.likeCount || 0 }}</span>
                        <span><StarOutlined /> {{ picture.favoriteCount || 0 }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a-col>
          </a-row>
          
          <div v-if="pictureList.length > 0" class="pagination-container">
            <a-pagination
              v-model:current="current"
              v-model:page-size="pageSize"
              :total="total"
              :show-total="(total: number) => `共 ${total} 张图片`"
              :show-size-changer="true"
              :page-size-options="['12', '24', '36', '48']"
              @change="onPageChange"
              @showSizeChange="onPageSizeChange"
            />
          </div>
          
          <a-empty 
            v-if="!loading && pictureList.length === 0"
            description="该用户还没有发布任何图片"
          />
        </a-spin>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, HeartOutlined, StarOutlined, UserAddOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { getUserVoByIdUsingGet } from '@/api/userController'
import {
  followUserUsingPost,
  unfollowUserUsingPost,
  isFollowingUsingGet,
} from '@/api/userFollowController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const userId = ref<string>(String(route.params.id))

const loginUserStore = useLoginUserStore()
const loginUserId = computed(() => loginUserStore.loginUser.id)
const showFollowButton = computed(() => {
  return loginUserId.value && userInfo.value.id && loginUserId.value !== userInfo.value.id
})

const userInfo = ref<API.UserVO>({})
const pictureList = ref<API.PictureVO[]>([])
const loading = ref(false)
const total = ref(0)
const current = ref(1)
const pageSize = ref(12)

const isFollowing = ref(false)
const followLoading = ref(false)

const fetchUserInfo = async () => {
  try {
    const res = await getUserVoByIdUsingGet({ id: userId.value as any })
    if (res.data.code === 0 && res.data.data) {
      userInfo.value = res.data.data
      // 检查是否已关注
      if (showFollowButton.value && userInfo.value.id) {
        checkFollowingStatus()
      }
    } else {
      message.error('获取用户信息失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    message.error('获取用户信息失败')
  }
}

const checkFollowingStatus = async () => {
  if (!userInfo.value.id || !loginUserId.value) {
    return
  }
  try {
    const res = await isFollowingUsingGet({ followeeId: userInfo.value.id })
    if (res.data.code === 0 && res.data.data !== undefined) {
      isFollowing.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

const handleFollow = async () => {
  if (!userInfo.value.id) {
    return
  }
  followLoading.value = true
  try {
    if (isFollowing.value) {
      // 取消关注
      const res = await unfollowUserUsingPost({ followeeId: userInfo.value.id })
      if (res.data.code === 0) {
        message.success('取消关注成功')
        isFollowing.value = false
      } else {
        message.error('取消关注失败：' + res.data.message)
      }
    } else {
      // 关注
      const res = await followUserUsingPost({ followeeId: userInfo.value.id })
      if (res.data.code === 0) {
        message.success('关注成功')
        isFollowing.value = true
      } else {
        message.error('关注失败：' + res.data.message)
      }
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  } finally {
    followLoading.value = false
  }
}

const fetchPictures = async () => {
  loading.value = true
  try {
    const res = await listPictureVoByPageUsingPost({
      userId: userId.value as any,
      current: current.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
    })
    if (res.data.code === 0 && res.data.data) {
      pictureList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取图片列表失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取图片列表失败:', error)
    message.error('获取图片列表失败')
  } finally {
    loading.value = false
  }
}

const onPageChange = (page: number, size: number) => {
  current.value = page
  pageSize.value = size
  fetchPictures()
}

const onPageSizeChange = (currentPage: number, size: number) => {
  current.value = 1
  pageSize.value = size
  fetchPictures()
}

const goToPictureDetail = (id: number | undefined) => {
  if (id) {
    router.push(`/picture/${id}`)
  }
}

onMounted(() => {
  fetchUserInfo()
  fetchPictures()
})
</script>

<style scoped>
#userProfilePage {
  padding: 24px;
}

.user-profile-container {
  max-width: 1200px;
  margin: 0 auto;
}

.user-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  border: 3px solid #e8f4ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.user-name-row h2 {
  margin: 0;
  font-size: 24px;
  color: #262626;
}

.follow-btn {
  height: 36px;
  border-radius: 8px;
  font-weight: 500;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-meta {
  color: #8c8c8c;
  font-size: 14px;
}

.empty-profile {
  color: #bfbfbf;
  font-style: italic;
}

.pictures-section {
  margin-top: 24px;
}

.pictures-section h3 {
  margin-bottom: 16px;
  font-size: 20px;
  color: #262626;
}

.empty-state {
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.picture-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.picture-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.picture-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;
}

.picture-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-card:hover .picture-image img {
  transform: scale(1.05);
}

.picture-hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.3) 60%,
    rgba(0, 0, 0, 0.7) 100%
  );
  display: flex;
  align-items: flex-end;
  padding: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.picture-card:hover .picture-hover-overlay {
  opacity: 1;
}

.picture-info {
  color: white;
}

.picture-name {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picture-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.picture-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.picture-actions-simple {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
  background: white;
}

.picture-actions-simple .ant-btn {
  width: 100%;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
</style>
