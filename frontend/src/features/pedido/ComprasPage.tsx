import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Package, ArrowLeft } from 'lucide-react'
import { useAuth } from '@/features/auth/useAuth'
import { formatEuro } from '@/shared/catalog-utils'
import type { PedidoDto } from '@/shared/types'

import { API_BASE_URL, authHeader } from '@/api/client'

export function ComprasPage() {
  const navigate = useNavigate()
  const { token, user } = useAuth()
  const [pedidos, setPedidos] = useState<PedidoDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user || !token) {
      navigate('/login', { replace: true })
      return
    }

    const controller = new AbortController()

    const loadPedidos = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await fetch(`${API_BASE_URL}/api/pedidos`, {
          headers: authHeader(token),
          signal: controller.signal,
        })
        if (!response.ok) throw new Error()
        const data = (await response.json()) as PedidoDto[]
        setPedidos(data)
      } catch (err) {
        if (!(err instanceof DOMException && err.name === 'AbortError')) {
          setError('No se pudieron cargar tus compras')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadPedidos()
    return () => controller.abort()
  }, [user, token, navigate])

  return (
    <main className="compras-page">
      <div className="favoritos-page__header">
        <button type="button" className="favoritos-page__back" onClick={() => navigate('/')}>
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Volver al catálogo</span>
        </button>
        <h1>
          <Package size={26} aria-hidden="true" />
          Mis compras
        </h1>
      </div>

      {loading && <p className="catalog-message">Cargando tus pedidos...</p>}
      {error && <p className="catalog-message catalog-message--error">{error}</p>}

      {!loading && !error && pedidos.length === 0 && (
        <div className="favoritos-page__empty">
          <Package size={56} aria-hidden="true" />
          <h2>Aún no has realizado ninguna compra</h2>
          <p>Cuando realices un pedido, aparecerá aquí el historial.</p>
          <button type="button" className="details-button favoritos-page__empty-cta" onClick={() => navigate('/')}>
            Explorar catálogo
          </button>
        </div>
      )}

      {!loading && pedidos.length > 0 && (
        <div className="compras-list">
          {pedidos.map((pedido) => (
            <article key={pedido.id} className="compra-card">
              <div className="compra-card__header">
                <div>
                  <span className="compra-card__id">Pedido #{pedido.id}</span>
                  <span className="compra-card__fecha">
                    {new Date(pedido.fecha).toLocaleDateString('es-ES', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </span>
                </div>
                <strong className="compra-card__total">{formatEuro(pedido.total)}€</strong>
              </div>

              <ul className="compra-card__items">
                {pedido.items.map((item) => (
                  <li key={item.id} className="compra-card__item">
                    <span className="compra-card__item-nombre">{item.nombreProducto}</span>
                    <span className="compra-card__item-qty">× {item.cantidad}</span>
                    <span className="compra-card__item-subtotal">{formatEuro(item.subtotal)}€</span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}
