import { defineStore } from 'pinia'
import { authApi } from '@/api'
import type { UserInfo } from '@/types'

function readUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem('crm_user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('crm_token') || '',
    user: readUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.role || '',
    roleName: (state) => state.user?.roleName || '',
    region: (state) => state.user?.region || '',
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isDsm: (state) => state.user?.role === 'DSM',
    isMr: (state) => state.user?.role === 'MR',
    isOps: (state) => state.user?.role === 'OPS',
    canWriteCustomer: (state) => state.user?.role === 'MR' || state.user?.role === 'DSM',
    canApprove: (state) => state.user?.role === 'DSM',
    canShip: (state) => state.user?.role === 'OPS',
    canCreateOrder: (state) => state.user?.role === 'MR' || state.user?.role === 'OPS'
  },
  actions: {
    async login(payload: { username: string; password: string }) {
      const result = await authApi.login(payload)
      this.token = result.token
      this.user = result.user
      localStorage.setItem('crm_token', result.token)
      localStorage.setItem('crm_user', JSON.stringify(result.user))
    },
    async fetchMe() {
      const user = await authApi.me()
      this.user = user
      localStorage.setItem('crm_user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('crm_token')
      localStorage.removeItem('crm_user')
    }
  }
})
