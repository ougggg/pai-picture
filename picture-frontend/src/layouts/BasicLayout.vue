<template>
  <!-- 基础布局容器 -->
  <div id="basicLayout">

    <a-layout style="min-height: 100vh">

      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>

      <a-layout-content class="content">
        <router-view/>
      </a-layout-content>

    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue';

</script>

<style scoped>
/* 头部区域样式 */
#basicLayout .header {
  padding-inline: 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  color: unset;
  margin-bottom: 4px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

/* 内容区域样式 */
.content {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 24px;
  min-height: calc(100vh - 64px);
}
</style>
