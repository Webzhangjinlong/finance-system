<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="bar">
          <span class="title">应收/应付账龄</span>
          <div class="filters">
            <el-date-picker v-model="asOf" type="date" placeholder="基准日（默认今天）" value-format="YYYY-MM-DD" @change="load" />
            <el-button type="primary" @click="load">查询</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane label="应收账龄" name="AR">
          <div class="cards">
            <div v-for="s in ar.summary" :key="s.bucket" class="card" :class="'c-' + s.bucket">
              <div class="card-name">{{ bucketText(s.bucket) }}</div>
              <div class="card-amt">{{ fmt(s.amount) }}</div>
              <div class="card-count">{{ s.count }} 笔</div>
            </div>
          </div>
          <el-table :data="ar.detail" border size="small" max-height="340">
            <el-table-column prop="docNo" label="单据号" width="150" />
            <el-table-column prop="counterparty" label="客户" />
            <el-table-column prop="dueDate" label="到期日" width="120" />
            <el-table-column prop="amount" label="金额" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="settled" label="已收" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.settled) }}</template>
            </el-table-column>
            <el-table-column prop="balance" label="余额" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.balance) }}</template>
            </el-table-column>
            <el-table-column label="逾期天数" width="100" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.days > 0 ? '#ea6668' : '#374151' }">{{ row.days > 0 ? row.days + ' 天' : '未到期' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="账龄" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="bucketTag(row.bucket)">{{ bucketText(row.bucket) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!ar.detail.length" description="暂无未结清应收" />
        </el-tab-pane>

        <el-tab-pane label="应付账龄" name="AP">
          <div class="cards">
            <div v-for="s in ap.summary" :key="s.bucket" class="card" :class="'c-' + s.bucket">
              <div class="card-name">{{ bucketText(s.bucket) }}</div>
              <div class="card-amt">{{ fmt(s.amount) }}</div>
              <div class="card-count">{{ s.count }} 笔</div>
            </div>
          </div>
          <el-table :data="ap.detail" border size="small" max-height="340">
            <el-table-column prop="docNo" label="单据号" width="150" />
            <el-table-column prop="counterparty" label="供应商" />
            <el-table-column prop="dueDate" label="到期日" width="120" />
            <el-table-column prop="amount" label="金额" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="settled" label="已付" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.settled) }}</template>
            </el-table-column>
            <el-table-column prop="balance" label="余额" align="right" width="130">
              <template #default="{ row }">{{ fmt(row.balance) }}</template>
            </el-table-column>
            <el-table-column label="逾期天数" width="100" align="center">
              <template #default="{ row }">
                <span :style="{ color: row.days > 0 ? '#ea6668' : '#374151' }">{{ row.days > 0 ? row.days + ' 天' : '未到期' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="账龄" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="bucketTag(row.bucket)">{{ bucketText(row.bucket) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!ap.detail.length" description="暂无未结清应付" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getArAging, getApAging } from '@/api'

const activeTab = ref('AR')
const asOf = ref('')
const ar = ref({ summary: [], detail: [] })
const ap = ref({ summary: [], detail: [] })

const BUCKETS = {
  NOT_DUE: '未到期', D0_30: '0-30 天', D31_60: '31-60 天', D61_90: '61-90 天', D90_PLUS: '90 天以上'
}
function bucketText(b) { return BUCKETS[b] || b }
function bucketTag(b) {
  return { NOT_DUE: 'success', D0_30: '', D31_60: 'warning', D61_90: 'warning', D90_PLUS: 'danger' }[b] || 'info'
}
function fmt(v) {
  return Number(v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  try {
    const params = asOf.value ? { asOf: asOf.value } : {}
    const [a, p] = await Promise.all([getArAging(params), getApAging(params)])
    ar.value = a.data
    ap.value = p.data
  } catch (e) {
    ElMessage.error(e?.msg || '账龄加载失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
.bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.title { font-size: 16px; font-weight: 600; }
.filters { display: flex; gap: 8px; }
.cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
.card { flex: 1 1 130px; min-width: 120px; padding: 12px; border-radius: 10px; border: 1px solid rgba(0, 0, 0, 0.08); }
.card-name { font-size: 12px; color: #6b7280; }
.card-amt { font-size: 18px; font-weight: 600; color: #1a1b1c; margin-top: 4px; }
.card-count { font-size: 12px; color: #9ca3af; margin-top: 2px; }
.c-NOT_DUE { background: rgba(82, 196, 26, 0.08); }
.c-D0_30 { background: rgba(139, 200, 234, 0.14); }
.c-D31_60 { background: rgba(250, 173, 20, 0.12); }
.c-D61_90 { background: rgba(250, 173, 20, 0.18); }
.c-D90_PLUS { background: rgba(234, 102, 104, 0.14); }
</style>
