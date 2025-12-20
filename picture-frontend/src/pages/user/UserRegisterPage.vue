<template>
  <!-- 用户注册页面容器 -->
  <div id="userRegisterPage">
    <h2 class="title">用户注册</h2>
    <div class="desc">企业级智能云图库</div>

    <a-form
      :model="formState"
      name="register"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <!-- 用户名称输入框 -->
      <a-form-item
        name="userName"
        :rules="[{ required: true, message: '请输入用户名称' }]"
      >
        <a-input
          v-model:value="formState.userName"
          placeholder="请输入用户名称"
          size="large"
        >
          <template #prefix>
            <UserOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input>
      </a-form-item>

      <!-- 账号输入框 -->
      <a-form-item
        name="userAccount"
        :rules="[
          { required: true, message: '请输入账号' },
          { min: 4, message: '账号长度至少4位' }
        ]"
      >
        <a-input
          v-model:value="formState.userAccount"
          placeholder="请输入账号（4位以上）"
          size="large"
        >
          <template #prefix>
            <IdcardOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input>
      </a-form-item>

      <!-- 密码输入框 -->
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
          {
            pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/,
            message: '密码需包含字母和数字'
          }
        ]"
      >
        <a-input-password
          v-model:value="formState.userPassword"
          placeholder="请设置密码（含字母和数字）"
          size="large"
        >
          <template #prefix>
            <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input-password>
      </a-form-item>

      <!-- 确认密码输入框 -->
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue('userPassword') === value) {
                return Promise.resolve();
              }
              return Promise.reject(new Error('两次输入的密码不一致'));
            },
          }),
        ]"
      >
        <a-input-password
          v-model:value="formState.checkPassword"
          placeholder="请再次输入密码"
          size="large"
        >
          <template #prefix>
            <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input-password>
      </a-form-item>

      <!-- 手机号输入框 -->
      <a-form-item
        name="userPhone"
        :rules="[
          { required: true, message: '请输入手机号' },
          {
            pattern: /^1[3-9]\d{9}$/,
            message: '请输入有效的手机号'
          }
        ]"
      >
        <a-input
          v-model:value="formState.userPhone"
          placeholder="请输入手机号"
          size="large"
        >
          <template #prefix>
            <MobileOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input>
      </a-form-item>

      <!-- 邮箱输入框 -->
      <a-form-item
        name="userEmail"
        :rules="[
          { required: true, message: '请输入邮箱' },
          {
            pattern: /^[\w.-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/,
            message: '请输入有效的邮箱'
          }
        ]"
      >
        <a-input
          v-model:value="formState.userEmail"
          placeholder="请输入邮箱"
          size="large"
        >
          <template #prefix>
            <MailOutlined style="color: rgba(0, 0, 0, 0.25)" />
          </template>
        </a-input>
      </a-form-item>

      <!-- 已有账号提示链接 -->
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>

      <!-- 提交按钮 -->
      <a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          style="width: 100%"
          size="large"
          :loading="submitting"
        >
          注册
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  UserOutlined,
  IdcardOutlined,
  LockOutlined,
  MailOutlined,
  MobileOutlined
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue'
import router from '@/router'

// 引入 API 方法
import { userRegisterUsingPost } from '@/api/userController'

// 提交状态
const submitting = ref(false)

// 表单状态
const formState = reactive({
  userName: '',
  userAccount: '',
  userPassword: '',
  checkPassword: '',
  userPhone: '',
  userEmail: ''
})

/**
 * 表单提交处理函数
 * 当用户点击注册按钮时触发
 */
const handleSubmit = async () => {
  try {
    // 开始提交状态
    submitting.value = true

    // 调用注册 API
    const res = await userRegisterUsingPost({
      userName: formState.userName,
      userAccount: formState.userAccount,
      userPassword: formState.userPassword,
      checkPassword: formState.checkPassword,
      userPhone: formState.userPhone,
      userEmail: formState.userEmail
    })

    // 注册成功处理
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')

      // 跳转到登录页面
      router.push({
        path: '/user/login',
        replace: true
      })
    } else {
      message.error('注册失败：' + (res.data.message || '未知错误'))
    }
  } catch (e: any) {
    // 错误处理
    message.error('注册失败：' + (e.message || '系统错误'))
  } finally {
    // 结束提交状态
    submitting.value = false
  }
}
</script>

<style scoped>
/* 注册页面容器样式 */
#userRegisterPage {
  max-width: 420px;   /* 增加宽度以容纳更多字段 */
  margin: 0 auto;     /* 水平居中 */
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 标题样式 */
.title {
  text-align: center;   /* 文字居中 */
  margin-bottom: 16px;  /* 底部间距 */
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
}

/* 描述文字样式 */
.desc {
  text-align: center;   /* 文字居中 */
  color: #666;          /* 深灰色文字 */
  margin-bottom: 32px;  /* 底部间距 */
  font-size: 16px;
}

/* 提示信息样式 */
.tips {
  color: #666;          /* 深灰色文字 */
  text-align: center;    /* 文字居中 */
  font-size: 14px;      /* 较小字体大小 */
  margin-bottom: 24px;  /* 底部间距 */
}

/* 表单项间距 */
:deep(.ant-form-item) {
  margin-bottom: 20px;
}

/* 链接样式 */
a {
  color: #1890ff;
  transition: color 0.3s;
}

a:hover {
  color: #40a9ff;
}

</style>
