<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>科目管理（F1）</b>
        <el-button type="primary" size="small" @click="openCreate">新增科目</el-button>
      </div>
    </template>

    <el-table :data="rows" row-key="id" default-expand-all border size="small" v-loading="loading"
      :tree-props="{ children: 'children' }">
      <el-table-column prop="subjectCode" label="科目编码" width="140" />
      <el-table-column prop="subjectName" label="科目名称" min-width="160" />
      <el-table-column prop="subjectType" label="类型" width="90">
        <template #default="{ row }">{{ typeText(row.subjectType) }}</template>
      </el-table-column>
      <el-table-column prop="direction" label="方向" width="70">
        <template #default="{ row }">{{ row.direction }}</template>
      </el-table-column>
      <el-table-column prop="subjectLevel" label="层级" width="60" />
      <el-table-column prop="isLeaf" label="末级" width="70">
        <template #default="{ row }">
          <el-tag :type="row.isLeaf === 1 ? 'success' : 'info'" size="small">{{ row.isLeaf === 1 ? '可记账' : '非末级' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑科目' : '新增科目'" width="460px">
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="上级科目">
          <el-tree-select v-model="dialog.form.parentId" :data="rows" :props="{ label: 'subjectName', value: 'id' }"
            check-strictly clearable placeholder="不选则为一级科目" style="width: 100%" />
        </el-form-item>
        <el-form-item label="科目编码" required>
          <el-input v-model="dialog.form.subjectCode" placeholder="如 1002" />
        </el-form-item>
        <el-form-item label="科目名称" required>
          <el-input v-model="dialog.form.subjectName" placeholder="如 银行存款" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="dialog.form.subjectType" style="width: 100%">
            <el-option label="资产" value="ASSET" />
            <el-option label="负债" value="LIABILITY" />
            <el-option label="权益" value="EQUITY" />
            <el-option label="成本" value="COST" />
            <el-option label="损益" value="PROFIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="方向" required>
          <el-radio-group v-model="dialog.form.direction">
            <el-radio label="DEBIT">借方</el-radio>
            <el-radio label="CREDIT">贷方</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSubjectTree, createSubject, updateSubject, deleteSubject } from '@/api'

const rows = ref([])
const loading = ref(false)
const dialog = reactive({
  visible: false,
  isEdit: false,
  saving: false,
  form: { id: null, parentId: null, subjectCode: '', subjectName: '', subjectType: 'ASSET', direction: 'DEBIT' }
})

const TYPE_MAP = { ASSET: '资产', LIABILITY: '负债', EQUITY: '权益', COST: '成本', PROFIT: '损益' }
const typeText = (t) => TYPE_MAP[t] || t

async function load() {
  loading.value = true
  try {
    const res = await getSubjectTree()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  dialog.form = { id: null, parentId: null, subjectCode: '', subjectName: '', subjectType: 'ASSET', direction: 'DEBIT' }
}
function openCreate() {
  dialog.isEdit = false
  resetForm()
  dialog.visible = true
}
function openEdit(row) {
  dialog.isEdit = true
  dialog.form = {
    id: row.id, parentId: row.parentId, subjectCode: row.subjectCode,
    subjectName: row.subjectName, subjectType: row.subjectType, direction: row.direction
  }
  dialog.visible = true
}

async function onSave() {
  const f = dialog.form
  if (!f.subjectCode || !f.subjectName) {
    ElMessage.warning('科目编码与名称必填')
    return
  }
  dialog.saving = true
  try {
    if (dialog.isEdit) {
      await updateSubject(f.id, { subjectCode: f.subjectCode, subjectName: f.subjectName, subjectType: f.subjectType, direction: f.direction })
    } else {
      await createSubject({ parentId: f.parentId, subjectCode: f.subjectCode, subjectName: f.subjectName, subjectType: f.subjectType, direction: f.direction })
    }
    ElMessage.success('保存成功')
    dialog.visible = false
    load()
  } finally {
    dialog.saving = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除科目「${row.subjectName}」？有子科目或发生额的科目会被保护。`, '删除确认', { type: 'warning' })
  await deleteSubject(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
