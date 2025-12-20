<template>
  <!-- 
    AI文生图页面 - AiPicturePage.vue
    功能：通过AI生成图片，支持预览和保存到空间
    特性：
    - AI文生图功能
    - 图片预览
    - 保存到指定空间
    - 提示词输入和生成状态管理
  -->
  <div id="aiPicturePage">
    <!-- 页面标题 -->
    <h2 style="margin-bottom: 16px">AI 文生图</h2>

    <!-- 空间信息提示，仅在spaceId存在时显示 -->
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>

    <!-- 提示词输入区域 -->
    <a-card title="输入提示词" style="margin-bottom: 16px">
      <a-textarea
        v-model:value="promptText"
        placeholder="请输入您想要生成的图片描述，例如：一只可爱的小猫在花园里玩耍"
        :auto-size="{ minRows: 3, maxRows: 6 }"
        :maxlength="500"
        show-count
      />
      <template #extra>
        <a-button 
          type="primary" 
          :loading="generating"
          :disabled="!promptText.trim()"
          @click="generatePicture"
        >
          {{ generating ? '生成中...' : '生成图片' }}
        </a-button>
      </template>
    </a-card>

    <!-- 生成结果区域 -->
    <a-card v-if="generatedImageUrl" title="生成结果" style="margin-bottom: 16px">
      <div class="image-preview">
        <img 
          :src="generatedImageUrl" 
          alt="AI生成的图片" 
          style="max-width: 100%; max-height: 500px; border-radius: 8px;"
        />
      </div>
      
      <template #extra>
        <a-space>
          <a-button 
            type="primary" 
            :loading="saving"
            @click="saveToSpace"
          >
            {{ saving ? '保存中...' : '保存到空间' }}
          </a-button>
          <a-button @click="regeneratePicture">
            重新生成
          </a-button>
        </a-space>
      </template>
    </a-card>

    <!-- 图片信息编辑表单，仅在图片生成后显示 -->
    <a-card v-if="generatedImageUrl" title="图片信息" style="margin-bottom: 16px">
      <a-form 
        name="pictureForm" 
        layout="vertical" 
        :model="pictureForm" 
        @finish="handleSave"
      >
        <a-form-item name="name" label="名称">
          <a-input 
            v-model:value="pictureForm.name" 
            placeholder="请输入图片名称" 
            allow-clear 
          />
        </a-form-item>
        <a-form-item name="introduction" label="简介">
          <a-textarea 
            v-model:value="pictureForm.introduction" 
            placeholder="请输入图片简介" 
            :auto-size="{ minRows: 2, maxRows: 4 }"
            allow-clear 
          />
        </a-form-item>
        <a-form-item name="category" label="分类">
          <a-auto-complete 
            v-model:value="pictureForm.category" 
            placeholder="请输入分类" 
            :options="categoryOptions"
            allow-clear 
          />
        </a-form-item>
        <a-form-item name="tags" label="标签">
          <a-select 
            v-model:value="pictureForm.tags" 
            mode="tags" 
            placeholder="请输入标签" 
            :options="tagOptions" 
            allow-clear 
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%">
            保存图片
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { aiCreatePictureUsingPost, uploadPictureByUrlUsingPost, editPictureUsingPost } from '@/api/pictureController.ts'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController.ts'

// 路由相关
const router = useRouter()
const route = useRoute()

// 空间ID
const spaceId = computed(() => {
  return route.query?.spaceId
})

// 生成状态
const generating = ref(false)
const saving = ref(false)
const promptText = ref('')
const generatedImageUrl = ref('')

// 图片表单数据
const pictureForm = reactive<{
  name: string
  introduction: string
  category: string
  tags: string[]
}>({
  name: '',
  introduction: '',
  category: '',
  tags: [],
})

// 分类和标签选项
const categoryOptions = ref<{ value: string; label: string }[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

/**
 * 生成AI图片
 */
const generatePicture = async () => {
  if (!promptText.value.trim()) {
    message.error('请输入提示词')
    return
  }

  generating.value = true
  
  try {
    const res = await aiCreatePictureUsingPost({
      text: promptText.value.trim()
    })
    
    if (res.data.code === 0 && res.data.data) {
      generatedImageUrl.value = res.data.data
      // 自动填充图片名称
      pictureForm.name = `AI生成-${promptText.value.substring(0, 20)}${promptText.value.length > 20 ? '...' : ''}`
      message.success('图片生成成功！')
    } else {
      message.error('生成失败：' + res.data.message)
    }
  } catch (error: any) {
    console.error('AI生成图片失败', error)
    message.error('生成失败：' + error.message)
  } finally {
    generating.value = false
  }
}

/**
 * 重新生成图片
 */
const regeneratePicture = () => {
  generatedImageUrl.value = ''
  pictureForm.name = ''
  pictureForm.introduction = ''
  pictureForm.category = ''
  pictureForm.tags = []
}

/**
 * 保存图片到空间
 */
const saveToSpace = async () => {
  if (!generatedImageUrl.value) {
    message.error('请先生成图片')
    return
  }

  if (!spaceId.value) {
    message.error('未指定保存空间')
    return
  }

  saving.value = true

  try {
    // 第一步：先保存图片到空间
    const uploadParams: API.PictureUploadRequest = {
      fileUrl: generatedImageUrl.value,
      // @ts-ignore
      spaceId: spaceId.value,
    }

    const uploadRes = await uploadPictureByUrlUsingPost(uploadParams)
    
    if (uploadRes.data.code === 0 && uploadRes.data.data) {
      const pictureId = uploadRes.data.data.id
      
      // 第二步：更新图片的详细信息
      // @ts-ignore
      const editRes = await editPictureUsingPost({
        id: pictureId,
        // @ts-ignore
        spaceId: spaceId.value,
        name: pictureForm.name,
        introduction: pictureForm.introduction,
        category: pictureForm.category,
        tags: pictureForm.tags,
      })
      
      if (editRes.data.code === 0) {
        message.success('图片保存成功！')
        // 跳转到图片详情页
        router.push({
          path: `/picture/${pictureId}`,
        })
      } else {
        message.error('更新图片信息失败：' + editRes.data.message)
      }
    } else {
      message.error('保存图片失败：' + uploadRes.data.message)
    }
  } catch (error: any) {
    console.error('保存图片失败', error)
    message.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

/**
 * 表单提交处理
 */
const handleSave = async () => {
  await saveToSpace()
}

/**
 * 获取标签和分类选项
 */
const getTagCategoryOptions = async () => {
  try {
    const res = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0 && res.data.data) {
      tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => ({
        value: data,
        label: data,
      }))
      categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => ({
        value: data,
        label: data,
      }))
    }
  } catch (error) {
    console.error('获取标签分类失败', error)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
/* 页面容器样式 */
#aiPicturePage {
  max-width: 800px;
  margin: 0 auto;
}

/* 图片预览区域样式 */
.image-preview {
  text-align: center;
  margin: 16px 0;
}

.image-preview img {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.image-preview img:hover {
  transform: scale(1.02);
}
</style>
