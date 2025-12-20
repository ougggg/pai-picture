<template>
  <!-- 
    批量编辑图片模态框组件 - BatchEditPictureModal.vue
    功能：提供批量编辑图片功能，支持分类、标签、命名规则等批量修改
    特性：
    - 批量修改图片分类和标签
    - 支持动态命名规则（{序号}占位符）
    - 仅对当前页面图片生效
    - 表单验证和错误处理
  -->
  <div class="batch-edit-picture-modal">

    <a-modal v-model:visible="visible" title="批量编辑图片" :footer="false" @cancel="closeModal">

      <!-- 操作范围提示 -->
      <a-typography-paragraph type="secondary">* 只对当前页面的图片生效</a-typography-paragraph>

      <!-- 批量创建表单 -->
      <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">

        <!-- 分类输入项 -->
        <a-form-item name="category" label="分类">
          <a-auto-complete
            v-model:value="formData.category"
            placeholder="请输入分类"
            :options="categoryOptions"
            allow-clear
          />
        </a-form-item>

        <!-- 分类输入项 -->
        <a-form-item name="tags" label="标签">
          <a-select
            v-model:value="formData.tags"
            mode="tags"
            placeholder="请输入标签"
            :options="tagOptions"
            allow-clear
          />
        </a-form-item>

        <!-- 命名规则输入项 -->
        <a-form-item name="nameRule" label="命名规则">
          <a-input
            v-model:value="formData.nameRule"
            placeholder="请输入命名规则，输入 {序号} 可动态生成"
            allow-clear
          />
        </a-form-item>

        <!-- 提交按钮 -->
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%">提交</a-button>
        </a-form-item>

      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import {
  editPictureByBatchUsingPost,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

interface Props {
  pictureList: API.PictureVO[]
  spaceId: number
  onSuccess: () => void
}

const props = withDefaults(defineProps<Props>(), {})

// 模态框显示状态
const visible = ref(false)

// 打开模态框
const openModal = () => {
  visible.value = true
}

// 关闭模态框
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})

/**
 * 表单数据对象
 * 使用reactive创建响应式对象，存储表单输入的值
 */
const formData = reactive<API.PictureEditByBatchRequest>({
  category: '',
  tags: [],
  nameRule: '',
})

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  //校验参数
  if (!props.pictureList) {
    return
  }

  // 调用批量编辑API
  const res = await editPictureByBatchUsingPost({
    pictureIdList: props.pictureList.map((picture) => picture.id),
    spaceId: props.spaceId,
    ...values,
  })

  // 判断是否操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    closeModal()
    props.onSuccess?.()
  } else {
    message.error('操作失败，' + res.data.message)
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
</script>
