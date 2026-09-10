<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="login-title">财务管理系统</div>
        <div class="login-sub">业财一体 · 多公司多账套（演示环境：admin / admin123）</div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-hint">功能说明：登录后可进入 科目 / 凭证 / 合同台账 / 审批待办 / 消息中心</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2fb 0%, #e6f4ee 100%);
}
.login-card {
  width: 380px;
  border-radius: 14px;
}
.login-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1b1c;
}
.login-sub {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
}
.login-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  border-top: 1px dashed #e4e3dd;
  padding-top: 10px;
}
</style>
