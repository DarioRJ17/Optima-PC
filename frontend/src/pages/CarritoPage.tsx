import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShoppingCart, Trash2, Plus, Minus, CheckCircle } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { formatEuro } from '../catalog-utils'
import type { CartItem } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type CarritoPageProps = {
  cartItems: CartItem[]
  onRemoveFromCart: (configuracionId: number) => void
  onUpdateQuantity: (configuracionId: number, cantidad: number) => void
  onClearCart: () => void
}

export function CarritoPage({ cartItems, onRemoveFromCart, onUpdateQuantity, onClearCart }: CarritoPageProps) {
  const navigate = useNavigate()
  const { token, isAuthenticated } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const total = cartItems.reduce((sum, item) => sum + item.precio * item.cantidad, 0)

  const handleCheckout = async () => {
    if (!isAuthenticated || !token) {
      navigate('/login')
      return
    }

    setLoading(true)
    setError('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/pedidos`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          items: cartItems.map((item) => ({
            configuracionId: item.configuracionId,
            cantidad: item.cantidad,
          })),
        }),
      })

      if (!response.ok) {
        setError('No se pudo realizar el pedido. Inténtalo de nuevo.')
        return
      }

      onClearCart()
      setSuccess(true)
    } catch {
      setError('No se pudo conectar con el servidor.')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <main className="carrito-page">
        <div className="carrito-success">
          <CheckCircle size={72} className="carrito-success__icon" aria-hidden="true" />
          <h1>¡Pedido realizado con éxito!</h1>
          <p>Tu pedido ha sido registrado correctamente. Puedes ver el detalle de tus compras en tu perfil.</p>
          <div className="carrito-success__actions">
            <button type="button" className="details-button" onClick={() => navigate('/compras')}>
              Ver mis compras
            </button>
            <button type="button" className="carrito-secondary-btn" onClick={() => navigate('/')}>
              Seguir comprando
            </button>
          </div>
        </div>
      </main>
    )
  }

  return (
    <main className="carrito-page">
      <div className="carrito-page__header">
        <button type="button" className="favoritos-page__back" onClick={() => navigate('/')}>
          ← Volver al catálogo
        </button>
        <h1>
          <ShoppingCart size={26} aria-hidden="true" />
          Carrito
        </h1>
      </div>

      {cartItems.length === 0 ? (
        <div className="favoritos-page__empty">
          <ShoppingCart size={56} aria-hidden="true" />
          <h2>Tu carrito está vacío</h2>
          <p>Añade productos desde el catálogo o los detalles de un producto.</p>
          <button type="button" className="details-button favoritos-page__empty-cta" onClick={() => navigate('/')}>
            Explorar catálogo
          </button>
        </div>
      ) : (
        <div className="carrito-shell">
          <ul className="carrito-items">
            {cartItems.map((item) => (
              <li key={item.configuracionId} className="carrito-item">
                <div className="carrito-item__info">
                  <p className="carrito-item__nombre">{item.nombre}</p>
                  <p className="carrito-item__precio-unit">{formatEuro(item.precio)}€ / ud.</p>
                </div>
                <div className="carrito-item__controls">
                  <button
                    type="button"
                    className="carrito-qty-btn"
                    onClick={() => onUpdateQuantity(item.configuracionId, item.cantidad - 1)}
                    disabled={item.cantidad <= 1}
                    aria-label="Reducir cantidad"
                  >
                    <Minus size={14} />
                  </button>
                  <span className="carrito-qty">{item.cantidad}</span>
                  <button
                    type="button"
                    className="carrito-qty-btn"
                    onClick={() => onUpdateQuantity(item.configuracionId, item.cantidad + 1)}
                    aria-label="Aumentar cantidad"
                  >
                    <Plus size={14} />
                  </button>
                </div>
                <p className="carrito-item__subtotal">{formatEuro(item.precio * item.cantidad)}€</p>
                <button
                  type="button"
                  className="carrito-remove-btn"
                  onClick={() => onRemoveFromCart(item.configuracionId)}
                  aria-label="Eliminar del carrito"
                >
                  <Trash2 size={16} />
                </button>
              </li>
            ))}
          </ul>

          <div className="carrito-summary">
            <div className="carrito-summary__row carrito-summary__row--total">
              <span>Total</span>
              <strong>{formatEuro(total)}€</strong>
            </div>
            <p className="carrito-summary__note">IVA incluido · Envío gratuito</p>

            {error && <p className="catalog-message catalog-message--error">{error}</p>}

            <button
              type="button"
              className="details-button carrito-checkout-btn"
              onClick={handleCheckout}
              disabled={loading}
            >
              {loading ? 'Procesando...' : isAuthenticated ? 'Realizar pedido' : 'Inicia sesión para comprar'}
            </button>
          </div>
        </div>
      )}
    </main>
  )
}
