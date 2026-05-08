import { useEffect, useState } from 'react'
import heroImage from '../assets/hero.png'
import { PCComponentItem, RatingSummary, StarRating, UserReviewItem } from '../components/common'
import { buildBadge, formatEuro } from '../catalog-utils'
import type { CatalogPremontado, UserReview } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type ProductDetailPageProps = {
  product: CatalogPremontado
  onBack: () => void
}

export function ProductDetailPage({ product, onBack }: ProductDetailPageProps) {
  const rating = Math.max(1, Math.min(5, Math.round(product.valoracionMedia || 0)))
  const price = product.precioReducido ?? product.precio

  const [reviews, setReviews] = useState<UserReview[]>([])
  const [reviewsLoading, setReviewsLoading] = useState(true)

  useEffect(() => {
    const loadReviews = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/catalogo/premontados/${product.id}/valoraciones`)
        if (response.ok) {
          const data = await response.json()
          setReviews(data)
        }
      } catch {
        setReviews([])
      } finally {
        setReviewsLoading(false)
      }
    }

    void loadReviews()
  }, [product.id])

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
            <p className="product-brand">{product.marca}</p>

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

            <button type="button" className="cta-button">
              🛒 Añadir al carrito
            </button>

            <div className="product-highlights">
              {product.sistemaOperativo && (
                <div className="highlight-item">
                  <span className="highlight-icon">✓</span>
                  <span>Sistema operativo {product.sistemaOperativo} incluido</span>
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

        <section className="product-connectivity">
          <h2>Conectividad y puertos</h2>
          <div className="ports-grid">
            <div className="port-item">
              <p className="port-name">DisplayPort 1.4</p>
              <p className="port-count">x3</p>
            </div>
            <div className="port-item">
              <p className="port-name">Ethernet RJ45</p>
              <p className="port-count">x1</p>
            </div>
            <div className="port-item">
              <p className="port-name">Audio Jack 3.5mm</p>
              <p className="port-count">x2</p>
            </div>
          </div>
        </section>

        <section className="product-features">
          <h2>Características adicionales</h2>
          <div className="features-grid">
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>Sistema operativo Windows 11 Pro incluido</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>Refrigeración líquida AIO 280mm</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>WiFi 6E y Bluetooth 5.2 integrados</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>Iluminación RGB sincronizable</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>Montaje y testeo profesional incluido</span>
            </div>
            <div className="feature-item">
              <span className="feature-icon">✓</span>
              <span>Garantía de 3 años</span>
            </div>
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