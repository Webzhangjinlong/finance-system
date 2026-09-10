import request from '@/utils/request'

// ==================== 认证（S1） ====================
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}
export function getProfile() {
  return request({ url: '/auth/profile', method: 'get' })
}

// ==================== 科目（F1） ====================
export function getSubjectTree() {
  return request({ url: '/finance/subject/tree', method: 'get' })
}
export function createSubject(data) {
  return request({ url: '/finance/subject', method: 'post', data })
}
export function updateSubject(id, data) {
  return request({ url: `/finance/subject/${id}`, method: 'put', data })
}
export function deleteSubject(id) {
  return request({ url: `/finance/subject/${id}`, method: 'delete' })
}

// ==================== 凭证（F2） ====================
export function pageVoucher(params) {
  return request({ url: '/finance/voucher', method: 'get', params })
}
export function getVoucher(id) {
  return request({ url: `/finance/voucher/${id}`, method: 'get' })
}
export function createVoucher(data) {
  return request({ url: '/finance/voucher', method: 'post', data })
}
export function auditVoucher(id) {
  return request({ url: `/finance/voucher/${id}/audit`, method: 'put' })
}
export function bookVoucher(id) {
  return request({ url: `/finance/voucher/${id}/book`, method: 'put' })
}
export function reverseVoucher(id) {
  return request({ url: `/finance/voucher/${id}/reverse`, method: 'put' })
}
export function deleteVoucher(id) {
  return request({ url: `/finance/voucher/${id}`, method: 'delete' })
}

// ==================== 合同（C1/C2/C3） ====================
export function pageContract(params) {
  return request({ url: '/contract/page', method: 'get', params })
}
export function getContract(id) {
  return request({ url: `/contract/${id}`, method: 'get' })
}
export function createContract(data) {
  return request({ url: '/contract', method: 'post', data })
}
export function submitContract(id, data) {
  return request({ url: `/contract/${id}/submit`, method: 'post', data })
}
export function voidContract(id, reason) {
  return request({ url: `/contract/${id}/void`, method: 'post', params: { reason } })
}
export function listPlans(contractId) {
  return request({ url: `/contract/${contractId}/plans`, method: 'get' })
}
export function syncPlans(contractId) {
  return request({ url: `/contract/${contractId}/plans/sync`, method: 'post' })
}
export function registerReceipt(planId, amount, remark) {
  return request({ url: `/contract/plans/${planId}/receipt`, method: 'post', params: { amount, remark } })
}
export function registerPayment(planId, amount, remark) {
  return request({ url: `/contract/plans/${planId}/payment`, method: 'post', params: { amount, remark } })
}

// ==================== 审批（W1） ====================
// todo 无参数：后端按当前登录用户查待办
export function todoTasks() {
  return request({ url: '/workflow/todo', method: 'get' })
}
export function approveTask(taskId, comment) {
  return request({ url: `/workflow/task/${taskId}/approve`, method: 'put', params: { comment } })
}
export function rejectTask(taskId, comment) {
  return request({ url: `/workflow/task/${taskId}/reject`, method: 'put', params: { comment } })
}

// ==================== 消息中心（W2） ====================
export function pageMessage(params) {
  return request({ url: '/message/list', method: 'get', params })
}
export function unreadCount() {
  return request({ url: '/message/unread-count', method: 'get' })
}
export function readMessage(id) {
  return request({ url: `/message/${id}/read`, method: 'put' })
}
export function readAllMessage() {
  return request({ url: '/message/read-all', method: 'put' })
}


// ==================== 财务报表（F5） ====================
export function getBalanceSheet(params) {
  return request({ url: '/finance/report/balance-sheet', method: 'get', params })
}
export function getIncomeStatement(params) {
  return request({ url: '/finance/report/income', method: 'get', params })
}
export function getCashFlow(params) {
  return request({ url: '/finance/report/cash-flow', method: 'get', params })
}

// ==================== 应收/应付账龄（F7） ====================
export function getArAging(params) {
  return request({ url: '/finance/ar/aging', method: 'get', params })
}
export function getApAging(params) {
  return request({ url: '/finance/ap/aging', method: 'get', params })
}
// ==================== 费用报销（F6） ====================
export function pageExpense(params) {
  return request({ url: '/expense', method: 'get', params })
}
export function getExpense(id) {
  return request({ url: /expense/, method: 'get' })
}
export function createExpense(data) {
  return request({ url: '/expense', method: 'post', data })
}
export function updateExpense(id, data) {
  return request({ url: /expense/, method: 'put', data })
}
export function deleteExpense(id) {
  return request({ url: /expense/, method: 'delete' })
}
export function submitExpense(id, data) {
  return request({ url: /expense//submit, method: 'put', data })
}
export function payExpense(id) {
  return request({ url: /expense//pay, method: 'put' })
}
// ==================== 健康检查 ====================
export function getHealth() {
  return request({ url: '/system/health', method: 'get' })
}
