<template>
  <div style="padding: 16px;">
    <el-card shadow="never">
      <div style="margin-bottom: 12px">
        <el-button type="success" @click="openCreate(null)">新增目录</el-button>
      </div>
      <el-table :data="treeData" v-loading="loading" row-key="id" :tree-props="{ children: 'children' }" stripe>
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.menuType === 'DIR' ? 'warning' : row.menuType === 'MENU' ? 'primary' : 'info'"
                    size="small">{{ row.menuType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由" min-width="130" />
        <el-table-column prop="perms" label="权限码" min-width="170" />
        <el-table-column prop="icon" label="图标" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreate(row.id)">新增子项</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlgVisible" :title="dlgMode === 'create' ? '新增菜单' : '编辑菜单'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="父级">
          <el-tree-select v-model="form.parentId" :data="parentTree" node-key="id" check-strictly
                          :props="{ label: 'menuName', children: 'children' }" clearable
                          placeholder="不选为顶级" style="width: 100%" />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.menuType" style="width: 140px">
            <el-option label="目录 DIR" value="DIR" />
            <el-option label="菜单 MENU" value="MENU" />
            <el-option label="按钮 BUTTON" value="BUTTON" />
          </el-select>
        </el-form-item>
        <el-form-item label="路由">
          <el-input v-model="form.path" placeholder="如 /system/user" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item label="权限码">
          <el-input v-model="form.perms" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { menuTree, createMenu, updateMenu, deleteMenu } from '@/api'

const loading = ref(false)
const treeData = ref([])

const dlgVisible = ref(false)
const dlgMode = ref('create')
const saving = ref(false)
const form = reactive({ id: null, parentId: null, menuName: '', menuType: 'MENU', path: '', component: '', perms: '', icon: '', sortOrder: 0, status: 'ACTIVE' })

const parentTree = computed(() => [{ id: 0, menuName: '顶级', children: treeData.value }])

async function load() {
  loading.value = true
  try {
    treeData.value = (await menuTree()).data
  } finally {
    loading.value = false
  }
}

function openCreate(parentId) {
  dlgMode.value = 'create'
  Object.assign(form, { id: null, parentId: parentId || 0, menuName: '', menuType: 'MENU', path: '', component: '', perms: '', icon: '', sortOrder: 0, status: 'ACTIVE' })
  dlgVisible.value = true
}

function openEdit(row) {
  dlgMode.value = 'edit'
  Object.assign(form, { id: row.id, parentId: row.parentId || 0, menuName: row.menuName, menuType: row.menuType, path: row.path || '', component: row.component || '', perms: row.perms || '', icon: row.icon || '', sortOrder: row.sortOrder, status: row.status })
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (dlgMode.value === 'create') {
      await createMenu(form)
      ElMessage.success('创建成功')
    } else {
      await updateMenu(form.id, form)
      ElMessage.success('保存成功')
    }
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」？`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
