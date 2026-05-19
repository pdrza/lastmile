// badge colorido que mostra o status da entrega
const cores = {
  PENDING:    'border-neon   text-neon   bg-neon/10',
  IN_TRANSIT: 'border-warning text-warning bg-warning/10',
  DELIVERED:  'border-success text-success bg-success/10',
  FAILED:     'border-magenta text-magenta bg-magenta/10',
}

const rotulos = {
  PENDING:    'PENDENTE',
  IN_TRANSIT: 'EM ROTA',
  DELIVERED:  'ENTREGUE',
  FAILED:     'FALHA',
}

export default function StatusBadge({ status }) {
  const classe = cores[status] ?? 'border-text-muted text-text-muted'
  return (
    <span className={`px-2 py-0.5 text-xs font-mono border rounded ${classe}`}>
      {rotulos[status] ?? status}
    </span>
  )
}
