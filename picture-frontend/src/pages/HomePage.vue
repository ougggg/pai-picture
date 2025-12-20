<template>
  <!-- 
    首页 - HomePage.vue
    功能：展示全站图片，提供搜索和筛选功能
    特性：
    - 全站图片展示
    - 关键词搜索功能
    - 分类和标签筛选
    - 分页显示
    - 响应式布局
  -->
  <div id="homePage">
    <!-- Ant Design的搜索输入框组件 -->
    <div class="search-bar">
      <a-input-search
        v-model:value="searchParams.searchText"
        placeholder="从海量图片中搜索"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>

    <!-- 分类和标签筛选 -->
    <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
    </a-tabs>

    <!-- 标签筛选区域 -->
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="(tag, index) in tagList"
          :key="tag"
          v-model:checked="selectedTagList[index]"
          @change="doSearch"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>

    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" />

    <!-- 加载更多提示（保持在DOM中以便 IntersectionObserver 监听） -->
    <div v-if="hasMore" class="load-more-trigger" ref="loadMoreRef">
      <a-spin v-show="loading" size="small" />
      <span v-show="!loading" class="load-more-text">向下滚动加载更多</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && dataList.length > 0" class="no-more-data">
      <a-divider>已加载全部图片</a-divider>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, nextTick, watch } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageWithCacheUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue'
import { useRoute } from 'vue-router'

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)

/**
 * 加载状态，控制加载中的显示
 * 初始值为false
 */
const loading = ref(false)

// 是否还有更多数据
const hasMore = ref(true)

// 加载触发器引用
const loadMoreRef = ref<HTMLElement>()

// 首次填充标志
let checkScheduled = false
let initialFillCompleted = false

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 30, // 每次请求30条数据
  sortField: 'createTime',
  sortOrder: 'descend',
})

/**
 * 获取图片数据的异步函数
 * 负责向后台请求分页图片数据并更新界面
 * @param append 是否追加数据（true=追加，false=替换）
 */
const fetchData = async (append = false) => {
  // 如果正在加载，则不重复请求（但首次加载除外）
  if (loading.value && append) {
    return
  }

  // 保存当前滚动位置（用于追加数据时保持位置）
  const scrollY = append ? window.scrollY : 0

  loading.value = true // 开始加载
  
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  
  try {
    const res = await listPictureVoByPageWithCacheUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      const newRecords = res.data.data.records ?? []
      
      // 追加或替换数据
      if (append) {
        // 使用 push 增量追加，避免创建新数组引用
        console.log(`📥 追加 ${newRecords.length} 条新数据到现有 ${dataList.value.length} 条`)
        dataList.value.push(...newRecords)
        
        // 等待DOM更新后恢复滚动位置
        nextTick(() => {
          // 如果滚动位置发生了变化（被重置），则恢复原位置
          if (Math.abs(window.scrollY - scrollY) > 10) {
            console.log(`🔄 恢复滚动位置: ${scrollY}`)
            window.scrollTo(0, scrollY)
          }
        })
      } else {
        // 替换数据（搜索/筛选时）
        console.log(`🔄 替换数据，新数据量: ${newRecords.length}`)
        dataList.value = newRecords
        // 搜索时回到顶部是正常的
        window.scrollTo(0, 0)
      }
      
      total.value = res.data.data.total ?? 0
      
      // 判断是否还有更多数据
      hasMore.value = dataList.value.length < total.value
      
      // 调试信息
      console.log('数据加载完成:', {
        当前页: searchParams.current,
        本次加载数量: newRecords.length,
        已加载总数: dataList.value.length,
        数据总数: total.value,
        是否还有更多: hasMore.value
      })
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('请求失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false // 结束加载
  }
}

/**
 * 加载下一页数据
 */
const loadMore = async () => {
  console.log('触发加载更多:', {
    hasMore: hasMore.value,
    loading: loading.value,
    current: searchParams.current
  })
  
  if (!hasMore.value || loading.value) {
    console.log('跳过加载: hasMore=', hasMore.value, 'loading=', loading.value)
    return
  }
  
  if (searchParams.current) {
    searchParams.current += 1
  }
  
  console.log('开始加载第', searchParams.current, '页')
  await fetchData(true)
}

/**
 * 创建 Intersection Observer 监听滚动到底部
 */
let observer: IntersectionObserver | null = null
let setupAttempts = 0
const MAX_SETUP_ATTEMPTS = 10

const setupInfiniteScroll = () => {
  console.log('设置无限滚动监听, loadMoreRef:', loadMoreRef.value)
  
  if (!loadMoreRef.value) {
    setupAttempts++
    if (setupAttempts < MAX_SETUP_ATTEMPTS) {
      console.log(`loadMoreRef 不存在，延迟重试 (${setupAttempts}/${MAX_SETUP_ATTEMPTS})`)
      setTimeout(() => {
        setupInfiniteScroll()
      }, 100)
    } else {
      console.warn('setupInfiniteScroll 达到最大重试次数，停止重试')
      setupAttempts = 0
    }
    return
  }

  // 重置重试计数
  setupAttempts = 0

  // 清理旧的 observer
  if (observer) {
    observer.disconnect()
  }

  observer = new IntersectionObserver(
    (entries) => {
      console.log('IntersectionObserver 触发:', {
        isIntersecting: entries[0].isIntersecting,
        hasMore: hasMore.value,
        loading: loading.value
      })
      
      // 当触发器元素进入视口时，加载更多数据
      if (entries[0].isIntersecting && hasMore.value && !loading.value) {
        console.log('✅ 满足条件，开始加载更多')
        loadMore()
      }
    },
    {
      rootMargin: '200px', // 提前200px开始加载，增加预加载距离
      threshold: 0.1, // 当10%可见时触发
    }
  )

  observer.observe(loadMoreRef.value)
  console.log('✅ IntersectionObserver 已设置')
}

/**
 * 检查内容高度是否填满屏幕（防抖版本）
 * 如果未填满且还有更多数据，自动加载下一页
 */
const checkAndFillScreen = async () => {
  // 防抖：如果已经有检查计划，跳过
  if (checkScheduled) {
    return
  }
  
  checkScheduled = true
  
  try {
    // 等待一小段时间，确保图片开始加载
    await new Promise(resolve => setTimeout(resolve, 500))
    
    const viewportHeight = window.innerHeight
    const documentHeight = document.documentElement.scrollHeight
    
    console.log('🔍 检查屏幕填充:', {
      差值: documentHeight - viewportHeight,
      hasMore: hasMore.value,
      loading: loading.value
    })
    
    // 如果内容高度小于视口高度+200px，且还有更多数据，继续加载
    if (documentHeight < viewportHeight + 200 && hasMore.value && !loading.value) {
      console.log('⚠️ 内容未填满，继续加载')
      checkScheduled = false // 重置标志，允许递归
      await loadMore()
      // 递归检查，直到填满屏幕或没有更多数据
      await checkAndFillScreen()
    } else {
      console.log('✅ 填充完成')
      // 标记首次填充已完成
      initialFillCompleted = true
    }
  } finally {
    checkScheduled = false
  }
}

/**
 * 延迟检查并填充屏幕（仅执行一次）
 */
const delayedCheckAndFill = () => {
  if (initialFillCompleted) {
    return
  }
  
  console.log('⏳ 延迟检查屏幕填充')
  setTimeout(() => {
    if (!initialFillCompleted) {
      checkAndFillScreen()
    }
  }, 1000) // 等待1秒，让图片有时间开始加载
}

// AI标签识别轮询相关
const route = useRoute()
let aiPollingTimer: number | null = null
const AI_POLLING_INTERVAL = 3000 // 每3秒轮询一次
const AI_POLLING_TIMEOUT = 60000 // 60秒后停止轮询
let aiPollingStartTime = 0

/**
 * 检查AI标签识别是否完成
 * 检查当前列表中所有没有标签的图片，看是否获得了标签
 */
const checkAITagRecognition = async () => {
  try {
    // 找出当前列表中所有没有标签的图片ID
    const picturesWithoutTags = dataList.value.filter(
      item => !item.tags || item.tags.length === 0
    )
    
    if (picturesWithoutTags.length === 0) {
      // 所有图片都有标签了，停止轮询
      stopAIPolling()
      return
    }
    
    // 重新获取当前页数据，检查这些图片的标签是否已更新
    const params = {
      ...searchParams,
      tags: [] as string[],
    }
    if (selectedCategory.value !== 'all') {
      params.category = selectedCategory.value
    }
    
    const res = await listPictureVoByPageWithCacheUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      const newRecords = res.data.data.records ?? []
      
      // 创建新数据的映射
      const newDataMap = new Map(newRecords.map(item => [item.id, item]))
      const currentDataMap = new Map(dataList.value.map(item => [item.id, item]))
      
      // 检查是否有图片的标签已更新
      let hasUpdated = false
      for (const pictureWithoutTag of picturesWithoutTags) {
        const newRecord = newDataMap.get(pictureWithoutTag.id)
        if (newRecord && newRecord.tags && newRecord.tags.length > 0) {
          // 找到对应的旧记录并更新
          const oldRecord = currentDataMap.get(pictureWithoutTag.id)
          if (oldRecord) {
            oldRecord.tags = newRecord.tags
            oldRecord.category = newRecord.category || oldRecord.category
            hasUpdated = true
          }
        }
      }
      
      if (hasUpdated) {
        console.log('✅ 检测到AI标签识别完成，已自动更新图片标签')
        // 检查是否还有图片没有标签
        const stillWithoutTags = dataList.value.filter(
          item => !item.tags || item.tags.length === 0
        )
        if (stillWithoutTags.length === 0) {
          // 所有图片都有标签了，停止轮询
          stopAIPolling()
          message.success('AI标签识别完成', 2)
        }
      }
    }
  } catch (error) {
    console.error('检查AI标签识别失败:', error)
  }
}

/**
 * 启动AI标签识别轮询
 */
const startAIPolling = () => {
  // 检查是否有批量上传标记
  const batchUploadTime = sessionStorage.getItem('batchUploadTime')
  if (!batchUploadTime) {
    return // 没有批量上传，不需要轮询
  }
  
  // 检查是否超时
  const uploadTime = parseInt(batchUploadTime)
  const now = Date.now()
  if (now - uploadTime > AI_POLLING_TIMEOUT) {
    sessionStorage.removeItem('batchUploadTime')
    sessionStorage.removeItem('batchUploadCount')
    return // 已超时，停止轮询
  }
  
  aiPollingStartTime = uploadTime // 使用上传时间作为起始时间
  console.log('🔄 启动AI标签识别轮询')
  
  // 延迟一下再开始检查，给后端一些时间处理
  setTimeout(() => {
    checkAITagRecognition()
  }, 2000)
  
  // 设置定时轮询
  aiPollingTimer = window.setInterval(() => {
    // 检查是否超时
    if (Date.now() - aiPollingStartTime > AI_POLLING_TIMEOUT) {
      stopAIPolling()
      return
    }
    
    checkAITagRecognition()
  }, AI_POLLING_INTERVAL)
}

/**
 * 停止AI标签识别轮询
 */
const stopAIPolling = () => {
  if (aiPollingTimer) {
    clearInterval(aiPollingTimer)
    aiPollingTimer = null
    sessionStorage.removeItem('batchUploadTime')
    sessionStorage.removeItem('batchUploadCount')
    console.log('⏹️ 停止AI标签识别轮询')
  }
}

// 页面加载时获取数据
onMounted(() => {
  fetchData()
  // 等待DOM渲染完成后设置监听（只设置一次）
  nextTick(() => {
    setupInfiniteScroll()
    delayedCheckAndFill()
    // 检查是否需要启动AI标签识别轮询
    startAIPolling()
  })
})

// 组件卸载时清理 observer 和轮询
onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
  stopAIPolling()
})

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  dataList.value = [] // 清空现有数据
  hasMore.value = true // 重置加载状态
  initialFillCompleted = false // 重置首次填充标志
  fetchData(false)
  
  // 重新设置监听（因为DOM可能已更新）
  nextTick(() => {
    if (observer) {
      observer.disconnect()
    }
    setupInfiniteScroll()
    // 重新检查屏幕填充
    delayedCheckAndFill()
  })
}

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

/**
 * 获取标签和分类选项的异步函数
 * 从后端API获取所有可用的标签和分类列表
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
/* 首页容器样式 */
#homePage {
  margin-bottom: 16px;  /* 设置底部外边距为16像素 */
}

/* 搜索框区域样式 */
#homePage .search-bar {
  max-width: 480px;     /* 设置最大宽度为480像素 */
  margin: 0 auto 16px;  /* 设置外边距：上下为0，左右自动（居中），底部为16像素 */
}

/* 标签筛选区域样式 */
#homePage .tag-bar {
  margin-bottom: 16px;  /* 设置底部外边距为16像素 */
}

/* 加载更多触发器样式 */
.load-more-trigger {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 32px 0;
  color: #999;
  min-height: 80px; /* 固定高度，防止内容切换时抖动 */
}

.load-more-text {
  margin-left: 8px;
  transition: opacity 0.3s ease;
}

/* 没有更多数据提示样式 */
.no-more-data {
  padding: 32px 0;
  text-align: center;
  color: #999;
}
</style>
