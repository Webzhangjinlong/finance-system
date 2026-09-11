<template>
  <div style="padding: 16px">
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">字典管理（S3）</span>
      </template>

      <!-- 类型列表 -->
      <div style="margin-bottom: 12px">
        <el-form inline>
          <el-form-item label="字典名称">
            <el-input v-model="query.dictName" placeholder="名称模糊" clearable style="width: 150px" @keyup.enter="load" />
          </el-form-item>
          <el-form-item label="字典类型">
            <el-input v-model="query.dictType" placeholder="类型模糊" clearable style="width: 150px" @keyup.enter="load" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="load">查询</el-button>
          </el-form-item>
          <el-form-item>
            <el-button type="success" @click="openType(null)">新增字典类型</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="rows" v-loading="loading" border size="small">
        <el-table-column prop="id" label="ID" width="200" />
        <el-table-column prop="dictName" label="字典名称" min-width="140" />
        <el-table-column prop="dictType" label="字典类型" min-width="160" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openData(row)">字典数据</el-button>
            <el-button size="small" @click="openType(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 10px" layout="total, prev, pager, next" :total="total"
        :page-size="query.size" :current-page="query.page" @current-change="p => { query.page = p; load() }" />

      <!-- 类型弹窗 -->
      <el-dialog v-model="typeDialog" :title="typeForm.id ? '编辑字典类型' : '新增字典类型'" width="460px">
        <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
          <el-form-item label="字典名称" prop="dictName">
            <el-input v-model="typeForm.dictName" placeholder="如：用户性别" />
          </el-form-item>
          <el-form-item label="字典类型" prop="dictType">
            <el-input v-model="typeForm.dictType" placeholder="如：sys_user_gender（小写字母/数字/下划线）" :disabled="!!typeForm.id" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="typeForm.status">
              <el-radio value="ACTIVE">正常</el-radio>
              <el-radio value="DISABLED">停用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="typeDialog = false">取消</el-button>
          <el-button type="primary" @click="saveType">保存</el-button>
        </template>
      </el-dialog>

      <!-- 数据弹窗 -->
      <el-dialog v-model="dataDialog" :title="`字典数据 · ${currentType?.dictType || ''}`" width="680px" top="6vh">
        <el-button type="success" size="small" style="margin-bottom: 10px" @click="openDataItem(null)">新增数据</el-button>
        <el-table :data="dataRows" v-loading="dataLoading" border size="small">
          <el-table-column prop="dictLabel" label="标签" min-width="120" />
          <el-table-column prop="dictValue" label="键值" min-width="120" />
          <el-table-column prop="dictSort" label="排序" width="70" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openDataItem(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeData(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="color: #909399; font-size: 12px; margin-top: 8px">
          变更后自动失效 Redis 缓存（dict:data:{dictType}），下次读取回源重建。
        </div>
      </el-dialog>

      <!-- 数据项弹窗 -->
      <el-dialog v-model="itemDialog" :title="itemForm.id ? '编辑字典数据' : '新增字典数据'" width="460px">
        <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px">
          <el-form-item label="字典标签" prop="dictLabel">
            <el-input v-model="itemForm.dictLabel" placeholder="如：男" />
          </el-form-item>
          <el-form-item label="字典键值" prop="dictValue">
            <el-input v-model="itemForm.dictValue" placeholder="如：MALE" />
          </el-form-item>
          <el-form-item label="排序" prop="dictSort">
            <el-input-number v-model="itemForm.dictSort" :min="1" :max="999" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="itemForm.status">
              <el-radio value="ACTIVE">正常</el-radio>
              <el-radio value="DISABLED">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="itemDialog = false">取消</el-button>
          <el-button type="primary" @click="saveDataItem">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictTypePage, createDictType, updateDictType, deleteDictType,
  getDictData, createDictData, updateDictData, deleteDictData } from '@/api/index.js'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, dictName: '', dictType: '' })

async function load() {
  loading.value = true
  try {
    const res = await getDictTypePage(query)
    rows.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } catch (e) {
    ElMessage.error('加载字典类型失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    loading.value = false
  }
}

// ---- 类型 ----
const typeDialog = ref(false)
const typeFormRef = ref()
const typeForm = reactive({ id: null, dictName: '', dictType: '', status: 'ACTIVE', remark: '' })
const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function openType(row) {
  Object.assign(typeForm, row ? { id: row.id, dictName: row.dictName, dictType: row.dictType,
    status: row.status, remark: row.remark } : { id: null, dictName: '', dictType: '', status: 'ACTIVE', remark: '' })
  typeDialog.value = true
}

async function saveType() {
  await typeFormRef.value.validate()
  try {
    if (typeForm.id) {
      await updateDictType(typeForm)
    } else {
      await createDictType(typeForm)
    }
    ElMessage.success('保存成功')
    typeDialog.value = false
    load()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function removeType(row) {
  await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」？将级联删除其全部字典数据。`, '提示', { type: 'warning' })
  try {
    await deleteDictType(row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.response?.data?.msg || e.message))
  }
}

// ---- 数据 ----
const dataDialog = ref(false)
const dataLoading = ref(false)
const dataRows = ref([])
const currentType = ref(null)

async function openData(row) {
  currentType.value = row
  dataDialog.value = true
  await loadData()
}

async function loadData() {
  dataLoading.value = true
  try {
    const res = await getDictData(currentType.value.dictType)
    dataRows.value = res.data || []
  } catch (e) {
    ElMessage.error('加载字典数据失败：' + (e?.response?.data?.msg || e.message))
  } finally {
    dataLoading.value = false
  }
}

const itemDialog = ref(false)
const itemFormRef = ref()
const itemForm = reactive({ id: null, dictType: '', dictLabel: '', dictValue: '', dictSort: 1, status: 'ACTIVE' })
const itemRules = {
  dictLabel: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入键值', trigger: 'blur' }],
  dictSort: [{ required: true, message: '请输入排序', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function openDataItem(row) {
  Object.assign(itemForm, row ? { ...row } : {
    id: null, dictType: currentType.value.dictType, dictLabel: '', dictValue: '', dictSort: 1, status: 'ACTIVE' })
  itemDialog.value = true
}

async function saveDataItem() {
  await itemFormRef.value.validate()
  try {
    if (itemForm.id) {
      await updateDictData(itemForm)
    } else {
      await createDictData(itemForm)
    }
    ElMessage.success('保存成功')
    itemDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.response?.data?.msg || e.message))
  }
}

async function removeData(row) {
  await ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」？`, '提示', { type: 'warning' })
  try {
    await deleteDictData(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.response?.data?.msg || e.message))
  }
}

load()
</script>
