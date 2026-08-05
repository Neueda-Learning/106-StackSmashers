import api from './axiosInstance'

export const login = (credentials) => api.post('/auth/login', credentials)
