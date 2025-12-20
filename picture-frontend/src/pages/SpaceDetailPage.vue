<template>
  <!-- 
    空间详情页面 - SpaceDetailPage.vue
    功能：展示空间详情和图片列表，提供空间管理和图片操作功能
    特性：
    - 空间信息展示（名称、存储使用情况）
    - 图片搜索和筛选功能
    - 按颜色搜索图片
    - 批量编辑图片功能
    - 无限滚动瀑布流展示图片列表
  -->
  <div id="spaceDetailPage">
    <!-- 空间信息头部区域 -->
    <a-flex justify="space-between">

      <h2>{{ space.spaceName }}（私有空间）</h2>

      <a-space size="middle">
        <!-- 操作按钮区域 -->
        <a-button
          type="primary"
          :href="`/ai_picture?spaceId=${id}`"
          target="_blank"
        >
          AI 生图
        </a-button>

        <a-button
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
          target="_blank"
        >
          + 创建图片
        </a-button>

        <a-button

          type="primary"
          ghost
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
          target="_blank"
        >
          空间分析
        </a-button>

        <a-button
        :icon="h(EditOutlined)"
        @click="doBatchEdit">
        批量编辑
        </a-button>

        <a-tooltip
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            type="circle"
            :size="42"
            :percent="Number((((space.totalSize ?? 0) * 100) / (space.maxSize ?? 1)).toFixed(1))"
          />
        </a-tooltip>

      </a-space>
    </a-flex>

    <div style="margin-bottom: 16px" />

    <!-- 图片搜索表单组件 -->
    <PictureSearchForm :onSearch="onSearch" />

    <div style="margin-bottom: 16px" />

    <!-- 按颜色搜索，跟其他搜索条件独立 -->
    <a-form-item label="按颜色搜索">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>

    <!-- 图片列表 -->
    <PictureList
      :dataList="dataList"
      :loading="loading"
      :showOp="true"
      :onReload="fetchData"
    />

    <!-- 加载更多提示（无限滚动） -->
    <div v-if="hasMore" class="load-more-trigger" ref="loadMoreRef">
      <a-spin v-show="loading" size="small" />
      <span v-show="!loading" class="load-more-text">向下滚动加载更多</span>
    </div>

    <!-- 没有更多数据提示 -->
    <div v-if="!hasMore && dataList.length > 0" class="no-more-data">
      <a-divider>已加载全部图片（共 {{ dataList.length }} 张 / {{ space.maxCount }} 张）</a-divider>
    </div>

    <!-- 批量编辑图片模态框（隐藏式） -->
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="(id as any)"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />

  </div>
</template>

<script setup lang="ts">
import {  h, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import {
  listPictureVoByPageUsingPost,
  searchPictureByColorUsingPost,
} from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue'
import { BarChartOutlined, EditOutlined } from '@ant-design/icons-vue'


// 定义组件属性接口
interface Props {
  id: string | number// 空间ID
}



// 接收父组件传递的属性
const props = defineProps<Props>()
// 空间详情数据
const space = ref<API.SpaceVO>({})

// -------- 获取空间详情 --------

/**
 * 获取空间详情的异步函数
 */
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id as any,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}


// --------- 获取图片列表 --------

// 定义数据
const dataList = ref<API.PictureVO[]>([]) //图片数据列表
const total = ref(0)
const loading = ref(false) // 加载状态
const hasMore = ref(true) // 是否还有更多数据
const loadMoreRef = ref<HTMLElement>() // 加载更多触发器引用

/**
 * 搜索条件对象
 * 包含分页参数和搜索筛选条件
 */
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 30, // 每次请求30条数据
  sortField: 'createTime',
  sortOrder: 'descend',
})

/**
 * 获取图片数据的异步函数
 * @param append 是否追加数据（true=追加，false=替换）
 */
const fetchData = async (append = false) => {
  // 如果正在加载且是追加模式，则不重复请求
  if (loading.value && append) {
    return
  }

  // 保存当前滚动位置（用于追加数据时保持位置）
  const scrollY = append ? window.scrollY : 0

  loading.value = true

  // 转换搜索参数
  const params = {
    spaceId: props.id as any,
    ...searchParams.value,
  }

  try {
    const res = await listPictureVoByPageUsingPost(params)
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
      
      console.log('数据加载完成:', {
        当前页: searchParams.value.current,
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
    loading.value = false
  }
}

/**
 * 加载下一页数据
 */
const loadMore = async () => {
  if (!hasMore.value || loading.value) {
    return
  }
  
  if (searchParams.value.current) {
    searchParams.value.current += 1
  }
  
  await fetchData(true)
}

/**
 * 创建 Intersection Observer 监听滚动到底部
 */
let observer: IntersectionObserver | null = null

const setupInfiniteScroll = () => {
  if (!loadMoreRef.value) {
    setTimeout(() => {
      setupInfiniteScroll()
    }, 100)
    return
  }

  // 清理旧的 observer
  if (observer) {
    observer.disconnect()
  }

  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && hasMore.value && !loading.value) {
        loadMore()
      }
    },
    {
      rootMargin: '200px', // 提前200px开始加载，提升用户体验
      threshold: 0.1, // 当10%可见时触发
    }
  )

  observer.observe(loadMoreRef.value)
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchSpaceDetail()
  fetchData()
  // 等待DOM渲染完成后设置监听
  nextTick(() => {
    setupInfiniteScroll()
  })
})

// 监听 hasMore 变化，确保触发器渲染后设置监听
watch(hasMore, (newValue) => {
  if (newValue) {
    nextTick(() => {
      setupInfiniteScroll()
    })
  }
})

// 监听数据加载完成，重新设置监听器
watch(loading, (newValue) => {
  if (!newValue && hasMore.value) {
    // 加载完成且还有更多数据时，确保监听器正常工作
    nextTick(() => {
      setupInfiniteScroll()
    })
  }
})

// 组件卸载时清理 observer
onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})


// 搜索处理
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  // 重置搜索条件
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  
  // 重置数据状态
  dataList.value = []
  hasMore.value = true
  
  fetchData(false)
  
  // 重新设置监听（因为DOM可能已更新）
  nextTick(() => {
    if (observer) {
      observer.disconnect()
    }
    setupInfiniteScroll()
  })
}

// 按照颜色搜索处理函数
const onColorChange = async (color: string) => {
  // 颜色搜索是独立功能，不使用无限滚动
  loading.value = true
  
  try {
    const res = await searchPictureByColorUsingPost({
      picColor: color,
      spaceId: props.id as any,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data ?? []
      dataList.value = data
      total.value = data.length
      hasMore.value = false // 颜色搜索不使用无限滚动
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('颜色搜索失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// ---- 批量编辑图片 -----

// 批量编辑模态框引用
const batchEditPictureModalRef = ref()

// 批量编辑图片成功回调
const onBatchEditPictureSuccess = () => {
  // 重新加载数据，从第一页开始
  searchParams.value.current = 1
  dataList.value = []
  hasMore.value = true
  fetchData(false)
  
  // 重新设置监听（因为DOM可能已更新）
  nextTick(() => {
    if (observer) {
      observer.disconnect()
    }
    setupInfiniteScroll()
  })
}

// 打开批量编辑模态框
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

</script>

<style scoped>
/* 页面容器样式 */
#spaceDetailPage {
  margin-bottom: 16px;  /* 底部间距 */
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
