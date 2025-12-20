<template>
  <!-- 
    分享模态框组件 - ShareModal.vue
    功能：提供图片分享功能，支持链接复制和二维码生成
    特性：
    - 分享链接复制功能
    - 二维码生成（手机扫码查看）
    - 模态框显示控制
    - 可配置标题和链接
  -->
  <div id="share-modal">
    <a-modal v-model:open="visible"
    :title="title"
    :footer="false"
    @cancel="closeModal">

      <h4>复制分享链接</h4>
      <a-typography-link copyable>
        {{ link }}
      </a-typography-link>
      <div style="margin-bottom: 16px" />
      <h4>手机扫码查看</h4>
      <a-qrcode :value="link" />
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

interface Props {
  title: string;
  link: string;
}

// 定义组件属性默认值
const props = withDefaults(defineProps<Props>(), {
  title: "分享图片",
  link: 'https://www.baidu.com/'
})

// 模态框显示状态，是否可见
const visible = ref(false)

/**
 * 打开模态框函数
 * 将模态框设置为可见状态
 */
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false;
}

// 暴露函数给父组件
defineExpose({
  openModal,
})
</script>
