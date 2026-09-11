<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">财务管理系统</div>
      <el-menu :default-active="$route.path" router background-color="#1f2937" text-color="#cbd5e1"
        active-text-color="#ffffff">
        <el-menu-item index="/dashboard"><el-icon><HomeFilled /></el-icon>工作台</el-menu-item>
        <el-menu-item index="/subject"><el-icon><List /></el-icon>科目管理</el-menu-item>
        <el-menu-item index="/voucher"><el-icon><Document /></el-icon>凭证管理</el-menu-item>
        <el-menu-item index="/contract"><el-icon><Files /></el-icon>合同台账</el-menu-item>
        <el-menu-item index="/approval"><el-icon><Checked /></el-icon>审批待办</el-menu-item>
        <el-menu-item index="/message"><el-icon><Bell /></el-icon>消息中心</el-menu-item>
<el-menu-item index="/report"><el-icon><TrendCharts /></el-icon>财务报表</el-menu-item>
        <el-menu-item index="/expense"><el-icon><Tickets /></el-icon>费用报销</el-menu-item>
<el-menu-item index="/voucher-rule"><el-icon><SetUp /></el-icon>凭证映射规则</el-menu-item>
<el-menu-item index="/aging"><el-icon><Timer /></el-icon>应收应付账龄</el-menu-item>
<el-sub-menu index="/system"><template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
  <el-menu-item index="/system/user"><el-icon><User /></el-icon>用户管理</el-menu-item>
  <el-menu-item index="/system/role"><el-icon><Avatar /></el-icon>角色管理</el-menu-item>
  <el-menu-item index="/system/menu"><el-icon><Menu /></el-icon>菜单管理</el-menu-item>
  <el-menu-item index="/system/attachment"><el-icon><Paperclip /></el-icon>附件管理</el-menu-item>
  <el-menu-item index="/system/log"><el-icon><Notebook /></el-icon>操作日志</el-menu-item>
<el-sub-menu index="/hr"><template #title><el-icon><OfficeBuilding /></el-icon><span>人事管理</span></template>
<el-menu-item index="/hr/employee"><el-icon><User /></el-icon>员工档案</el-menu-item>
<el-menu-item index="/hr/attendance"><el-icon><Calendar /></el-icon>考勤管理</el-menu-item>
<el-menu-item index="/hr/salary"><el-icon><Money /></el-icon>工资核算</el-menu-item>
</el-sub-menu>
</el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ $route.meta.title }}</div>
        <div class="header-right">
          <el-badge :value="unread" :hidden="unread === 0" class="msg-badge" @click="$router.push('/message')">
            <el-icon :size="18" style="cursor: pointer"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="user-name">{{ userStore.nickname || userStore.username }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, List, Document, Files, Checked, Bell, TrendCharts, Timer, Tickets, Setting, User, Avatar, Menu, Paperclip, Notebook, OfficeBuilding, Calendar, Money, SetUp } from '@element-plus/icons-vue'
import { unreadCount } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const unread = ref(0)

async function loadUnread() {
  try {
    const res = await unreadCount()
    unread.value = res.data || 0
  } catch (e) {
    /* 未读角标失败不影响布局 */
  }
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  }
}

onMounted(loadUnread)
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #1f2937;
}
.logo {
  height: 56px;
  line-height: 56px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: #111827;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e3dd;
}
.header-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1b1c;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}
.msg-badge {
  cursor: pointer;
}
.user-name {
  cursor: pointer;
  color: #374151;
  font-size: 13px;
}
.main {
  background: #f4f3ee;
}
</style>
