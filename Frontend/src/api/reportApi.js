import api from './axiosInstance'

export const getTransactionReport = (params) => api.get('/reports/transactions', { params })
export const getAlertReport       = (params) => api.get('/reports/alerts',       { params })
export const getAccountReport     = ()       => api.get('/reports/accounts')
export const getAuditReport       = (params) => api.get('/reports/audit',        { params })
