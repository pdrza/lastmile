import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// cores dos marcadores por status
const COR_STATUS = {
  PENDING:    '#64b5f6',
  IN_TRANSIT: '#c89639',
  DELIVERED:  '#1dd1a1',
  FAILED:     '#de4f41',
}

// cria ícone circular colorido para o marcador
function criarIcone(cor, label) {
  return L.divIcon({
    className: '',
    html: `
      <div style="
        width:28px; height:28px; border-radius:50%;
        background:#ffffff; border:3px solid ${cor};
        display:flex; align-items:center; justify-content:center;
        color:${cor}; font-size:11px; font-weight:800;
        font-family:'Nunito',sans-serif;
        box-shadow:0 2px 6px rgba(20,20,20,.25);
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
      width:32px; height:32px; border-radius:10px;
      background:#1dd1a1; border:2px solid #ffffff;
      display:flex; align-items:center; justify-content:center;
      color:#ffffff; font-size:15px;
      box-shadow:0 2px 8px rgba(20,20,20,.3);
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
  const pontosRota = rota && loja
    ? [
        [loja.latitude, loja.longitude],
        ...rota.deliveries.map(d => [d.latitude, d.longitude]),
        [loja.latitude, loja.longitude],
      ]
    : []

  return (
    <MapContainer
      center={centro}
      zoom={13}
      style={{ height: '100%', width: '100%' }}
    >
      {/* mapa claro CartoDB Voyager — combina com o tema claro */}
      <TileLayer
        url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
        attribution='&copy; <a href="https://carto.com/">CARTO</a>'
      />

      {/* marcador da loja (ponto de origem) */}
      {loja?.latitude && (
        <Marker position={[loja.latitude, loja.longitude]} icon={iconeOrigem}>
          <Popup>
            <div style={{ fontFamily: "'Nunito',sans-serif", color: '#141414', padding: '2px' }}>
              <strong style={{ color: '#15b98c' }}>Ponto de partida</strong><br />
              {loja.addressText}
            </div>
          </Popup>
        </Marker>
      )}

      {/* marcadores das entregas */}
      {entregas.map((entrega) => {
        if (!entrega.latitude) return null
        const cor = COR_STATUS[entrega.status] ?? '#6b7280'
        const label = entrega.deliveryOrder ?? '·'
        return (
          <Marker
            key={entrega.id}
            position={[entrega.latitude, entrega.longitude]}
            icon={criarIcone(cor, label)}
          >
            <Popup>
              <div style={{ fontFamily: "'Nunito',sans-serif", color: '#141414', padding: '2px', minWidth: '140px' }}>
                {entrega.deliveryOrder && (
                  <div style={{ color: '#6b7280', fontSize: '10px' }}>parada #{entrega.deliveryOrder}</div>
                )}
                <strong style={{ color: '#141414' }}>{entrega.customerName}</strong>
                <div style={{ fontSize: '11px', color: '#6b7280', marginTop: '2px' }}>{entrega.addressText}</div>
                <div style={{ color: cor, fontSize: '11px', fontWeight: 700, marginTop: '4px' }}>{entrega.status}</div>
              </div>
            </Popup>
          </Marker>
        )
      })}

      {/* linha da rota otimizada */}
      {pontosRota.length > 1 && (
        <Polyline
          positions={pontosRota}
          pathOptions={{ color: '#141414', weight: 3, opacity: 0.8, dashArray: '6 4' }}
        />
      )}
    </MapContainer>
  )
}
