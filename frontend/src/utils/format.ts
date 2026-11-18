export function money(value?: number | null): string {
  const number = Number(value || 0)
  return `¥ ${number.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

export function plainMoney(value?: number | null): string {
  return Number(value || 0).toFixed(2)
}

export function maskPhone(phone?: string): string {
  if (!phone || phone.length !== 11) return phone || '—'
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

export function dateTime(value?: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '—'
}

export function dateOnly(value?: string): string {
  return value ? value.slice(0, 10) : '—'
}

export function today(): string {
  const d = new Date()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

export function statusType(status?: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (!status) return 'info'
  if (['APPROVED', 'COMPLETED', 'ACTIVE', 'SUCCESS'].includes(status)) return 'success'
  if (['SUBMITTED', 'PENDING', 'PLANNED', 'CHECKED_IN', 'POTENTIAL'].includes(status)) return 'warning'
  if (['REJECTED', 'CANCELLED', 'CHURNED', 'FAILED'].includes(status)) return 'danger'
  if (['SHIPPED', 'PARTIAL'].includes(status)) return 'primary'
  return 'info'
}

export function statusLabel(status?: string, fallback?: string): string {
  return fallback || status || '—'
}
