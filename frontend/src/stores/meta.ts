import { defineStore } from 'pinia'
import { metaApi } from '@/api'
import type { MetaOptions } from '@/types'

const empty: MetaOptions = {
  roles: {},
  regions: {},
  customerTypes: {},
  customerLevels: {},
  customerStatuses: {},
  visitPurposes: {},
  visitStatuses: {},
  visitResults: {},
  orderStatuses: {},
  paymentMethods: {},
  contactRoles: {},
  owners: []
}

export const useMetaStore = defineStore('meta', {
  state: () => ({
    options: empty,
    loaded: false,
    loading: false
  }),
  actions: {
    async load(force = false) {
      if (this.loaded && !force) return this.options
      if (this.loading) return this.options
      this.loading = true
      try {
        this.options = await metaApi.options()
        this.loaded = true
        return this.options
      } finally {
        this.loading = false
      }
    }
  }
})
