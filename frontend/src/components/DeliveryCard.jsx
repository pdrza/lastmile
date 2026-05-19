import StatusBadge from './StatusBadge'

export default function DeliveryCard({ entrega, selecionada, onClick }) {
  return (
    <div
      onClick={onClick}
      className={`
        p-3 border rounded cursor-pointer transition-all duration-150
        ${selecionada
          ? 'border-neon bg-neon/10 glow'
          : 'border-bg-border bg-bg-surface hover:border-neon/50'}
      `}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          {entrega.deliveryOrder && (
            <span className="text-xs text-text-muted font-mono mr-2">
              #{entrega.deliveryOrder}
            </span>
          )}
          <span className="text-text-main font-medium text-sm">
            {entrega.customerName}
          </span>
          <p className="text-text-muted text-xs mt-1 truncate">
            {entrega.addressText}
          </p>
        </div>
        <StatusBadge status={entrega.status} />
      </div>
    </div>
  )
}
