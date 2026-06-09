import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bookmark, ArrowLeft, Cpu, ShoppingCart, Trash2 } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { formatEuro } from '../catalog-utils'
import type { CartItem, MiConfiguracionDto } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type MisConfiguracionesPageProps = {
  onAddToCart?: (item: CartItem) => void
}

export function MisConfiguracionesPage({ onAddToCart }: MisConfiguracionesPageProps) {
  const navigate = useNavigate()
  const { token, user } = useAuth()
  const [configs, setConfigs] = useState<MiConfiguracionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [deleting, setDeleting] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<Record<number, string>>({})

  useEffect(() => {
    if (!user || !token) {
      navigate('/login', { replace: true })
      return
    }

    const controller = new AbortController()

    const loadConfigs = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await fetch(`${API_BASE_URL}/api/mis-configuraciones`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        })
        if (!response.ok) throw new Error()
        const data = (await response.json()) as MiConfiguracionDto[]
        setConfigs(data)
      } catch (err) {
        if (!(err instanceof DOMException && err.name === 'AbortError')) {
          setError('No se pudieron cargar tus configuraciones')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadConfigs()
    return () => controller.abort()
  }, [user, token, navigate])

  const handleEliminar = async (id: number) => {
    if (!token) return
    setDeleting(id)
    setDeleteError((prev) => ({ ...prev, [id]: '' }))
    try {
      const response = await fetch(`${API_BASE_URL}/api/mis-configuraciones/${id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (response.status === 409) {
        const data = (await response.json()) as { message?: string }
        setDeleteError((prev) => ({
          ...prev,
          [id]: data.message ?? 'No puedes eliminar una configuración que ya ha sido pedida',
        }))
        return
      }
      if (!response.ok) {
        setDeleteError((prev) => ({ ...prev, [id]: 'No se pudo eliminar la configuración' }))
        return
      }
      setConfigs((prev) => prev.filter((c) => c.id !== id))
    } catch {
      setDeleteError((prev) => ({ ...prev, [id]: 'Error al conectar con el servidor' }))
    } finally {
      setDeleting(null)
    }
  }

  const handleCargar = (config: MiConfiguracionDto) => {
    navigate('/montar-pc', {
      state: {
        preseleccion: config.componentes.map((c) => ({
          id: c.id,
          tipo: c.tipo,
          nombre: c.nombre,
          precio: c.precio,
        })),
      },
    })
  }

  const handleAddToCart = (config: MiConfiguracionDto) => {
    onAddToCart?.({
      configuracionId: config.id,
      nombre: config.nombre,
      precio: config.precio,
      cantidad: 1,
    })
    navigate('/carrito')
  }

  return (
    <main className="compras-page">
      <div className="favoritos-page__header">
        <button type="button" className="favoritos-page__back" onClick={() => navigate('/')}>
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Volver al catálogo</span>
        </button>
        <h1>
          <Bookmark size={26} aria-hidden="true" />
          Mis configuraciones
        </h1>
      </div>

      {loading && <p className="catalog-message">Cargando tus configuraciones...</p>}
      {error && <p className="catalog-message catalog-message--error">{error}</p>}

      {!loading && !error && configs.length === 0 && (
        <div className="favoritos-page__empty">
          <Bookmark size={56} aria-hidden="true" />
          <h2>Aún no has guardado ninguna configuración</h2>
          <p>Desde el configurador puedes guardar tus builds para recuperarlas más tarde.</p>
          <button
            type="button"
            className="details-button favoritos-page__empty-cta"
            onClick={() => navigate('/montar-pc')}
          >
            Ir al configurador
          </button>
        </div>
      )}

      {!loading && configs.length > 0 && (
        <div className="compras-list">
          {configs.map((config) => (
            <article key={config.id} className="compra-card mis-config-card">
              <div className="compra-card__header">
                <div>
                  <span className="compra-card__id">{config.nombre}</span>
                  <span className="compra-card__fecha">
                    {new Date(config.fechaCreacion).toLocaleDateString('es-ES', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </span>
                </div>
                <strong className="compra-card__total">{formatEuro(config.precio)}€</strong>
              </div>

              <ul className="compra-card__items">
                {config.componentes.map((comp) => (
                  <li key={comp.id} className="compra-card__item">
                    <span className="compra-card__item-nombre">{comp.nombre}</span>
                    <span className="compra-card__item-subtotal">{formatEuro(comp.precio)}€</span>
                  </li>
                ))}
              </ul>

              {deleteError[config.id] && (
                <p className="catalog-message catalog-message--error mis-config-delete-error">
                  {deleteError[config.id]}
                </p>
              )}

              <div className="mis-config-actions">
                <button
                  type="button"
                  className="mis-config-btn mis-config-btn--load"
                  onClick={() => handleCargar(config)}
                >
                  <Cpu size={15} aria-hidden="true" />
                  Cargar en configurador
                </button>
                <button
                  type="button"
                  className="mis-config-btn mis-config-btn--cart"
                  onClick={() => handleAddToCart(config)}
                >
                  <ShoppingCart size={15} aria-hidden="true" />
                  Añadir al carrito
                </button>
                <button
                  type="button"
                  className="mis-config-btn mis-config-btn--delete"
                  onClick={() => handleEliminar(config.id)}
                  disabled={deleting === config.id}
                  aria-label="Eliminar configuración"
                >
                  <Trash2 size={15} aria-hidden="true" />
                  {deleting === config.id ? 'Eliminando...' : 'Eliminar'}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </main>
  )
}
