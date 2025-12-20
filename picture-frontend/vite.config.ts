import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    host: '0.0.0.0', // 允许外部访问
    port: 5166, // 端口号
    // 代理后端API
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
        // 不需要重写路径，因为后端的 context-path 就是 /api
      }
    }
  }
})
