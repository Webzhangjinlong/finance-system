<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="query.bizType" placeholder="业务类型" clearable style="width: 160px">
          <el-option label="费用报销" value="EXPENSE" />
          <el-option label="合同" value="CONTRACT" />
          <el-option label="凭证" value="VOUCHER" />
          <el-option label="其他" value="OTHER" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-upload :show-file-list="false" :http-request="doUpload" accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.txt,.csv">
          <el-button type="success">上传附件</el-button>
        </el-upload>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="120" />
        <el-table-column prop="bizType" label="业务类型" width="110">
          <template #default="{ row }">{{ row.bizType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="bizId" label="业务ID" width="110">
          <template #default="{ row }">{{ row.bizId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="download(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="mt-2" layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page" @current-change="onPage" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadAttachment, pageAttachment, getAttachmentFile } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ bizType: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const res = await pageAttachment(query)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  query.page = p
  load()
}

async function doUpload(opt) {
  const fd = new FormData()
  fd.append('file', opt.file)
  fd.append('bizType', query.bizType || '')
  try {
    await uploadAttachment(fd)
    ElMessage.success('上传成功')
    load()
  } catch (e) {
    ElMessage.error(e?.message || '上传失败')
  }
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

async function download(row) {
  try {
    const blob = await getAttachmentFile(row.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || '下载失败')
  }
}

onMounted(load)
</script>
