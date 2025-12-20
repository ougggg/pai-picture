<!-- 
  我的空间页面 - MySpacePage.vue
  功能：作为中间页，根据用户空间状态进行页面跳转
  特性：
  - 用户登录状态检查
  - 空间存在性验证
  - 自动跳转逻辑（空间详情页或创建空间页）
  - 加载状态提示
-->

<template>
  <!-- 我的空间页面容器 -->
  <div id="mySpacePage">
    <p>正在跳转，请稍后。。。</p>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { onMounted } from 'vue'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 检查用户空间状态的异步函数
 * 根据用户登录状态和空间创建情况决定跳转到哪个页面
 */
const checkUserSpace = async () => {

  // 用户未登录，则直接跳转到登录页面
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
    return
  }

  // 如果用户已登录，会获取该用户已创建的空间
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    current: 1,
    pageSize: 1,
    spaceType: SPACE_TYPE_ENUM.PRIVATE,
  })
  if (res.data.code === 0) {
    // 如果有，则进入第一个空间
    if (res.data.data?.records?.length > 0) {
      const space = res.data.data.records[0]
      router.replace(`/space/${space.id}`)
    } else {
      // 如果没有，则跳转到创建空间页面
      router.replace('/add_space')
      message.warn('请先创建空间')
    }
  } else {
    message.error('加载我的空间失败，' + res.data.message)
  }
}

// 在页面加载时检查用户空间
onMounted(() => {
  checkUserSpace()
})
</script>
