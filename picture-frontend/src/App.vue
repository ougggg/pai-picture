<template>
<div id="app" :locale="zhCN">
  <a-config-provider
    :theme="{
      token: {
        colorPrimary: '#1890ff',
        borderRadius: 8,
        colorBgContainer: '#ffffff',
      },
    }"
  >
    <BasicLayout/>
  </a-config-provider>
</div>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
dayjs.locale('zh-cn');

import BasicLayout from './layouts/BasicLayout.vue'

</script>

<style scoped></style>
