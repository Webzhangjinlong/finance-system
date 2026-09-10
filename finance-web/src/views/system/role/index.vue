<template>
  <div style="padding: 16px;">
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="角色名称/标识" clearable style="width: 200px"
                    @keyup.enter="load" @clear="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openCreate">新增角色</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="roleName" label="角色名称" min-width="130" />
        <el-table-column prop="roleKey" label="角色标识" min-width="120" />
        <el-table-column prop="roleSort" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="openAssign(row)">分配菜单</el-button>
            <el-button size="small" type="danger" :disabled="row.id === 1001" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 12px" background layout="total, prev, pager, next"
                     :total="total" :page-size="query.size" :current-page="query.page"
                     @current-change="p => { query.page = p; load() }" />
    </el-card>

    <!-- 新建/编辑 -->
    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增角色' : '编辑角色'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称" required>
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色标识" required>
          <el-input v-model="form.roleKey" placeholder="如 finance_viewer" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">正常</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="assignVisible" title="分配菜单权限" width="520px">
      <el-tree ref="menuTreeRef" :data="menuTreeData" show-checkbox node-key="id"
               :props="{ label: 'menuName', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="doAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageRole, createRole, updateRole, deleteRole, menuTree, getRole, assignRoleMenus } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const dlgVisible = ref(false)
const dlgMode = ref('create')
const saving = ref(false)
const form = reactive({ id: null, roleName: '', roleKey: '', roleSort: 0, status: 'ACTIVE', remark: '' })

const assignVisible = ref(false)
const menuTreeRef = ref(null)
const menuTreeData = ref([])
const currentRole = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await pageRole(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dlgMode.value = 'create'
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: 'ACTIVE', remark: '' })
  dlgVisible.value = true
}

function openEdit(row) {
  dlgMode.value = 'edit'
  Object.assign(form, { id: row.id, roleName: row.roleName, roleKey: row.roleKey, roleSort: row.roleSort, status: row.status, remark: row.remark })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await createRole(form)
      ElMessage.success('创建成功')
    } else {
      await updateRole(form.id, form)
      ElMessage.success('保存成功')
    }
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function openAssign(row) {
  currentRole.value = row
  if (!menuTreeData.value.length) {
    menuTreeData.value = (await menuTree()).data
  }
  const detail = (await getRole(row.id)).data
  assignVisible.value = true
  await nextTick()
  menuTreeRef.value.setCheckedKeys(detail.menuIds || [])
}

async function doAssign() {
  const ids = [...menuTreeRef.value.getCheckedKeys(), ...menuTreeRef.value.getHalfCheckedKeys()]
  await assignRoleMenus(currentRole.value.id, { ids })
  ElMessage.success('菜单权限已更新（相关用户需重新登录生效）')
  assignVisible.value = false
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
