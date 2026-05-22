import axios from 'axios'

// baseURL relativa: o frontend e a API ficam na mesma origem.
// No pacote Docker o Spring Boot serve os dois em http://localhost:8080.
// Em dev (npm run dev), o proxy do Vite encaminha /api para o backend.
const api = axios.create({
  baseURL: '',
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
