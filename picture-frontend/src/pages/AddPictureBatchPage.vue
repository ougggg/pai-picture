<template>
  <!-- 
    批量创建图片页面 - AddPictureBatchPage.vue
    功能：提供批量创建图片功能，支持关键词搜索和批量抓取
    特性：
    - 关键词搜索图片
    - 批量抓取数量控制
    - 动态命名规则（名称前缀）
    - 任务执行状态管理
  -->
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px">批量创建</h2>
    <!-- 批量创建图片表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="searchText" label="关键词">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear />
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <a-input-number v-model:value="formData.count" placeholder="请输入数量" style="min-width: 180px" :min="1" :max="30"
          allow-clear />
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <a-input v-model:value="formData.namePrefix" placeholder="请输入名称前缀，会自动补充序号" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          执行任务
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  uploadPictureByBatchUsingPost,
} from '@/api/pictureController.ts'
import { useRouter } from 'vue-router'

/**
 * 表单数据对象
 * 使用reactive创建响应式对象，存储表单输入的值
 * 类型为API.PictureUploadByBatchRequest，确保类型安全
 */
const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})
// 提交任务状态
const loading = ref(false)

const router = useRouter()

/**
 * 表单提交处理函数
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
 try {
    // 调用批量上传图片API
    const res = await uploadPictureByBatchUsingPost({
      ...formData,  // 展开所有表单数据
    })

    // 处理API响应
    if (res.data.code === 0 && res.data.data) {
      // 任务执行成功，显示成功消息
      // 注意：AI标签识别在后台异步进行，不影响上传结果
      message.success(`创建成功，共 ${res.data.data} 条，AI标签识别正在后台进行中...`, 3)

      // 记录批量上传时间，用于主页轮询检测
      sessionStorage.setItem('batchUploadTime', Date.now().toString())
      sessionStorage.setItem('batchUploadCount', res.data.data.toString())

      // 立即跳转到主页，不等待AI识别完成
      router.push({
        path: `/`,
      })
    } else {
      // 任务执行失败，显示错误消息
      message.error('创建失败，' + res.data.message)
    }
  } catch (error) {
    // 异常处理
    console.error('批量创建失败', error)
    message.error('批量创建失败，请稍后重试')
  } finally {
    loading.value = false  // 任务结束，隐藏加载状态
  }
}
</script>

<style scoped>
/* 页面容器样式 */
#addPictureBatchPage {
  max-width: 720px;   /* 最大宽度限制 */
  margin: 0 auto;     /* 水平居中 */
}
</style>
