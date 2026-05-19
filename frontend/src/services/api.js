import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
})

// antes de toda requisição: injeta o token JWT do localStorage
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// se receber 401 (token inválido/expirado): limpa e manda pro login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
