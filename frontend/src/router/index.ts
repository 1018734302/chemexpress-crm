import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/layout/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'customers',
          name: 'customers',
          component: () => import('@/views/CustomerListView.vue'),
          meta: { title: '客户管理' }
        },
        {
          path: 'customers/:id',
          name: 'customer-detail',
          component: () => import('@/views/CustomerDetailView.vue'),
          meta: { title: '客户详情' }
        },
        {
          path: 'visits',
          name: 'visits',
          component: () => import('@/views/VisitListView.vue'),
          meta: { title: '拜访管理' }
        },
        {
          path: 'visits/:id',
          name: 'visit-detail',
          component: () => import('@/views/VisitDetailView.vue'),
          meta: { title: '拜访详情' }
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/OrderListView.vue'),
          meta: { title: '订单管理' }
        },
        {
          path: 'orders/new',
          name: 'order-create',
          component: () => import('@/views/OrderFormView.vue'),
          meta: { title: '新建订单' }
        },
        {
          path: 'orders/:id/edit',
          name: 'order-edit',
          component: () => import('@/views/OrderFormView.vue'),
          meta: { title: '编辑订单' }
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/OrderDetailView.vue'),
          meta: { title: '订单详情' }
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('@/views/ProductListView.vue'),
          meta: { title: '产品中心' }
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UserListView.vue'),
          meta: { title: '用户管理', adminOnly: true }
        },
        {
          path: '403',
          name: 'forbidden',
          component: () => import('@/views/ForbiddenView.vue'),
          meta: { title: '无权限' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('crm_token')
  if (to.path !== '/login' && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  if (to.meta.adminOnly) {
    let role = ''
    try {
      const raw = localStorage.getItem('crm_user')
      role = raw ? JSON.parse(raw).role : ''
    } catch {
      role = ''
    }
    if (role !== 'ADMIN') {
      return '/403'
    }
  }
  document.title = `${to.meta.title || '工作台'} · 皓元医药营销 CRM`
  return true
})

export default router
