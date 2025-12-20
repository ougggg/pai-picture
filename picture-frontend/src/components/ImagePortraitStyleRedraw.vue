<template>
  <!-- 
    AI风格重绘模态框组件 - ImagePortraitStyleRedraw.vue
    功能：提供AI人物风格重绘功能，支持多种预设风格选择
    特性：
    - 原图预览
    - 多种风格选择（复古漫画、3D童话、二次元等）
    - 风格预览图展示
    - 异步任务创建和状态轮询
  -->
  <a-modal
    class="image-portrait-style-redraw"
    v-model:visible="visible"
    title="✨ AI风格重绘"
    :footer="false"
    width="900px"
    @cancel="closeModal"
  >
    <!-- 使用栅格系统布局 -->
    <a-row :gutter="16">
      <!-- 左侧列：原始图片 -->
      <a-col :span="12">
        <h4>原始图片</h4>
        <div class="image-preview">
          <img :src="picture?.url" :alt="picture?.name" />
        </div>
      </a-col>
      <!-- 右侧列：风格选择 -->
      <a-col :span="12">
        <h4>选择重绘风格</h4>
        <div class="style-selection">
          <!-- 风格选项网格 -->
          <a-row :gutter="[8, 8]">
            <a-col
              v-for="style in styleOptions"
              :key="style.index"
              :span="6"
            >
              <div
                class="style-item"
                :class="{ active: selectedStyleIndex === style.index }"
                @click="selectStyle(style.index)"
              >
                <div class="style-preview">
                  <img
                    v-if="style.previewUrl"
                    :src="style.previewUrl"
                    :alt="style.name"
                  />
                  <div v-else class="style-placeholder">
                    {{ style.name }}
                  </div>
                </div>
                <div class="style-name">{{ style.name }}</div>
              </div>
            </a-col>
          </a-row>
        </div>
      </a-col>
    </a-row>

    <div style="margin-top: 24px" />

    <!-- 操作按钮 -->
    <a-flex justify="center" gap="16">
      <a-button
        type="primary"
        :loading="!!taskId"
        :disabled="selectedStyleIndex === null"
        @click="createTask"
      >
        立即重绘
      </a-button>
      <a-button v-if="resultImageUrl" type="primary" :loading="uploadLoading" @click="handleUpload">
        应用结果
      </a-button>
    </a-flex>

    <!-- 结果预览（如果有结果） -->
    <div v-if="resultImageUrl" style="margin-top: 24px">
      <h4>重绘结果</h4>
      <div class="image-preview">
        <img :src="resultImageUrl" :alt="picture?.name" />
      </div>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import {
  createPicturePortraitStyleRedrawTaskUsingPost,
  getPicturePortraitStyleRedrawTaskUsingGet,
  uploadPictureByUrlUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

/**
 * 风格选项配置
 */
interface StyleOption {
  index: number
  name: string
  previewUrl?: string
}

const styleOptions = ref<StyleOption[]>([
  { index: 0, name: '复古漫画' },
  { index: 1, name: '3D童话' },
  { index: 2, name: '二次元' },
  { index: 3, name: '小清新' },
  { index: 4, name: '未来科技' },
  { index: 5, name: '国画古风' },
  { index: 6, name: '将军百战' },
  { index: 7, name: '炫彩卡通' },
  { index: 8, name: '清雅国风' },
  { index: 9, name: '喜迎新年' },
  { index: 14, name: '国风工笔' },
  { index: 15, name: '恭贺新禧' },
  { index: 30, name: '童话世界' },
  { index: 31, name: '黏土世界' },
  { index: 32, name: '像素世界' },
  { index: 33, name: '冒险世界' },
  { index: 34, name: '日漫世界' },
  { index: 35, name: '3D世界' },
  { index: 36, name: '二次元世界' },
  { index: 37, name: '手绘世界' },
  { index: 38, name: '蜡笔世界' },
  { index: 39, name: '冰箱贴世界' },
  { index: 40, name: '吧唧世界' },
])

/**
 * 选中的风格索引
 */
const selectedStyleIndex = ref<number | null>(null)

/**
 * 重绘结果图片URL
 */
const resultImageUrl = ref<string>('')

/**
 * 任务ID
 */
const taskId = ref<string>()

/**
 * 选择风格
 */
const selectStyle = (index: number) => {
  selectedStyleIndex.value = index
}

/**
 * 创建风格重绘任务
 */
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  if (selectedStyleIndex.value === null) {
    message.warning('请先选择风格')
    return
  }

  try {
    const res = await createPicturePortraitStyleRedrawTaskUsingPost({
      pictureId: props.picture.id,
      styleIndex: selectedStyleIndex.value,
    })

    if (res.data.code === 0 && res.data.data?.output) {
      message.success('创建任务成功，请耐心等待，不要退出界面')
      taskId.value = res.data.data.output.taskId || undefined
      if (taskId.value) {
        startPolling()
      }
    } else {
      message.error('创建任务失败，' + res.data.message)
    }
  } catch (error: any) {
    console.error('创建风格重绘任务失败', error)
    message.error('创建任务失败，' + error.message)
  }
}

// 轮询定时器
let pollingTimer: ReturnType<typeof setInterval> | null = null

/**
 * 开始轮询查询任务状态
 */
const startPolling = () => {
  if (!taskId.value) {
    return
  }

  pollingTimer = setInterval(async () => {
    try {
      const res = await getPicturePortraitStyleRedrawTaskUsingGet({
        taskId: taskId.value!,
      })
      if (res.data.code === 0 && res.data.data?.output) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('风格重绘任务执行成功')
          // 从results数组中获取第一张图片的URL
          if (taskResult.results && taskResult.results.length > 0 && taskResult.results[0]?.url) {
            resultImageUrl.value = taskResult.results[0].url
          }
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('风格重绘任务执行失败')
          clearPolling()
        }
      }
    } catch (error: any) {
      console.error('风格重绘任务轮询失败', error)
      message.error('任务轮询失败，' + error.message)
      clearPolling()
    }
  }, 3000) // 每3秒轮询一次
}

/**
 * 清理轮询定时器
 */
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = undefined
  }
}

// 是否正在上传
const uploadLoading = ref(false)

/**
 * 处理结果图片上传
 */
const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      props.onSuccess?.(res.data.data)
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error: any) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  uploadLoading.value = false
}

// 是否可见
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
  selectedStyleIndex.value = null
  resultImageUrl.value = ''
  taskId.value = undefined
  clearPolling()
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
  clearPolling()
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<style scoped>
.image-portrait-style-redraw {
  text-align: center;
}

.image-preview {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
  background: #fafafa;
}

.image-preview img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.style-selection {
  max-height: 400px;
  overflow-y: auto;
}

.style-item {
  cursor: pointer;
  padding: 8px;
  border: 2px solid #d9d9d9;
  border-radius: 4px;
  transition: all 0.3s;
  background: #fff;
}

.style-item:hover {
  border-color: #1890ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.style-item.active {
  border-color: #1890ff;
  background: #e6f7ff;
}

.style-preview {
  width: 100%;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 2px;
  margin-bottom: 4px;
  overflow: hidden;
}

.style-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.style-placeholder {
  font-size: 12px;
  color: #999;
  text-align: center;
  padding: 8px;
}

.style-name {
  font-size: 12px;
  text-align: center;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

h4 {
  margin-bottom: 12px;
  font-weight: 500;
}
</style>

