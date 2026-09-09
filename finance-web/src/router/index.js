import { createRouter, createWebHistory } from 'vue-router'

// 路由表：P0 模块逐步补充（dashboard 为占位首页）
const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '工作台' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：登录态校验骨架（接入认证后启用）
router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · 财务管理系统` : '财务管理系统'
  next()
})

export default router
