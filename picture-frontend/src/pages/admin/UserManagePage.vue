<template>
  <!-- 用户管理页面容器 -->
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
      </a-form-item>
      <a-form-item label="手机号">
        <a-input v-model:value="searchParams.userPhone" placeholder="输入手机号" allow-clear />
      </a-form-item>
      <a-form-item label="邮箱">
        <a-input v-model:value="searchParams.userEmail" placeholder="输入邮箱" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>

    <div style="margin-bottom: 16px" />

    <!-- 用户数据表格 -->
    <a-table :columns="columns" :data-source="dataList" :pagination="pagination" @change="doTableChange">
      <!-- 自定义表格单元格内容 -->
      <template #bodyCell="{ column, record }">
        <!-- 头像列自定义显示 -->
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-avatar :src="record.userAvatar" :size="80">
            <template #icon>
              <UserOutlined />
            </template>
          </a-avatar>
        </template>

        <!-- 用户角色列自定义显示 -->
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>

        <!-- 创建时间列自定义显示 -->
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>

        <!-- 操作列自定义显示 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" @click="doEdit(record)">编辑</a-button>
            <a-button danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>

      </template>
    </a-table>

    <!-- 编辑用户身份模态框 -->
    <a-modal v-model:open="editModalVisible" title="编辑用户身份" ok-text="确定" cancel-text="取消" @ok="handleEditSubmit"
      @cancel="handleEditCancel">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="editForm.userName" disabled />
        </a-form-item>
        <a-form-item label="账号">
          <a-input v-model:value="editForm.userAccount" disabled />
        </a-form-item>
        <a-form-item label="用户身份" required>
          <a-select v-model:value="editForm.userRole" placeholder="请选择用户身份">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUserUsingPost, listUserVoByPageUsingPost, updateUserUsingPost } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { UserOutlined } from '@ant-design/icons-vue'


/**
 * 表格列配置
 * 定义表格的列标题、数据字段和操作列
 */
const columns = [
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '手机号',
    dataIndex: 'userPhone',
  },
  {
    title: '邮箱',
    dataIndex: 'userEmail',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 定义用户数据列表
const dataList = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取用户数据的异步函数
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// 编辑模态框相关
const editModalVisible = ref(false)
const editForm = reactive<{
  id?: number
  userName?: string
  userAccount?: string
  userRole?: string
}>({})

// 打开编辑模态框
const doEdit = (record: API.UserVO) => {
  editForm.id = record.id
  editForm.userName = record.userName
  editForm.userAccount = record.userAccount
  editForm.userRole = record.userRole
  editModalVisible.value = true
}

// 提交编辑
const handleEditSubmit = async () => {
  if (!editForm.id || !editForm.userRole) {
    message.error('请选择用户身份')
    return
  }

  const res = await updateUserUsingPost({
    id: editForm.id,
    userRole: editForm.userRole
  })

  if (res.data.code === 0) {
    message.success('编辑成功')
    editModalVisible.value = false
    // 刷新数据
    fetchData()
  } else {
    message.error('编辑失败，' + res.data.message)
  }
}

// 取消编辑
const handleEditCancel = () => {
  editModalVisible.value = false
  // 重置表单
  Object.assign(editForm, {
    id: undefined,
    userName: undefined,
    userAccount: undefined,
    userRole: undefined
  })
}

// 删除数据
const doDelete = async (id: number) => {
  if (!id) {
    return
  }
  const res = await deleteUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
</script>
