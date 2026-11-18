export interface UserInfo {
  id: number
  username: string
  name: string
  role: 'ADMIN' | 'DSM' | 'MR' | 'OPS'
  roleName: string
  region: string
}

export interface UserRow {
  id: number
  username: string
  name: string
  role: 'ADMIN' | 'DSM' | 'MR' | 'OPS'
  roleName: string
  region: string
  phone?: string
  enabled: boolean
  createdAt?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface MetaOptions {
  roles: Record<string, string>
  regions: Record<string, string>
  customerTypes: Record<string, string>
  customerLevels: Record<string, string>
  customerStatuses: Record<string, string>
  visitPurposes: Record<string, string>
  visitStatuses: Record<string, string>
  visitResults: Record<string, string>
  orderStatuses: Record<string, string>
  paymentMethods: Record<string, string>
  contactRoles: Record<string, string>
  owners: Array<{ id: number; name: string; region: string }>
}

export interface Customer {
  id: number
  code: string
  name: string
  type: string
  typeName: string
  level: string
  region: string
  ownerId: number
  ownerName: string
  status: string
  statusName: string
  address: string
  annualPotential: number | null
  tags?: string
  source?: string
  remark?: string
  lastVisitDate?: string
  createdAt?: string
  updatedAt?: string
  version?: number
}

export interface Contact {
  id: number
  customerId: number
  name: string
  title?: string
  role: string
  roleName?: string
  phone?: string
  email?: string
  primaryContact: boolean
  status: string
  remark?: string
}

export interface Product {
  id: number
  code: string
  tradeName: string
  genericName: string
  spec: string
  dosageForm: string
  insuranceType: string
  price: number
  status: string
  indication?: string
}

export interface Visit {
  id: number
  code: string
  customerId: number
  customerName: string
  contactId?: number
  contactName?: string
  repId: number
  repName: string
  region: string
  visitDate: string
  purpose: string
  purposeName: string
  status: string
  statusName: string
  checkinTime?: string
  checkinDistance?: number
  result?: string
  feedback?: string
  nextStep?: string
  rejectReason?: string
  approverId?: number
  approvedAt?: string
  cancelReason?: string
  remark?: string
  products?: Product[]
  events?: BusinessEvent[]
}

export interface OrderItem {
  id: number
  productId: number
  productName: string
  productSpec: string
  unitPrice: number
  quantity: number
  discount: number
  amount: number
}

export interface Payment {
  id: number
  orderId: number
  payDate: string
  amount: number
  method: string
  methodName?: string
  referenceNo?: string
  remark?: string
  createdByName?: string
  createdAt?: string
}

export interface BusinessEvent {
  id: number
  bizType: string
  bizId: number
  action: string
  detail: string
  operatorName?: string
  createdAt: string
}

export interface Order {
  id: number
  code: string
  customerId: number
  customerName: string
  repId: number
  repName: string
  region: string
  status: string
  statusName: string
  totalAmount: number
  paidAmount: number
  receiverAddress?: string
  expectedDate?: string
  remark?: string
  rejectReason?: string
  approverId?: number
  approvedAt?: string
  logisticsCompany?: string
  trackingNo?: string
  shippedAt?: string
  cancelReason?: string
  completedAt?: string
  createdAt: string
  items?: OrderItem[]
  payments?: Payment[]
  events?: BusinessEvent[]
}

export interface DashboardKpi {
  key: string
  label: string
  value: string
  hint?: string
}

export interface DashboardData {
  kpis: DashboardKpi[]
  months: string[]
  salesTrend: number[]
  customerTypes: Array<{ name: string; value: number }>
  todayVisits?: Visit[]
  recentOrders?: Order[]
  recentEvents?: BusinessEvent[]
}
