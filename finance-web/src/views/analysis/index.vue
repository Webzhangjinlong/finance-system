<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">财务分析（F9）</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- ============ 科目余额表（试算平衡） ============ -->
        <el-tab-pane label="科目余额表" name="trial">
          <el-form inline style="margin-bottom: 12px">
            <el-form-item label="期间">
              <el-date-picker
                v-model="period"
                type="month"
                value-format="YYYY-M"
                placeholder="选择期间"
                style="width: 140px"
                @change="loadTrialBalance"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="trialLoading" @click="loadTrialBalance">查询</el-button>
            </el-form-item>
            <el-form-item v-if="trial.balanced !== null">
              <el-tag v-if="trial.balanced === true" type="success">试算平衡 ✓</el-tag>
              <el-tag v-else type="danger">试算不平衡 ✗</el-tag>
            </el-form-item>
          </el-form>

          <el-table :data="trial.rows" border size="small" max-height="460" show-summary
                    :summary-method="trialSummary" style="width: 100%">
            <el-table-column prop="subjectCode" label="科目编码" width="100" fixed="left" />
            <el-table-column prop="subjectName" label="科目名称" min-width="140" fixed="left" />
            <el-table-column prop="subjectType" label="类型" width="80" />
            <el-table-column label="期初借方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.openingDebit) }}</template>
            </el-table-column>
            <el-table-column label="期初贷方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.openingCredit) }}</template>
            </el-table-column>
            <el-table-column label="本期借方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.periodDebit) }}</template>
            </el-table-column>
            <el-table-column label="本期贷方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.periodCredit) }}</template>
            </el-table-column>
            <el-table-column label="期末借方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.closingDebit) }}</template>
            </el-table-column>
            <el-table-column label="期末贷方" align="right" width="110">
              <template #default="{ row }">{{ fmt(row.closingCredit) }}</template>
            </el-table-column>
          </el-table>
          <div style="color: #909399; font-size: 12px; margin-top: 8px">
            口径：仅已过账凭证；期初 = 截止上期累计，本期 = 期间发生，期末 = 期初 + 本期，按科目方向归位借贷栏。
          </div>
        </el-tab-pane>

        <!-- ============ 费用月度趋势 ============ -->
        <el-tab-pane label="费用月度趋势" name="trend">
          <el-form inline style="margin-bottom: 12px">
            <el-form-item label="年份">
              <el-date-picker
                v-model="trendYear"
                type="year"
                value-format="YYYY"
                placeholder="选择年份"
                style="width: 140px"
                @change="loadExpenseTrend"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="trendLoading" @click="loadExpenseTrend">查询</el-button>
            </el-form-item>
          </el-form>
          <div id="expense-trend-chart" style="width: 100%; height: 400px"></div>
          <div style="color: #909399; font-size: 12px; margin-top: 8px">
            口径：费用类科目（损益 + 借方方向）当年 1-12 月借方发生额汇总，仅已过账凭证。
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getTrialBalance, getExpenseTrend } from '@/api/index.js'

const activeTab = ref('trial')
const period = ref('2026-9')
const trial = reactive({ rows: [], balanced: null, openingDebitTotal: '0', openingCreditTotal: '0',
  periodDebitTotal: '0', periodCreditTotal: '0', closingDebitTotal: '0', closingCreditTotal: '0' })
const trialLoading = ref(false)
const trendYear = ref('2026')
const trendLoading = ref(false)
let chart = null

function fmt(v) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function parsePeriod(p) {
  const m = String(p || '2026-9').split('-')
  return { year: Number(m[0]), month: Number(m[1] || 1) }
}

function trialSummary({ columns }) {
  const sums = ['合计', '', '']
  const fields = ['openingDebitTotal', 'openingCreditTotal', 'periodDebitTotal',
    'periodCreditTotal', 'closingDebitTotal', 'closingCreditTotal']
  fields.forEach((f, i) => {
    sums.push(fmt(trial[f]))
  })
  return sums
}

async function loadTrialBalance() {
  const { year, month } = parsePeriod(period.value)
  trialLoading.value = true
  try {
    const res = await getTrialBalance({ periodYear: year, periodMonth: month })
    Object.assign(trial, res.data)
  } catch (e) {
    ElMessage.error('加载科目余额表失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    trialLoading.value = false
  }
}

async function loadExpenseTrend() {
  trendLoading.value = true
  try {
    const res = await getExpenseTrend({ periodYear: Number(trendYear.value) })
    renderTrend(res.data)
  } catch (e) {
    ElMessage.error('加载费用趋势失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    trendLoading.value = false
  }
}

function renderTrend(rows) {
  nextTick(() => {
    const el = document.getElementById('expense-trend-chart')
    if (!el) return
    if (!chart) chart = echarts.init(el)
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
    const subjects = {}
    ;(rows || []).forEach(r => {
      if (!subjects[r.subjectCode]) subjects[r.subjectCode] = { name: r.subjectName || r.subjectCode, data: Array(12).fill(0) }
      subjects[r.subjectCode].data[r.periodMonth - 1] = Number(r.periodDebit || 0)
    })
    chart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis', confine: true },
      legend: { data: Object.keys(subjects).map(k => subjects[k].name), type: 'scroll', bottom: 0 },
      grid: { left: 60, right: 20, top: 30, bottom: 60, containLabel: true },
      xAxis: { type: 'category', data: months },
      yAxis: { type: 'value', name: '金额（元）' },
      series: Object.keys(subjects).map(k => ({
        name: subjects[k].name,
        type: 'line',
        smooth: true,
        data: subjects[k].data,
        emphasis: { focus: 'series' }
      })),
      resizeObserver: true
    })
  })
}

function onResize() {
  if (chart) chart.resize()
}

onMounted(() => {
  loadTrialBalance()
  loadExpenseTrend()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (chart) { chart.dispose(); chart = null }
})
</script>
