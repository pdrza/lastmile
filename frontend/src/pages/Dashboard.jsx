import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../services/api'
import DeliveryCard from '../components/DeliveryCard'
import NewDeliveryForm from '../components/NewDeliveryForm'
import RouteMap from '../components/RouteMap'

const FILTROS = ['TODOS', 'PENDING', 'IN_TRANSIT', 'DELIVERED', 'FAILED']

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
  }, [])

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
      <header className="border-b border-bg-border bg-bg-surface px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-neon font-mono font-bold tracking-widest text-lg glow">
            OPTIROUTE
          </h1>
          <span className="text-text-muted text-xs">/ dashboard</span>
        </div>
        <button
          onClick={sair}
          className="text-text-muted text-xs font-mono hover:text-magenta transition-colors"
        >
          SAIR
        </button>
      </header>

      {/* toast de mensagem */}
      {mensagem && (
        <div className={`fixed top-4 right-4 z-50 px-4 py-3 rounded border text-sm font-mono
          ${mensagem.tipo === 'ok'
            ? 'bg-success/10 border-success text-success'
            : 'bg-magenta/10 border-magenta text-magenta'}`}>
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
              className="w-full py-2 border border-neon text-neon text-xs font-mono rounded
                         hover:bg-neon/20 glow transition-all tracking-wider"
            >
              + NOVA ENTREGA
            </button>
            <button
              onClick={otimizarRota}
              disabled={!temPendentes || otimizando}
              className="w-full py-2 border border-magenta text-magenta text-xs font-mono rounded
                         hover:bg-magenta/20 glow-magenta transition-all tracking-wider
                         disabled:opacity-30 disabled:cursor-not-allowed"
            >
              {otimizando ? 'OTIMIZANDO...' : '⚡ OTIMIZAR ROTA'}
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
            <div className="flex flex-wrap gap-1">
              {FILTROS.map(f => (
                <button
                  key={f}
                  onClick={() => setFiltro(f)}
                  className={`px-2 py-0.5 text-xs font-mono rounded border transition-colors
                    ${filtro === f
                      ? 'border-neon text-neon bg-neon/10'
                      : 'border-bg-border text-text-muted hover:border-text-muted'}`}
                >
                  {f === 'TODOS' ? 'TODOS' : f.replace('_', ' ')}
                </button>
              ))}
            </div>
          </div>

          {/* lista de entregas */}
          <div className="flex-1 overflow-y-auto p-3 space-y-2">
            {entregasFiltradas.length === 0 ? (
              <p className="text-text-muted text-xs text-center py-8">
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
            <p className="text-text-muted text-xs font-mono">
              {entregasFiltradas.length} entrega(s)
              {filtro !== 'TODOS' && ` com status ${filtro.replace('_', ' ')}`}
            </p>
          </div>
        </div>

        {/* coluna direita — mapa */}
        <div className="flex-1 flex flex-col">
          {/* info da rota (quando existir) */}
          {rotaAtual && (
            <div className="p-3 border-b border-bg-border bg-bg-surface flex items-center gap-6">
              <div>
                <span className="text-text-muted text-xs">TEMPO ESTIMADO</span>
                <p className="text-neon font-mono font-bold">
                  {Math.round(rotaAtual.totalTimeSeconds / 60)} min
                </p>
              </div>
              {rotaAtual.totalDistanceMeters > 0 && (
                <div>
                  <span className="text-text-muted text-xs">DISTÂNCIA</span>
                  <p className="text-neon font-mono font-bold">
                    {(rotaAtual.totalDistanceMeters / 1000).toFixed(1)} km
                  </p>
                </div>
              )}
              <div className="ml-auto">
                <span className="text-text-muted text-xs">PARADAS</span>
                <p className="text-neon font-mono font-bold">
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
