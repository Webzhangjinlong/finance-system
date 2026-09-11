import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 财务管理系统前端构建配置
// 约束：Vite 5 + Vue 3；路径别名 @ -> src；开发代理 /api -> 后端 9010（剥掉 /api 前缀）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9010',
        changeOrigin: true,
        // 后端接口无 /api 前缀，dev 代理剥掉后转发
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false
  }
})
