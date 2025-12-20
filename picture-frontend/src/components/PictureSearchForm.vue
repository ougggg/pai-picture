<template>
  <!-- 
    图片搜索表单组件 - PictureSearchForm.vue
    功能：提供多维度图片搜索功能，支持关键词、分类、标签、日期等筛选
    特性：
    - 现代化的搜索栏设计（主搜索框 + 折叠式高级筛选）
    - 玻璃拟态风格
    - 响应式布局
  -->
  <div class="picture-search-form">
    <!-- 搜索头部：关键词搜索 + 操作按钮 -->
    <div class="search-header">
      <a-input
        v-model:value="searchParams.searchText"
        class="search-input"
        placeholder="搜索图片名称或简介..."
        allow-clear
        size="large"
        @pressEnter="doSearch"
      >
        <template #prefix>
          <SearchOutlined style="color: rgba(0, 0, 0, 0.25)" />
        </template>
      </a-input>
      <div class="search-actions">
        <a-button type="primary" size="large" @click="doSearch">搜索</a-button>
        <a-button size="large" @click="doClear">重置</a-button>
        <a-button type="link" @click="toggleCollapse" class="collapse-btn">
          {{ collapsed ? '展开筛选' : '收起筛选' }}
          <component :is="collapsed ? DownOutlined : UpOutlined" />
        </a-button>
      </div>
    </div>

    <!-- 高级筛选区域 -->
    <div v-show="!collapsed" class="advanced-filters">
      <a-form name="searchForm" layout="vertical" :model="searchParams" @finish="doSearch">
        <a-row :gutter="[16, 16]">
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="分类" name="category">
              <a-auto-complete
                v-model:value="searchParams.category"
                placeholder="请输入分类"
                :options="categoryOptions"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="标签" name="tags">
              <a-select
                v-model:value="searchParams.tags"
                mode="tags"
                placeholder="请输入标签"
                :options="tagOptions"
                allow-clear
              />
            </a-form-item>
          </a-col>
          
          <a-col :xs="24" :sm="24" :md="12" :lg="8">
            <a-form-item label="日期范围" name="dateRange">
              <a-range-picker
                style="width: 100%"
                show-time
                v-model:value="dateRange"
                :placeholder="['开始时间', '结束时间']"
                format="YYYY/MM/DD HH:mm:ss"
                :presets="rangePresets"
                @change="onRangeChange"
              />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="名称" name="name">
              <a-input v-model:value="searchParams.name" placeholder="请输入名称" allow-clear />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="简介" name="introduction">
              <a-input v-model:value="searchParams.introduction" placeholder="请输入简介" allow-clear />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :sm="12" :md="8" :lg="6">
            <a-form-item label="格式" name="picFormat">
              <a-input v-model:value="searchParams.picFormat" placeholder="请输入格式" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { SearchOutlined, DownOutlined, UpOutlined } from '@ant-design/icons-vue'

interface Props {
  onSearch?: (searchParams: API.PictureQueryRequest) => void
}

const props = defineProps<Props>()

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({})

// 折叠状态
const collapsed = ref(true)

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}

// 执行搜索数据
const doSearch = () => {
  props.onSearch?.(searchParams)
}

// 标签和分类选项
const categoryOptions = ref<{ value: string; label: string }[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

/**
 * 获取标签和分类选项
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

// 日期范围选择器的值
const dateRange = ref<[]>([])

/**
 * 日期范围变化处理函数
 */
const onRangeChange = (dates: any[], dateStrings: string[]) => {
  if (dates?.length >= 2) {
    searchParams.startEditTime = dates[0].toDate()
    searchParams.endEditTime = dates[1].toDate()
  } else {
    searchParams.startEditTime = undefined
    searchParams.endEditTime = undefined
  }
}

// 时间范围预设
const rangePresets = ref([
  { label: '过去 7 天', value: [dayjs().add(-7, 'd'), dayjs()] },
  { label: '过去 14 天', value: [dayjs().add(-14, 'd'), dayjs()] },
  { label: '过去 30 天', value: [dayjs().add(-30, 'd'), dayjs()] },
  { label: '过去 90 天', value: [dayjs().add(-90, 'd'), dayjs()] },
])

// 重置表单
const doClear = () => {
  Object.keys(searchParams).forEach((key) => {
    const paramKey = key as keyof API.PictureQueryRequest
    searchParams[paramKey] = undefined
  })
  dateRange.value = []
  props.onSearch?.(searchParams)
}
</script>

<style scoped>
.picture-search-form {
  background: white;
  padding: 24px;
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
  transition: var(--transition-base);
}

.picture-search-form:hover {
  box-shadow: var(--shadow-md);
}

.search-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-input {
  flex: 1;
}

.search-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.advanced-filters {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 表单项样式优化 */
:deep(.ant-form-item) {
  margin-bottom: 0;
}

/* 输入框圆角优化 */
:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-picker),
:deep(.ant-btn) {
  border-radius: var(--border-radius-md);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .search-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-actions {
    justify-content: space-between;
  }
}
</style>
