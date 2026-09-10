import { createRouter, createWebHistory } from 'vue-router'

// 路由表：Gate 11 前端核心页面（对接 Gate 6-10 后端接口）
const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'subject',
        name: 'Subject',
        component: () => import('@/views/subject/index.vue'),
        meta: { title: '科目管理' }
      },
      {
        path: 'voucher',
        name: 'Voucher',
        component: () => import('@/views/voucher/index.vue'),
        meta: { title: '凭证管理' }
      },
      {
        path: 'contract',
        name: 'Contract',
        component: () => import('@/views/contract/index.vue'),
        meta: { title: '合同台账' }
      },
      {
        path: 'approval',
        name: 'Approval',
        component: () => import('@/views/approval/index.vue'),
        meta: { title: '审批待办' }
      },
      {
        path: 'message',
        name: 'Message',
        component: () => import('@/views/message/index.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/index.vue'),
        meta: { title: '财务报表' },
      },
      {
        path: 'expense',
        name: 'Expense',
        component: () => import('@/views/expense/index.vue'),
        meta: { title: '费用报销' }
      },
      {
        path: 'aging',
        name: 'Aging',
        component: () => import('@/views/aging/index.vue'),
        meta: { title: '应收应付账龄' }
      },
      {
        path: 'system/user',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/menu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理' }
      },
      {
        path: 'hr/employee',
        component: () => import('@/views/hr/employee/index.vue'),
        meta: { title: '员工档案' }
      },
      {
        path: 'hr/attendance',
        component: () => import('@/views/hr/attendance/index.vue'),
        meta: { title: '考勤管理' }
      },
      {
        path: 'hr/salary',
        component: () => import('@/views/hr/salary/index.vue'),
        meta: { title: '工资核算' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页；已登录访问 /login 回工作台
router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} · 财务管理系统` : '财务管理系统'
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
