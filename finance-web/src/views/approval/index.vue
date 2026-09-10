<template>
  <el-card shadow="never">
    <template #header>
      <div class="head">
        <b>审批待办（W1 / C2）— 当前登录人</b>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </template>

    <el-empty v-if="!loading && rows.length === 0" description="暂无待办（去「合同台账」提交一份合同审批）" />
    <el-table v-else :data="rows" border size="small" v-loading="loading">
      <el-table-column prop="businessType" label="业务类型" width="110" />
      <el-table-column prop="businessId" label="业务单号" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'RUNNING' ? 'warning' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignee" label="审批人" width="110" />
      <el-table-column prop="createTime" label="发起时间" min-width="150" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" size="small" @click="onApprove(row)">同意</el-button>
          <el-button link type="danger" size="small" @click="onReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { todoTasks, approveTask, rejectTask } from '@/api'

const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await todoTasks()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function onApprove(row) {
  const { value } = await ElMessageBox.prompt('审批意见（可空）', '同意审批', { inputPlaceholder: '同意' })
  await approveTask(row.taskId, value || '同意')
  ElMessage.success('已同意（合同自动生效并生成收付款计划）')
  load()
}
async function onReject(row) {
  const { value } = await ElMessageBox.prompt('驳回意见', '驳回', { type: 'warning', inputPlaceholder: '驳回原因' })
  await rejectTask(row.taskId, value || '驳回')
  ElMessage.success('已驳回（合同回到草稿）')
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
