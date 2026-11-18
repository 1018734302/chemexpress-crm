import http from './http'
import type {
  Contact,
  Customer,
  DashboardData,
  MetaOptions,
  Order,
  PageResult,
  Product,
  UserInfo,
  UserRow,
  Visit
} from '@/types'

export const authApi = {
  login: (data: { username: string; password: string }) =>
    http.post<any, { token: string; user: UserInfo }>('/auth/login', data),
  me: () => http.get<any, UserInfo>('/auth/me'),
  logout: () => http.post('/auth/logout')
}

export const metaApi = {
  options: () => http.get<any, MetaOptions>('/meta')
}

export const dashboardApi = {
  get: () => http.get<any, DashboardData>('/dashboard')
}

export const customerApi = {
  page: (params: any) => http.get<any, PageResult<Customer>>('/customers', { params }),
  stats: (params: any) => http.get<any, Record<string, number>>('/customers/stats', { params }),
  owners: (region?: string) => http.get<any, Array<{ id: number; name: string; region: string }>>('/customers/owners', { params: { region } }),
  detail: (id: number) => http.get<any, Customer>(`/customers/${id}`),
  create: (data: any) => http.post<any, Customer>('/customers', data),
  update: (id: number, data: any) => http.put<any, Customer>(`/customers/${id}`, data),
  remove: (id: number) => http.delete(`/customers/${id}`),
  transfer: (id: number, ownerId: number) => http.post<any, Customer>(`/customers/${id}/transfer`, { ownerId })
}

export const contactApi = {
  list: (customerId: number) => http.get<any, Contact[]>(`/customers/${customerId}/contacts`),
  create: (customerId: number, data: any) => http.post<any, Contact>(`/customers/${customerId}/contacts`, data),
  update: (customerId: number, id: number, data: any) =>
    http.put<any, Contact>(`/customers/${customerId}/contacts/${id}`, data),
  remove: (customerId: number, id: number) => http.delete(`/customers/${customerId}/contacts/${id}`)
}

export const visitApi = {
  page: (params: any) => http.get<any, PageResult<Visit>>('/visits', { params }),
  detail: (id: number) => http.get<any, Visit>(`/visits/${id}`),
  create: (data: any) => http.post<any, Visit>('/visits', data),
  update: (id: number, data: any) => http.put<any, Visit>(`/visits/${id}`, data),
  checkin: (id: number) => http.post<any, Visit>(`/visits/${id}/checkin`),
  submit: (id: number, data: any) => http.post<any, Visit>(`/visits/${id}/submit`, data),
  approve: (id: number) => http.post<any, Visit>(`/visits/${id}/approve`),
  reject: (id: number, reason: string) => http.post<any, Visit>(`/visits/${id}/reject`, { reason }),
  cancel: (id: number, reason: string) => http.post<any, Visit>(`/visits/${id}/cancel`, { reason })
}

export const orderApi = {
  page: (params: any) => http.get<any, PageResult<Order>>('/orders', { params }),
  detail: (id: number) => http.get<any, Order>(`/orders/${id}`),
  create: (data: any) => http.post<any, Order>('/orders', data),
  update: (id: number, data: any) => http.put<any, Order>(`/orders/${id}`, data),
  submit: (id: number) => http.post<any, Order>(`/orders/${id}/submit`),
  approve: (id: number) => http.post<any, Order>(`/orders/${id}/approve`),
  reject: (id: number, reason: string) => http.post<any, Order>(`/orders/${id}/reject`, { reason }),
  ship: (id: number, data: any) => http.post<any, Order>(`/orders/${id}/ship`, data),
  payment: (id: number, data: any) => http.post<any, Order>(`/orders/${id}/payments`, data),
  cancel: (id: number, reason: string) => http.post<any, Order>(`/orders/${id}/cancel`, { reason }),
  remove: (id: number) => http.delete(`/orders/${id}`)
}

export const productApi = {
  page: (params: any) => http.get<any, PageResult<Product>>('/products', { params }),
  options: () => http.get<any, Product[]>('/products/options')
}

export const systemApi = {
  reset: (confirm: string) => http.post<any, any>('/system/reset', { confirm })
}

export const userApi = {
  page: (params: any) => http.get<any, PageResult<UserRow>>('/users', { params }),
  create: (data: any) => http.post<any, UserRow>('/users', data),
  update: (id: number, data: any) => http.put<any, UserRow>(`/users/${id}`, data),
  resetPassword: (id: number, password: string) =>
    http.post<any, void>(`/users/${id}/password`, { password })
}
