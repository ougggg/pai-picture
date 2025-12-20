<template>
  <!-- 
    图片列表组件 - PictureList.vue
    功能：展示图片瀑布流列表，支持图片预览、操作按钮、分享等功能
    特性：
    - 响应式瀑布流布局（支持不同屏幕尺寸）
    - 图片悬停效果和遮罩层显示
    - 支持图片操作（分享、搜索、编辑、删除）
    - 支持缩略图和原图切换显示
  -->
  <div class="picture-list">
    <!-- 首次加载状态（只在没有数据时显示） -->
    <div v-if="loading && props.dataList.length === 0" class="loading-container">
      <a-spin size="large" tip="加载中..." />
    </div>

    <!-- 瀑布流容器（有数据或加载完成后始终显示） -->
    <div v-else-if="columns.length > 0" class="waterfall-container" ref="waterfallRef">
      <!-- 瀑布流列 -->
      <div 
        v-for="(column, columnIndex) in columns" 
        :key="columnIndex" 
        class="waterfall-column"
      >
        <!-- 单张图片卡片 -->
        <div 
          v-for="picture in column" 
          :key="picture.id" 
          class="picture-card-wrapper"
        >
          <!-- 图片区域 -->
          <div class="picture-card" @click="doClickPicture(picture)">
            <div class="picture-image" :style="getImageStyle(picture)">
              <img
                :alt="picture.name"
                :src="picture.thumbnailUrl ?? picture.url"
                loading="lazy"
              />
            </div>

            <!-- 悬浮遮罩层 - 显示标题、标签和点赞收藏按钮 -->
            <div class="picture-overlay">
              <!-- 图片标题 -->
              <div class="picture-title">{{ picture.name }}</div>

              <!-- 标签区域 -->
              <div class="picture-tags">
                <a-tag color="green" size="small">
                  {{ picture.category ?? '默认' }}
                </a-tag>
                <a-tag v-for="tag in picture.tags" :key="tag" size="small">
                  {{ tag }}
                </a-tag>
              </div>

              <!-- 点赞收藏统计和操作按钮 -->
              <div class="picture-stats-actions">
                <!-- 点赞按钮 -->
                <div 
                  class="stat-action-item" 
                  :class="{ 'liked': picture.hasLiked }"
                  @click="(e: Event) => doLike(picture, e)"
                  title="点赞"
                >
                  <HeartFilled v-if="picture.hasLiked" />
                  <HeartOutlined v-else />
                  <span>{{ picture.likeCount || 0 }}</span>
                </div>
                <!-- 收藏按钮 -->
                <div 
                  class="stat-action-item" 
                  :class="{ 'favorited': picture.hasFavorited }"
                  @click="(e: Event) => doFavorite(picture, e)"
                  title="收藏"
                >
                  <StarFilled v-if="picture.hasFavorited" />
                  <StarOutlined v-else />
                  <span>{{ picture.favoriteCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮区域 - 仅在空间管理页面显示 -->
          <div v-if="showOp" class="picture-actions-bottom">
            <a-button
              type="text"
              @click="(e: Event) => doShare(picture, e)"
              class="action-btn-bottom"
              title="分享"
            >
              <ShareAltOutlined />
            </a-button>
            <a-button
              type="text"
              @click="(e: Event) => doSearch(picture, e)"
              class="action-btn-bottom"
              title="搜索相似"
            >
              <SearchOutlined />
            </a-button>
            <a-button
              type="text"
              @click="(e: Event) => doEdit(picture, e)"
              class="action-btn-bottom"
              title="编辑"
            >
              <EditOutlined />
            </a-button>
            <a-button
              type="text"
              @click="(e: Event) => doDelete(picture, e)"
              class="action-btn-bottom"
              title="删除"
            >
              <DeleteOutlined />
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分享模态框 -->
    <ShareModal ref="shareModalRef" :link="shareLink" title="分享图片" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
  HeartOutlined,
  HeartFilled,
  StarOutlined,
  StarFilled,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { 
  likePictureUsingPost, 
  unlikePictureUsingPost 
} from '@/api/pictureLikeController.ts'
import { 
  favoritePictureUsingPost, 
  unfavoritePictureUsingPost 
} from '@/api/pictureFavoriteController.ts'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { SERVER_URL } from '@/request'

// 定义组件属性接口
interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
}

// 定义组件属性默认值
const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
})

const router = useRouter()
const waterfallRef = ref<HTMLElement>()

// 瀑布流列数（响应式）
const columnCount = ref(5)

// 瀑布流列数组
const columns = ref<API.PictureVO[][]>([])

// 记录上次处理的数据长度，用于判断是追加还是重置
const lastDataLength = ref(0)

// 记录每列的累计高度
const columnHeights = ref<number[]>([])

/**
 * 根据屏幕宽度计算瀑布流列数
 */
const calculateColumnCount = () => {
  const width = window.innerWidth
  if (width < 576) {
    columnCount.value = 1 // 手机
  } else if (width < 768) {
    columnCount.value = 2 // 平板竖屏
  } else if (width < 992) {
    columnCount.value = 3 // 平板横屏
  } else if (width < 1200) {
    columnCount.value = 4 // 小屏电脑
  } else if (width < 1600) {
    columnCount.value = 5 // 普通电脑
  } else {
    columnCount.value = 6 // 大屏电脑
  }
}

/**
 * 初始化列数组和高度数组
 */
const initializeColumns = () => {
  columns.value = Array.from({ length: columnCount.value }, () => [])
  columnHeights.value = Array(columnCount.value).fill(0)
}

/**
 * 计算图片的预估高度
 */
const calculateImageHeight = (picture: API.PictureVO): number => {
  const aspectRatio = picture.picHeight && picture.picWidth
    ? picture.picHeight / picture.picWidth
    : 1 // 默认比例为 1:1
  
  // 假设列宽为 240px，加上操作按钮高度约 60px
  return 240 * aspectRatio + 60 + 16 // 16px 是间距
}

/**
 * 将新图片追加到高度最小的列
 */
const appendNewImages = (newImages: API.PictureVO[]) => {
  newImages.forEach((picture) => {
    // 找到高度最小的列
    const minHeight = Math.min(...columnHeights.value)
    const minIndex = columnHeights.value.indexOf(minHeight)

    // 将图片添加到该列
    columns.value[minIndex].push(picture)

    // 更新该列的高度
    columnHeights.value[minIndex] += calculateImageHeight(picture)
  })
}

/**
 * 将图片分配到各列（基于宽高比的智能分配）
 * 支持增量渲染，避免每次都重建整个DOM
 */
const distributeImages = () => {
  const currentDataLength = props.dataList.length
  
  console.log('distributeImages 调用:', {
    currentDataLength,
    lastDataLength: lastDataLength.value,
    columnsLength: columns.value.length,
    columnCount: columnCount.value
  })
  
  // 情况1：数据被清空或减少（搜索/筛选条件改变），需要完全重建
  if (currentDataLength < lastDataLength.value) {
    console.log('🔄 检测到数据重置，完全重建瀑布流')
    initializeColumns()
    lastDataLength.value = 0
    // 如果还有数据，重新分配所有数据
    if (currentDataLength > 0) {
      appendNewImages(props.dataList)
      lastDataLength.value = currentDataLength
    }
    return
  }
  
  // 情况2：列数改变（窗口大小改变），需要完全重建
  if (columns.value.length !== columnCount.value) {
    console.log('🔄 检测到列数改变，完全重建瀑布流')
    initializeColumns()
    lastDataLength.value = 0
    // 重新分配所有数据
    if (currentDataLength > 0) {
      appendNewImages(props.dataList)
      lastDataLength.value = currentDataLength
    }
    return
  }
  
  // 情况3：首次加载
  if (lastDataLength.value === 0 && currentDataLength > 0) {
    console.log(`🎯 首次加载 ${currentDataLength} 张图片`)
    initializeColumns()
    appendNewImages(props.dataList)
    lastDataLength.value = currentDataLength
    return
  }
  
  // 情况4：有新数据追加（关键的增量加载逻辑）
  if (currentDataLength > lastDataLength.value) {
    const newImages = props.dataList.slice(lastDataLength.value)
    console.log(`✅ 增量追加 ${newImages.length} 张新图片（从${lastDataLength.value}到${currentDataLength}），保持滚动位置`)
    
    // 只追加新图片，不触碰已有的DOM
    appendNewImages(newImages)
    lastDataLength.value = currentDataLength
    return
  }
  
  // 情况5：数据长度没变化，不做任何操作
  console.log('⏸️ 数据长度未变化，跳过处理')
}

/**
 * 计算图片容器的样式（根据宽高比）
 */
const getImageStyle = (picture: API.PictureVO) => {
  if (picture.picWidth && picture.picHeight) {
    const aspectRatio = picture.picHeight / picture.picWidth
    // 设置高度为宽度的比例
    return {
      paddingBottom: `${aspectRatio * 100}%`,
    }
  }
  // 默认高度
  return {
    height: '240px',
  }
}

/**
 * 监听数据变化，智能分配图片
 * 注意：不使用 deep，只监听数组长度变化
 */
watch(
  () => props.dataList.length,
  (newLength, oldLength) => {
    console.log('📊 dataList长度变化:', { oldLength, newLength, lastDataLength: lastDataLength.value })
    // 只有当长度真正发生变化时才处理
    if (newLength !== lastDataLength.value || lastDataLength.value === 0) {
      distributeImages()
    } else {
      console.log('⏸️ 长度未变化或已处理，跳过分配')
    }
  },
  { immediate: true }
)

/**
 * 监听列数变化，重新分配所有图片
 */
watch(
  () => columnCount.value,
  () => {
    console.log('列数改变，重建瀑布流')
    // 列数改变时，需要完全重建
    lastDataLength.value = 0
    distributeImages()
  }
)

/**
 * 窗口大小变化时重新计算列数
 */
const handleResize = () => {
  calculateColumnCount()
}

// 组件挂载时添加窗口大小变化监听
onMounted(() => {
  calculateColumnCount()
  window.addEventListener('resize', handleResize)
})

// 组件卸载时移除监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索图片处理函数
const doSearch = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑图片处理函数
const doEdit = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除图片数据
const doDelete = async (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props.onReload?.()
  } else {
    message.error('删除失败')
  }
}

// 点赞图片
const doLike = async (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  const pictureId = picture.id
  if (!pictureId) {
    return
  }
  try {
    if (picture.hasLiked) {
      // 取消点赞
      const res = await unlikePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('取消点赞成功')
        picture.hasLiked = false
        picture.likeCount = (picture.likeCount || 1) - 1
      } else {
        message.error('取消点赞失败：' + res.data.message)
      }
    } else {
      // 点赞
      const res = await likePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('点赞成功')
        picture.hasLiked = true
        picture.likeCount = (picture.likeCount || 0) + 1
      } else {
        message.error('点赞失败：' + res.data.message)
      }
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

// 收藏图片
const doFavorite = async (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  const pictureId = picture.id
  if (!pictureId) {
    return
  }
  try {
    if (picture.hasFavorited) {
      // 取消收藏
      const res = await unfavoritePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('取消收藏成功')
        picture.hasFavorited = false
        picture.favoriteCount = (picture.favoriteCount || 1) - 1
      } else {
        message.error('取消收藏失败：' + res.data.message)
      }
    } else {
      // 收藏
      const res = await favoritePictureUsingPost({ pictureId })
      if (res.data.code === 0) {
        message.success('收藏成功')
        picture.hasFavorited = true
        picture.favoriteCount = (picture.favoriteCount || 0) + 1
      } else {
        message.error('收藏失败：' + res.data.message)
      }
    }
  } catch (error: any) {
    message.error('操作失败：' + error.message)
  }
}

// ----- 分享操作 ----

// 分享模态框引用
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>('')
// 分享图片
const doShare = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 使用配置的服务器IP地址生成分享链接，以便手机扫码访问
  shareLink.value = `${SERVER_URL}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
/* 加载状态容器 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  width: 100%;
}

/* 瀑布流容器 */
.waterfall-container {
  display: flex;
  gap: 16px; /* 列之间的间距 */
  align-items: flex-start; /* 顶部对齐 */
}

/* 瀑布流列 */
.waterfall-column {
  flex: 1; /* 平均分配宽度 */
  display: flex;
  flex-direction: column; /* 垂直排列 */
  gap: 16px; /* 图片之间的间距 */
}

/* 图片卡片包装器 */
.picture-card-wrapper {
  border-radius: var(--border-radius-lg);
  overflow: hidden;
  transition: var(--transition-base);
  box-shadow: var(--shadow-sm);
  background: white;
  break-inside: avoid;
}

/* 图片卡片主体 */
.picture-card {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: var(--border-radius-lg) var(--border-radius-lg) 0 0;
  cursor: pointer;
}

/* 图片区域 */
.picture-image {
  width: 100%;
  position: relative;
  overflow: hidden;
}

/* 当使用 padding-bottom 实现宽高比时 */
.picture-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

/* 悬浮遮罩层 */
.picture-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0) 0%,
    rgba(0, 0, 0, 0.2) 50%,
    rgba(0, 0, 0, 0.8) 100%
  );
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

/* 图片标题 */
.picture-title {
  color: white;
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 8px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 标签区域 */
.picture-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 12px;
}

/* 标签样式优化 */
.picture-tags :deep(.ant-tag) {
  margin: 0;
  backdrop-filter: blur(4px);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  font-weight: 500;
}

/* 绿色标签特殊样式 */
.picture-tags :deep(.ant-tag-green) {
  background: rgba(82, 196, 26, 0.4);
  border-color: rgba(82, 196, 26, 0.6);
}

/* 点赞收藏统计和操作区域 */
.picture-stats-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

/* 统计操作项样式 */
.stat-action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  backdrop-filter: blur(8px);
  background: rgba(0, 0, 0, 0.3);
  padding: 6px 12px;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 悬停效果 */
.stat-action-item:hover {
  background: rgba(0, 0, 0, 0.5);
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.3);
}

/* 已点赞样式 */
.stat-action-item.liked {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.2);
  border-color: rgba(255, 77, 79, 0.3);
}

/* 已收藏样式 */
.stat-action-item.favorited {
  color: #faad14;
  background: rgba(250, 173, 20, 0.2);
  border-color: rgba(250, 173, 20, 0.3);
}

/* 底部操作按钮区域 */
.picture-actions-bottom {
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: white;
  border-top: 1px solid #f0f0f0;
}

/* 底部操作按钮样式 */
.action-btn-bottom {
  background: transparent !important;
  border: none !important;
  border-radius: 6px !important;
  transition: all 0.2s ease !important;
  padding: 4px 8px !important;
}

.action-btn-bottom:hover {
  color: var(--primary-color) !important;
  background: rgba(24, 144, 255, 0.1) !important;
  transform: translateY(-1px);
}

/* 卡片悬停效果 */
.picture-card-wrapper:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
}

/* 悬停时显示遮罩层 */
.picture-card-wrapper:hover .picture-overlay {
  opacity: 1;
}

/* 悬停时图片轻微放大 */
.picture-card-wrapper:hover .picture-image img {
  transform: scale(1.1);
}

/* 响应式布局 */
@media (max-width: 576px) {
  .waterfall-container {
    gap: 8px;
  }
  
  .waterfall-column {
    gap: 8px;
  }
}
</style>
