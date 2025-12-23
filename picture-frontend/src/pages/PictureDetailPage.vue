<template>
  <!-- 
    图片详情页面 - PictureDetailPage.vue
    功能：展示图片详细信息和操作功能
    特性：
    - 图片预览（支持大图显示）
    - 图片详细信息展示（作者、名称、简介、分类、标签等）
    - 图片操作（下载、分享）
    - 响应式布局（图片和信息分栏显示）
  -->
  <div id="pictureDetailPage">
    <a-row :gutter="[16, 16]">

      <!-- 图片预览区域（左侧大图） -->
      <a-col :sm="24" :md="16" :xl="18">
        <!-- 图片预览卡片 -->
        <a-card title="图片预览">
          <!-- 图片显示组件，设置最大高度和对象适配方式 -->
          <a-image :src="picture.url" style="max-height: 600px; object-fit: contain" />
        </a-card>

        <!-- AI风格重绘区域 -->
        <a-card v-if="showStyleRedraw" title="✨ AI风格重绘" style="margin-top: 16px">
          <div class="style-redraw-container">
            <!-- 风格选择区域 -->
            <div class="style-selection-section">
              <h4 style="margin-bottom: 16px">选择重绘风格</h4>
              <div class="style-selection">
                <a-space wrap :size="[8, 8]">
                  <div
                    v-for="style in styleOptions"
                    :key="style.index"
                    class="style-tag"
                    :class="{ 'style-tag-active': selectedStyleIndex === style.index }"
                    :style="{
                      background: selectedStyleIndex === style.index ? style.gradient : 'transparent',
                      borderColor: style.color,
                      color: selectedStyleIndex === style.index ? '#fff' : style.color,
                    }"
                    @click="selectStyle(style.index)"
                  >
                    {{ style.name }}
                  </div>
                </a-space>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div style="margin-top: 24px; text-align: center">
              <a-space>
                <a-button
                  type="primary"
                  :loading="!!styleRedrawTaskId"
                  :disabled="selectedStyleIndex === null"
                  @click="createStyleRedrawTask"
                >
                  立即重绘
                </a-button>
                <a-button v-if="styleRedrawResultUrl" type="primary" :loading="styleRedrawUploadLoading" @click="handleStyleRedrawUpload">
                  保存到空间
                </a-button>
                <a-button v-if="styleRedrawResultUrl" @click="handleStyleRedrawDownload">
                  <template #icon>
                    <DownloadOutlined />
                  </template>
                  下载结果
                </a-button>
                <a-button @click="cancelStyleRedraw">取消</a-button>
              </a-space>
            </div>

            <!-- 结果预览 -->
            <div v-if="styleRedrawResultUrl" style="margin-top: 24px">
              <h4>重绘结果</h4>
              <div class="result-preview">
                <a-image :src="styleRedrawResultUrl" style="max-height: 400px; object-fit: contain" />
              </div>
              <!-- 图片名称输入 -->
              <div style="margin-top: 16px">
                <div style="margin-bottom: 8px">图片名称：</div>
                <a-input
                  v-model:value="styleRedrawPictureName"
                  placeholder="请输入图片名称"
                  :maxlength="50"
                  show-count
                />
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

     <!-- 图片信息区域（右侧信息栏） -->
      <a-col :sm="24" :md="8" :xl="6">
        <!-- 图片信息卡片 -->
        <a-card title="图片信息">
          <!-- 使用描述列表展示图片信息，单列布局 -->
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar 
                  :size="24" 
                  :src="picture.user?.userAvatar" 
                  class="user-avatar-clickable"
                  @click="goToUserProfile"
                />
                <div class="user-name-clickable" @click="goToUserProfile">{{ picture.user?.userName }}</div>
                <a-button 
                  v-if="picture.user?.id && picture.user.id !== loginUserId"
                  type="link" 
                  size="small"
                  :loading="followLoading"
                  @click="handleFollowAuthor"
                >
                  {{ isFollowingAuthor ? '已关注' : '关注' }}
                </a-button>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ picture.category ?? '默认' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
            <a-descriptions-item label="主色调">
              <a-space>
                {{ picture.picColor ?? '-' }}
                <div
                  v-if="picture.picColor"
                  :style="{
                    width: '16px',
                    height: '16px',
                    backgroundColor: toHexColor(picture.picColor),
                  }"
                />
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="点赞数">
              <a-space>
                <HeartOutlined :style="{ color: '#ff4d4f' }" />
                {{ picture.likeCount || 0 }}
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="收藏数">
              <a-space>
                <StarOutlined :style="{ color: '#faad14' }" />
                {{ picture.favoriteCount || 0 }}
              </a-space>
            </a-descriptions-item>
          </a-descriptions>

          <!-- 图片操作按钮区域 -->
          <a-space wrap>
            <a-button 
              :type="picture.hasLiked ? 'default' : 'primary'"
              @click="doLike"
              :danger="picture.hasLiked"
            >
              <template #icon>
                <HeartFilled v-if="picture.hasLiked" />
                <HeartOutlined v-else />
              </template>
              {{ picture.hasLiked ? '取消点赞' : '点赞' }}
            </a-button>
            <a-button 
              :type="picture.hasFavorited ? 'default' : 'primary'"
              @click="doFavorite"
              :style="picture.hasFavorited ? { color: '#faad14', borderColor: '#faad14' } : {}"
            >
              <template #icon>
                <StarFilled v-if="picture.hasFavorited" />
                <StarOutlined v-else />
              </template>
              {{ picture.hasFavorited ? '取消收藏' : '收藏' }}
            </a-button>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
            <a-button :icon="h(ShareAltOutlined)" type="primary" ghost @click="doShare">
              分享
            </a-button>
            <a-button type="primary" ghost @click="doStyleRedraw">
              ✨AI风格重绘
            </a-button>


            <!-- 新增按钮：保存到私人空间 -->
            <a-button v-if="canSaveToPrivate" type="primary" ghost @click="doSaveToPrivate">
              保存到私人空间
            </a-button>

             <!-- 新增按钮：发布到公共空间 -->
            <a-popconfirm
              v-if="canPublishToPublic"
              title="确定要发布到公共图库吗？需经过管理员审核。"
              ok-text="确定"
              cancel-text="取消"
              @confirm="doPublishToPublic"
            >
               <a-button type="primary" ghost>
                发布到公共空间
              </a-button>
            </a-popconfirm>

          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <!-- 分享模态框组件 -->
    <ShareModal ref="shareModalRef" title="分享图片" :link="shareLink || ''" />

    <!-- 选择空间模态框 -->
    <a-modal
      v-model:visible="showSpaceModal"
      title="选择要保存的空间"
      @ok="handleSaveToPrivate"
      :confirmLoading="spaceLoading"
    >
      <div v-if="userSpaces.length > 0">
        <a-select v-model:value="selectedSpaceId" style="width: 100%" placeholder="请选择空间">
          <a-select-option v-for="space in userSpaces" :key="space.id" :value="space.id">
            {{ space.spaceName }}
          </a-select-option>
        </a-select>
      </div>
      <div v-else>
        <p>您还没有创建空间，请前往创建。</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { 
  likePictureUsingPost, 
  unlikePictureUsingPost 
} from '@/api/pictureLikeController.ts'
import { 
  favoritePictureUsingPost, 
  unfavoritePictureUsingPost 
} from '@/api/pictureFavoriteController.ts'
import {
  followUserUsingPost,
  unfollowUserUsingPost,
  isFollowingUsingGet,
} from '@/api/userFollowController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined,
  ShareAltOutlined,
  HeartOutlined,
  HeartFilled,
  StarOutlined,
  StarFilled,
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { downloadImage, formatSize, toHexColor } from '@/utils'
import ShareModal from '@/components/ShareModal.vue'
import { SERVER_URL } from '@/request'
import {
  createPicturePortraitStyleRedrawTaskUsingPost,
  getPicturePortraitStyleRedrawTaskUsingGet,
  uploadPictureByUrlUsingPost,
  savePictureToPrivateUsingPost,
  publishPictureToPublicUsingPost,
} from '@/api/pictureController.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'


// 定义组件属性接口
interface Props {
  id: string | number
}

const props = defineProps<Props>()
// 图片详情数据
const picture = ref<API.PictureVO>({})

// 关注相关
const loginUserStore = useLoginUserStore()
const loginUserId = computed(() => loginUserStore.loginUser.id)
const isFollowingAuthor = ref(false)
const followLoading = ref(false)

// /**
//  * 创建权限检查器的工厂函数
//  * 用于根据权限字符串生成计算属性
//  * @param {string} permission - 权限字符串
//  * @returns {ComputedRef<boolean>} 是否拥有该权限
//  */
// function createPermissionChecker(permission: string) {
//   return computed(() => {
//     return (picture.value.permissionList ?? []).includes(permission)
//   })
// }

// // 定义权限检查
// const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
// const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id as any,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      // 检查是否已关注作者
      if (picture.value.user?.id && loginUserId.value) {
        checkFollowingStatus()
      }
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

// 检查是否已关注作者
const checkFollowingStatus = async () => {
  if (!picture.value.user?.id || !loginUserId.value) {
    return
  }
  try {
    const res = await isFollowingUsingGet({ followeeId: picture.value.user.id })
    if (res.data.code === 0 && res.data.data !== undefined) {
      isFollowingAuthor.value = res.data.data
    }
  } catch (error) {
    console.error('检查关注状态失败:', error)
  }
}

// 关注/取消关注作者
const handleFollowAuthor = async () => {
  if (!picture.value.user?.id) {
    return
  }
  followLoading.value = true
  try {
    if (isFollowingAuthor.value) {
      // 取消关注
      const res = await unfollowUserUsingPost({ followeeId: picture.value.user.id })
      if (res.data.code === 0) {
        message.success('取消关注成功')
        isFollowingAuthor.value = false
      } else {
        message.error('取消关注失败：' + res.data.message)
      }
    } else {
      // 关注
      const res = await followUserUsingPost({ followeeId: picture.value.user.id })
      if (res.data.code === 0) {
        message.success('关注成功')
        isFollowingAuthor.value = true
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

// 组件挂载时获取图片详情
onMounted(() => {
  fetchPictureDetail()
})

const router = useRouter()

/**
 * 编辑图片处理函数
 * 跳转到图片编辑页面
 */
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除图片处理函数
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
  } else {
    message.error('删除失败')
  }
}

// 下载图片处理函数
const doDownload = () => {
  downloadImage(picture.value.url)
}

// 点赞图片
const doLike = async () => {
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  try {
    if (picture.value.hasLiked) {
      // 取消点赞
      const res = await unlikePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('取消点赞成功')
        picture.value.hasLiked = false
        picture.value.likeCount = (picture.value.likeCount || 1) - 1
      } else {
        message.error('取消点赞失败：' + res.data.message)
      }
    } else {
      // 点赞
      const res = await likePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('点赞成功')
        picture.value.hasLiked = true
        picture.value.likeCount = (picture.value.likeCount || 0) + 1
      } else {
        message.error('点赞失败：' + res.data.message)
      }
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

// 收藏图片
const doFavorite = async () => {
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  try {
    if (picture.value.hasFavorited) {
      // 取消收藏
      const res = await unfavoritePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('取消收藏成功')
        picture.value.hasFavorited = false
        picture.value.favoriteCount = (picture.value.favoriteCount || 1) - 1
      } else {
        message.error('取消收藏失败：' + res.data.message)
      }
    } else {
      // 收藏
      const res = await favoritePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('收藏成功')
        picture.value.hasFavorited = true
        picture.value.favoriteCount = (picture.value.favoriteCount || 0) + 1
      } else {
        message.error('收藏失败：' + res.data.message)
      }
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()

// ----- AI风格重绘操作 ----
const showStyleRedraw = ref(false)
const selectedStyleIndex = ref<number | null>(null)
const styleRedrawTaskId = ref<string>()
const styleRedrawResultUrl = ref<string>('')
const styleRedrawPictureName = ref<string>('')
const styleRedrawUploadLoading = ref(false)

/**
 * 风格选项配置
 */
interface StyleOption {
  index: number
  name: string
  color: string
  gradient: string
}

const styleOptions = ref<StyleOption[]>([
  { index: 0, name: '复古漫画', color: '#8B4513', gradient: 'linear-gradient(135deg, #8B4513 0%, #D2691E 100%)' },
  { index: 1, name: '3D童话', color: '#FF69B4', gradient: 'linear-gradient(135deg, #FF69B4 0%, #FFB6C1 100%)' },
  { index: 2, name: '二次元', color: '#FF1493', gradient: 'linear-gradient(135deg, #FF1493 0%, #FF69B4 100%)' },
  { index: 3, name: '小清新', color: '#90EE90', gradient: 'linear-gradient(135deg, #90EE90 0%, #E0FFE0 100%)' },
  { index: 4, name: '未来科技', color: '#00CED1', gradient: 'linear-gradient(135deg, #00CED1 0%, #00FFFF 100%)' },
  { index: 5, name: '国画古风', color: '#CD853F', gradient: 'linear-gradient(135deg, #CD853F 0%, #DEB887 100%)' },
  { index: 6, name: '将军百战', color: '#8B0000', gradient: 'linear-gradient(135deg, #8B0000 0%, #DC143C 100%)' },
  { index: 7, name: '炫彩卡通', color: '#FF4500', gradient: 'linear-gradient(135deg, #FF4500 0%, #FFD700 50%, #FF1493 100%)' },
  { index: 8, name: '清雅国风', color: '#9370DB', gradient: 'linear-gradient(135deg, #9370DB 0%, #BA55D3 100%)' },
  { index: 9, name: '喜迎新年', color: '#FF0000', gradient: 'linear-gradient(135deg, #FF0000 0%, #FFD700 100%)' },
  { index: 14, name: '国风工笔', color: '#8B7355', gradient: 'linear-gradient(135deg, #8B7355 0%, #D2B48C 100%)' },
  { index: 15, name: '恭贺新禧', color: '#DC143C', gradient: 'linear-gradient(135deg, #DC143C 0%, #FFD700 100%)' },
  { index: 30, name: '童话世界', color: '#FFB6C1', gradient: 'linear-gradient(135deg, #FFB6C1 0%, #FFC0CB 100%)' },
  { index: 31, name: '黏土世界', color: '#DDA0DD', gradient: 'linear-gradient(135deg, #DDA0DD 0%, #EE82EE 100%)' },
  { index: 32, name: '像素世界', color: '#4169E1', gradient: 'linear-gradient(135deg, #4169E1 0%, #1E90FF 100%)' },
  { index: 33, name: '冒险世界', color: '#228B22', gradient: 'linear-gradient(135deg, #228B22 0%, #32CD32 100%)' },
  { index: 34, name: '日漫世界', color: '#FF6347', gradient: 'linear-gradient(135deg, #FF6347 0%, #FFA500 100%)' },
  { index: 35, name: '3D世界', color: '#00BFFF', gradient: 'linear-gradient(135deg, #00BFFF 0%, #87CEEB 100%)' },
  { index: 36, name: '二次元世界', color: '#FF1493', gradient: 'linear-gradient(135deg, #FF1493 0%, #FF69B4 50%, #FFB6C1 100%)' },
  { index: 37, name: '手绘世界', color: '#D2691E', gradient: 'linear-gradient(135deg, #D2691E 0%, #F4A460 100%)' },
  { index: 38, name: '蜡笔世界', color: '#FFD700', gradient: 'linear-gradient(135deg, #FFD700 0%, #FFFF00 100%)' },
  { index: 39, name: '冰箱贴世界', color: '#20B2AA', gradient: 'linear-gradient(135deg, #20B2AA 0%, #48D1CC 100%)' },
  { index: 40, name: '吧唧世界', color: '#FF69B4', gradient: 'linear-gradient(135deg, #FF69B4 0%, #FFB6C1 50%, #FFC0CB 100%)' },
])

// 轮询定时器
let styleRedrawPollingTimer: ReturnType<typeof setInterval> | null = null

/**
 * 选择风格
 */
const selectStyle = (index: number) => {
  selectedStyleIndex.value = index
}

/**
 * 创建风格重绘任务
 * 调用后端API创建风格重绘任务
 */
const createStyleRedrawTask = async () => {
  if (!picture.value.id) {
    return
  }
  if (selectedStyleIndex.value === null) {
    message.warning('请先选择风格')
    return
  }

  const res = await createPicturePortraitStyleRedrawTaskUsingPost({
    pictureId: picture.value.id,
    styleIndex: selectedStyleIndex.value,
  })

  // 处理API响应
  // 前端进行轮询，询问后端结果
  if (res.data.code === 0 && res.data.data?.output) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    console.log(res.data.data.output.taskId) // 保存任务ID
    styleRedrawTaskId.value = res.data.data.output.taskId || undefined // 开始轮询查询任务状态
    // 开启轮询
    if (styleRedrawTaskId.value) {
      startStyleRedrawPolling()
    }
  } else {
    message.error('风格重绘任务失败，' + res.data.message)
  }
}

/**
 * 开始轮询查询任务状态
 * 定时查询风格重绘任务的执行状态
 */
const startStyleRedrawPolling = () => {
  if (!styleRedrawTaskId.value) {
    return
  }

  styleRedrawPollingTimer = setInterval(async () => {
    try {
      const res = await getPicturePortraitStyleRedrawTaskUsingGet({
        taskId: styleRedrawTaskId.value!,
      })
      if (res.data.code === 0 && res.data.data?.output) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('风格重绘任务执行成功')
          // 从results数组中获取第一张图片的URL
          if (taskResult.results && taskResult.results.length > 0 && taskResult.results[0]?.url) {
            styleRedrawResultUrl.value = taskResult.results[0].url
            // 设置默认图片名称（使用原图名称）
            if (picture.value.name) {
              styleRedrawPictureName.value = picture.value.name
            }
          }
          // 清理轮询
          clearStyleRedrawPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('风格重绘任务执行失败')
          // 清理轮询
          clearStyleRedrawPolling()
        }
      }
    } catch (error: any) {
      console.error('风格重绘任务轮询失败', error)
      message.error('风格重绘任务轮询失败，' + error.message)
      // 清理轮询
      clearStyleRedrawPolling()
    }
  }, 3000) // 每 3 秒轮询一次
}

/**
 * 清理轮询定时器
 */
const clearStyleRedrawPolling = () => {
  if (styleRedrawPollingTimer) {
    clearInterval(styleRedrawPollingTimer)
    styleRedrawPollingTimer = null
    styleRedrawTaskId.value = undefined
  }
}

/**
 * 处理结果图片保存到空间
 * 将风格重绘结果保存到当前登录用户的个人空间
 */
const handleStyleRedrawUpload = async () => {
  if (!styleRedrawResultUrl.value) {
    message.warning('没有可保存的结果')
    return
  }

  // 获取当前登录用户的第一个空间
  let userSpaceId: number | undefined
  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUserId.value,
      current: 1,
      pageSize: 1,
    })
    if (res.data.code === 0 && res.data.data?.records && res.data.data.records.length > 0) {
      userSpaceId = res.data.data.records[0].id
    } else {
      message.warning('您还没有创建空间，请先创建空间')
      return
    }
  } catch (error: any) {
    console.error('获取用户空间失败', error)
    message.error('获取用户空间失败，' + error.message)
    return
  }

  if (!userSpaceId) {
    message.warning('您还没有创建空间，请先创建空间')
    return
  }

  styleRedrawUploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: styleRedrawResultUrl.value,
      spaceId: userSpaceId,
      picName: styleRedrawPictureName.value || picture.value.name || '风格重绘图片',
    }
    // 不传入id，创建新图片保存到空间
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片已保存到您的空间')
      // 刷新图片详情
      fetchPictureDetail()
      // 不关闭风格重绘区域，用户可以继续操作
    } else {
      message.error('保存图片失败，' + res.data.message)
    }
  } catch (error: any) {
    console.error('保存图片失败', error)
    message.error('保存图片失败，' + error.message)
  }
  styleRedrawUploadLoading.value = false
}

/**
 * 下载重绘结果图片
 */
const handleStyleRedrawDownload = () => {
  if (!styleRedrawResultUrl.value) {
    message.warning('没有可下载的结果')
    return
  }
  downloadImage(styleRedrawResultUrl.value)
}

/**
 * 取消风格重绘
 */
const cancelStyleRedraw = () => {
  showStyleRedraw.value = false
  selectedStyleIndex.value = null
  styleRedrawResultUrl.value = ''
  styleRedrawPictureName.value = ''
  styleRedrawTaskId.value = undefined
  clearStyleRedrawPolling()
}
/**
 * 分享处理函数
 * 生成分享链接并打开分享模态框
 */
const doShare = () => {
  // 使用配置的服务器IP地址生成分享链接，以便手机扫码访问
  shareLink.value = `${SERVER_URL}/picture/${picture.value.id}`

  // 打开分享模态框
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}

// 跳转到用户主页
const goToUserProfile = () => {
  if (picture.value.user?.id) {
    router.push(`/user/${picture.value.user.id}`)
  }
}

// AI风格重绘处理函数
const doStyleRedraw = () => {
  showStyleRedraw.value = !showStyleRedraw.value
  if (!showStyleRedraw.value) {
    cancelStyleRedraw()
  }
}

// ----- 1. 保存到私人空间操作 -----
const showSpaceModal = ref(false)
const selectedSpaceId = ref<number>()
const userSpaces = ref<API.SpaceVO[]>([])
const spaceLoading = ref(false)

// 检查是否显示"保存到私人空间"按钮：当前图片在公共空间(spaceId 为空) & 用户已登录
const canSaveToPrivate = computed(() => {
  return !picture.value.spaceId && loginUserId.value
})

const doSaveToPrivate = async () => {
  if (!loginUserId.value) {
    message.warning('请先登录')
    return
  }
  // 获取用户空间列表
  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUserId.value,
      current: 1,
      pageSize: 100, // 获取足够多的空间，暂不分页
    })
    if (res.data.code === 0) {
      userSpaces.value = res.data.data?.records || []
      if (userSpaces.value.length === 0) {
        message.warning('您还没有创建空间，请先创建空间')
        // 可以在这里引导跳转到创建空间页面
        return
      }
      // 默认选中第一个
      selectedSpaceId.value = userSpaces.value[0].id
      showSpaceModal.value = true
    } else {
       message.error('获取空间列表失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取空间列表失败：' + error.message)
  }
}

const handleSaveToPrivate = async () => {
  if (!selectedSpaceId.value || !picture.value.id) {
    return
  }
  spaceLoading.value = true
  try {
    const res = await savePictureToPrivateUsingPost({
      pictureId: picture.value.id,
      spaceId: selectedSpaceId.value,
    })
    if (res.data.code === 0) {
      message.success('保存成功')
      showSpaceModal.value = false
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('保存失败：' + error.message)
  } finally {
    spaceLoading.value = false
  }
}

// ----- 2. 发布到公共空间操作 -----
// 检查是否显示"发布到公共空间"按钮：当前图片在私有空间 & 用户是作者
const canPublishToPublic = computed(() => {
  return !!picture.value.spaceId && picture.value.user?.id === loginUserId.value
})

const doPublishToPublic = async () => {
  if (!picture.value.id) {
    return
  }
  try {
    const res = await publishPictureToPublicUsingPost({
      pictureId: picture.value.id,
    })
    if (res.data.code === 0) {
      message.success('已提交发布申请，请等待管理员审核')
    } else {
       message.error('发布失败：' + res.data.message)
    }
  } catch (error: any) {
     message.error('发布失败：' + error.message)
  }
}
</script>

<style scoped>
/* 页面容器样式 */
#pictureDetailPage {
  margin-bottom: 16px;  /* 底部间距 */
}

.user-avatar-clickable {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.user-avatar-clickable:hover {
  transform: scale(1.1);
}

.user-name-clickable {
  cursor: pointer;
  transition: color 0.3s ease;
}

.user-name-clickable:hover {
  color: #1890ff;
}

/* 风格重绘样式 */
.style-redraw-container {
  padding: 8px 0;
}

.style-selection-section {
  margin-bottom: 16px;
}

.style-selection {
  min-height: 60px;
}

.style-tag {
  cursor: pointer;
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  border: 2px solid;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.style-tag::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.style-tag:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: pulse 1.5s ease-in-out infinite;
}

.style-tag:hover::before {
  width: 300px;
  height: 300px;
}

.style-tag-active {
  font-weight: 600;
  box-shadow: 0 0 20px rgba(24, 144, 255, 0.4), 0 4px 12px rgba(0, 0, 0, 0.15);
  animation: glow 2s ease-in-out infinite;
  transform: scale(1.05);
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  50% {
    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.3);
  }
}

@keyframes glow {
  0%, 100% {
    box-shadow: 0 0 20px rgba(24, 144, 255, 0.4), 0 4px 12px rgba(0, 0, 0, 0.15);
  }
  50% {
    box-shadow: 0 0 30px rgba(24, 144, 255, 0.6), 0 4px 16px rgba(0, 0, 0, 0.2);
  }
}

.result-preview {
  text-align: center;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 16px;
  background: #fafafa;
}
</style>
