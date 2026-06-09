import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Heart, ArrowLeft } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { ProductCardView } from '../components/common'
import { toProductCard } from '../catalog-utils'
import type { FavoritoDto } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type FavoritosPageProps = {
  favoritosIds: Set<number>
  toggleFavorito: (id: number) => void
}

export function FavoritosPage({ favoritosIds, toggleFavorito }: FavoritosPageProps) {
  const navigate = useNavigate()
  const { token, user } = useAuth()
  const [favoritos, setFavoritos] = useState<FavoritoDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user || !token) {
      navigate('/login', { replace: true })
      return
    }

    const controller = new AbortController()

    const loadFavoritos = async () => {
      setLoading(true)
      setError('')
      try {
        const response = await fetch(`${API_BASE_URL}/api/catalogo/favoritos`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal,
        })
        if (!response.ok) throw new Error()
        const data = (await response.json()) as FavoritoDto[]
        setFavoritos(data)
      } catch (err) {
        if (!(err instanceof DOMException && err.name === 'AbortError')) {
          setError('No se pudieron cargar tus favoritos')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }

    void loadFavoritos()
    return () => controller.abort()
  }, [user, token, navigate])

  const displayed = favoritos.filter((f) => favoritosIds.has(f.premontado.id))

  return (
    <main className="favoritos-page">
      <div className="favoritos-page__header">
        <button type="button" className="favoritos-page__back" onClick={() => navigate('/')}>
          <ArrowLeft size={18} aria-hidden="true" />
          <span>Volver al catálogo</span>
        </button>
        <h1>
          <Heart size={26} fill="#ef4444" stroke="#ef4444" aria-hidden="true" />
          Mis favoritos
        </h1>
      </div>

      {loading && <p className="catalog-message">Cargando favoritos...</p>}
      {error && <p className="catalog-message catalog-message--error">{error}</p>}

      {!loading && !error && displayed.length === 0 && (
        <div className="favoritos-page__empty">
          <Heart size={56} aria-hidden="true" />
          <h2>Aún no tienes favoritos</h2>
          <p>Pulsa el corazón en cualquier tarjeta del catálogo para guardar aquí tus equipos favoritos.</p>
          <button type="button" className="details-button favoritos-page__empty-cta" onClick={() => navigate('/')}>
            Explorar catálogo
          </button>
        </div>
      )}

      {!loading && displayed.length > 0 && (
        <div className="product-grid">
          {displayed.map((f) => {
            const card = toProductCard(f.premontado, 'featured')
            return (
              <ProductCardView
                key={f.premontado.id}
                product={card}
                onViewDetails={() => navigate(`/productos/${f.premontado.id}`)}
                favorita={true}
                onToggleFavorite={() => toggleFavorito(f.premontado.id)}
              />
            )
          })}
        </div>
      )}
    </main>
  )
}
