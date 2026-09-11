<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.title" placeholder="模块标题" clearable style="width: 160px" @keyup.enter="onSearch" />
        <el-input v-model="query.operName" placeholder="操作人" clearable style="width: 140px" @keyup.enter="onSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-date-picker v-model="timeRange" type="datetimerange" range-separator="至"
                        start-placeholder="开始时间" end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss" style="width: 340px" />
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="title" label="模块" width="130" />
        <el-table-column prop="businessType" label="业务类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="tagType(row.businessType)">{{ row.businessType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" width="100" />
        <el-table-column label="请求" width="200">
          <template #default="{ row }">
            <span class="mono">{{ row.requestMethod }} {{ row.operUrl }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operIp" label="IP" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">{{ row.costTime }}ms</template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" width="175" />
        <el-table-column label="详情" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="mt-2" layout="total, prev, pager, next" :total="total"
                     :page-size="query.size" :current-page="query.page" @current-change="onPage" />
    </el-card>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="720px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="模块">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ current.businessType }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ current.operName }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.operIp }}</el-descriptions-item>
        <el-descriptions-item label="方法">{{ current.method }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ current.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="URL">{{ current.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="状态" :span="2">
          <el-tag size="small" :type="current.status === 1 ? 'success' : 'danger'">
            {{ current.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ current.operTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="detail-pre">{{ current.operParam || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <pre class="detail-pre">{{ current.jsonResult || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="错误信息" :span="2">
          <pre class="detail-pre err">{{ current.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { pageOperLog } from '@/api'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const timeRange = ref(null)
const query = reactive({ title: '', operName: '', status: null, beginTime: '', endTime: '', page: 1, size: 10 })

const detailVisible = ref(false)
const current = ref({})

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    if (timeRange.value && timeRange.value.length === 2) {
      params.beginTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    }
    const res = await pageOperLog(params)
    rows.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onPage(p) {
  query.page = p
  load()
}

function tagType(t) {
  return ({ INSERT: 'primary', UPDATE: 'warning', DELETE: 'danger', AUDIT: 'success', BOOK: 'success', CLOSE: 'info', PAY: 'success', GRANT: 'warning', UPLOAD: 'info', IMPORT: 'primary', OTHER: 'info' })[t] || 'info'
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.mono {
  font-family: Consolas, Menlo, monospace;
  font-size: 12px;
  color: #374151;
}
.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  background: #f9fafb;
  padding: 8px;
  border-radius: 6px;
}
.detail-pre.err {
  color: #c0392b;
}
</style>
