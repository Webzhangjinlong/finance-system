import { defineStore } from 'pinia'

// 登录态存储：token 持久化 localStorage，user 供布局/工作台展示
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    companyCode: localStorage.getItem('companyCode') || 'DEMO'
  }),
  actions: {
    setLogin(data) {
      this.token = data.token || ''
      this.username = data.username || ''
      this.nickname = data.nickname || ''
      this.companyCode = data.companyCode || 'DEMO'
      localStorage.setItem('token', this.token)
      localStorage.setItem('username', this.username)
      localStorage.setItem('nickname', this.nickname)
      localStorage.setItem('companyCode', this.companyCode)
    },
    logout() {
      this.token = ''
      this.username = ''
      this.nickname = ''
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('companyCode')
    }
  }
})
