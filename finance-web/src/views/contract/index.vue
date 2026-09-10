<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>合同台账（C1/C2/C3）</b>
        <el-button type="primary" size="small" @click="openCreate">新建合同</el-button>
      </div>
    </template>

    <el-table :data="rows" border size="small" v-loading="loading">
      <el-table-column prop="contractNo" label="合同编号" width="150" />
      <el-table-column prop="contractName" label="合同名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="counterparty" label="往来方" width="140" show-overflow-tooltip />
      <el-table-column prop="contractType" label="类型" width="80">
        <template #default="{ row }">{{ row.contractType === 'SALES' ? '销售' : '采购' }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="110" align="right" />
      <el-table-column prop="startDate" label="开始" width="100" />
      <el-table-column prop="endDate" label="结束" width="100" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="contractStatusType(row.status)" size="small">{{ contractStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="primary" size="small" @click="openSubmit(row)">提交审批</el-button>
          <el-button v-if="row.status !== 'VOID' && row.status !== 'TERMINATED'" link type="warning" size="small" @click="openVoid(row)">作废</el-button>
          <el-button link type="info" size="small" @click="openPlans(row)">计划</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />

    <!-- 新建合同 -->
    <el-dialog v-model="form.visible" title="新建合同" width="560px">
      <el-form :model="form.data" label-width="90px" size="small">
        <el-form-item label="合同名称" required>
          <el-input v-model="form.data.contractName" />
        </el-form-item>
        <el-form-item label="往来方" required>
          <el-input v-model="form.data.counterparty" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.data.contractType">
            <el-radio label="SALES">销售（收款计划）</el-radio>
            <el-radio label="PURCHASE">采购（付款计划）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="form.data.amount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="签订日期">
          <el-date-picker v-model="form.data.signDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.data.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.data.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.data.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.visible = false">取消</el-button>
        <el-button type="primary" :loading="form.saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交审批 -->
    <el-dialog v-model="submit.visible" title="提交审批" width="420px">
      <el-form label-width="90px" size="small">
        <el-form-item label="审批人" required>
          <el-input v-model="submit.approver" placeholder="一期单人审批：填 admin" />
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="submit.comment" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submit.visible = false">取消</el-button>
        <el-button type="primary" :loading="submit.saving" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 计划详情 -->
    <el-drawer v-model="plans.visible" :title="`收付款计划 · ${plans.contract?.contractNo || ''}`" size="560px">
      <div class="drawer-toolbar">
        <el-button size="small" type="primary" :loading="plans.syncing" @click="doSync">同步到期计划（生成应收/应付）</el-button>
        <span class="tip">状态：UNPAID 未收付 / PARTIAL 部分 / PAID 结清 / OVERDUE 逾期（每日 08:30 自动标记）</span>
      </div>
      <el-table :data="plans.rows" border size="small">
        <el-table-column prop="planNo" label="计划号" width="120" />
        <el-table-column prop="planType" label="方向" width="70">
          <template #default="{ row }">{{ row.planType === 'RECEIPT' ? '收款' : '付款' }}</template>
        </el-table-column>
        <el-table-column prop="planDate" label="计划日期" width="100" />
        <el-table-column prop="amount" label="金额" width="100" align="right" />
        <el-table-column prop="paidAmount" label="已收付" width="100" align="right" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="planStatusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核销" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'PAID'" link type="success" size="small"
              @click="openSettle(row)">{{ row.planType === 'RECEIPT' ? '收款核销' : '付款核销' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="settle.visible" :title="settle.plan ? `${settle.plan.planType === 'RECEIPT' ? '收款' : '付款'}核销 · ${settle.plan.planNo}` : ''" width="420px" append-to-body>
        <el-form label-width="90px" size="small">
          <el-form-item label="本次金额" required>
            <el-input-number v-model="settle.amount" :min="0.01" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="settle.remark" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="settle.visible = false">取消</el-button>
          <el-button type="primary" :loading="settle.saving" @click="onSettle">确认核销</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageContract, createContract, submitContract, voidContract, listPlans, syncPlans, registerReceipt, registerPayment } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10 })

const CS = { DRAFT: '草稿', APPROVING: '审批中', APPROVED: '已生效', REJECTED: '已驳回', COMPLETED: '已完成', VOID: '已作废', TERMINATED: '已终止' }
const contractStatusText = (s) => CS[s] || s
const contractStatusType = (s) => ({ DRAFT: 'info', APPROVING: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: 'success', VOID: 'danger', TERMINATED: 'info' }[s] || 'info')
const planStatusType = (s) => ({ UNPAID: 'info', PARTIAL: 'warning', PAID: 'success', OVERDUE: 'danger' }[s] || 'info')

const form = reactive({
  visible: false, saving: false,
  data: { contractName: '', counterparty: '', contractType: 'SALES', amount: 0, signDate: '', startDate: '', endDate: '', remark: '' }
})
const submit = reactive({ visible: false, saving: false, id: null, approver: 'admin', comment: '' })
const plans = reactive({ visible: false, syncing: false, contract: null, rows: [] })
const settle = reactive({ visible: false, saving: false, plan: null, amount: 0, remark: '' })

async function load() {
  loading.value = true
  try {
    const res = await pageContract({ page: query.page, size: query.size })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.data = {
    contractName: '', counterparty: '', contractType: 'SALES', amount: 0,
    signDate: new Date().toISOString().slice(0, 10),
    startDate: new Date().toISOString().slice(0, 10),
    endDate: '', remark: ''
  }
  form.visible = true
}

async function onSave() {
  const f = form.data
  if (!f.contractName || !f.counterparty || !f.amount || !f.endDate) {
    ElMessage.warning('名称/往来方/金额/结束日期必填')
    return
  }
  form.saving = true
  try {
    await createContract(f)
    ElMessage.success('合同已创建（草稿，编号自动生成）')
    form.visible = false
    load()
  } finally {
    form.saving = false
  }
}

function openSubmit(row) {
  submit.id = row.id
  submit.approver = 'admin'
  submit.comment = ''
  submit.visible = true
}
async function onSubmit() {
  submit.saving = true
  try {
    await submitContract(submit.id, { approver: submit.approver, comment: submit.comment })
    ElMessage.success('已提交审批，可在「审批待办」中处理')
    submit.visible = false
    load()
  } finally {
    submit.saving = false
  }
}

async function openVoid(row) {
  const { value } = await ElMessageBox.prompt(`作废合同 ${row.contractNo}？请填写作废原因`, '作废', { type: 'warning', inputPlaceholder: '作废原因' })
  await voidContract(row.id, value || '作废')
  ElMessage.success('已作废')
  load()
}

async function openPlans(row) {
  plans.contract = row
  plans.visible = true
  await loadPlans(row.id)
}
async function loadPlans(id) {
  const res = await listPlans(id)
  plans.rows = res.data || []
}
async function doSync() {
  plans.syncing = true
  try {
    const res = await syncPlans(plans.contract.id)
    ElMessage.success(`同步完成，生成 ${res.data || 0} 张应收/应付单（幂等）`)
    loadPlans(plans.contract.id)
  } finally {
    plans.syncing = false
  }
}

function openSettle(row) {
  settle.plan = row
  settle.amount = Number(row.amount) - Number(row.paidAmount || 0)
  settle.remark = ''
  settle.visible = true
}
async function onSettle() {
  const p = settle.plan
  settle.saving = true
  try {
    if (p.planType === 'RECEIPT') {
      await registerReceipt(p.id, settle.amount, settle.remark)
    } else {
      await registerPayment(p.id, settle.amount, settle.remark)
    }
    ElMessage.success('核销成功，计划状态已回写')
    settle.visible = false
    loadPlans(plans.contract.id)
    load()
  } finally {
    settle.saving = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.drawer-toolbar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.tip {
  font-size: 12px;
  color: #6b7280;
}
</style>
