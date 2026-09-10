<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>凭证管理（F2）</b>
        <div>
          <el-button type="primary" size="small" @click="openCreate">录入凭证</el-button>
        </div>
      </div>
    </template>

    <el-form inline size="small" style="margin-bottom: 10px">
      <el-form-item label="期间">
        <el-input v-model="query.periodYear" placeholder="年" style="width: 80px" />
        <span style="margin: 0 4px">-</span>
        <el-input v-model="query.periodMonth" placeholder="月" style="width: 70px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" border size="small" v-loading="loading">
      <el-table-column prop="voucherNo" label="凭证号" width="150" />
      <el-table-column prop="voucherDate" label="日期" width="110" />
      <el-table-column prop="periodYear" label="期间" width="90">
        <template #default="{ row }">{{ row.periodYear }}-{{ String(row.periodMonth).padStart(2, '0') }}</template>
      </el-table-column>
      <el-table-column prop="totalDebit" label="借方合计" width="120" align="right" />
      <el-table-column prop="totalCredit" label="贷方合计" width="120" align="right" />
      <el-table-column prop="voucherStatus" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.voucherStatus)" size="small">{{ statusText(row.voucherStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="摘要" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">分录</el-button>
          <el-button v-if="row.voucherStatus === 'DRAFT'" link type="warning" size="small" @click="doAudit(row)">审核</el-button>
          <el-button v-if="row.voucherStatus === 'AUDITED'" link type="success" size="small" @click="doBook(row)">过账</el-button>
          <el-button v-if="row.voucherStatus === 'BOOKED'" link type="danger" size="small" @click="doReverse(row)">红冲</el-button>
          <el-button v-if="row.voucherStatus === 'DRAFT'" link type="danger" size="small" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />

    <!-- 分录明细 -->
    <el-dialog v-model="detail.visible" title="凭证分录" width="640px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="凭证号">{{ detail.voucher.voucherNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.voucher.voucherStatus) }}</el-descriptions-item>
        <el-descriptions-item label="借方合计">{{ detail.voucher.totalDebit }}</el-descriptions-item>
        <el-descriptions-item label="贷方合计">{{ detail.voucher.totalCredit }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.voucher.entries || []" border size="small">
        <el-table-column prop="subjectCode" label="科目" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="140" />
        <el-table-column prop="debitAmount" label="借方" width="120" align="right" />
        <el-table-column prop="creditAmount" label="贷方" width="120" align="right" />
      </el-table>
    </el-dialog>

    <!-- 录入凭证 -->
    <el-dialog v-model="form.visible" title="录入凭证（借贷必须平衡）" width="760px">
      <el-form :model="form.data" label-width="70px" size="small">
        <el-row>
          <el-col :span="8">
            <el-form-item label="期间">
              <el-input v-model="form.data.periodYear" style="width: 70px" /> - <el-input v-model="form.data.periodMonth" style="width: 60px" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="日期">
              <el-date-picker v-model="form.data.voucherDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="摘要">
              <el-input v-model="form.data.remark" placeholder="整单摘要" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="form.entries" border size="small">
        <el-table-column label="科目" min-width="180">
          <template #default="{ row }">
            <el-tree-select v-model="row.subjectId" :data="subjectTree" :props="{ label: 'subjectName', value: 'id' }"
              check-strictly filterable style="width: 100%"
              :render-after-expand="false" :default-expand-all="true" />
          </template>
        </el-table-column>
        <el-table-column label="摘要" min-width="140">
          <template #default="{ row }"><el-input v-model="row.summary" /></template>
        </el-table-column>
        <el-table-column label="借方金额" width="130">
          <template #default="{ row }"><el-input-number v-model="row.debitAmount" :min="0" :precision="2" style="width: 100%" /></template>
        </el-table-column>
        <el-table-column label="贷方金额" width="130">
          <template #default="{ row }"><el-input-number v-model="row.creditAmount" :min="0" :precision="2" style="width: 100%" /></template>
        </el-table-column>
        <el-table-column width="60">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="form.entries.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="form-foot">
        <el-button size="small" @click="addEntry">+ 添加分录</el-button>
        <span class="balance" :class="balanceOk ? 'ok' : 'bad'">
          借方合计 {{ sumDebit }} / 贷方合计 {{ sumCredit }} {{ balanceOk ? '（平衡）' : '（不平衡）' }}
        </span>
      </div>
      <template #footer>
        <el-button @click="form.visible = false">取消</el-button>
        <el-button type="primary" :loading="form.saving" :disabled="!balanceOk" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageVoucher, getVoucher, createVoucher, auditVoucher, bookVoucher, reverseVoucher, deleteVoucher, getSubjectTree } from '@/api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, periodYear: '', periodMonth: '' })

const STATUS = { DRAFT: '草稿', AUDITED: '已审核', BOOKED: '已过账', REVERSED: '已冲销' }
const statusText = (s) => STATUS[s] || s
const statusType = (s) => ({ DRAFT: 'info', AUDITED: 'warning', BOOKED: 'success', REVERSED: 'danger' }[s] || 'info')

const detail = reactive({ visible: false, voucher: {} })
const form = reactive({
  visible: false,
  saving: false,
  data: { periodYear: '', periodMonth: '', voucherDate: '', remark: '' },
  entries: []
})
const subjectTree = ref([])

const sumDebit = computed(() => form.entries.reduce((a, e) => a + (Number(e.debitAmount) || 0), 0))
const sumCredit = computed(() => form.entries.reduce((a, e) => a + (Number(e.creditAmount) || 0), 0))
const balanceOk = computed(() => form.entries.length > 0 && Math.abs(sumDebit.value - sumCredit.value) < 0.001)

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.periodYear) params.periodYear = query.periodYear
    if (query.periodMonth) params.periodMonth = query.periodMonth
    const res = await pageVoucher(params)
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  const res = await getVoucher(row.id)
  detail.voucher = res.data
  detail.visible = true
}

function openCreate() {
  form.data = {
    periodYear: new Date().getFullYear(),
    periodMonth: new Date().getMonth() + 1,
    voucherDate: new Date().toISOString().slice(0, 10),
    remark: ''
  }
  form.entries = [{ subjectId: null, summary: '', debitAmount: 0, creditAmount: 0 }]
  form.visible = true
}
function addEntry() {
  form.entries.push({ subjectId: null, summary: '', debitAmount: 0, creditAmount: 0 })
}

async function onSave() {
  const payload = {
    periodYear: Number(form.data.periodYear),
    periodMonth: Number(form.data.periodMonth),
    voucherDate: form.data.voucherDate,
    remark: form.data.remark,
    entries: form.entries.map((e) => ({
      subjectId: e.subjectId,
      summary: e.summary || form.data.remark,
      debitAmount: e.debitAmount,
      creditAmount: e.creditAmount
    }))
  }
  form.saving = true
  try {
    await createVoucher(payload)
    ElMessage.success('凭证已保存（凭证号已占号）')
    form.visible = false
    load()
  } finally {
    form.saving = false
  }
}

async function doAudit(row) {
  await ElMessageBox.confirm(`确认审核凭证 ${row.voucherNo}？`, '审核', { type: 'warning' })
  await auditVoucher(row.id)
  ElMessage.success('已审核')
  load()
}
async function doBook(row) {
  await ElMessageBox.confirm(`确认过账凭证 ${row.voucherNo}？过账后不可修改。`, '过账', { type: 'warning' })
  await bookVoucher(row.id)
  ElMessage.success('已过账')
  load()
}
async function doReverse(row) {
  await ElMessageBox.confirm(`确认红冲凭证 ${row.voucherNo}？将生成冲销凭证。`, '红冲', { type: 'warning' })
  await reverseVoucher(row.id)
  ElMessage.success('已红冲')
  load()
}
async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除草稿凭证 ${row.voucherNo}？`, '删除', { type: 'warning' })
  await deleteVoucher(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  try {
    const res = await getSubjectTree()
    subjectTree.value = res.data || []
  } catch (e) { /* 科目树加载失败不影响凭证列表 */ }
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.balance {
  font-size: 13px;
  font-weight: 600;
}
.balance.ok {
  color: #52c41a;
}
.balance.bad {
  color: #ea6668;
}
</style>
