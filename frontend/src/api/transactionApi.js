import api from './axiosInstance'

export const getTransactions = (params) => api.get('/transactions', { params })
export const getTransaction  = (id)     => api.get(`/transactions/${id}`)
export const submitTransaction = (data) => api.post('/transactions', data)
export const getByAccount = (accountId, params) =>
  api.get(`/transactions/account/${accountId}`, { params })
