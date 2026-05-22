import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'

export default function Login() {
  const navegar = useNavigate()
  const [modo, setModo] = useState('login') // 'login' ou 'cadastro'
  const [form, setForm] = useState({ name: '', email: '', password: '', address: '' })
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  function atualizar(campo) {
    return (e) => setForm(prev => ({ ...prev, [campo]: e.target.value }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      if (modo === 'login') {
        const { data } = await api.post('/api/auth/login', {
          email: form.email,
          password: form.password,
        })
        localStorage.setItem('token', data.token)
      } else {
        const { data } = await api.post('/api/auth/register', {
          name: form.name,
          email: form.email,
          password: form.password,
          address: form.address,
        })
        localStorage.setItem('token', data.token)
      }
      navegar('/dashboard')
    } catch (err) {
      setErro(err.response?.data?.detail ?? 'Credenciais inválidas')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4 relative z-10">
      {/* logo */}
      <div className="mb-8 text-center">
        <h1 className="text-4xl font-extrabold text-text-main tracking-tight">
          Last <span className="text-brand">Mile</span>
        </h1>
        <p className="text-text-muted text-sm mt-1">
          Sistema de roteirização de entregas
        </p>
      </div>

      {/* card do formulário */}
      <div className="w-full max-w-sm bg-bg-surface border border-bg-border rounded-2xl shadow-soft p-6 space-y-4">
        {/* toggle login / cadastro */}
        <div className="flex bg-bg-deep border border-bg-border rounded-full p-1">
          <button
            type="button"
            onClick={() => setModo('login')}
            className={`flex-1 py-2 text-sm font-bold rounded-full transition-all
              ${modo === 'login'
                ? 'bg-brand text-white shadow-soft'
                : 'text-text-muted hover:text-text-main'}`}
          >
            Entrar
          </button>
          <button
            type="button"
            onClick={() => setModo('cadastro')}
            className={`flex-1 py-2 text-sm font-bold rounded-full transition-all
              ${modo === 'cadastro'
                ? 'bg-brand text-white shadow-soft'
                : 'text-text-muted hover:text-text-main'}`}
          >
            Cadastrar
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          {modo === 'cadastro' && (
            <Campo label="Nome da Loja" value={form.name} onChange={atualizar('name')} placeholder="Ex: Floricultura das Rosas" />
          )}
          <Campo label="E-mail" type="email" value={form.email} onChange={atualizar('email')} placeholder="loja@exemplo.com" />
          <Campo label="Senha" type="password" value={form.password} onChange={atualizar('password')} placeholder="••••••••" />
          {modo === 'cadastro' && (
            <Campo label="Endereço da Loja" value={form.address} onChange={atualizar('address')} placeholder="Rua X, 100, Cidade, UF" />
          )}

          {erro && (
            <p className="text-danger text-sm border border-danger/30 bg-danger/10 rounded-lg px-3 py-2">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={carregando}
            className="w-full py-2.5 bg-brand text-white text-sm font-bold
                       rounded-full shadow-soft hover:bg-brand-dark hover:shadow-soft-lg
                       transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {carregando
              ? '...'
              : modo === 'login' ? 'Acessar sistema' : 'Criar conta'}
          </button>
        </form>
      </div>

      {/* decoração de grade na base */}
      <div className="mt-8 flex gap-1 opacity-40">
        {Array.from({ length: 12 }).map((_, i) => (
          <div
            key={i}
            className="w-1 bg-brand rounded-full"
            style={{ height: `${8 + (i % 4) * 6}px` }}
          />
        ))}
      </div>
    </div>
  )
}

function Campo({ label, type = 'text', value, onChange, placeholder }) {
  return (
    <div>
      <label className="text-text-muted text-xs font-bold block mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        required
        placeholder={placeholder}
        className="w-full bg-bg-deep border border-bg-border rounded-lg px-3 py-2 text-text-main text-sm
                   placeholder:text-text-muted/60
                   focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/20 transition-all"
      />
    </div>
  )
}
