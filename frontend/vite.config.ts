import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const repoRoot = fileURLToPath(new URL('..', import.meta.url))

// 开发期 /api 代理到本机后端；生产由 Nginx 同域反代（见执行计划部署拓扑）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      /** 设计素材唯一出处（login-bg-dusk.jpg 等） */
      '@docs': fileURLToPath(new URL('../docs', import.meta.url))
    }
  },
  server: {
    port: 5173,
    fs: {
      allow: [repoRoot]
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
