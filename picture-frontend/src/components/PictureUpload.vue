<template>
  <!-- 
    图片上传组件 - PictureUpload.vue
    功能：提供图片文件上传功能，支持拖拽上传和格式验证
    特性：
    - 支持拖拽上传和点击上传
    - 文件格式验证（jpg、jpeg、png、webp）
    - 文件大小限制（最大2MB）
    - 上传进度显示和状态管理
  -->
  <div class="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
    >
      <!-- 已上传图片的显示 -->
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />

      <!-- 未上传时的提示区域 -->
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">
          {{ loading ? 'AI智能识别中...' : '点击或拖拽上传图片' }}
        </div>
      </div>

    </a-upload>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

// 接收父组件传递的属性
const props = defineProps<Props>()

/**
 * 处理图片上传
 * @param file
 */
const handleUpload = async ({ file }: any) => {
  loading.value = true
  try {
    const params: API.PictureUploadRequest = props.picture ? { id: props.picture.id } : {}
    params.spaceId = props.spaceId;
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  loading.value = false
}

/**
 * 加载状态
 * 控制上传过程中的加载图标显示
 */
const loading = ref<boolean>(false)

/**
 * 上传前校验函数
 * 在上传前对文件进行格式和大小校验
 * @param file
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  // 校验图片格式
  const isValidFormat = file.type === 'image/jpeg' || file.type === 'image/png' || 
                        file.type === 'image/webp' || file.type === 'image/jpg'
  if (!isValidFormat) {
    message.error('不支持上传该格式的图片，推荐 jpg、jpeg、png 或 webp')
  }
  // 校验图片大小
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('不能上传超过 2M 的图片')
  }
  return isValidFormat && isLt2M
}
</script>

<style scoped>
/* 上传组件样式调整 */
.picture-upload :deep(.ant-upload) {
  width: 100% !important;      /* 宽度100% */
  height: 100% !important;     /* 高度100% */
  min-width: 152px;           /* 最小宽度 */
  min-height: 152px;          /* 最小高度 */
}

/* 已上传图片的样式 */
.picture-upload img {
  max-width: 100%;            /* 最大宽度100% */
  max-height: 480px;         /* 最大高度限制 */
}

/* 上传图标样式 */
.ant-upload-select-picture-card i {
  font-size: 32px;           /* 图标大小 */
  color: #999;               /* 图标颜色 */
}

/* 上传提示文字样式 */
.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;           /* 上边距 */
  color: #666;              /* 文字颜色 */
}
</style>
