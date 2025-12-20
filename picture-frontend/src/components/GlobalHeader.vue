<template>
  <!-- 
    全局头部导航栏组件 - GlobalHeader.vue
    功能：提供网站顶部导航栏，包含Logo、菜单和用户信息
    特性：
    - 响应式布局（Logo、菜单、用户信息三栏布局）
    - 权限控制（管理员菜单动态显示）
    - 用户登录状态管理
    - 路由高亮和跳转功能
  -->
  <div id="globalHeader">
    <a-row :wrap="false">

      <!-- Logo和标题区域 -->
      <a-col flex="240px">
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">派大星的云图之家</div>
          </div>
        </router-link>
      </a-col>

      <!-- 导航菜单区域，自动填充剩余空间 -->
      <a-col flex="auto">
        <a-menu v-model:selectedKeys="current" mode="horizontal" :items="items" @click="doMenuClick"
                class="centered-menu"/>
      </a-col>

      <!-- 用户信息展示栏 -->
      <a-col flex="240px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">

            <a-dropdown>
              <a-space>
                <div class="userAvatar">
                  <a-avatar :src="loginUserStore.loginUser.userAvatar"/>
                </div>
                <div class="userName">{{ loginUserStore.loginUser.userName ?? '无名大侠' }}</div>
              </a-space>
              <!-- 下拉菜单内容 -->
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                  <router-link to="/user/center">
                    <UserOutlined/>
                    个人中心
                    </router-link>
                  </a-menu-item>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <CloudOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined/>
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>

          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, LogoutOutlined, UserOutlined,CloudOutlined } from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

/**
 * 原始菜单项配置（未过滤）
 * 包含所有可能的菜单项，包括管理员专属菜单
 */
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/my_space',
    icon: () => h(UserOutlined),
    label: '我的空间',
    title: '我的空间',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
]

/**
 * 菜单项过滤函数
 * 根据用户权限过滤菜单项，非管理员无法看到/admin开头的菜单
 * @param {MenuProps['items']} menus - 待过滤的菜单项数组
 * @returns {MenuProps['items']} 过滤后的菜单项数组
 */
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 检查是否为管理员专属菜单（以/admin开头）
    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      // 只有管理员角色才能看到管理员菜单
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false  // 非管理员，过滤掉该菜单
      }
    }
    return true  // 普通菜单或管理员有权限的菜单，保留
  })
}

// 展示在菜单的路由数组
const items = computed(() => filterMenus(originItems))

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 用户退出登录处理函数
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
/* 全局头部样式 */
#globalHeader .title-bar {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: var(--transition-base);
}

#globalHeader .title-bar:hover {
  opacity: 0.8;
}

/* Logo图片样式 */
.logo {
  height: 40px;
  width: 40px;
  margin-right: 12px;
}

/* 标题文字样式 */
.title {
  color: var(--primary-color);
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 导航菜单 */
.centered-menu {
  display: flex;
  justify-content: center;
  background: transparent;
  border-bottom: none;
}

:deep(.ant-menu-horizontal) {
  border-bottom: none;
  background: transparent;
}

:deep(.ant-menu-item) {
  border-radius: var(--border-radius-md);
  margin: 0 4px;
  transition: var(--transition-base);
}

:deep(.ant-menu-item:hover),
:deep(.ant-menu-item-selected) {
  color: var(--primary-color) !important;
  background: rgba(24, 144, 255, 0.1);
}

:deep(.ant-menu-item::after) {
  display: none !important;
}

/* 用户信息展示区域 */
.user-login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.userAvatar {
  cursor: pointer;
  transition: var(--transition-base);
  padding: 4px;
  border-radius: 50%;
}

.userAvatar:hover {
  background: rgba(0, 0, 0, 0.05);
}

.userName {
  color: var(--text-color);
  font-weight: 500;
  margin-left: 8px;
}
</style>
