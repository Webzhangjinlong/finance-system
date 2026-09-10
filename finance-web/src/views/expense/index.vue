<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>费用报销（F6）</b>
        <div>
          <el-button type="primary" size="small" @click="openCreate">新建报销</el-button>
        </div>
      </div>
    </template>

    <el-form inline size="small" style="margin-bottom: 10px">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="单号/事由" style="width: 180px" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" style="width: 140px" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="审批中" value="SUBMITTED" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已打款" value="PAID" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" border size="small" v-loading="loading">
      <el-table-column prop="claimNo" label="报销单号" width="150" />
      <el-table-column prop="expenseType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ typeText(row.expenseType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" min-width="160" show-overflow-tooltip />
      <el-table-column prop="amount" label="金额" width="120" align="right">
        <template #default="{ row }">{{ fmt(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="申请人" width="90" />
      <el-table-column prop="approver" label="审批人" width="90" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">明细</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="warning" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" link type="success" size="small" @click="openSubmit(row)">提交</el-button>
          <el-button v-if="row.status === 'APPROVED'" link type="danger" size="small" @click="doPay(row)">打款</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="danger" size="small" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next" :total="total"
      :page-size="query.size" :current-page="query.page" @current-change="(p) => { query.page = p; load() }" />

    <!-- 新建/编辑 -->
    <el-dialog v-model="form.visible" :title="form.id ? '编辑报销单' : '新建报销单'" width="680px">
      <el-form :model="form.data" label-width="90px" size="small">
        <el-form-item label="费用类型">
          <el-select v-model="form.data.expenseType" style="width: 200px">
            <el-option label="差旅" value="TRAVEL" />
            <el-option label="餐饮" value="MEAL" />
            <el-option label="办公" value="OFFICE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="form.data.reason" maxlength="255" style="width: 100%" />
        </el-form-item>
        <el-form-item label="发票号">
          <el-input v-model="form.data.invoiceNo" maxlength="64" style="width: 240px" placeholder="可选" />
        </el-form-item>
        <el-form-item label="明细行">
          <div style="width: 100%">
            <div v-for="(it, i) in form.data.items" :key="i" style="display: flex; gap: 6px; margin-bottom: 6px">
              <el-select v-model="it.subjectCode" placeholder="费用科目" style="width: 200px" filterable>
                <el-option v-for="s in leafSubjects" :key="s.subjectCode" :label="s.subjectCode + ' ' + s.subjectName"
                  :value="s.subjectCode" />
              </el-select>
              <el-input v-model="it.summary" placeholder="摘要" style="width: 180px" />
              <el-input-number v-model="it.amount" :min="0.01" :precision="2" :controls="false" style="width: 130px" />
              <el-button link type="danger" @click="form.data.items.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="form.data.items.push({ subjectCode: '', summary: '', amount: null })">
              + 添加明细
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="form.visible = false">取消</el-button>
        <el-button type="primary" size="small" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提交审批 -->
    <el-dialog v-model="submit.visible" title="提交审批" width="420px">
      <el-form label-width="80px" size="small">
        <el-form-item label="审批人">
          <el-input v-model="submit.approver" placeholder="审批人用户名（如 admin）" style="width: 100%" />
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="submit.comment" type="textarea" :rows="2" placeholder="可选" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="submit.visible = false">取消</el-button>
        <el-button type="primary" size="small" @click="doSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 明细 -->
    <el-dialog v-model="detail.visible" title="报销单明细" width="640px">
      <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="单号">{{ detail.row.claimNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(detail.row.status) }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeText(detail.row.expenseType) }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ fmt(detail.row.amount) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.row.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detail.row.approver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="打款人">{{ detail.row.payer || '-' }}</el-descriptions-item>
        <el-descriptions-item label="打款时间">{{ detail.row.paidAt ? detail.row.paidAt.replace('T', ' ').slice(0, 19) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.items" border size="small">
        <el-table-column prop="subjectCode" label="科目" width="120" />
        <el-table-column prop="summary" label="摘要" min-width="160" />
        <el-table-column prop="amount" label="金额" width="140" align="right">
          <template #default="{ row }">{{ fmt(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageExpense, getExpense, createExpense, updateExpense, deleteExpense,
  submitExpense, payExpense, getSubjectTree } from '../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

const leafSubjects = ref([])
const form = reactive({ visible: false, id: null, data: { expenseType: 'TRAVEL', reason: '', invoiceNo: '', items: [] } })
const submit = reactive({ visible: false, id: null, approver: '', comment: '' })
const detail = reactive({ visible: false, row: {}, items: [] })

function load() {
  loading.value = true
  pageExpense(query).then(r => {
    rows.value = r.records
    total.value = Number(r.total)
  }).finally(() => { loading.value = false })
}

function loadSubjects() {
  getSubjectTree().then(tree => {
    const out = []
    const walk = (nodes) => nodes.forEach(n => {
      if (n.children && n.children.length) walk(n.children)
      else out.push(n)
    })
    walk(tree)
    leafSubjects.value = out
  })
}

function openCreate() {
  form.id = null
  form.data = { expenseType: 'TRAVEL', reason: '', invoiceNo: '', items: [{ subjectCode: '', summary: '', amount: null }] }
  form.visible = true
}

function openEdit(row) {
  form.id = row.id
  getExpense(row.id).then(r => {
    form.data = {
      expenseType: r.expenseType, reason: r.reason, invoiceNo: r.invoiceNo || '',
      items: (r.items || []).map(i => ({ subjectCode: i.subjectCode, summary: i.summary || '', amount: Number(i.amount) }))
    }
    form.visible = true
  })
}

function save() {
  const d = form.data
  if (!d.expenseType || !d.reason) return ElMessage.warning('请填写类型与事由')
  if (!d.items || !d.items.length) return ElMessage.warning('请至少添加一条明细')
  for (const it of d.items) {
    if (!it.subjectCode) return ElMessage.warning('明细科目不能为空')
    if (!it.amount || it.amount <= 0) return ElMessage.warning('明细金额必须大于 0')
  }
  const payload = { expenseType: d.expenseType, reason: d.reason, invoiceNo: d.invoiceNo, items: d.items }
  const req = form.id ? updateExpense(form.id, payload) : createExpense(payload)
  req.then(() => {
    ElMessage.success('保存成功')
    form.visible = false
    load()
  })
}

function openSubmit(row) {
  submit.id = row.id
  submit.approver = 'admin'
  submit.comment = ''
  submit.visible = true
}

function doSubmit() {
  if (!submit.approver) return ElMessage.warning('请填写审批人')
  submitExpense(submit.id, { approver: submit.approver, comment: submit.comment }).then(() => {
    ElMessage.success('已提交审批')
    submit.visible = false
    load()
  })
}

function doPay(row) {
  ElMessageBox.confirm(`确认对报销单 ${row.claimNo}（${fmt(row.amount)}）打款并生成凭证？`, '打款确认', { type: 'warning' })
    .then(() => payExpense(row.id).then(() => {
      ElMessage.success('打款成功，凭证已生成')
      load()
    }))
}

function doDelete(row) {
  ElMessageBox.confirm(`确认删除报销单 ${row.claimNo}？`, '删除确认', { type: 'warning' })
    .then(() => deleteExpense(row.id).then(() => {
      ElMessage.success('已删除')
      load()
    }))
}

function showDetail(row) {
  detail.row = row
  getExpense(row.id).then(r => {
    detail.row = r
    detail.items = r.items || []
    detail.visible = true
  })
}

function fmt(v) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function typeText(t) {
  return ({ TRAVEL: '差旅', MEAL: '餐饮', OFFICE: '办公', OTHER: '其他' })[t] || t
}
function statusText(s) {
  return ({ DRAFT: '草稿', SUBMITTED: '审批中', APPROVED: '已通过', PAID: '已打款', REJECTED: '已驳回' })[s] || s
}
function statusType(s) {
  return ({ DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'primary', PAID: 'success', REJECTED: 'danger' })[s] || 'info'
}

onMounted(() => { load(); loadSubjects() })
</script>
