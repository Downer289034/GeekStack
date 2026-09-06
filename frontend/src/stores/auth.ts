import { defineStore } from 'pinia'
import { TOKEN_KEY } from '@/api/request'

/** 登录态：token 存 Pinia + localStorage（刷新保持，登出清除） */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) ?? ''
  }),
  getters: {
    isLoggedIn: (state) => state.token !== ''
  },
  actions: {
    setToken(token: string) {
      this.token = token
      localStorage.setItem(TOKEN_KEY, token)
    },
    clearToken() {
      this.token = ''
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
