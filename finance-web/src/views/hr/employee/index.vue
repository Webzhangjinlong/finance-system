<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="工号 / 姓名" clearable style="width: 200px"
                  @keyup.enter="load" />
        <el-select v-model="query.deptId" placeholder="部门" clearable style="width: 160px">
          <el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px">
          <el-option label="在职" value="ONBOARD" />
          <el-option label="离职" value="LEAVE" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button type="success" @click="openCreate">新增员工</el-button>
        <el-button @click="openDeptDialog">部门管理</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="empNo" label="工号" width="110" />
        <el-table-column prop="empName" label="姓名" width="110" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">{{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="部门" width="130" />
        <el-table-column prop="position" label="职位" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="hireDate" label="入职日期" width="110" />
        <el-table-column prop="contractExpireDate" label="合同到期" width="110">
          <template #default="{ row }">{{ row.contractExpireDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ONBOARD' ? 'success' : 'info'">
              {{ row.status === 'ONBOARD' ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'ONBOARD'" link type="warning" @click="toggleStatus(row, 'LEAVE')">离职</el-button>
            <el-button v-else link type="success" @click="toggleStatus(row, 'ONBOARD')">复职</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total"
                     layout="total, prev, pager, next" style="margin-top: 12px" @change="load" />
    </el-card>

    <!-- 员工表单 -->
    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑员工' : '新增员工'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="工号" prop="empNo"><el-input v-model="form.empNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名" prop="empName"><el-input v-model="form.empName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option label="男" value="MALE" /><el-option label="女" value="FEMALE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="deptId">
              <el-select v-model="form.deptId" style="width: 100%">
                <el-option v-for="d in flatDepts" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="职位"><el-input v-model="form.position" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="入职日期"><el-date-picker v-model="form.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="合同到期"><el-date-picker v-model="form.contractExpireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工资账号"><el-input v-model="form.salaryAccount" /></el-form-item></el-col>
<el-col :span="12"><el-form-item label="社保基数"><el-input-number v-model="form.socialBase" :min="0" :precision="2" :controls="false" placeholder="申报缴费基数" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 部门管理 -->
    <el-dialog v-model="deptDialog.visible" title="部门管理" width="560px">
      <div style="margin-bottom: 10px">
        <el-input v-model="deptForm.deptName" placeholder="部门名称" style="width: 200px" />
        <el-select v-model="deptForm.parentId" placeholder="上级部门" clearable style="width: 160px">
          <el-option v-for="d in flatDepts" :key="d.id" :label="d.deptName" :value="d.id" />
        </el-select>
        <el-button type="success" @click="saveDept">新增</el-button>
      </div>
      <el-table :data="depts" row-key="id" default-expand-all :tree-props="{ children: 'children' }" size="small">
        <el-table-column prop="deptName" label="部门名称" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeDept(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  departmentTree, createDepartment, deleteDepartment,
  pageEmployee, createEmployee, updateEmployee, updateEmployeeStatus, deleteEmployee
} from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const depts = ref([])
const query = reactive({ keyword: '', deptId: null, status: '', page: 1, size: 10 })
const dialog = reactive({ visible: false, id: null })
const formRef = ref()
const form = reactive({})
const rules = {
  empNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  empName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }]
}
const deptDialog = reactive({ visible: false })
const deptForm = reactive({ deptName: '', parentId: null })

const flatDepts = computed(() => {
  const out = []
  const walk = list => list.forEach(d => { out.push(d); if (d.children) walk(d.children) })
  walk(depts.value)
  return out
})

async function load() {
  loading.value = true
  try {
    const res = await pageEmployee(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadDepts() {
  const res = await departmentTree()
  depts.value = res.data
}

function openCreate() {
  dialog.id = null
  Object.keys(form).forEach(k => delete form[k])
  dialog.visible = true
}

function openEdit(row) {
  dialog.id = row.id
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row)
  dialog.visible = true
}

async function save() {
  await formRef.value.validate()
  if (dialog.id) {
    await updateEmployee(dialog.id, form)
  } else {
    await createEmployee(form)
  }
  ElMessage.success('保存成功')
  dialog.visible = false
  load()
}

async function toggleStatus(row, status) {
  await updateEmployeeStatus(row.id, status)
  ElMessage.success(status === 'LEAVE' ? '已离职' : '已复职')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除员工 ${row.empName}？`, '提示', { type: 'warning' })
  await deleteEmployee(row.id)
  ElMessage.success('已删除')
  load()
}

function openDeptDialog() {
  deptDialog.visible = true
  loadDepts()
}

async function saveDept() {
  if (!deptForm.deptName) { ElMessage.warning('请输入部门名称'); return }
  await createDepartment(deptForm)
  ElMessage.success('部门已创建')
  deptForm.deptName = ''
  deptForm.parentId = null
  loadDepts()
  load()
}

async function removeDept(row) {
  await ElMessageBox.confirm(`确认删除部门 ${row.deptName}？`, '提示', { type: 'warning' })
  await deleteDepartment(row.id)
  ElMessage.success('已删除')
  loadDepts()
  load()
}

onMounted(() => { load(); loadDepts() })
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.page :deep(.el-table) { font-size: 13px; }
</style>
