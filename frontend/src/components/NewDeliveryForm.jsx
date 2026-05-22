import { useState } from 'react'
import api from '../services/api'

export default function NewDeliveryForm({ onCriada, onCancelar }) {
  const [nomeCliente, setNomeCliente] = useState('')
  const [endereco, setEndereco] = useState('')
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setErro('')
    setCarregando(true)
    try {
      const { data } = await api.post('/api/deliveries', {
        customerName: nomeCliente,
        address: endereco,
      })
      onCriada(data)
    } catch (err) {
      setErro(err.response?.data?.detail ?? 'Erro ao cadastrar entrega')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="p-4 border border-bg-border bg-bg-deep rounded-xl space-y-3">
      <h3 className="text-text-main font-extrabold text-sm">
        Nova entrega
      </h3>

      <div>
        <label className="text-text-muted text-xs font-bold block mb-1">Nome do Cliente</label>
        <input
          type="text"
          value={nomeCliente}
          onChange={e => setNomeCliente(e.target.value)}
          required
          placeholder="Ex: João Silva"
          className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-text-main text-sm
                     placeholder:text-text-muted/60
                     focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/20 transition-all"
        />
      </div>

      <div>
        <label className="text-text-muted text-xs font-bold block mb-1">Endereço completo</label>
        <input
          type="text"
          value={endereco}
          onChange={e => setEndereco(e.target.value)}
          required
          placeholder="Ex: Rua XV de Novembro, 1000, Curitiba, PR"
          className="w-full bg-bg-surface border border-bg-border rounded-lg px-3 py-2 text-text-main text-sm
                     placeholder:text-text-muted/60
                     focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/20 transition-all"
        />
        <p className="text-text-muted text-xs mt-1">
          O sistema transforma o endereço em coordenadas automaticamente.
        </p>
      </div>

      {erro && (
        <p className="text-danger text-sm border border-danger/30 bg-danger/10 rounded-lg px-3 py-2">
          {erro}
        </p>
      )}

      <div className="flex gap-2 pt-1">
        <button
          type="submit"
          disabled={carregando}
          className="flex-1 py-2 bg-brand text-white text-sm font-bold rounded-full
                     shadow-soft hover:bg-brand-dark hover:shadow-soft-lg transition-all
                     disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {carregando ? 'Geocodificando...' : 'Cadastrar'}
        </button>
        <button
          type="button"
          onClick={onCancelar}
          className="px-4 py-2 border border-bg-border text-text-muted text-sm font-bold rounded-full
                     hover:border-text-muted hover:text-text-main transition-all"
        >
          Cancelar
        </button>
      </div>
    </form>
  )
}
