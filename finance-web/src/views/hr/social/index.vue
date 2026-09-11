<template>
  <div style="padding: 16px">
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px">
          <span style="font-weight: 600">社保公积金（五险一金 · 智能核算）</span>
        </div>
      </template>

      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        title="政策规则配置化：费率/基数上下限存规则表，换政策只改表不改代码；工资核算自动按基数（申报基数或基本工资，clamp 上下限）× 比例计算个人/单位缴费。" />

      <el-tabs v-model="activeTab">
        <!-- ============ Tab 1: 规则配置 ============ -->
        <el-tab-pane label="规则配置" name="rule">
          <div style="display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap">
            <el-select v-model="ruleQuery.socialType" placeholder="全部险种" clearable style="width: 180px">
              <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-button type="primary" @click="loadRules">查询</el-button>
            <el-button type="success" @click="openRuleDialog()">新增规则</el-button>
            <div style="flex: 1"></div>
            <el-select v-model="calcMonth" style="width: 120px">
              <el-option v-for="m in 12" :key="m" :label="m + ' 月'" :value="m" />
            </el-select>
            <el-button :loading="calculating" @click="doCalculate">一键核算缴费</el-button>
          </div>
          <el-table :data="rules" v-loading="ruleLoading" border size="small">
            <el-table-column prop="typeName" label="险种" width="120" />
            <el-table-column prop="socialType" label="类型编码" width="140" />
            <el-table-column prop="effectiveMonth" label="生效月" width="100" />
            <el-table-column label="基数下限" width="120" align="right">
              <template #default="{ row }">{{ fmt(row.baseFloor) }}</template>
            </el-table-column>
            <el-table-column label="基数上限" width="120" align="right">
              <template #default="{ row }">{{ fmt(row.baseCeiling) }}</template>
            </el-table-column>
            <el-table-column label="个人比例" width="110" align="right">
              <template #default="{ row }">{{ pct(row.personalRate) }}</template>
            </el-table-column>
            <el-table-column label="单位比例" width="110" align="right">
              <template #default="{ row }">{{ pct(row.companyRate) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openRuleDialog(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 10px" layout="total, prev, pager, next" :total="ruleTotal"
            :page-size="10" :current-page="rulePage" @current-change="p => { rulePage = p; loadRules() }" />
        </el-tab-pane>

        <!-- ============ Tab 2: 缴费明细 ============ -->
        <el-tab-pane label="缴费明细" name="detail">
          <div style="display: flex; gap: 8px; margin-bottom: 10px; flex-wrap: wrap">
            <el-select v-model="detailYear" style="width: 110px">
              <el-option v-for="y in years" :key="y" :label="y + ' 年'" :value="y" />
            </el-select>
            <el-select v-model="detailMonth" style="width: 100px">
              <el-option v-for="m in 12" :key="m" :label="m + ' 月'" :value="m" />
            </el-select>
            <el-select v-model="detailEmployeeId" placeholder="全部员工" clearable filterable style="width: 220px">
              <el-option v-for="e in employees" :key="e.id" :label="`${e.empNo} ${e.empName}`" :value="e.id" />
            </el-select>
            <el-button type="primary" @click="loadDetails">查询</el-button>
          </div>
          <el-table :data="details" v-loading="detailLoading" border size="small">
            <el-table-column prop="empNo" label="工号" width="110" />
            <el-table-column prop="empName" label="姓名" width="110" />
            <el-table-column prop="typeName" label="险种" width="120" />
            <el-table-column prop="salaryYear" label="年" width="70" align="center" />
            <el-table-column prop="salaryMonth" label="月" width="70" align="center" />
            <el-table-column label="缴费基数" width="130" align="right">
              <template #default="{ row }">{{ fmt(row.baseAmount) }}</template>
            </el-table-column>
            <el-table-column label="个人缴纳" width="130" align="right">
              <template #default="{ row }"><b>{{ fmt(row.personalAmount) }}</b></template>
            </el-table-column>
            <el-table-column label="单位缴纳" width="130" align="right">
              <template #default="{ row }">{{ fmt(row.companyAmount) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination style="margin-top: 10px" layout="total, prev, pager, next" :total="detailTotal"
            :page-size="10" :current-page="detailPage" @current-change="p => { detailPage = p; loadDetails() }" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 规则弹窗 -->
    <el-dialog v-model="ruleDialog" :title="ruleForm.id ? '编辑规则' : '新增规则'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="险种" required>
          <el-select v-model="ruleForm.socialType" style="width: 100%" :disabled="!!ruleForm.id">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效月" required>
          <el-input v-model="ruleForm.effectiveMonth" placeholder="如 2026-10" />
        </el-form-item>
        <el-form-item label="基数下限" required>
          <el-input-number v-model="ruleForm.baseFloor" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="基数上限" required>
          <el-input-number v-model="ruleForm.baseCeiling" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="个人比例" required>
          <el-input-number v-model="ruleForm.personalRate" :min="0" :max="1" :step="0.005" :precision="4" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位比例" required>
          <el-input-number v-model="ruleForm.companyRate" :min="0" :max="1" :step="0.005" :precision="4" :controls="false" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { socialRulePage, socialRuleSave, socialRuleDelete, socialCalculate, socialDetailPage, pageEmployee } from '@/api/index.js'

const types = [
  { value: 'PENSION', label: '养老保险' },
  { value: 'MEDICAL', label: '医疗保险' },
  { value: 'UNEMPLOYMENT', label: '失业保险' },
  { value: 'INJURY', label: '工伤保险' },
  { value: 'MATERNITY', label: '生育保险' },
  { value: 'HOUSING_FUND', label: '住房公积金' }
]

const activeTab = ref('rule')
const years = [new Date().getFullYear() - 1, new Date().getFullYear(), new Date().getFullYear() + 1]

function fmt(v) {
  return v == null ? '0.00' : Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function pct(v) {
  return v == null ? '0%' : (Number(v) * 100).toFixed(1).replace(/\.0$/, '') + '%'
}

// ===== 规则 =====
const rules = ref([])
const ruleTotal = ref(0)
const rulePage = ref(1)
const ruleLoading = ref(false)
const ruleQuery = ref({ socialType: null })
const ruleDialog = ref(false)
const ruleForm = ref({})
const saving = ref(false)
const calculating = ref(false)
const calcYear = ref(new Date().getFullYear())
const calcMonth = ref(new Date().getMonth() + 1)

async function loadRules() {
  ruleLoading.value = true
  try {
    const res = await socialRulePage({ socialType: ruleQuery.value.socialType || undefined, page: rulePage.value, size: 10 })
    rules.value = res.data.records || []
    ruleTotal.value = Number(res.data.total || 0)
  } catch (e) {
    ElMessage.error('加载规则失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    ruleLoading.value = false
  }
}

function openRuleDialog(row) {
  ruleForm.value = row ? { ...row } : { socialType: 'PENSION', effectiveMonth: `${calcYear.value}-${String(calcMonth.value).padStart(2, '0')}`, baseFloor: 4800, baseCeiling: 24000, personalRate: 0.08, companyRate: 0.16 }
  ruleDialog.value = true
}

async function saveRule() {
  if (!ruleForm.value.socialType || !ruleForm.value.effectiveMonth) {
    ElMessage.warning('请填写险种与生效月')
    return
  }
  if (Number(ruleForm.value.baseCeiling) < Number(ruleForm.value.baseFloor)) {
    ElMessage.warning('基数上限不能小于下限')
    return
  }
  saving.value = true
  try {
    await socialRuleSave({
      socialType: ruleForm.value.socialType,
      effectiveMonth: ruleForm.value.effectiveMonth,
      baseFloor: ruleForm.value.baseFloor,
      baseCeiling: ruleForm.value.baseCeiling,
      personalRate: ruleForm.value.personalRate,
      companyRate: ruleForm.value.companyRate
    })
    ElMessage.success('规则已保存')
    ruleDialog.value = false
    loadRules()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    saving.value = false
  }
}

async function removeRule(row) {
  await ElMessageBox.confirm(`确认删除 ${row.typeName}（${row.effectiveMonth}）规则？`, '删除规则', { type: 'warning' })
  try {
    await socialRuleDelete(row.id)
    ElMessage.success('已删除')
    loadRules()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function doCalculate() {
  calculating.value = true
  try {
    const n = await socialCalculate({ year: calcYear.value, month: calcMonth.value })
    ElMessage.success(`核算完成：写入 ${n.data} 条缴费明细`)
    activeTab.value = 'detail'
    detailYear.value = calcYear.value
    detailMonth.value = calcMonth.value
    loadDetails()
  } catch (e) {
    ElMessage.error('核算失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    calculating.value = false
  }
}

// ===== 明细 =====
const details = ref([])
const detailTotal = ref(0)
const detailPage = ref(1)
const detailLoading = ref(false)
const detailYear = ref(new Date().getFullYear())
const detailMonth = ref(new Date().getMonth() + 1)
const detailEmployeeId = ref(null)
const employees = ref([])

async function loadDetails() {
  detailLoading.value = true
  try {
    const res = await socialDetailPage({ year: detailYear.value, month: detailMonth.value, employeeId: detailEmployeeId.value || undefined, page: detailPage.value, size: 10 })
    details.value = res.data.records || []
    detailTotal.value = Number(res.data.total || 0)
  } catch (e) {
    ElMessage.error('加载明细失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    detailLoading.value = false
  }
}

async function loadEmployees() {
  try {
    const res = await pageEmployee({ status: 'ONBOARD', page: 1, size: 200 })
    employees.value = res.data.records || []
  } catch (e) { /* 不阻塞 */ }
}

onMounted(() => {
  loadRules()
  loadEmployees()
  loadDetails()
})
</script>
