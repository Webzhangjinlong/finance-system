<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="bar">
          <span class="title">财务报表</span>
          <div class="filters">
            <el-select v-model="periodYear" style="width: 120px" @change="load">
              <el-option v-for="y in years" :key="y" :label="y + ' 年'" :value="y" />
            </el-select>
            <el-select v-model="periodMonth" style="width: 110px" @change="load">
              <el-option v-for="m in 12" :key="m" :label="m + ' 月'" :value="m" />
            </el-select>
            <el-button type="primary" @click="load">查询</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 资产负债表 -->
        <el-tab-pane label="资产负债表" name="balance">
          <el-alert
            v-if="balance.balanced === true"
            title="校验通过：资产 = 负债 + 权益（含本期净利润）"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-alert
            v-else-if="balance.balanced === false"
            title="⚠ 不平衡：资产 ≠ 负债 + 权益，请检查凭证"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-table :data="balance.items" border size="small" max-height="420">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="科目名称" />
            <el-table-column label="类别" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="sectionTag(row.section)">{{ sectionText(row.section) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="balance" label="余额" align="right" width="160">
              <template #default="{ row }">{{ fmt(row.balance) }}</template>
            </el-table-column>
          </el-table>
          <div class="totals">
            <span>资产合计：<b>{{ fmt(balance.totalAssets) }}</b></span>
            <span>负债合计：<b>{{ fmt(balance.totalLiabilities) }}</b></span>
            <span>权益合计：<b>{{ fmt(balance.totalEquity) }}</b></span>
            <span>本期净利润：<b>{{ fmt(balance.netProfit) }}</b></span>
            <span>负债+权益合计：<b>{{ fmt(balance.totalLiabEquity) }}</b></span>
          </div>
        </el-tab-pane>

        <!-- 利润表 -->
        <el-tab-pane label="利润表" name="income">
          <el-table :data="income.revenues" border size="small" max-height="180">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="收入科目" />
            <el-table-column prop="amount" label="本期发生额（贷）" align="right" width="160">
              <template #default="{ row }">{{ fmt(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <el-table :data="income.expenses" border size="small" max-height="180" style="margin-top: 8px">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="费用科目" />
            <el-table-column prop="amount" label="本期发生额（借）" align="right" width="160">
              <template #default="{ row }">{{ fmt(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <div class="totals">
            <span>收入合计：<b>{{ fmt(income.totalRevenue) }}</b></span>
            <span>费用合计：<b>{{ fmt(income.totalExpense) }}</b></span>
            <span>净利润：<b :style="{ color: Number(income.netProfit) >= 0 ? '#52c41a' : '#ea6668' }">{{ fmt(income.netProfit) }}</b></span>
          </div>
        </el-tab-pane>

        <!-- 现金流量表 -->
        <el-tab-pane label="现金流量表" name="cash">
          <el-table :data="cash.items" border size="small" max-height="260">
            <el-table-column prop="subjectCode" label="科目编码" width="120" />
            <el-table-column prop="subjectName" label="货币资金科目" />
            <el-table-column prop="inflow" label="流入" align="right" width="150">
              <template #default="{ row }">{{ fmt(row.inflow) }}</template>
            </el-table-column>
            <el-table-column prop="outflow" label="流出" align="right" width="150">
              <template #default="{ row }">{{ fmt(row.outflow) }}</template>
            </el-table-column>
          </el-table>
          <div class="totals">
            <span>现金流入：<b>{{ fmt(cash.inflow) }}</b></span>
            <span>现金流出：<b>{{ fmt(cash.outflow) }}</b></span>
            <span>净现金流量：<b :style="{ color: Number(cash.netCash) >= 0 ? '#52c41a' : '#ea6668' }">{{ fmt(cash.netCash) }}</b></span>
          </div>
          <el-alert v-if="cash.note" :title="cash.note" type="info" :closable="false" style="margin-top: 8px" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBalanceSheet, getIncomeStatement, getCashFlow } from '@/api'

const activeTab = ref('balance')
const now = new Date()
const periodYear = ref(now.getFullYear())
const periodMonth = ref(now.getMonth() + 1)
const years = []
for (let y = now.getFullYear() - 3; y <= now.getFullYear(); y++) years.push(y)

const balance = ref({ items: [], balanced: null })
const income = ref({ revenues: [], expenses: [], totalRevenue: 0, totalExpense: 0, netProfit: 0 })
const cash = ref({ items: [], inflow: 0, outflow: 0, netCash: 0, note: '' })

function fmt(v) {
  return Number(v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function sectionTag(s) {
  return { ASSET: 'success', LIABILITY: 'warning', EQUITY: 'info', PROFIT: 'danger' }[s] || 'info'
}
function sectionText(s) {
  return { ASSET: '资产', LIABILITY: '负债', EQUITY: '权益', PROFIT: '损益' }[s] || s
}

async function load() {
  try {
    const params = { periodYear: periodYear.value, periodMonth: periodMonth.value }
    const [b, i, c] = await Promise.all([
      getBalanceSheet(params),
      getIncomeStatement(params),
      getCashFlow(params)
    ])
    balance.value = b.data
    income.value = i.data
    cash.value = c.data
  } catch (e) {
    ElMessage.error(e?.msg || '报表加载失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 16px; }
.bar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.title { font-size: 16px; font-weight: 600; }
.filters { display: flex; gap: 8px; }
.totals { display: flex; gap: 20px; flex-wrap: wrap; margin-top: 12px; font-size: 13px; color: #374151; }
</style>
