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
// ==================== 系统管理（S2） ====================
export function pageUser(params) {
  return request({ url: '/system/user/page', method: 'get', params })
}
export function getUser(id) {
  return request({ url: `/system/user/${id}`, method: 'get' })
}
export function createUser(data) {
  return request({ url: '/system/user', method: 'post', data })
}
export function updateUser(id, data) {
  return request({ url: `/system/user/${id}`, method: 'put', data })
}
export function deleteUser(id) {
  return request({ url: `/system/user/${id}`, method: 'delete' })
}
export function resetUserPwd(id, data) {
  return request({ url: `/system/user/${id}/reset-pwd`, method: 'put', data })
}
export function assignUserRoles(id, data) {
  return request({ url: `/system/user/${id}/roles`, method: 'put', data })
}
export function pageRole(params) {
  return request({ url: '/system/role/page', method: 'get', params })
}
export function listRole() {
  return request({ url: '/system/role/list', method: 'get' })
}
export function getRole(id) {
  return request({ url: `/system/role/${id}`, method: 'get' })
}
export function createRole(data) {
  return request({ url: '/system/role', method: 'post', data })
}
export function updateRole(id, data) {
  return request({ url: `/system/role/${id}`, method: 'put', data })
}
export function deleteRole(id) {
  return request({ url: `/system/role/${id}`, method: 'delete' })
}
export function assignRoleMenus(id, data) {
  return request({ url: `/system/role/${id}/menus`, method: 'put', data })
}
export function menuTree() {
  return request({ url: '/system/menu/tree', method: 'get' })
}
export function getMenu(id) {
  return request({ url: `/system/menu/${id}`, method: 'get' })
}
export function createMenu(data) {
  return request({ url: '/system/menu', method: 'post', data })
}
export function updateMenu(id, data) {
  return request({ url: `/system/menu/${id}`, method: 'put', data })
}
export function deleteMenu(id) {
  return request({ url: `/system/menu/${id}`, method: 'delete' })
}
// ==================== 人事（H1 部门/员工） ====================
export function departmentTree() {
  return request({ url: '/hr/department/tree', method: 'get' })
}
export function createDepartment(data) {
  return request({ url: '/hr/department', method: 'post', data })
}
export function updateDepartment(id, data) {
  return request({ url: `/hr/department/${id}`, method: 'put', data })
}
export function deleteDepartment(id) {
  return request({ url: `/hr/department/${id}`, method: 'delete' })
}
export function pageEmployee(params) {
  return request({ url: '/hr/employee/page', method: 'get', params })
}
export function getEmployee(id) {
  return request({ url: `/hr/employee/${id}`, method: 'get' })
}
export function createEmployee(data) {
  return request({ url: '/hr/employee', method: 'post', data })
}
export function updateEmployee(id, data) {
  return request({ url: `/hr/employee/${id}`, method: 'put', data })
}
export function updateEmployeeStatus(id, status) {
  return request({ url: `/hr/employee/${id}/status`, method: 'put', params: { status } })
}
export function deleteEmployee(id) {
  return request({ url: `/hr/employee/${id}`, method: 'delete' })
}
// ==================== 考勤（H2） ====================
export function pageAttendance(params) {
  return request({ url: '/hr/attendance/page', method: 'get', params })
}
export function upsertAttendance(data) {
  return request({ url: '/hr/attendance', method: 'post', data })
}
// ==================== 工资（H3） ====================
export function pageSalary(params) {
  return request({ url: '/salary/list', method: 'get', params })
}
export function editSalaryDraft(id, data) {
  return request({ url: `/salary/${id}`, method: 'put', data })
}
export function calculateSalary(params) {
  return request({ url: '/salary/calculate', method: 'post', params })
}
export function submitSalary(id) {
  return request({ url: `/salary/${id}/submit`, method: 'put' })
}
export function approveSalary(id) {
  return request({ url: `/salary/${id}/approve`, method: 'put' })
}
export function getSalarySlip(id) {
  return request({ url: `/salary/${id}/slip`, method: 'get' })
}
// ==================== 健康检查 ====================
export function getHealth() {
  return request({ url: '/system/health', method: 'get' })
}
