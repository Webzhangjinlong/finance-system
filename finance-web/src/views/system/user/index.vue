<template>
  <div style="padding: 16px;">
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/昵称" clearable style="width: 200px"
                    @keyup.enter="load" @clear="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 140px" @change="load">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openCreate">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" min-width="110" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="openReset(row)">重置密码</el-button>
            <el-button size="small" type="primary" @click="openAssign(row)">分配角色</el-button>
            <el-button size="small" type="danger" :disabled="row.id === 1001" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 12px" background layout="total, prev, pager, next"
                     :total="total" :page-size="query.size" :current-page="query.page"
                     @current-change="p => { query.page = p; load() }" />
    </el-card>

    <!-- 新建/编辑 -->
    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增用户' : '编辑用户'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="dlgMode === 'edit'" placeholder="3-32 位字母数字下划线" />
        </el-form-item>
        <el-form-item v-if="dlgMode === 'create'" label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">正常</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-form label-width="90px">
        <el-form-item label="新密码" required>
          <el-input v-model="newPwd" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doReset">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="420px">
      <el-select v-model="selectedRoles" multiple style="width: 100%" placeholder="选择角色">
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="doAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageUser, createUser, updateUser, deleteUser, resetUserPwd, listRole, getUser, assignUserRoles } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })

const dlgVisible = ref(false)
const dlgMode = ref('create')
const saving = ref(false)
const form = reactive({ id: null, username: '', password: '', nickname: '', email: '', phone: '', status: 'ACTIVE' })

const resetVisible = ref(false)
const newPwd = ref('')
const currentUser = ref(null)

const assignVisible = ref(false)
const roles = ref([])
const selectedRoles = ref([])

async function load() {
  loading.value = true
  try {
    const res = await pageUser(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dlgMode.value = 'create'
  Object.assign(form, { id: null, username: '', password: '', nickname: '', email: '', phone: '', status: 'ACTIVE' })
  dlgVisible.value = true
}

function openEdit(row) {
  dlgMode.value = 'edit'
  Object.assign(form, { id: row.id, username: row.username, password: '', nickname: row.nickname, email: row.email, phone: row.phone, status: row.status })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await createUser({ username: form.username, password: form.password, nickname: form.nickname, email: form.email, phone: form.phone, status: form.status })
      ElMessage.success('创建成功')
    } else {
      await updateUser(form.id, { username: form.username, nickname: form.nickname, email: form.email, phone: form.phone, status: form.status })
      ElMessage.success('保存成功')
    }
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openReset(row) {
  currentUser.value = row
  newPwd.value = ''
  resetVisible.value = true
}

async function doReset() {
  await resetUserPwd(currentUser.value.id, { password: newPwd.value })
  ElMessage.success('密码已重置（请该用户重新登录）')
  resetVisible.value = false
}

async function openAssign(row) {
  currentUser.value = row
  if (!roles.value.length) {
    roles.value = (await listRole()).data
  }
  const detail = (await getUser(row.id)).data
  selectedRoles.value = detail.roleIds || []
  assignVisible.value = true
}

async function doAssign() {
  await assignUserRoles(currentUser.value.id, { ids: selectedRoles.value })
  ElMessage.success('角色已更新（该用户需重新登录生效）')
  assignVisible.value = false
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
