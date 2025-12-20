<template>
  <!-- 
    AI扩图模态框组件 - ImageOutPainting.vue
    功能：提供AI图片扩展功能，支持图片尺寸检查和任务轮询
    特性：
    - 图片尺寸验证（512x512 到 4096x4096）
    - 异步任务创建和状态轮询
    - 扩图结果预览和应用
    - 支持URL上传扩图结果
  -->
  <a-modal
    class="image-out-painting"
    v-model:visible="visible"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
  >
  <!-- 使用栅格系统布局 -->
    <a-row gutter="16">
      <!-- 左侧列：原始图片 -->
      <a-col span="12">
        <h4>原始图片</h4>
        <img :src="picture?.url" :alt="picture?.name" style="max-width: 100%" />
      </a-col>
      <!-- 右侧列：扩图结果 -->
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          :alt="picture?.name"
          style="max-width: 100%"
        />
      </a-col>
    </a-row>

    <div style="margin-bottom: 16px" />

    <a-flex justify="center" gap="16">
      <a-button type="primary"
      :loading="!!taskId"
      ghost
      @click="createTask">
      生成图片
      </a-button>


      <a-button v-if="resultImageUrl"
      type="primary"
      :loading="uploadLoading"
      @click="handleUpload">
        应用结果
      </a-button>
    </a-flex>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
// 导入API接口
import {
  createPictureOutPaintingTaskUsingPost,  // 创建AI扩图任务
  getPictureOutPaintingTaskUsingGet,       // 获取AI扩图任务状态
  uploadPictureByUrlUsingPost,              // 通过URL上传图片
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

/**
 * 扩图结果图片URL
 * 存储AI扩图生成的结果图片地址
 */
const resultImageUrl = ref<string>('')

// 任务 id
const taskId = ref<string>()

/**
 * 创建AI扩图任务
 * 调用后端API创建图片扩展任务
 */
const createTask = async () => {
  if (!props.picture?.id) {
    return
  }
  
  // 再次检查图片尺寸，确保符合要求
  const isValidSize = await checkImageDimensions()
  if (!isValidSize) {
    closeModal()
    return
  }
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    // 根据需要设置扩图参数
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  })
  // 处理API响应
  //前端进行轮询，询问后端结果
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    console.log(res.data.data.output.taskId)  // 保存任务ID
    taskId.value = res.data.data.output.taskId  // 开始轮询查询任务状态
    // 开启轮询
    startPolling()
  } else {
    message.error('图片任务失败，' + res.data.message)
  }
}

// 轮询定时器
let pollingTimer: NodeJS.Timeout = null

/**
 * 开始轮询查询任务状态
 * 定时查询AI扩图任务的执行状态
 */
const startPolling = () => {
  if (!taskId.value) {
    return
  }

  pollingTimer = setInterval(async () => {
    try {
      const res = await getPictureOutPaintingTaskUsingGet({
        taskId: taskId.value,
      })
      if (res.data.code === 0 && res.data.data) {
        const taskResult = res.data.data.output
        if (taskResult.taskStatus === 'SUCCEEDED') {
          message.success('扩图任务执行成功')
          resultImageUrl.value = taskResult.outputImageUrl
          // 清理轮询
          clearPolling()
        } else if (taskResult.taskStatus === 'FAILED') {
          message.error('扩图任务执行失败')
          // 清理轮询
          clearPolling()
        }
      }
    } catch (error) {
      console.error('扩图任务轮询失败', error)
      message.error('扩图任务轮询失败，' + error.message)
      // 清理轮询
      clearPolling()
    }
  }, 3000) // 每 3 秒轮询一次
}

/**
 * 清理轮询定时器
 * 停止轮询并重置任务ID
 */
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    taskId.value = null
  }
}

// 是否正在上传
const uploadLoading = ref(false)

/**
 * 处理结果图片上传
 * 将AI扩图结果保存到系统
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
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      // 关闭弹窗
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  uploadLoading.value = false
}

// 是否可见
const visible = ref(false)

/**
 * 检查图片尺寸是否符合AI扩图要求
 * 要求：分辨率不低于512*512且不大于4096*4096，单边长度范围【512，4096】
 * @returns {Promise<boolean>} 是否符合要求
 */
const checkImageDimensions = () => {
  return new Promise<boolean>((resolve) => {
    if (!props.picture?.url) {
      message.error('图片不存在')
      resolve(false)
      return
    }

    const img = new Image()
    img.onload = () => {
      const width = img.width
      const height = img.height
      
      // 检查图片尺寸是否在有效范围内
      if (width < 512 || height < 512) {
        message.error('图片尺寸过小，AI扩图要求图片分辨率不低于512*512')
        resolve(false)
      } else if (width > 4096 || height > 4096) {
        message.error('图片尺寸过大，AI扩图要求图片分辨率不大于4096*4096')
        resolve(false)
      } else {
        resolve(true)
      }
    }
    
    img.onerror = () => {
      message.error('图片加载失败')
      resolve(false)
    }
    
    img.src = props.picture.url
  })
}

// 打开弹窗
const openModal = async () => {
  // 先检查图片尺寸是否符合要求
  const isValidSize = await checkImageDimensions()
  if (isValidSize) {
    visible.value = true
  }
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>

<style>
/* 组件样式 */
.image-out-painting {
  text-align: center;  /* 文本居中 */
}
</style>
