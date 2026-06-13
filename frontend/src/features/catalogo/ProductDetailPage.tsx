import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Check, ShoppingCart } from 'lucide-react'
import heroImage from '@/assets/hero.png'
import { useAuth } from '@/features/auth/useAuth'
import { PCComponentItem, RatingSummary, StarRating, UserReviewItem } from '@/shared/components/common'
import { buildBadge, formatEuro, usoLabel, usosOrdenados } from '@/shared/catalog-utils'
import type { CartItem, CatalogPremontado, UserReview } from '@/shared/types'

import { API_BASE_URL, authHeader } from '@/api/client'

type ProductDetailPageProps = {
  productId?: number
  onBack: () => void
  onReviewSubmitted?: () => void
  onAddToCart?: (item: CartItem) => void
  cartItems?: CartItem[]
}

export function ProductDetailPage({ productId: propProductId, onBack, onReviewSubmitted, onAddToCart, cartItems = [] }: ProductDetailPageProps) {
  const params = useParams()
  const navigate = useNavigate()
  const { token, isAuthenticated, user } = useAuth()
  const productId = propProductId ?? (params.id ? Number(params.id) : undefined)
  const [product, setProduct] = useState<CatalogPremontado | null>(null)
  const [productLoading, setProductLoading] = useState(true)
  const [productError, setProductError] = useState('')

  const [reviews, setReviews] = useState<UserReview[]>([])
  const [reviewsLoading, setReviewsLoading] = useState(true)
  const [isReviewFormOpen, setIsReviewFormOpen] = useState(false)
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewComment, setReviewComment] = useState('')
  const [reviewLoading, setReviewLoading] = useState(false)
  const [reviewError, setReviewError] = useState('')
  const [reviewSuccess, setReviewSuccess] = useState('')
  const [reviewLocked, setReviewLocked] = useState(false)

  const loadProduct = useCallback(async () => {
    setProductLoading(true)
    setProductError('')
    try {
      const resp = await fetch(`${API_BASE_URL}/api/catalogo/premontados/${productId}`)
      if (!resp.ok) {
        setProductError('No se encontró el producto')
        setProduct(null)
        return
      }
      const data = (await resp.json()) as CatalogPremontado
      setProduct(data)
    } catch {
      setProductError('Error al cargar el producto')
      setProduct(null)
    } finally {
      setProductLoading(false)
    }
  }, [productId])

  const loadReviews = useCallback(async () => {
    setReviewsLoading(true)
    try {
      const response = await fetch(`${API_BASE_URL}/api/catalogo/premontados/${productId}/valoraciones`)
      if (response.ok) {
        const data = (await response.json()) as UserReview[]
        setReviews(data)
        // If the user is logged in, detect whether they already have a review
        if (isAuthenticated && user) {
          const fullName = `${user.nombre} ${user.apellidos}`.trim()
          const hasReview = data.some((r) => r.usuarioNombre === fullName)
          setReviewLocked(Boolean(hasReview))
          if (hasReview) setIsReviewFormOpen(false)
        }
      } else {
        setReviews([])
      }
    } catch {
      setReviews([])
    } finally {
      setReviewsLoading(false)
    }
  }, [productId, isAuthenticated, user])

  useEffect(() => {
    void loadProduct()
  }, [loadProduct])

  useEffect(() => {
    void loadReviews()
  }, [loadReviews])

  const openReviewForm = () => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }

    setReviewSuccess('')
    setReviewError('')
    setIsReviewFormOpen((current) => !current)
  }

  const submitReview = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!isAuthenticated || !token) {
      navigate('/login')
      return
    }

    const comentario = reviewComment.trim()
    if (!comentario) {
      setReviewError('Escribe un comentario antes de enviarlo')
      return
    }

    setReviewLoading(true)
    setReviewError('')
    setReviewSuccess('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/catalogo/premontados/${productId}/valoraciones`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...authHeader(token),
        },
        body: JSON.stringify({ puntuacion: reviewRating, comentario }),
      })

      if (response.status === 401 || response.status === 403) {
        navigate('/login')
        return
      }

      if (!response.ok) {
        let message = 'No se pudo enviar la reseña'
        try {
          const errorData = (await response.json()) as { message?: string }
          message = errorData.message || message
        } catch {
          // mantener mensaje genérico
        }

        if (response.status === 409) {
          setReviewLocked(true)
        }

        setReviewError(message)
        return
      }

      setReviewRating(5)
      setReviewComment('')
      setIsReviewFormOpen(false)
      setReviewLocked(true)
      setReviewSuccess('Tu reseña se ha publicado correctamente')
      await Promise.all([loadProduct(), loadReviews()])
      onReviewSubmitted?.()
    } catch {
      setReviewError('No se pudo conectar con el backend')
    } finally {
      setReviewLoading(false)
    }
  }

  if (productLoading) {
    return (
      <main className="product-detail-page">
        <p>Cargando producto…</p>
      </main>
    )
  }

  if (!product) {
    return (
      <main className="product-detail-page">
        <button type="button" className="back-link" onClick={onBack}>
          &larr; Volver a productos
        </button>
        <p>{productError || 'Producto no disponible'}</p>
      </main>
    )
  }

  const rating = Math.max(0, Math.min(5, product.valoracionMedia || 0))
  const price = product.precioReducido ?? product.precio

  return (
    <main className="product-detail-page">
      <button type="button" className="back-link" onClick={onBack}>
        &larr; Volver a productos
      </button>

      <div className="product-detail-shell">
        <section className="product-detail-header">
          <div className="product-detail-image">
            <img src={product.imagenUrl || heroImage} alt={product.titulo} />
          </div>

          <div className="product-detail-info">
            <span className="pill pill--primary">{buildBadge(product)}</span>

            <h1>{product.titulo}</h1>
            <p className="product-brand"><strong>Marca:</strong> {product.marca}</p>

            {product.usosPrevistos.length > 0 && (
              <p className="product-usos">
                <strong>Tipos de uso:</strong> {usosOrdenados(product).map(usoLabel).join(', ')}
              </p>
            )}

            <div className="product-rating-inline">
              <span className="rating-count">
                <StarRating value={rating} /> ({product.numeroValoraciones} valoraciones)
              </span>
            </div>

            <div className="product-price-section">
              {product.precioReducido && product.descuento ? (
                <>
                  <span className="discount-badge">-{product.descuento}%</span>
                  <div className="price-row">
                    <strong className="price">{formatEuro(price)}€</strong>
                    <span className="old-price">{formatEuro(product.precio)}€</span>
                  </div>
                </>
              ) : (
                <strong className="price">{formatEuro(price)}€</strong>
              )}
            </div>

            <p className="product-price-note">IVA incluido • Envío gratis</p>

            {onAddToCart && product && (() => {
              const enCarrito = cartItems.some((i) => i.configuracionId === product.id)
              return (
                <button
                  type="button"
                  className={`cta-button${enCarrito ? ' cta-button--added' : ''}`}
                  onClick={() => onAddToCart({
                    configuracionId: product.id,
                    nombre: product.titulo,
                    precio: product.precioReducido ?? product.precio,
                    imagenUrl: product.imagenUrl,
                    cantidad: 1,
                  })}
                >
                  {enCarrito ? (
                    <>
                      <Check size={18} aria-hidden="true" /> Añadido al carrito
                    </>
                  ) : (
                    <>
                      <ShoppingCart size={18} aria-hidden="true" /> Añadir al carrito
                    </>
                  )}
                </button>
              )
            })()}

            <div className="product-highlights">
              {product.sistemaOperativo && (
                <div className="highlight-item">
                  <span className="highlight-icon">✓</span>
                  { product.sistemaOperativo === "WINDOWS"
                    ? <span>Windows 11 Pro preinstalado</span>
                    : product.sistemaOperativo === "LINUX"
                      ? <span>Linux preinstalado</span>
                      : <span>Sistema operativo no incluido</span>
                  }
                  
                </div>
              )}
              <div className="highlight-item">
                <span className="highlight-icon">✓</span>
                <span>Refrigeración líquida AIO 280mm</span>
              </div>
              <div className="highlight-item">
                <span className="highlight-icon">✓</span>
                <span>WiFi 6E y Bluetooth 5.2 integrados</span>
              </div>
            </div>
          </div>
        </section>

        <section className="product-components">
          <div className="section-header">
            <h2>Componentes del ordenador</h2>
            <p className="section-subtitle">Conoce cada pieza que compone este potente equipo y su función en el sistema</p>
          </div>

          <div className="components-grid">
            {product.componentes.map((component) => (
              <PCComponentItem key={component.id} component={component} />
            ))}
          </div>
        </section>

        {product.descripcion && (
          <section className="product-description">
            <h2>Descripción</h2>
            <p>{product.descripcion}</p>
          </section>
        )}

        <section className="product-reviews">
          <RatingSummary product={product} />

          <div className="review-actions">
            <button
              type="button"
              className="cta-button review-cta"
              onClick={openReviewForm}
              disabled={reviewLocked}
            >
              {isAuthenticated ? (reviewLocked ? 'Reseña enviada' : 'Añadir reseña') : 'Inicia sesión para reseñar'}
            </button>
            {!isAuthenticated ? <p className="review-help">Debes iniciar sesión para publicar una reseña.</p> : null}
            {reviewSuccess ? <p className="review-feedback review-feedback--success">{reviewSuccess}</p> : null}
          </div>

          {isReviewFormOpen && isAuthenticated && !reviewLocked ? (
            <form className="review-form" onSubmit={submitReview}>
              <div className="review-form__header">
                <h3>Escribe tu reseña</h3>
                <p>Solo puedes dejar una valoración por premontado.</p>
              </div>

              <div className="review-rating-picker" role="radiogroup" aria-label="Puntuación">
                {Array.from({ length: 5 }, (_, index) => index + 1).map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={value <= reviewRating ? 'review-rating-option review-rating-option--active' : 'review-rating-option'}
                    onClick={() => setReviewRating(value)}
                    aria-pressed={value === reviewRating}
                  >
                    ★
                  </button>
                ))}
              </div>

              <label className="review-form__field" htmlFor="review-comment">
                <span>Comentario</span>
                <textarea
                  id="review-comment"
                  rows={4}
                  value={reviewComment}
                  onChange={(event) => setReviewComment(event.currentTarget.value)}
                  placeholder="Cuéntanos qué te ha parecido este ordenador"
                />
              </label>

              {reviewError ? <p className="review-feedback review-feedback--error">{reviewError}</p> : null}

              <div className="review-form__actions">
                <button type="button" className="review-secondary-button" onClick={() => setIsReviewFormOpen(false)}>
                  Cancelar
                </button>
                <button type="submit" className="cta-button" disabled={reviewLoading}>
                  {reviewLoading ? 'Enviando...' : 'Publicar reseña'}
                </button>
              </div>
            </form>
          ) : null}

          <div className="reviews-list">
            <h3>Comentarios de clientes</h3>
            {reviewsLoading ? (
              <p>Cargando comentarios...</p>
            ) : reviews.length > 0 ? (
              reviews.map((review) => <UserReviewItem key={review.id} review={review} />)
            ) : (
              <p>No hay comentarios disponibles</p>
            )}
          </div>
        </section>
      </div>
    </main>
  )
}