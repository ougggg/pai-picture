<template>
  <!-- 个人中心页面容器 -->
  <div id="userCenterPage">
    <div class="user-center-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>个人中心</h2>
      </div>

      <!-- 个人信息卡片 -->
      <a-card title="个人信息" class="info-card">
        <div class="user-info">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <a-avatar :size="120" :src="userInfo.userAvatar" class="user-avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
          </div>

          <!-- 基本信息 -->
          <div class="basic-info">
            <a-descriptions :column="2" bordered>
              <a-descriptions-item label="用户名">
                {{ userInfo.userName || '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="账号">
                {{ userInfo.userAccount }}
              </a-descriptions-item>
              <a-descriptions-item label="手机号">
                {{ userInfo.userPhone || '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="邮箱">
                {{ userInfo.userEmail || '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="用户角色">
                <a-tag :color="userInfo.userRole === 'admin' ? 'green' : 'blue'">
                  {{ userInfo.userRole === 'admin' ? '管理员' : '普通用户' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="注册时间">
                {{ dayjs(userInfo.createTime).format('YYYY-MM-DD HH:mm:ss') }}
              </a-descriptions-item>
              <a-descriptions-item label="个人简介" :span="2">
                {{ userInfo.userProfile || '这个人很懒，什么都没有留下...' }}
              </a-descriptions-item>
            </a-descriptions>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <a-space>
            <a-button type="primary" @click="showEditModal">
              <EditOutlined /> 编辑个人信息
            </a-button>
            <a-button @click="showPasswordModal">
              <KeyOutlined /> 修改密码
            </a-button>
          </a-space>
        </div>
      </a-card>

      <!-- 统计卡片 -->
      <div class="stat-cards-container">
        <a-card hoverable class="stat-card-clickable" @click="switchTab('published')">
          <a-statistic
            title="发布的图片"
            :value="myPictureTotal"
            :prefix="h(PictureOutlined)"
            :value-style="{ color: '#1890ff' }"
          />
        </a-card>
        <a-card hoverable class="stat-card-clickable" @click="switchTab('liked')">
          <a-statistic
            title="点赞的图片"
            :value="myLikeTotal"
            :prefix="h(HeartOutlined)"
            :value-style="{ color: '#ff4d4f' }"
          />
        </a-card>
        <a-card hoverable class="stat-card-clickable" @click="switchTab('favorited')">
          <a-statistic
            title="收藏的图片"
            :value="myFavoriteTotal"
            :prefix="h(StarOutlined)"
            :value-style="{ color: '#faad14' }"
          />
        </a-card>
        <a-card hoverable class="stat-card-clickable" @click="goToFollowPage">
          <a-statistic
            title="我的关注"
            :value="followingTotal"
            :prefix="h(UserAddOutlined)"
            :value-style="{ color: '#52c41a' }"
          />
        </a-card>
        <a-card hoverable class="stat-card-clickable" @click="goToFollowPage">
          <a-statistic
            title="我的粉丝"
            :value="followersTotal"
            :prefix="h(TeamOutlined)"
            :value-style="{ color: '#722ed1' }"
          />
        </a-card>
      </div>

      <!-- 我的图片展示区 -->
      <a-card style="margin-top: 24px" class="info-card my-pictures-card">
        <template #title>
          <div class="card-header">
            <h3 class="card-title">{{ getCardTitle() }}</h3>
            <a-input-search
              v-model:value="searchText"
              placeholder="搜索图片名称..."
              style="width: 300px"
              enter-button
              @search="onSearch"
              allow-clear
              @change="onSearchChange"
            />
          </div>
        </template>
        
        <!-- 我发布的图片 -->
        <div v-if="activeTab === 'published'">
            <a-spin :spinning="myPictureLoading">
              <a-row :gutter="[16, 16]" v-if="myPictureList.length > 0">
                <a-col 
                  v-for="picture in myPictureList" 
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
                    <div class="picture-actions">
                      <a-button type="text" size="small" @click="editPicture(picture)">
                        <EditOutlined />
                      </a-button>
                      <a-button type="text" size="small" danger @click="deletePicture(picture)">
                        <DeleteOutlined />
                      </a-button>
                    </div>
                  </div>
                </a-col>
              </a-row>
              
              <div v-if="myPictureList.length > 0" class="pagination-container">
                <a-pagination
                  v-model:current="myPicturePage"
                  v-model:page-size="myPicturePageSize"
                  :total="myPictureTotal"
                  :show-total="(total: number) => `共 ${total} 张图片`"
                  :show-size-changer="true"
                  :page-size-options="['12', '24', '36', '48']"
                  @change="onMyPicturePageChange"
                  @showSizeChange="onMyPicturePageSizeChange"
          />
        </div>
              
        <a-empty 
          v-if="!myPictureLoading && myPictureList.length === 0"
          description="还没有发布任何图片"
        >
          <a-button type="primary" @click="goToAddPicture">立即上传</a-button>
        </a-empty>
            </a-spin>
        </div>

          <!-- 我点赞的图片 -->
        <div v-if="activeTab === 'liked'">
            <a-spin :spinning="myLikeLoading">
              <a-row :gutter="[16, 16]" v-if="myLikeList.length > 0">
                <a-col 
                  v-for="picture in myLikeList" 
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
                    <div class="picture-actions-simple">
                      <a-button type="text" size="small" @click="goToPictureDetail(picture.id)">
                        查看详情
                      </a-button>
                    </div>
                  </div>
                </a-col>
              </a-row>
              
              <div v-if="myLikeList.length > 0" class="pagination-container">
                <a-pagination
                  v-model:current="myLikePage"
                  v-model:page-size="myLikePageSize"
                  :total="myLikeTotal"
                  :show-total="(total: number) => `共 ${total} 张图片`"
                  :show-size-changer="true"
                  :page-size-options="['12', '24', '36', '48']"
                  @change="onMyLikePageChange"
                  @showSizeChange="onMyLikePageSizeChange"
                />
              </div>
              
              <a-empty 
                v-if="!myLikeLoading && myLikeList.length === 0"
                description="还没有点赞任何图片"
              >
                <a-button type="primary" @click="switchTab('published')">去图片广场逛逛</a-button>
              </a-empty>
            </a-spin>
        </div>

          <!-- 我收藏的图片 -->
        <div v-if="activeTab === 'favorited'">
            <a-spin :spinning="myFavoriteLoading">
              <a-row :gutter="[16, 16]" v-if="myFavoriteList.length > 0">
                <a-col 
                  v-for="picture in myFavoriteList" 
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
                    <div class="picture-actions-simple">
                      <a-button type="text" size="small" @click="goToPictureDetail(picture.id)">
                        查看详情
                      </a-button>
                    </div>
                  </div>
                </a-col>
              </a-row>
              
              <div v-if="myFavoriteList.length > 0" class="pagination-container">
                <a-pagination
                  v-model:current="myFavoritePage"
                  v-model:page-size="myFavoritePageSize"
                  :total="myFavoriteTotal"
                  :show-total="(total: number) => `共 ${total} 张图片`"
                  :show-size-changer="true"
                  :page-size-options="['12', '24', '36', '48']"
                  @change="onMyFavoritePageChange"
                  @showSizeChange="onMyFavoritePageSizeChange"
                />
              </div>
              
              <a-empty 
                v-if="!myFavoriteLoading && myFavoriteList.length === 0"
                description="还没有收藏任何图片"
              >
                <a-button type="primary" @click="switchTab('published')">去图片广场逛逛</a-button>
              </a-empty>
            </a-spin>
        </div>
      </a-card>

      <!-- 编辑个人信息模态框 -->
      <a-modal
        v-model:open="editModalVisible"
        title="编辑个人信息"
        ok-text="保存"
        cancel-text="取消"
        :confirm-loading="editLoading"
        @ok="handleEditSubmit"
        @cancel="handleEditCancel"
        width="600px"
      >
        <a-form
          ref="editFormRef"
          :model="editForm"
          :rules="editRules"
          layout="vertical"
        >
          <a-form-item label="头像">
            <div class="avatar-edit-section">
              <a-avatar :size="80" :src="editForm.userAvatar || userInfo.userAvatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <div class="avatar-upload-edit">
                <a-upload
                  :show-upload-list="false"
                  :before-upload="beforeUpload"
                  :custom-request="uploadAvatarInEdit"
                  accept="image/*"
                >
                  <a-button type="primary" size="small" :loading="avatarUploading">
                    <UploadOutlined /> 更换头像
                  </a-button>
                </a-upload>
              </div>
            </div>
          </a-form-item>
          <a-form-item label="用户名" name="userName">
            <a-input v-model:value="editForm.userName" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item label="手机号" name="userPhone">
            <a-input v-model:value="editForm.userPhone" placeholder="请输入手机号" />
          </a-form-item>
          <a-form-item label="邮箱" name="userEmail">
            <a-input v-model:value="editForm.userEmail" placeholder="请输入邮箱" />
          </a-form-item>
          <a-form-item label="个人简介" name="userProfile">
            <a-textarea
              v-model:value="editForm.userProfile"
              placeholder="请输入个人简介"
              :rows="4"
              :maxlength="500"
              show-count
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 修改密码模态框 -->
      <a-modal
        v-model:open="passwordModalVisible"
        title="修改密码"
        ok-text="确认修改"
        cancel-text="取消"
        :confirm-loading="passwordLoading"
        @ok="handlePasswordSubmit"
        @cancel="handlePasswordCancel"
      >
        <a-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          layout="vertical"
        >
          <a-form-item label="当前密码" name="oldPassword">
            <a-input-password
              v-model:value="passwordForm.oldPassword"
              placeholder="请输入当前密码"
            />
          </a-form-item>
          <a-form-item label="新密码" name="newPassword">
            <a-input-password
              v-model:value="passwordForm.newPassword"
              placeholder="请输入新密码（8-20位，包含字母和数字）"
            />
          </a-form-item>
          <a-form-item label="确认新密码" name="confirmPassword">
            <a-input-password
              v-model:value="passwordForm.confirmPassword"
              placeholder="请再次输入新密码"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, 
  EditOutlined, 
  KeyOutlined, 
  UploadOutlined,
  HeartOutlined,
  StarOutlined,
  PictureOutlined,
  DeleteOutlined,
  UserAddOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import type { FormInstance, UploadProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { Modal } from 'ant-design-vue'
// 引入 API 方法
import {
  getUserProfileUsingGet,
  updateUserProfileUsingPost,
  updateUserPasswordUsingPost,
  uploadAvatarUsingPost
} from '@/api/userController'
import { 
  listPictureVoByPageUsingPost,
  deletePictureUsingPost 
} from '@/api/pictureController'
import { listLikedPictureByPageUsingPost } from '@/api/pictureLikeController'
import { listFavoritedPictureByPageUsingPost } from '@/api/pictureFavoriteController'
import { 
  listFollowingByPageUsingPost,
  listFollowersByPageUsingPost 
} from '@/api/userFollowController'

const router = useRouter()

// 用户信息
const userInfo = ref<API.UserVO>({})

// 当前激活的Tab
const activeTab = ref('published')

// 搜索相关
const searchText = ref('')

// 我发布的图片相关
const myPictureList = ref<API.PictureVO[]>([])
const myPictureTotal = ref(0)
const myPictureLoading = ref(false)
const myPicturePage = ref(1)
const myPicturePageSize = ref(12)

// 我点赞的图片相关
const myLikeList = ref<API.PictureVO[]>([])
const myLikeListOriginal = ref<API.PictureVO[]>([]) // 原始数据，用于前端搜索
const myLikeTotal = ref(0)
const myLikeLoading = ref(false)
const myLikePage = ref(1)
const myLikePageSize = ref(12)

// 我收藏的图片相关
const myFavoriteList = ref<API.PictureVO[]>([])
const myFavoriteListOriginal = ref<API.PictureVO[]>([]) // 原始数据，用于前端搜索
const myFavoriteTotal = ref(0)
const myFavoriteLoading = ref(false)
const myFavoritePage = ref(1)
const myFavoritePageSize = ref(12)

// 关注和粉丝统计
const followingTotal = ref(0)
const followersTotal = ref(0)

// 编辑个人信息相关
const editModalVisible = ref(false) // 编辑模态框显示状态
const editLoading = ref(false) // 编辑保存中的加载状态
const avatarUploading = ref(false) // 头像上传中的加载状态
const editFormRef = ref<FormInstance>() // 编辑表单的引用
const editForm = reactive({    // 编辑表单的数据模型
  userName: '',
  userPhone: '',
  userEmail: '',
  userProfile: '',
  userAvatar: ''
})

// 修改密码相关状态和方法
const passwordModalVisible = ref(false) // 密码模态框显示状态
const passwordLoading = ref(false) // 密码修改中的加载状态
const passwordFormRef = ref<FormInstance>() // 密码表单的引用
const passwordForm = reactive({ // 密码表单的数据模型
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const editRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度为2-20个字符', trigger: 'blur' }
  ],
  userPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  userEmail: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  userProfile: [
    { max: 500, message: '个人简介不能超过500个字符', trigger: 'blur' }
  ]
}

// 密码表单的验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度为8-20位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,20}$/, message: '密码必须包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (value !== passwordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

// 获取用户信息
const fetchUserProfile = async () => {
  try {
    const res = await getUserProfileUsingGet()

    if (res.data.code === 0 && res.data.data) {
      userInfo.value = res.data.data
    } else {
      message.error('获取用户信息失败：' + res.data.message)
    }
  } catch (error) {
    message.error('获取用户信息失败')
    console.error('获取用户信息错误:', error)
  }
}

// 显示编辑模态框
const showEditModal = () => {
  editForm.userName = userInfo.value.userName || ''
  editForm.userPhone = userInfo.value.userPhone || ''
  editForm.userEmail = userInfo.value.userEmail || ''
  editForm.userProfile = userInfo.value.userProfile || ''
  editForm.userAvatar = userInfo.value.userAvatar || ''
  editModalVisible.value = true
}

/**
 * 处理编辑表单提交
 * 验证表单数据并更新用户信息到后端
 */
const handleEditSubmit = async () => {
  try {
    // 验证表单数据是否符合规则
    await editFormRef.value?.validate()
    // 开始提交，显示加载状态
    editLoading.value = true

    // 调用后端接口更新用户信息
    const res = await updateUserProfileUsingPost({
      userName: editForm.userName,
      userPhone: editForm.userPhone,
      userEmail: editForm.userEmail,
      userProfile: editForm.userProfile,
      userAvatar: editForm.userAvatar
    })

    // 根据返回结果进行处理
    if (res.data.code === 0) {
     // 更新成功，显示成功消息
      message.success('个人信息更新成功')
      // 关闭模态框
      editModalVisible.value = false
      // 刷新页面以确保全局数据同步更新
      window.location.reload()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch (error) {
    message.error('更新个人信息失败')
    console.error('更新个人信息错误:', error)
  } finally {
    // 重置加载状态
    editLoading.value = false
  }
}

// 取消编辑
const handleEditCancel = () => {
  editModalVisible.value = false
  editFormRef.value?.resetFields()
}

// 显示修改密码模态框
const showPasswordModal = () => {
  passwordModalVisible.value = true
}

// 处理密码修改提交
const handlePasswordSubmit = async () => {
  try {
    // 验证密码表单数据
    await passwordFormRef.value?.validate()
    passwordLoading.value = true

     // 调用后端接口更新密码
    const res = await updateUserPasswordUsingPost({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })

    if (res.data.code === 0) {
      message.success('密码修改成功')
      passwordModalVisible.value = false
      // 重置表单
      Object.assign(passwordForm, {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
    } else {
      message.error('密码修改失败：' + res.data.message)
    }
  } catch (error) {
    message.error('修改密码失败')
    console.error('修改密码错误:', error)
  } finally {
    passwordLoading.value = false
  }
}

// 取消修改密码
const handlePasswordCancel = () => {
  passwordModalVisible.value = false
  passwordFormRef.value?.resetFields()
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
}

// 头像上传前验证
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
   // 检查是否为图片类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件!')
    return false
  }
  // 检查文件大小是否超过2MB
  const isLt5M = file.size / 1024 / 1024 < 2
  if (!isLt5M) {
    message.error('图片大小不能超过2MB!')
    return false
  }
  return true
}

// 在编辑模态框中上传头像
const uploadAvatarInEdit: UploadProps['customRequest'] = async (options) => {
  const { file } = options

  try {
    avatarUploading.value = true
    const res = await uploadAvatarUsingPost({
      file: file as File
    }) as any

    if (res.data.code === 0 && res.data.data) {
      // 更新编辑表单中的头像
      editForm.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error('头像上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('头像上传失败')
    console.error('上传头像错误:', error)
  } finally {
    avatarUploading.value = false
  }
}

// 获取我发布的图片
const fetchMyPictures = async () => {
  if (!userInfo.value.id) {
    return
  }
  
  myPictureLoading.value = true
  
  try {
    const res = await listPictureVoByPageUsingPost({
      userId: userInfo.value.id,
      current: myPicturePage.value,
      pageSize: myPicturePageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend',
      searchText: searchText.value || undefined,
    })
    
    if (res.data.code === 0 && res.data.data) {
      myPictureList.value = res.data.data.records ?? []
      myPictureTotal.value = res.data.data.total ?? 0
    } else {
      message.error('获取图片失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取图片失败:', error)
    message.error('获取图片失败')
  } finally {
    myPictureLoading.value = false
  }
}

// 我发布的图片 - 页码改变
const onMyPicturePageChange = (page: number, pageSize: number) => {
  myPicturePage.value = page
  fetchMyPictures()
}

// 我发布的图片 - 每页条数改变
const onMyPicturePageSizeChange = (current: number, size: number) => {
  myPicturePage.value = 1
  myPicturePageSize.value = size
  fetchMyPictures()
}

// 获取我点赞的图片
const fetchMyLikes = async () => {
  myLikeLoading.value = true
  
  try {
    const res = await listLikedPictureByPageUsingPost({
      current: 1,
      pageSize: 1000, // 获取所有数据用于前端搜索
    })
    
    if (res.data.code === 0 && res.data.data) {
      myLikeListOriginal.value = res.data.data.records ?? []
      // 应用搜索过滤
      filterLikeList()
    } else {
      message.error('获取点赞图片失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取点赞图片失败:', error)
    message.error('获取点赞图片失败')
  } finally {
    myLikeLoading.value = false
  }
}

// 前端过滤点赞图片
const filterLikeList = () => {
  let filteredList = myLikeListOriginal.value
  
  // 如果有搜索文本，进行过滤
  if (searchText.value) {
    filteredList = filteredList.filter(picture => 
      picture.name?.toLowerCase().includes(searchText.value.toLowerCase())
    )
  }
  
  // 计算总数
  myLikeTotal.value = filteredList.length
  
  // 分页
  const start = (myLikePage.value - 1) * myLikePageSize.value
  const end = start + myLikePageSize.value
  myLikeList.value = filteredList.slice(start, end)
}

// 我点赞的图片 - 页码改变
const onMyLikePageChange = (page: number, pageSize: number) => {
  myLikePage.value = page
  filterLikeList()
}

// 我点赞的图片 - 每页条数改变
const onMyLikePageSizeChange = (current: number, size: number) => {
  myLikePage.value = 1
  myLikePageSize.value = size
  filterLikeList()
}

// 获取我收藏的图片
const fetchMyFavorites = async () => {
  myFavoriteLoading.value = true
  
  try {
    const res = await listFavoritedPictureByPageUsingPost({
      current: 1,
      pageSize: 1000, // 获取所有数据用于前端搜索
    })
    
    if (res.data.code === 0 && res.data.data) {
      myFavoriteListOriginal.value = res.data.data.records ?? []
      // 应用搜索过滤
      filterFavoriteList()
    } else {
      message.error('获取收藏图片失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取收藏图片失败:', error)
    message.error('获取收藏图片失败')
  } finally {
    myFavoriteLoading.value = false
  }
}

// 前端过滤收藏图片
const filterFavoriteList = () => {
  let filteredList = myFavoriteListOriginal.value
  
  // 如果有搜索文本，进行过滤
  if (searchText.value) {
    filteredList = filteredList.filter(picture => 
      picture.name?.toLowerCase().includes(searchText.value.toLowerCase())
    )
  }
  
  // 计算总数
  myFavoriteTotal.value = filteredList.length
  
  // 分页
  const start = (myFavoritePage.value - 1) * myFavoritePageSize.value
  const end = start + myFavoritePageSize.value
  myFavoriteList.value = filteredList.slice(start, end)
}

// 我收藏的图片 - 页码改变
const onMyFavoritePageChange = (page: number, pageSize: number) => {
  myFavoritePage.value = page
  filterFavoriteList()
}

// 我收藏的图片 - 每页条数改变
const onMyFavoritePageSizeChange = (current: number, size: number) => {
  myFavoritePage.value = 1
  myFavoritePageSize.value = size
  filterFavoriteList()
}

// Tab切换处理
const onTabChange = (key: string) => {
  activeTab.value = key
  // 清空搜索框
  searchText.value = ''
  // 根据切换的Tab加载对应的数据
  if (key === 'liked') {
    if (myLikeListOriginal.value.length === 0) {
      fetchMyLikes()
    } else {
      filterLikeList()
    }
  } else if (key === 'favorited') {
    if (myFavoriteListOriginal.value.length === 0) {
      fetchMyFavorites()
    } else {
      filterFavoriteList()
    }
  } else if (key === 'published') {
    fetchMyPictures()
  }
}

// 切换Tab（从统计卡片点击）
const switchTab = (tab: string) => {
  activeTab.value = tab
  // 清空搜索框
  searchText.value = ''
  // 根据切换的Tab加载对应的数据
  if (tab === 'liked' && myLikeList.value.length === 0) {
    fetchMyLikes()
  } else if (tab === 'favorited' && myFavoriteList.value.length === 0) {
    fetchMyFavorites()
  } else if (tab === 'published') {
    fetchMyPictures()
  }
}

// 获取卡片标题
const getCardTitle = () => {
  const titleMap: Record<string, string> = {
    published: '我发布的图片',
    liked: '我点赞的图片',
    favorited: '我收藏的图片'
  }
  return titleMap[activeTab.value] || '我的图片'
}

// 搜索处理
const onSearch = () => {
  // 根据当前激活的Tab调用对应的搜索方法
  if (activeTab.value === 'published') {
    myPicturePage.value = 1
    fetchMyPictures()
  } else if (activeTab.value === 'liked') {
    myLikePage.value = 1
    filterLikeList()
  } else if (activeTab.value === 'favorited') {
    myFavoritePage.value = 1
    filterFavoriteList()
  }
}

// 搜索框内容改变（清空时重新加载）
const onSearchChange = (e: Event) => {
  const value = (e.target as HTMLInputElement).value
  if (!value) {
    onSearch()
  }
}

const goToAddPicture = () => {
  router.push('/add_picture')
}

const goToFollowPage = () => {
  router.push('/user/follow')
}

const goToPictureDetail = (id: number | undefined) => {
  if (id) {
    router.push(`/picture/${id}`)
  }
}

// 编辑图片
const editPicture = (picture: API.PictureVO) => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
    },
  })
}

// 删除图片
const deletePicture = async (picture: API.PictureVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除图片"${picture.name}"吗？`,
    okText: '确定',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      try {
        const res = await deletePictureUsingPost({ id: picture.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 重新加载当前页图片列表
          fetchMyPictures()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        console.error('删除图片失败:', error)
        message.error('删除图片失败')
      }
    },
  })
}

// 页面加载时获取用户信息
onMounted(async () => {
  await fetchUserProfile()
  // 用户信息加载完成后，获取"我发布的"图片
  fetchMyPictures()
  // 获取点赞和收藏的统计数量（不加载具体列表，等切换Tab时再加载）
  fetchLikeAndFavoriteCount()
  // 获取关注和粉丝统计
  fetchFollowStats()
})

// 获取点赞和收藏的统计数量
const fetchLikeAndFavoriteCount = async () => {
  try {
    // 获取点赞数据（获取全部用于前端搜索）
    const likeRes = await listLikedPictureByPageUsingPost({
      current: 1,
      pageSize: 1000,
    })
    if (likeRes.data.code === 0 && likeRes.data.data) {
      myLikeListOriginal.value = likeRes.data.data.records ?? []
      myLikeTotal.value = likeRes.data.data.total ?? 0
    }
    
    // 获取收藏数据（获取全部用于前端搜索）
    const favoriteRes = await listFavoritedPictureByPageUsingPost({
      current: 1,
      pageSize: 1000,
    })
    if (favoriteRes.data.code === 0 && favoriteRes.data.data) {
      myFavoriteListOriginal.value = favoriteRes.data.data.records ?? []
      myFavoriteTotal.value = favoriteRes.data.data.total ?? 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
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
</script>

<style scoped>
/* 个人中心页面主容器样式 */
#userCenterPage {
  padding: 24px; /* 页面内边距 */

  min-height: 100vh; /* 最小高度为视窗高度 */
}

/* 用户中心内容容器 */
.user-center-container {
  max-width: 1200px; /* 最大宽度限制 */
  margin: 0 auto; /* 水平居中 */
}

/* 页面标题区域 */
.page-header {
  margin-bottom: 24px; /* 底部外边距 */
}

/* 页面标题文字样式 */
.page-header h2 {
  margin: 0; /* 清除默认外边距 */
  color: #262626; /* 深灰色文字 */
  font-size: 24px; /* 字体大小 */
  font-weight: 600; /* 字体粗细 */
}

/* 个人信息卡片样式 */
.info-card {
  background: white; /* 白色背景 */
  border-radius: 8px; /* 圆角边框 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 阴影效果 */
}

/* 用户信息展示区域 */
.user-info {
  display: flex; /* 弹性布局 */
  gap: 32px; /* 子元素间距 */
  margin-bottom: 24px; /* 底部外边距 */
}

/* 头像区域容器 */
.avatar-section {
  display: flex; /* 弹性布局 */
  flex-direction: column; /* 垂直排列 */
  align-items: center; /* 水平居中 */
  gap: 16px; /* 子元素间距 */
  min-width: 160px; /* 最小宽度 */
}

/* 用户头像样式 */
.user-avatar {
  border: 3px solid #f0f0f0; /* 浅灰色边框 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 阴影效果 */
}

/* 头像上传按钮区域 */
.avatar-upload {
  text-align: center; /* 文字居中 */
}

/* 基本信息展示区域 */
.basic-info {
  flex: 1; /* 占据剩余空间 */
}

/* 操作按钮区域 */
.action-buttons {
  text-align: center; /* 文字居中 */
  padding-top: 16px; /* 顶部内边距 */
  border-top: 1px solid #f0f0f0; /* 顶部分割线 */
}


/* 表单样式优化 - 描述列表标签 */
:deep(.ant-descriptions-item-label) {
  font-weight: 600; /* 字体粗细 */
  color: #595959; /* 中灰色文字 */
}

/* 表单样式优化 - 描述列表内容 */
:deep(.ant-descriptions-item-content) {
  color: #262626; /* 深灰色文字 */
}

/* 模态框样式 - 头部边框 */
:deep(.ant-modal-header) {
  border-bottom: 1px solid #f0f0f0; /* 底部分割线 */
}

/* 模态框样式 - 表单标签 */
:deep(.ant-form-item-label > label) {
  font-weight: 600; /* 字体粗细 */
}

/* 编辑模态框中的头像区域 */
.avatar-edit-section {
  display: flex; /* 弹性布局 */
  align-items: center; /* 垂直居中 */
  gap: 16px; /* 子元素间距 */
}

/* 编辑模态框中的头像上传按钮区域 */
.avatar-upload-edit {
  display: flex; /* 弹性布局 */
  flex-direction: column; /* 垂直排列 */
  gap: 8px; /* 子元素间距 */
}

/* 统计卡片容器 */
.stat-cards-container {
  display: flex;
  gap: 16px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.stat-cards-container .stat-card-clickable {
  flex: 1;
  min-width: 180px;
}

/* 统计卡片可点击样式 */
.stat-card-clickable {
  cursor: pointer; /* 鼠标指针 */
  transition: all 0.3s ease; /* 过渡效果 */
}

.stat-card-clickable:hover {
  transform: translateY(-4px); /* 悬停时向上移动 */
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15); /* 增强阴影效果 */
}

/* 统计卡片内容居中 */
:deep(.stat-card-clickable .ant-card-body) {
  padding: 24px; /* 内边距 */
}

/* 统计数字样式 */
:deep(.ant-statistic-title) {
  font-size: 14px; /* 字体大小 */
  color: #8c8c8c; /* 灰色文字 */
  margin-bottom: 8px; /* 底部外边距 */
}

:deep(.ant-statistic-content) {
  font-size: 30px; /* 数字字体大小 */
  font-weight: 600; /* 字体粗细 */
}

/* 我发布的图片卡片样式 */
.my-pictures-card :deep(.ant-card-body) {
  padding: 20px;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.card-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

/* 图片卡片容器 */
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

/* 图片区域 */
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

/* 悬浮信息层 */
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
  flex-direction: column;
  justify-content: flex-end;
  padding: 12px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.picture-card:hover .picture-hover-overlay {
  opacity: 1;
}

/* 图片信息 */
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
  gap: 16px;
  font-size: 12px;
}

.picture-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 操作按钮区域 */
.picture-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
  background: white;
}

.picture-actions .ant-btn {
  flex: 1;
}

/* 简化版操作按钮（点赞和收藏图片） */
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

/* 分页器容器 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
</style>
