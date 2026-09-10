<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.employeeId" placeholder="员工" clearable filterable style="width: 180px">
          <el-option v-for="e in employees" :key="e.id" :label="`${e.empNo} ${e.empName}`" :value="e.id" />
        </el-select>
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="月份" style="width: 150px" />
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openForm">录入考勤</el-button>
        <span class="tip">说明：同一员工同一天重复录入会覆盖原记录（upsert）</span>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="empNo" label="工号" width="110" />
        <el-table-column prop="empName" label="姓名" width="110" />
        <el-table-column prop="workDate" label="日期" width="120" />
        <el-table-column label="上班打卡" width="170">
          <template #default="{ row }">{{ fmt(row.checkIn) }}</template>
        </el-table-column>
        <el-table-column label="下班打卡" width="170">
          <template #default="{ row }">{{ fmt(row.checkOut) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                     layout="total, prev, pager, next" style="margin-top: 12px" @change="load" />
    </el-card>

    <el-dialog v-model="dialog.visible" title="录入考勤" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="员工" required>
          <el-select v-model="form.employeeId" style="width: 100%">
            <el-option v-for="e in employees" :key="e.id" :label="`${e.empNo} ${e.empName}`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="上班时间">
          <el-date-picker v-model="form.checkIn" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下班时间">
          <el-date-picker v-model="form.checkOut" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常" value="NORMAL" />
            <el-option label="迟到" value="LATE" />
            <el-option label="早退" value="EARLY" />
            <el-option label="缺勤" value="ABSENT" />
            <el-option label="请假" value="LEAVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAttendance, upsertAttendance, pageEmployee } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const employees = ref([])
const month = ref(null)
const query = reactive({ employeeId: null, year: null, month: null, page: 1, size: 10 })
const dialog = reactive({ visible: false })
const form = reactive({ employeeId: null, workDate: null, checkIn: null, checkOut: null, status: 'NORMAL' })

const STATUS_MAP = {
  NORMAL: ['正常', 'success'],
  LATE: ['迟到', 'warning'],
  EARLY: ['早退', 'warning'],
  ABSENT: ['缺勤', 'danger'],
  LEAVE: ['请假', 'info']
}

function statusText(s) { return (STATUS_MAP[s] || [s, 'info'])[0] }
function statusType(s) { return (STATUS_MAP[s] || [s, 'info'])[1] }
function fmt(t) { return t ? t.replace('T', ' ').substring(0, 19) : '-' }

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
    const res = await pageAttendance(query)
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

function openForm() {
  Object.assign(form, { employeeId: null, workDate: null, checkIn: null, checkOut: null, status: 'NORMAL' })
  dialog.visible = true
}

async function save() {
  if (!form.employeeId || !form.workDate) { ElMessage.warning('请选择员工与日期'); return }
  await upsertAttendance(form)
  ElMessage.success('考勤已保存')
  dialog.visible = false
  load()
}

watch(month, () => load())
onMounted(() => { load(); loadEmployees() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; align-items: center; }
.tip { color: #999; font-size: 12px; margin-left: 8px; }
</style>
