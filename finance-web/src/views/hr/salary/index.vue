<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="核算月份" style="width: 150px" />
        <el-select v-model="query.employeeId" placeholder="员工" clearable filterable style="width: 180px">
          <el-option v-for="e in employees" :key="e.id" :label="`${e.empNo} ${e.empName}`" :value="e.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" :loading="calcLoading" @click="runCalculate">批量核算（累计预扣个税）</el-button>
        <span class="tip">流程：草稿 → 提交复核 → 发放确认；已复核/已发放不可重算</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="empNo" label="工号" width="100" />
        <el-table-column prop="empName" label="姓名" width="100" />
        <el-table-column label="年月" width="90">
          <template #default="{ row }">{{ row.salaryYear }}-{{ String(row.salaryMonth).padStart(2, '0') }}</template>
        </el-table-column>
        <el-table-column label="应发" width="110" align="right">
          <template #default="{ row }">{{ fmtMoney(gross(row)) }}</template>
        </el-table-column>
        <el-table-column label="社保公积金" width="110" align="right">
          <template #default="{ row }">{{ fmtMoney(add(row.socialSecurity, row.housingFund)) }}</template>
        </el-table-column>
        <el-table-column label="缺勤/其他扣款" width="120" align="right">
          <template #default="{ row }">{{ fmtMoney(row.otherDeduct) }}</template>
        </el-table-column>
        <el-table-column label="个税(累计预扣)" width="120" align="right">
          <template #default="{ row }">
            <span style="color: #e6a23c">{{ fmtMoney(row.tax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实发" width="110" align="right">
          <template #default="{ row }">
            <b>{{ fmtMoney(row.netPay) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="230" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="warning" @click="submit(row)">提交复核</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="success" @click="approve(row)">发放确认</el-button>
            <el-button link type="primary" @click="showSlip(row)">工资条</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                     layout="total, prev, pager, next" style="margin-top: 12px" @change="load" />
    </el-card>

    <!-- 编辑草稿 -->
    <el-dialog v-model="dialog.visible" title="编辑工资草稿" width="620px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="基本工资"><el-input-number v-model="form.baseSalary" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="奖金"><el-input-number v-model="form.bonus" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="补贴"><el-input-number v-model="form.allowance" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="加班费"><el-input-number v-model="form.overtimePay" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="社保"><el-input-number v-model="form.socialSecurity" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="公积金"><el-input-number v-model="form.housingFund" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="其他扣款"><el-input-number v-model="form.otherDeduct" :min="0" :precision="2" :controls="false" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-alert type="info" :closable="false" show-icon title="保存草稿后点击“批量核算”计算个税与实发；个税按累计预扣法（5000×月减除费用 + 阶梯税率）。" />
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveDraft">保存</el-button>
      </template>
    </el-dialog>

    <!-- 工资条 -->
    <el-dialog v-model="slip.visible" title="工资条（仅本人可见）" width="560px">
      <template v-if="slip.data">
        <div class="slip-head">
          <div>{{ slip.data.empName }}（{{ slip.data.empNo }}）</div>
          <div class="sub">{{ slip.data.salaryYear }}年{{ slip.data.salaryMonth }}月 · {{ statusText(slip.data.status) }}</div>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="基本工资">{{ fmtMoney(slip.data.baseSalary) }}</el-descriptions-item>
          <el-descriptions-item label="奖金">{{ fmtMoney(slip.data.bonus) }}</el-descriptions-item>
          <el-descriptions-item label="补贴">{{ fmtMoney(slip.data.allowance) }}</el-descriptions-item>
          <el-descriptions-item label="加班费">{{ fmtMoney(slip.data.overtimePay) }}</el-descriptions-item>
          <el-descriptions-item label="社保">{{ fmtMoney(slip.data.socialSecurity) }}</el-descriptions-item>
          <el-descriptions-item label="公积金">{{ fmtMoney(slip.data.housingFund) }}</el-descriptions-item>
          <el-descriptions-item label="其他扣款">{{ fmtMoney(slip.data.otherDeduct) }}</el-descriptions-item>
          <el-descriptions-item label="个人所得税">
            <span style="color: #e6a23c">{{ fmtMoney(slip.data.tax) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实发工资" :span="2">
            <b style="color: #52c41a; font-size: 16px">{{ fmtMoney(slip.data.netPay) }}</b>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="slip.data.items && slip.data.items.length" style="margin-top: 12px">
          <el-table :data="slip.data.items" size="small" border>
            <el-table-column prop="itemName" label="项目" />
            <el-table-column label="金额" align="right">
              <template #default="{ row }">{{ fmtMoney(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageSalary, editSalaryDraft, calculateSalary, submitSalary, approveSalary, getSalarySlip, pageEmployee } from '@/api'

const loading = ref(false)
const calcLoading = ref(false)
const rows = ref([])
const total = ref(0)
const employees = ref([])
const month = ref(null)
const query = reactive({ employeeId: null, year: null, month: null, page: 1, size: 10 })
const dialog = reactive({ visible: false, id: null })
const form = reactive({})
const slip = reactive({ visible: false, data: null })

const STATUS_MAP = { DRAFT: ['草稿', 'info'], CONFIRMED: ['已复核', 'warning'], PAID: ['已发放', 'success'] }
function statusText(s) { return (STATUS_MAP[s] || [s, 'info'])[0] }
function statusType(s) { return (STATUS_MAP[s] || [s, 'info'])[1] }
function fmtMoney(v) { return '¥ ' + (v == null ? '0.00' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })) }
function add(a, b) { return (Number(a) || 0) + (Number(b) || 0) }
function gross(r) { return add(add(add(r.baseSalary, r.bonus), r.allowance), r.overtimePay) }

async function load() {
  loading.value = true
  try {
    if (month.value) {
      const [y, m] = month.value.split('-')
      query.year = +y
      query.month = +m
    } else {
      query.year = null
      query.month = null
    }
    const res = await pageSalary(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadEmployees() {
  const res = await pageEmployee({ page: 1, size: 200, status: 'ONBOARD' })
  employees.value = res.data.records
}

async function runCalculate() {
  if (!month.value) { ElMessage.warning('请先选择核算月份'); return }
  const [y, m] = month.value.split('-')
  calcLoading.value = true
  try {
    const res = await calculateSalary({ year: +y, month: +m, employeeId: query.employeeId || undefined })
    ElMessage.success(`核算完成，共 ${res.data} 条`)
    load()
  } finally {
    calcLoading.value = false
  }
}

function openEdit(row) {
  dialog.id = row.id
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, {
    baseSalary: row.baseSalary, bonus: row.bonus, allowance: row.allowance, overtimePay: row.overtimePay,
    socialSecurity: row.socialSecurity, housingFund: row.housingFund, otherDeduct: row.otherDeduct
  })
  dialog.visible = true
}

async function saveDraft() {
  await editSalaryDraft(dialog.id, form)
  ElMessage.success('草稿已保存，可执行批量核算')
  dialog.visible = false
  load()
}

async function submit(row) {
  await ElMessageBox.confirm(`确认提交 ${row.empName} ${row.salaryYear}-${row.salaryMonth} 工资复核？`, '提示', { type: 'warning' })
  await submitSalary(row.id)
  ElMessage.success('已提交复核')
  load()
}

async function approve(row) {
  await ElMessageBox.confirm(`确认发放 ${row.empName} ${row.salaryYear}-${row.salaryMonth} 工资 ${fmtMoney(row.netPay)}？`, '提示', { type: 'warning' })
  await approveSalary(row.id)
  ElMessage.success('已发放')
  load()
}

async function showSlip(row) {
  slip.data = null
  slip.visible = true
  try {
    const res = await getSalarySlip(row.id)
    slip.data = res.data
  } catch (e) {
    slip.visible = false
    throw e
  }
}

watch(month, () => load())
onMounted(() => { load(); loadEmployees() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; align-items: center; }
.tip { color: #999; font-size: 12px; margin-left: 8px; }
.slip-head { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.slip-head .sub { font-size: 12px; color: #999; font-weight: 400; margin-top: 4px; }
</style>
