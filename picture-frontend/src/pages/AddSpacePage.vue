<template>
  <!-- 
    添加/编辑空间页面 - AddSpacePage.vue
    功能：提供空间创建和编辑功能，支持不同空间级别选择
    特性：
    - 空间信息编辑（名称、级别）
    - 空间级别介绍和说明
    - 支持创建和编辑两种模式
    - 表单验证和错误处理
  -->
  <div id="addSpacePage">

    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改空间' : '创建空间' }}
    </h2>

    <!-- 空间信息表单 -->
    <a-form name="spaceForm" layout="vertical" :model="spaceForm" @finish="handleSubmit">
      <a-form-item name="spaceName" label="空间名称">
        <a-input v-model:value="spaceForm.spaceName" placeholder="请输入空间" allow-clear />
      </a-form-item>

      <!-- 空间级别选择项 -->
      <a-form-item name="spaceLevel" label="空间级别">
        <a-select v-model:value="spaceForm.spaceLevel" style="min-width: 180px" placeholder="请选择空间级别"
          :options="SPACE_LEVEL_OPTIONS" allow-clear />
      </a-form-item>

      <!-- 提交按钮 -->
      <a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading" style="width: 100%">
          提交
        </a-button>
      </a-form-item>
    </a-form>

    <!-- 空间级别介绍卡片 -->
    <a-card title="空间级别介绍">
      <!-- 提示信息 -->
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系管理员派大星
      </a-typography-paragraph>

      <!-- 遍历显示所有空间级别的详细信息 -->
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}：大小 {{ formatSize(spaceLevel.maxSize) }}，数量
        {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import {  onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  addSpaceUsingPost,
  getSpaceVoByIdUsingGet,
  listSpaceLevelUsingGet,
  updateSpaceUsingPost,
} from '@/api/spaceController.ts'
import { useRoute, useRouter } from 'vue-router'

// 导入空间相关常量
import {
  SPACE_LEVEL_OPTIONS,
} from '@/constants/space.ts'

import { formatSize } from '../utils'

const space = ref<API.SpaceVO>()
const spaceForm = reactive<API.SpaceAddRequest | API.SpaceEditRequest>({})
const loading = ref(false)

const route = useRoute()

// 空间级别列表
const spaceLevelList = ref<API.SpaceLevel[]>([])




// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('获取空间级别失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchSpaceLevelList()
})

const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const spaceId = space.value?.id
  loading.value = true
  let res
  if (spaceId) {
    // 更新
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...spaceForm,
    })
  } else {
    // 创建
    res = await addSpaceUsingPost({
      ...spaceForm
    })
  }
  // 操作成功
  if (res.data.code === 0) {
    if (spaceId) {
      // 修改操作：后端返回 boolean 类型
      if (res.data.data === true) {
        message.success('修改成功')
        // 修改成功后跳转回空间管理页面
        router.push({
          path: '/admin/spaceManage',
        })
      } else {
        message.error('修改失败')
        // 修改失败时不跳转，停留在当前页面
      }
    } else {
      // 创建操作：后端返回空间ID
      if (res.data.data) {
        message.success('创建成功')
        // 跳转到空间详情页
        router.push({
          path: `/space/${res.data.data}`,
        })
      } else {
        message.error('创建失败')
      }
    }
  } else {
    message.error('操作失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 获取原有空间数据
 * 当路由参数中包含id时，表示是编辑操作，需要获取原有数据
 */
const getOldSpace = async () => {
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      space.value = data                    // 设置当前空间
      // 填充表单数据
      spaceForm.spaceName = data.spaceName  // 空间名称
      spaceForm.spaceLevel = data.spaceLevel // 空间级别
    }
  }
}

onMounted(() => {
  getOldSpace()
})
</script>

<style scoped>
/* 页面容器样式 */
#addSpacePage {
  max-width: 720px;
  /* 最大宽度限制 */
  margin: 0 auto;
  /* 水平居中 */
}
</style>
