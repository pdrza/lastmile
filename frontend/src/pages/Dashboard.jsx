import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import DeliveryCard from '../components/DeliveryCard'
import NewDeliveryForm from '../components/NewDeliveryForm'
import RouteMap from '../components/RouteMap'

const FILTROS = ['TODOS', 'PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED']

const ROTULO_FILTRO = {
  TODOS:      'Todos',
  PENDING:    'Pendente',
  IN_TRANSIT: 'Em rota',
  DELIVERED:  'Entregue',
  FAILED:     'Falha',
}

export default function Dashboard() {
  const navegar = useNavigate()
  const [entregas, setEntregas] = useState([])
  const [rotaAtual, setRotaAtual] = useState(null)
  const [loja, setLoja] = useState(null)
  const [filtro, setFiltro] = useState('TODOS')
  const [mostrarFormulario, setMostrarFormulario] = useState(false)
  const [entregaSelecionada, setEntregaSelecionada] = useState(null)
  const [otimizando, setOtimizando] = useState(false)
  const [mensagem, setMensagem] = useState(null) // { tipo: 'ok'|'erro', texto }

  useEffect(() => {
    carregarEntregas()
    carregarLoja()
  }, [])

  async function carregarLoja() {
    try {
      const { data } = await api.get('/api/stores/me')
      setLoja(data)
    } catch {
      // fallback: mapa centraliza em Curitiba
    }
  }

  async function carregarEntregas() {
    try {
      const { data } = await api.get('/api/deliveries')
      setEntregas(data)
    } catch {
      mostrarMensagem('erro', 'Erro ao carregar entregas')
    }
  }

  async function otimizarRota() {
    setOtimizando(true)
    setMensagem(null)
    try {
      const { data } = await api.post('/api/routes/optimize')
      setRotaAtual(data)
      // atualiza a lista para refletir os novos status IN_TRANSIT
      await carregarEntregas()
      mostrarMensagem('ok', `Rota criada! ${data.deliveries.length} entregas ordenadas.`)
    } catch (err) {
      mostrarMensagem('erro', err.response?.data?.detail ?? 'Erro ao otimizar rota')
    } finally {
      setOtimizando(false)
    }
  }

  function mostrarMensagem(tipo, texto) {
    setMensagem({ tipo, texto })
    setTimeout(() => setMensagem(null), 4000)
  }

  function sair() {
    localStorage.removeItem('token')
    navegar('/login')
  }

  function handleEntregaCriada(novaEntrega) {
    setEntregas(prev => [novaEntrega, ...prev])
    setMostrarFormulario(false)
    mostrarMensagem('ok', 'Entrega cadastrada!')
  }

  const entregasFiltradas = filtro === 'TODOS'
    ? entregas
    : entregas.filter(e => e.status === filtro)

  const temPendentes = entregas.some(e => e.status === 'PENDING')

  return (
    <div className="min-h-screen flex flex-col relative z-10">
      {/* header */}
      <header className="bg-bg-surface border-b border-bg-border shadow-soft px-5 py-3 flex items-center justify-between">
        <div className="flex items-baseline gap-3">
          <h1 className="text-text-main font-extrabold text-xl tracking-tight">
            Last <span className="text-brand">Mile</span>
          </h1>
          <span className="text-text-muted text-sm">/ dashboard</span>
        </div>
        <button
          onClick={sair}
          className="text-text-muted text-sm font-bold hover:text-danger transition-colors"
        >
          Sair
        </button>
      </header>

      {/* toast de mensagem */}
      {mensagem && (
        <div className={`fixed top-4 right-4 z-50 px-4 py-3 rounded-xl shadow-soft-lg border text-sm font-bold
          ${mensagem.tipo === 'ok'
            ? 'bg-brand-soft border-brand text-brand-dark'
            : 'bg-danger/10 border-danger text-danger'}`}>
          {mensagem.texto}
        </div>
      )}

      {/* conteúdo principal: duas colunas */}
      <div className="flex flex-1 overflow-hidden">

        {/* coluna esquerda — lista de entregas */}
        <div className="w-80 flex-shrink-0 flex flex-col border-r border-bg-border bg-bg-surface overflow-hidden">

          {/* ações */}
          <div className="p-3 border-b border-bg-border space-y-2">
            <button
              onClick={() => setMostrarFormulario(v => !v)}
              className="w-full py-2 border border-brand text-brand text-sm font-bold rounded-full
                         hover:bg-brand-soft transition-all"
            >
              + Nova entrega
            </button>
            <button
              onClick={otimizarRota}
              disabled={!temPendentes || otimizando}
              className="w-full py-2 bg-brand text-white text-sm font-bold rounded-full
                         shadow-soft hover:bg-brand-dark hover:shadow-soft-lg transition-all
                         disabled:opacity-40 disabled:cursor-not-allowed disabled:shadow-none"
            >
              {otimizando ? 'Otimizando...' : '⚡ Otimizar rota'}
            </button>
          </div>

          {/* formulário de nova entrega (expansível) */}
          {mostrarFormulario && (
            <div className="p-3 border-b border-bg-border">
              <NewDeliveryForm
                onCriada={handleEntregaCriada}
                onCancelar={() => setMostrarFormulario(false)}
              />
            </div>
          )}

          {/* filtros de status */}
          <div className="p-3 border-b border-bg-border">
            <div className="flex flex-wrap gap-1.5">
              {FILTROS.map(f => (
                <button
                  key={f}
                  onClick={() => setFiltro(f)}
                  className={`px-3 py-1 text-xs font-bold rounded-full border transition-all
                    ${filtro === f
                      ? 'border-brand text-white bg-brand'
                      : 'border-bg-border text-text-muted bg-bg-deep hover:border-brand hover:text-brand'}`}
                >
                  {ROTULO_FILTRO[f] ?? f}
                </button>
              ))}
            </div>
          </div>

          {/* lista de entregas */}
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {entregasFiltradas.length === 0 ? (
              <p className="text-text-muted text-sm text-center py-8">
                Nenhuma entrega encontrada.
              </p>
            ) : (
              entregasFiltradas.map(entrega => (
                <DeliveryCard
                  key={entrega.id}
                  entrega={entrega}
                  selecionada={entregaSelecionada?.id === entrega.id}
                  onClick={() => setEntregaSelecionada(
                    entregaSelecionada?.id === entrega.id ? null : entrega
                  )}
                />
              ))
            )}
          </div>

          {/* contador */}
          <div className="p-3 border-t border-bg-border">
            <p className="text-text-muted text-xs">
              {entregasFiltradas.length} entrega(s)
              {filtro !== 'TODOS' && ` com status ${ROTULO_FILTRO[filtro]?.toLowerCase()}`}
            </p>
          </div>
        </div>

        {/* coluna direita — mapa */}
        <div className="flex-1 flex flex-col">
          {/* info da rota (quando existir) */}
          {rotaAtual && (
            <div className="p-4 border-b border-bg-border bg-bg-surface shadow-soft flex items-center gap-8">
              <div>
                <span className="text-text-muted text-xs font-bold uppercase tracking-wide">Tempo estimado</span>
                <p className="text-text-main font-mono font-bold text-lg">
                  {Math.round(rotaAtual.totalTimeSeconds / 60)} min
                </p>
              </div>
              {rotaAtual.totalDistanceMeters > 0 && (
                <div>
                  <span className="text-text-muted text-xs font-bold uppercase tracking-wide">Distância</span>
                  <p className="text-text-main font-mono font-bold text-lg">
                    {(rotaAtual.totalDistanceMeters / 1000).toFixed(1)} km
                  </p>
                </div>
              )}
              <div className="ml-auto">
                <span className="text-text-muted text-xs font-bold uppercase tracking-wide">Paradas</span>
                <p className="text-brand font-mono font-bold text-lg">
                  {rotaAtual.deliveries.length}
                </p>
              </div>
            </div>
          )}

          {/* mapa */}
          <div className="flex-1">
            <RouteMap
              loja={loja}
              entregas={entregas}
              rota={rotaAtual}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
