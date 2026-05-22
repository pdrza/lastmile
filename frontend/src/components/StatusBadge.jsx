// badge colorido que mostra o status da entrega
const cores = {
  PENDING:    'border-accent  text-accent  bg-accent/10',
  IN_TRANSIT: 'border-warning text-warning bg-warning/10',
  DELIVERED:  'border-success text-success bg-success/10',
  FAILED:     'border-danger  text-danger  bg-danger/10',
}

const rotulos = {
  PENDING:    'Pendente',
  IN_TRANSIT: 'Em rota',
  DELIVERED:  'Entregue',
  FAILED:     'Falha',
}

export default function StatusBadge({ status }) {
  const classe = cores[status] ?? 'border-text-muted text-text-muted'
  return (
    <span className={`px-2.5 py-0.5 text-xs font-bold border rounded-full whitespace-nowrap ${classe}`}>
      {rotulos[status] ?? status}
    </span>
  )
}
