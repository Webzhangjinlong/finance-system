<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.sourceType" placeholder="来源类型" clearable style="width: 150px">
          <el-option label="费用报销 EXPENSE" value="EXPENSE" />
          <el-option label="合同 CONTRACT" value="CONTRACT" />
          <el-option label="收款 PAYMENT" value="PAYMENT" />
          <el-option label="付款 RECEIPT" value="RECEIPT" />
        </el-select>
        <el-select v-model="query.enabled" placeholder="状态" clearable style="width: 110px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="规则名称" clearable style="width: 160px" @keyup.enter="onSearch" />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button type="success" @click="openCreate">新增规则</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="ruleCode" label="规则编码" width="180" />
        <el-table-column prop="ruleName" label="规则名称" width="150" />
        <el-table-column prop="sourceType" label="来源类型" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ row.sourceType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="eventType" label="触发事件" width="100">
          <template #default="{ row }">{{ row.eventType || '任意' }}</template>
        </el-table-column>
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.direction === 'DEBIT' ? 'warning' : 'success'">
              {{ row.direction === 'DEBIT' ? '借' : '贷' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectCode" label="映射科目" width="100" />
        <el-table-column prop="summaryTemplate" label="摘要模板" min-width="160">
          <template #default="{ row }">{{ row.summaryTemplate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="amountRatio" label="比例" width="70">
          <template #default="{ row }">{{ row.amountRatio }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.enabled === 'ACTIVE' ? 'success' : 'info'">
              {{ row.enabled === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.enabled === 'ACTIVE'" link type="warning" @click="onSwitch(row, 'DISABLED')">停用</el-button>
            <el-button v-else link type="success" @click="onSwitch(row, 'ACTIVE')">启用</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="mt-2" layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page" @current-change="onPage" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑映射规则' : '新增映射规则'" width="560px">
      <el-form ref="formRef" :model="form" label-width="90px" :rules="rules">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" :disabled="!!form.id" placeholder="如 EXPENSE_PAID_CREDIT" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="如 报销打款贷方" />
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="form.sourceType" style="width: 100%">
            <el-option label="费用报销 EXPENSE" value="EXPENSE" />
            <el-option label="合同 CONTRACT" value="CONTRACT" />
            <el-option label="收款 PAYMENT" value="PAYMENT" />
            <el-option label="付款 RECEIPT" value="RECEIPT" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发事件">
          <el-input v-model="form.eventType" placeholder="PAID/APPROVED/BOOKED，留空=任意" />
        </el-form-item>
        <el-form-item label="方向" prop="direction">
          <el-radio-group v-model="form.direction">
            <el-radio value="DEBIT">借</el-radio>
            <el-radio value="CREDIT">贷</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="映射科目" prop="subjectCode">
          <el-input v-model="form.subjectCode" placeholder="如 1002 银行存款" />
        </el-form-item>
        <el-form-item label="摘要模板">
          <el-input v-model="form.summaryTemplate" placeholder="如 报销打款 {claimNo}" />
        </el-form-item>
        <el-form-item label="金额比例">
          <el-input-number v-model="form.amountRatio" :min="0.01" :max="999.99" :step="0.01" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageVoucherRule, createVoucherRule, updateVoucherRule, switchVoucherRuleStatus, deleteVoucherRule } from '@/api'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, sourceType: null, enabled: null, keyword: '' })

const formVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, ruleCode: '', ruleName: '', sourceType: 'EXPENSE', eventType: '', direction: 'CREDIT', subjectCode: '', summaryTemplate: '', amountRatio: 1 })
const rules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  direction: [{ required: true, message: '请选择方向', trigger: 'change' }],
  subjectCode: [{ required: true, message: '请输入映射科目', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await pageVoucherRule(query)
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() { query.page = 1; load() }
function onPage(p) { query.page = p; load() }

function openCreate() {
  Object.assign(form, { id: null, ruleCode: '', ruleName: '', sourceType: 'EXPENSE', eventType: '', direction: 'CREDIT', subjectCode: '', summaryTemplate: '', amountRatio: 1 })
  formVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, ruleCode: row.ruleCode, ruleName: row.ruleName, sourceType: row.sourceType,
    eventType: row.eventType || '', direction: row.direction, subjectCode: row.subjectCode,
    summaryTemplate: row.summaryTemplate || '', amountRatio: Number(row.amountRatio) || 1
  })
  formVisible.value = true
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateVoucherRule(form.id, form)
    } else {
      await createVoucherRule(form)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onSwitch(row, enabled) {
  try {
    await switchVoucherRuleStatus(row.id, enabled)
    ElMessage.success(enabled === 'ACTIVE' ? '已启用' : '已停用')
    load()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」？`, '提示', { type: 'warning' })
    await deleteVoucherRule(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.mt-2 { margin-top: 12px; justify-content: flex-end; }
</style>
