<template>
  <!-- 用户登录页面容器 -->
  <div id="userLoginPage">
    <h2 class="title">用户登录</h2>
    <div class="desc">企业级智能云图库</div>

    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <!-- 账号输入框 -->
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>

      <!-- 密码输入框（带显示/隐藏功能） -->
      <a-form-item name="userPassword" :rules="[
        { required: true, message: '请输入密码' },
        { min: 8, message: '密码长度不能小于 8 位' },
      ]">
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>

      <!-- 注册提示链接 -->
      <div class="tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>

      <!-- 提交按钮 -->
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { userLoginUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import router from '@/router' // 用于接受表单输入的值

// 用于接受表单输入的值
const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

/**
 * 登录用户状态管理store实例
 * 用于管理全局登录用户状态
 */
const loginUserStore = useLoginUserStore()

/**
 * 表单提交处理函数
 * 当用户点击登录按钮或按回车键时触发
 * @param {any} values - 表单提交的值，包含userAccount和userPassword
 */
const handleSubmit = async (values: any) => {
  // 调用用户登录API接口
  const res = await userLoginUsingPost(values)
  // 登录成功，把登录态保存到全局状态中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,// 使用replace模式，不会在历史记录中留下当前页面
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 登录页面容器样式 */
#userLoginPage {
  max-width: 360px;   /* 最大宽度限制 */
  margin: 0 auto;     /* 水平居中 */
}

/* 标题样式 */
.title {
  text-align: center;   /* 文字居中 */
  margin-bottom: 16px;  /* 底部间距 */
}

/* 描述文字样式 */
.desc {
  text-align: center;   /* 文字居中 */
  color: #bbb;          /* 浅灰色文字 */
  margin-bottom: 16px;   /* 底部间距 */
}

/* 提示信息样式 */
.tips {
  color: #bbb;          /* 浅灰色文字 */
  text-align: right;    /* 文字右对齐 */
  font-size: 13px;      /* 较小字体大小 */
  margin-bottom: 16px;  /* 底部间距 */
}
</style>
