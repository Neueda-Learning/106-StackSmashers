import api from './axiosInstance'

export const getRules    = ()          => api.get('/rules')
export const getRule     = (id)        => api.get(`/rules/${id}`)
export const updateRule  = (id, data)  => api.put(`/rules/${id}`, data)
