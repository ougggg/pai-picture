<template>
  <!-- 
    URL图片上传组件 - UrlPictureUpload.vue
    功能：通过图片URL地址上传图片到系统
    特性：
    - 支持通过URL地址上传图片
    - 图片预览功能
    - URL格式验证
    - 上传状态管理
  -->
  <div class="url-picture-upload">
    <a-input-group compact>
      <!-- URL输入框 -->
      <a-input v-model:value="fileUrl"
      style="width: calc(100% - 120px)"
      placeholder="请输入图片地址" />
      <!-- 提交按钮 -->
      <a-button type="primary"
      style="width: 120px"
      :loading="loading"
      @click="handleUpload">
        {{ loading ? 'AI识别中' : '提交' }}
      </a-button>
     <!-- 图片预览区域 -->
    </a-input-group>
    <div class="img-wrapper">
      <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
    </div>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { uploadPictureByUrlUsingPost } from '@/api/pictureController.ts'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()
const fileUrl = ref<string>()    //图片URL地址
const loading = ref<boolean>(false)

/**
 * 处理URL图片上传
 * 通过图片URL地址上传图片到服务器
 */
const handleUpload = async () => {
   // 验证URL是否为空
  if (!fileUrl.value) {
    message.error('请输入图片地址')
    return
  }

  loading.value = true

  try {
    const params: API.PictureUploadRequest = { fileUrl: fileUrl.value }
    params.spaceId = props.spaceId;

    // 如果是编辑现有图片，设置图片ID
    if (props.picture) {
      params.id = props.picture.id
    }

    // 调用URL图片上传API
    const res = await uploadPictureByUrlUsingPost(params)
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
</script>

<style scoped>
/* 组件容器样式 */
.url-picture-upload {
  margin-bottom: 16px;  /* 底部间距 */
}

/* 图片预览样式 */
.url-picture-upload img {
  max-width: 100%;      /* 最大宽度100% */
  max-height: 480px;   /* 最大高度限制 */
}

/* 图片包装容器样式 */
.url-picture-upload .img-wrapper {
  text-align: center;   /* 居中显示 */
  margin-top: 16px;     /* 顶部间距 */
}
</style>
