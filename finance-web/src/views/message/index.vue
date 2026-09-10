<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>消息中心（W2）— 仅本人可见</b>
        <div>
          <el-button size="small" type="primary" plain @click="onReadAll" :disabled="rows.every((r) => r.isRead === 1)">
            全部已读
          </el-button>
          <el-button size="small" @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" border size="small" v-loading="loading" @row-click="onRead">
      <el-table-column width="60">
        <template #default="{ row }">
          <el-badge is-dot :hidden="row.isRead === 1" />
        </template>
      </el-table-column>
      <el-table-column prop="messageType" label="类型" width="150">
        <template #default="{ row }">{{ typeText(row.messageType) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="160" />
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="remindDate" label="提醒日期" width="110" />
      <el-table-column prop="createTime" label="发送时间" width="170" />
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageMessage, readMessage, readAllMessage } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, unreadOnly: false })

const TYPES = {
  CONTRACT_EXPIRE: '合同到期提醒',
  PLAN_DUE: '计划到期提醒',
  AR_OVERDUE: '应收逾期提醒',
  AP_OVERDUE: '应付逾期提醒'
}
const typeText = (t) => TYPES[t] || t

async function load() {
  loading.value = true
  try {
    const res = await pageMessage({ page: query.page, size: query.size, unreadOnly: query.unreadOnly })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function onRead(row) {
  if (row.isRead === 1) return
  await readMessage(row.id)
  row.isRead = 1
  load()
}

async function onReadAll() {
  await readAllMessage()
  ElMessage.success('已全部标记已读')
  load()
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
:deep(.el-table__row) {
  cursor: pointer;
}
</style>
