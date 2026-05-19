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
        <h1 className="text-4xl font-mono font-bold text-neon tracking-[0.2em] glow">
          OPTIROUTE
        </h1>
        <p className="text-text-muted text-xs tracking-widest mt-1">
          SISTEMA DE ROTEIRIZAÇÃO LAST-MILE
        </p>
      </div>

      {/* card do formulário */}
      <div className="w-full max-w-sm border border-bg-border bg-bg-surface rounded p-6 space-y-4">
        {/* toggle login / cadastro */}
        <div className="flex border border-bg-border rounded overflow-hidden">
          <button
            type="button"
            onClick={() => setModo('login')}
            className={`flex-1 py-2 text-xs font-mono tracking-wider transition-colors
              ${modo === 'login'
                ? 'bg-neon/20 text-neon border-r border-bg-border'
                : 'text-text-muted hover:text-text-main border-r border-bg-border'}`}
          >
            ENTRAR
          </button>
          <button
            type="button"
            onClick={() => setModo('cadastro')}
            className={`flex-1 py-2 text-xs font-mono tracking-wider transition-colors
              ${modo === 'cadastro'
                ? 'bg-neon/20 text-neon'
                : 'text-text-muted hover:text-text-main'}`}
          >
            CADASTRAR
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
            <p className="text-magenta text-xs border border-magenta/30 bg-magenta/10 rounded px-3 py-2">
              {erro}
            </p>
          )}

          <button
            type="submit"
            disabled={carregando}
            className="w-full py-2.5 bg-neon/20 border border-neon text-neon text-sm font-mono
                       rounded hover:bg-neon/30 glow transition-all
                       disabled:opacity-50 disabled:cursor-not-allowed tracking-widest"
          >
            {carregando
              ? '...'
              : modo === 'login' ? 'ACESSAR SISTEMA' : 'CRIAR CONTA'}
          </button>
        </form>
      </div>

      {/* decoração de grade na base */}
      <div className="mt-8 flex gap-1 opacity-30">
        {Array.from({ length: 12 }).map((_, i) => (
          <div
            key={i}
            className="w-1 bg-neon rounded-full"
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
      <label className="text-text-muted text-xs block mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={onChange}
        required
        placeholder={placeholder}
        className="w-full bg-bg-deep border border-bg-border rounded px-3 py-2 text-text-main text-sm
                   font-mono placeholder:text-text-muted/50
                   focus:outline-none focus:border-neon transition-colors"
      />
    </div>
  )
}
