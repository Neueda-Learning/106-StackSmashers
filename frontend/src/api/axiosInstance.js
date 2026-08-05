import axios from 'axios'

// VITE_API_URL is set on Vercel (e.g. https://tms-backend.onrender.com/api)
// Locally it falls back to '/api' which the Vite dev proxy forwards to localhost:8080
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('tms_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Handle 401 — redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('tms_token')
      localStorage.removeItem('tms_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
