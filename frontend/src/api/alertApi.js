import api from './axiosInstance'

export const getAlerts       = (params) => api.get('/alerts', { params })
export const getAlert        = (id)     => api.get(`/alerts/${id}`)
export const getAlertAudit   = (id)     => api.get(`/alerts/${id}/audit`)
export const getAlertStats   = ()       => api.get('/alerts/stats')
export const getForwardedAlerts = ()    => api.get('/alerts/investigation')
export const forwardAlert    = (id, body) => api.post(`/alerts/${id}/forward`, body || {})
export const dismissAlert    = (id, body) => api.post(`/alerts/${id}/dismiss`, body || {})
export const closeAlert      = (id, body) => api.post(`/alerts/${id}/close`,   body || {})
