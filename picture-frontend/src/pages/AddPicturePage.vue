<template>
  <!-- 
    添加/编辑图片页面 - AddPicturePage.vue
    功能：提供图片上传和编辑功能，支持文件上传和URL上传两种方式
    特性：
    - 双模式上传（文件上传、URL上传）
    - 图片编辑功能（裁剪、AI扩图）
    - 图片信息编辑（名称、简介、分类、标签）
    - 支持编辑已有图片
  -->
  <div id="addPicturePage">

    <!-- 页面标题，根据是否有图片id判断是修改还是创建 -->
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>

    <!-- 空间信息提示，仅在spaceId存在时显示 -->
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>

    <!-- 选择上传方式 -->
    <a-tabs v-model:activeKey="uploadType">
      <a-tab-pane key="file" tab="文件上传">
        <!-- 图片上传组件 -->
        <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传" force-render>
        <!-- URL 图片上传组件 -->
        <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
      </a-tab-pane>
    </a-tabs>

    <!-- 图片编辑工具栏，仅在picture存在时显示 -->
    <div v-if="picture" class="edit-bar">
      <a-space size="middle">
        <a-button :icon="h(EditOutlined)" @click="doEditPicture">编辑图片</a-button>
        <a-button type="primary" :icon="h(FullscreenOutlined)" @click="doImagePainting">
          AI 扩图
        </a-button>
      </a-space>

      <!-- 图片裁剪组件（隐藏式） -->
      <ImageCropper ref="imageCropperRef" :imageUrl="picture?.url" :picture="picture" :spaceId="spaceId" :space="space"
        :onSuccess="onCropSuccess" />

      <!-- AI扩图组件（隐藏式） -->
      <ImageOutPainting ref="imageOutPaintingRef" :picture="picture" :spaceId="spaceId"
        :onSuccess="onImageOutPaintingSuccess" />
    </div>

    <!-- 图片信息编辑表单，仅在picture存在时显示 -->
    <a-form v-if="picture" name="pictureForm" layout="vertical" :model="pictureForm" @finish="handleSubmit">
      <a-form-item name="name" label="名称">
        <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
      </a-form-item>
      <a-form-item name="introduction" label="简介">
        <a-textarea v-model:value="pictureForm.introduction" placeholder="请输入简介" :auto-size="{ minRows: 2, maxRows: 5 }"
          allow-clear />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-auto-complete v-model:value="pictureForm.category" placeholder="请输入分类" :options="categoryOptions"
          allow-clear />
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select 
          v-model:value="pictureForm.tags" 
          mode="tags" 
          placeholder="请选择或输入标签" 
          :options="tagOptions" 
          :max-tag-count="5"
          :max-tag-text-length="10"
          allow-clear 
          size="large"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">提交</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
// 导入组件
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import ImageCropper from '@/components/ImageCropper.vue'
import ImageOutPainting from '@/components/ImageOutPainting.vue'
import {computed, h, onMounted, reactive, ref} from 'vue'
import { message } from 'ant-design-vue'
import { EditOutlined, FullscreenOutlined } from '@ant-design/icons-vue'

// 导入API接口
import {
  editPictureUsingPost,          // 编辑图片接口
  getPictureVoByIdUsingGet,       // 根据ID获取图片接口
  listPictureTagCategoryUsingGet, // 获取标签分类列表接口
} from '@/api/pictureController.ts'

import { getSpaceVoByIdUsingGet } from '@/api/spaceController.ts'

import { useRoute, useRouter } from 'vue-router'

// 初始化路由和路由实例
const router = useRouter()
const route = useRoute()

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})
const uploadType = ref<'file' | 'url'>('file')
// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})

/**
 * 图片上传成功回调函数
 * 自动填充AI识别的标签和分类
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
  // 自动填充AI识别的标签
  if (newPicture.tags && newPicture.tags.length > 0) {
    pictureForm.tags = newPicture.tags
  }
  // 自动填充AI识别的分类
  if (newPicture.category) {
    pictureForm.category = newPicture.category
  }
}

/**
 * 表单提交处理函数
 * @param values
 */
const handleSubmit = async (values: any) => {
  console.log(values)
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
}

// 分类选项列表
const categoryOptions = ref<string[]>([])
// 标签选项列表
const tagOptions = ref<string[]>([])

/**
 * 获取标签和分类选项
 * @param values
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

/**
 * 获取已有图片数据
 * 当路由参数中包含id时，表示是编辑操作，需要获取原有数据
 */
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data                    // 设置当前图片
      pictureForm.name = data.name            // 填充名称
      pictureForm.introduction = data.introduction  // 填充简介
      pictureForm.category = data.category    // 填充分类
      pictureForm.tags = data.tags            // 填充标签
    }
  }
}

onMounted(() => {
  getOldPicture()
})

// ----- 图片编辑器引用 ------

/**
 * 图片裁剪组件引用
 * 用于调用裁剪组件的打开方法
 */
const imageCropperRef = ref()

// 打开图片编辑（裁剪）模态框
const doEditPicture = async () => {
  imageCropperRef.value?.openModal()
}

// 图片裁剪成功回调
const onCropSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- AI 扩图引用 -----
const imageOutPaintingRef = ref()

// 打开 AI 扩图弹窗
const doImagePainting = async () => {
  imageOutPaintingRef.value?.openModal()
}

// AI 扩图保存事件
const onImageOutPaintingSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
}

// ----- 空间信息 ------

// 获取空间信息
const space = ref<API.SpaceVO>()

/**
 * 获取空间详细信息
 * 当spaceId存在时，获取空间的详细信息
 */
const fetchSpace = async () => {
  // 获取数据
  if (spaceId.value) {
    const res = await getSpaceVoByIdUsingGet({
      id: spaceId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    }
  }
}


</script>

<style scoped>
/* 页面容器样式 */
#addPicturePage {
  max-width: 720px;   /* 最大宽度限制 */
  margin: 0 auto;     /* 水平居中 */
}

/* 编辑工具栏样式 */
#addPicturePage .edit-bar {
  text-align: center;   /* 居中显示 */
  margin: 16px 0;       /* 上下边距 */
}

/* 标签选择器样式优化 */
#addPicturePage :deep(.ant-select-selection-item) {
  font-size: 14px !important;      /* 增大标签字体 */
  padding: 4px 8px !important;     /* 增加内边距 */
  margin: 4px 4px 4px 0 !important; /* 增加标签间距 */
  line-height: 20px !important;    /* 调整行高 */
}

/* 标签选择器下拉选项样式 */
#addPicturePage :deep(.ant-select-item-option-content) {
  font-size: 14px;                 /* 增大选项字体 */
  padding: 4px 0;                  /* 调整选项内边距 */
}

/* 标签关闭按钮样式 */
#addPicturePage :deep(.ant-select-selection-item-remove) {
  font-size: 12px;                 /* 调整关闭按钮大小 */
}
</style>
