import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, type UserProfile } from '@/api/auth'
import { clearTokens, getAccessToken, setTokens } from '@/api/tokenStorage'

/** 登录态：令牌持久化在 localStorage（刷新保持），用户资料登录后拉取一次。 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getAccessToken(),
    user: null as UserProfile | null
  }),
  getters: {
    isLoggedIn: (state) => state.token !== ''
  },
  actions: {
    /** 保存令牌对并置为登录态（注册成功即调用，实现自动登录）。 */
    async applyTokens(pair: { accessToken: string; refreshToken: string }) {
      setTokens(pair)
      this.token = pair.accessToken
      await this.fetchCurrentUser()
    },
    /** 拉取当前用户资料；失败静默（401 由拦截器统一处理刷新/跳转）。 */
    async fetchCurrentUser() {
      if (!getAccessToken()) {
        return
      }
      try {
        this.user = await fetchMe()
      } catch {
        // 拦截器已提示；保持本地状态，等待跳转或重试
      }
    },
    async login(username: string, password: string) {
      const pair = await loginApi(username, password)
      await this.applyTokens(pair)
    },
    /** 登出：清除本地凭据与资料（后端 JWT 无状态，无需服务端撤销）。 */
    logout() {
      clearTokens()
      this.token = ''
      this.user = null
    }
  }
})
