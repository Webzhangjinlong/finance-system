<template>
  <div style="padding: 16px">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span style="font-weight: 600">期末结账（F4）</span>
          <el-button type="primary" size="small" @click="load">刷新</el-button>
        </div>
      </template>

      <el-alert type="warning" :closable="false" style="margin-bottom: 12px"
        title="结账前置校验（后端强制）：该期间无未审核/未过账凭证 + 试算平衡；结账自动执行损益结转并生成结转凭证，期间转为 CLOSED（只读）。" />

      <el-table :data="rows" v-loading="loading" border size="small">
        <el-table-column prop="periodYear" label="年" width="80" align="center" />
        <el-table-column prop="periodMonth" label="月" width="80" align="center" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.periodStatus === 'CLOSED' ? 'success' : 'info'" size="small">
              {{ row.periodStatus === 'CLOSED' ? '已结账' : '开启' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="起止日期" min-width="200">
          <template #default="{ row }">
            {{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="closedBy" label="结账人" width="120" />
        <el-table-column label="结账时间" width="180">
          <template #default="{ row }">{{ row.closedAt ? fmtTime(row.closedAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.periodStatus !== 'CLOSED'" link type="primary" size="small" @click="doClose(row)">结账</el-button>
            <el-button v-else link type="danger" size="small" @click="doReopen(row)">反结账</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="color: #909399; font-size: 12px; margin-top: 8px">
        已结账期间凭证/账簿只读；反结账会删除该期间结转凭证并恢复 OPEN。
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPeriod, closePeriod, reopenPeriod } from '@/api/index.js'

const loading = ref(false)
const rows = ref([])

function fmtTime(t) {
  return String(t || '').replace('T', ' ').substring(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res = await listPeriod()
    rows.value = res.data || []
  } catch (e) {
    ElMessage.error('加载期间失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    loading.value = false
  }
}

async function doClose(row) {
  await ElMessageBox.confirm(
    `确认对 ${row.periodYear} 年 ${row.periodMonth} 月执行期末结账？\n将校验未过账凭证与试算平衡，并自动执行损益结转。`,
    '期末结账', { type: 'warning', confirmButtonText: '结账' })
  try {
    await closePeriod(row.periodYear, row.periodMonth)
    ElMessage.success('结账成功')
    load()
  } catch (e) {
    ElMessage.error('结账失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function doReopen(row) {
  await ElMessageBox.confirm(
    `确认对 ${row.periodYear} 年 ${row.periodMonth} 月执行反结账？\n将删除该期间结转凭证并恢复为开启状态。`,
    '反结账', { type: 'warning', confirmButtonText: '反结账' })
  try {
    await reopenPeriod(row.periodYear, row.periodMonth)
    ElMessage.success('反结账成功')
    load()
  } catch (e) {
    ElMessage.error('反结账失败：' + (e?.response?.data?.msg || e.message))
  }
}

onMounted(load)
</script>
