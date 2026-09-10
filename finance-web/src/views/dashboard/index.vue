<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><b>系统状态</b></template>
          <div class="kpi">{{ healthText }}</div>
          <div class="kpi-sub">后端 /auth + 财务核心接口（Gate 6-10 已合入 main）</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><b>未读消息</b></template>
          <div class="kpi" :style="{ color: unread > 0 ? '#ea6668' : '#52c41a' }">{{ unread }}</div>
          <div class="kpi-sub">到期提醒 / 逾期提醒（W2 消息中心，登录人本人）</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><b>审批待办</b></template>
          <div class="kpi">{{ todos.length }}</div>
          <div class="kpi-sub">当前登录人待办（W1 审批流，单人顺序审批一期）</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><b>功能入口（一条完整业务链：科目 → 凭证 → 结账 → 合同 → 审批 → 计划核销 → 消息）</b></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="f in entries" :key="f.path">
          <div class="entry" @click="$router.push(f.path)">
            <el-icon :size="22"><component :is="f.icon" /></el-icon>
            <div class="entry-name">{{ f.name }}</div>
            <div class="entry-desc">{{ f.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header><b>演示路径建议</b></template>
      <ol class="steps">
        <li>科目管理：新建 资产类/负债类/收入类 科目（未级才能记账）</li>
        <li>凭证管理：录入一张借贷平衡凭证（如 借 银行存款 100000 / 贷 实收资本 100000）→ 审核 → 过账</li>
        <li>合同台账：新建销售合同（金额 8 万）→ 提交审批（审批人填 admin）</li>
        <li>审批待办：点 同意 → 合同生效并自动生成收付款计划</li>
        <li>合同台账：查看该合同的计划 → 核销收款 → 状态变为 PAID</li>
        <li>消息中心：查看站内提醒并标记已读（到期提醒由每日定时任务 08:00/08:30 触发）</li>
      </ol>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { List, Document, Files, Checked, Bell } from '@element-plus/icons-vue'
import { getHealth, unreadCount, todoTasks } from '@/api'

const healthText = ref('检测中...')
const unread = ref(0)
const todos = ref([])
const entries = [
  { path: '/subject', name: '科目管理', desc: 'F1 树形科目/末级记账', icon: List },
  { path: '/voucher', name: '凭证管理', desc: 'F2 录入/审核/过账/冲销', icon: Document },
  { path: '/contract', name: '合同台账', desc: 'C1 台账 + 计划 + 核销', icon: Files },
  { path: '/approval', name: '审批待办', desc: 'W1/C2 合同审批', icon: Checked },
  { path: '/message', name: '消息中心', desc: 'W2 站内消息', icon: Bell }
]

onMounted(async () => {
  try {
    const h = await getHealth()
    healthText.value = h.data?.status || h.msg || 'OK'
  } catch (e) {
    healthText.value = '后端未连接'
  }
  try {
    const u = await unreadCount()
    unread.value = u.data || 0
  } catch (e) { /* ignore */ }
  try {
    const t = await todoTasks()
    todos.value = t.data || []
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.kpi {
  font-size: 30px;
  font-weight: 700;
  color: #1a1b1c;
}
.kpi-sub {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
  line-height: 1.6;
}
.entry {
  border: 1px solid #e4e3dd;
  border-radius: 12px;
  padding: 18px 14px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
  margin-bottom: 12px;
}
.entry:hover {
  border-color: #9bbbf4;
  background: #f5f8ff;
}
.entry-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1b1c;
  margin-top: 8px;
}
.entry-desc {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}
.steps {
  margin: 0;
  padding-left: 20px;
  line-height: 2;
  color: #374151;
  font-size: 13px;
}
</style>
