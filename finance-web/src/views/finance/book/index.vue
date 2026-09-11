<template>
  <div style="padding: 16px">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span style="font-weight: 600">账簿查询（F3）</span>
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
            <el-select v-model="periodYear" style="width: 110px">
              <el-option v-for="y in years" :key="y" :label="y + ' 年'" :value="y" />
            </el-select>
            <el-select v-model="periodMonth" style="width: 100px">
              <el-option v-for="m in 12" :key="m" :label="m + ' 月'" :value="m" />
            </el-select>
            <el-select v-model="subjectId" placeholder="全部科目" clearable filterable style="width: 220px">
              <el-option v-for="s in subjectOptions" :key="s.id" :label="s.label" :value="s.id" />
            </el-select>
            <el-button type="primary" @click="load">查询</el-button>
            <el-button :loading="exporting" @click="doExport">导出 Excel</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane label="总账" name="ledger">
          <el-table :data="rows" v-loading="loading" border size="small" max-height="520">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="科目名称" min-width="160" />
            <el-table-column label="期初借方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.initialDebit) }}</template>
            </el-table-column>
            <el-table-column label="期初贷方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.initialCredit) }}</template>
            </el-table-column>
            <el-table-column label="本期借方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodDebit) }}</template>
            </el-table-column>
            <el-table-column label="本期贷方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodCredit) }}</template>
            </el-table-column>
            <el-table-column label="期末借方" align="right" width="130">
              <template #default="{ row }"><b>{{ fmt(row.endingDebit) }}</b></template>
            </el-table-column>
            <el-table-column label="期末贷方" align="right" width="130">
              <template #default="{ row }"><b>{{ fmt(row.endingCredit) }}</b></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="明细账" name="detail">
          <el-table :data="rows" v-loading="loading" border size="small" max-height="520">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="科目名称" min-width="150" />
            <el-table-column label="日期" width="110">
              <template #default="{ row }">{{ row.voucherDate || '-' }}</template>
            </el-table-column>
            <el-table-column prop="voucherNo" label="凭证号" width="140" />
            <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
            <el-table-column label="借方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodDebit) }}</template>
            </el-table-column>
            <el-table-column label="贷方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodCredit) }}</template>
            </el-table-column>
            <el-table-column label="方向" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.direction" size="small" :type="row.direction === 'DEBIT' ? 'success' : 'warning'">
                  {{ row.direction === 'DEBIT' ? '借' : '贷' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="日记账" name="journal">
          <el-table :data="rows" v-loading="loading" border size="small" max-height="520">
            <el-table-column label="日期" width="110">
              <template #default="{ row }">{{ row.voucherDate || '-' }}</template>
            </el-table-column>
            <el-table-column prop="voucherNo" label="凭证号" width="140" />
            <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
            <el-table-column prop="subjectName" label="科目" min-width="150" />
            <el-table-column label="借方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodDebit) }}</template>
            </el-table-column>
            <el-table-column label="贷方" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.periodCredit) }}</template>
            </el-table-column>
            <el-table-column label="方向" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.direction" size="small" :type="row.direction === 'DEBIT' ? 'success' : 'warning'">
                  {{ row.direction === 'DEBIT' ? '借' : '贷' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <el-alert type="info" :closable="false" style="margin-top: 10px"
        title="账簿数据来源：已过账（BOOKED）凭证；总账按科目汇总，明细账/日记账按凭证行展开并附运行余额。" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBook, exportBook, getSubjectTree } from '@/api/index.js'

const loading = ref(false)
const exporting = ref(false)
const rows = ref([])
const activeTab = ref('ledger')
const years = computed(() => {
  const y = new Date().getFullYear()
  return [y - 1, y, y + 1]
})
const periodYear = ref(new Date().getFullYear())
const periodMonth = ref(new Date().getMonth() + 1)
const subjectId = ref(null)
const subjectOptions = ref([])

function fmt(v) {
  return v == null ? '0.00' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function flatTree(nodes, arr = []) {
  (nodes || []).forEach(n => {
    arr.push({ id: n.id, label: `${n.subjectCode} ${n.subjectName}` })
    if (n.children && n.children.length) flatTree(n.children, arr)
  })
  return arr
}

async function load() {
  loading.value = true
  try {
    const res = await getBook(activeTab.value, {
      periodYear: periodYear.value,
      periodMonth: periodMonth.value,
      subjectId: subjectId.value || undefined
    })
    rows.value = res.data || []
  } catch (e) {
    ElMessage.error('查询失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    loading.value = false
  }
}

async function doExport() {
  exporting.value = true
  try {
    const blob = await exportBook(activeTab.value, {
      periodYear: periodYear.value,
      periodMonth: periodMonth.value,
      subjectId: subjectId.value || undefined
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const typeName = { ledger: '总账', detail: '明细账', journal: '日记账' }[activeTab.value]
    a.download = `${typeName}-${periodYear.value}${String(periodMonth.value).padStart(2, '0')}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getSubjectTree()
    subjectOptions.value = flatTree(res.data || [])
  } catch (e) {
    // 科目树加载失败不阻塞账簿查询
  }
  load()
})
</script>
