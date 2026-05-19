import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// cores dos marcadores por status
const COR_STATUS = {
  PENDING:    '#bf00ff',
  IN_TRANSIT: '#ffaa00',
  DELIVERED:  '#00ffaa',
  FAILED:     '#ff006e',
}

// cria ícone circular colorido para o marcador
function criarIcone(cor, label) {
  return L.divIcon({
    className: '',
    html: `
      <div style="
        width:28px; height:28px; border-radius:50%;
        background:${cor}22; border:2px solid ${cor};
        display:flex; align-items:center; justify-content:center;
        color:${cor}; font-size:11px; font-weight:bold; font-family:monospace;
        box-shadow:0 0 8px ${cor}88;
      ">${label}</div>
    `,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  })
}

const iconeOrigem = L.divIcon({
  className: '',
  html: `
    <div style="
      width:32px; height:32px; border-radius:4px;
      background:#bf00ff33; border:2px solid #bf00ff;
      display:flex; align-items:center; justify-content:center;
      color:#bf00ff; font-size:14px;
      box-shadow:0 0 12px #bf00ff88;
    ">⬡</div>
  `,
  iconSize: [32, 32],
  iconAnchor: [16, 16],
})

export default function RouteMap({ loja, entregas, rota }) {
  // centro inicial: posição da loja ou Curitiba como fallback
  const centro = loja?.latitude
    ? [loja.latitude, loja.longitude]
    : [-25.4284, -49.2733]

  // pontos da polyline: origem → entregas na ordem otimizada
  const pontosRota = rota
    ? [
        [loja.latitude, loja.longitude],
        ...rota.deliveries.map(d => [d.latitude, d.longitude]),
        [loja.latitude, loja.longitude], // volta à origem
      ]
    : []

  return (
    <MapContainer
      center={centro}
      zoom={13}
      style={{ height: '100%', width: '100%', borderRadius: '4px' }}
    >
      {/* mapa escuro CartoDB — compatível com a estética cyberpunk */}
      <TileLayer
        url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        attribution='&copy; <a href="https://carto.com/">CARTO</a>'
      />

      {/* marcador da loja (ponto de origem) */}
      {loja?.latitude && (
        <Marker position={[loja.latitude, loja.longitude]} icon={iconeOrigem}>
          <Popup>
            <div style={{ fontFamily: 'monospace', color: '#bf00ff', background: '#0d0010', padding: '4px' }}>
              <strong>Ponto de partida</strong><br />
              {loja.addressText}
            </div>
          </Popup>
        </Marker>
      )}

      {/* marcadores das entregas */}
      {entregas.map((entrega) => {
        if (!entrega.latitude) return null
        const cor = COR_STATUS[entrega.status] ?? '#e8d0ff'
        const label = entrega.deliveryOrder ?? '·'
        return (
          <Marker
            key={entrega.id}
            position={[entrega.latitude, entrega.longitude]}
            icon={criarIcone(cor, label)}
          >
            <Popup>
              <div style={{ fontFamily: 'monospace', background: '#160020', color: '#e8d0ff', padding: '4px', minWidth: '140px' }}>
                {entrega.deliveryOrder && (
                  <div style={{ color: '#7a5a99', fontSize: '10px' }}>parada #{entrega.deliveryOrder}</div>
                )}
                <strong style={{ color: '#e8d0ff' }}>{entrega.customerName}</strong>
                <div style={{ fontSize: '11px', color: '#7a5a99', marginTop: '2px' }}>{entrega.addressText}</div>
                <div style={{ color: cor, fontSize: '11px', marginTop: '4px' }}>{entrega.status}</div>
              </div>
            </Popup>
          </Marker>
        )
      })}

      {/* linha da rota otimizada */}
      {pontosRota.length > 1 && (
        <Polyline
          positions={pontosRota}
          pathOptions={{ color: '#ff006e', weight: 2, opacity: 0.8, dashArray: '6 4' }}
        />
      )}
    </MapContainer>
  )
}
