import heroImage from '../assets/hero.png'
import type { CatalogPremontado, PCComponent, ProductCard, UserReview } from '../types'

export function StarRating({ value }: { value: number }) {
  return (
    <div className="star-rating" aria-label={`${value} de 5 estrellas`}>
      {Array.from({ length: 5 }).map((_, index) => (
        <span key={index} className={index < value ? 'star star--filled' : 'star'}>
          ★
        </span>
      ))}
    </div>
  )
}

export function PerformanceMeter({ value }: { value: number }) {
  return (
    <div className="performance-meter" aria-label={`Rendimiento ${value} sobre 100`}>
      <div className="performance-meter__header">
        <span>⚡ Rendimiento/€</span>
        <span className="performance-meter__info">
          ⓘ
          <div className="performance-meter__tooltip" role="tooltip">
            <strong>Métrica de Rendimiento/€</strong>
            <p>
              Evalúa la relación calidad-precio calculando el rendimiento de los componentes
              principales (CPU, GPU, RAM y almacenamiento) dividido entre el precio. Un valor
              más alto indica mejor rendimiento por cada euro invertido.
            </p>
          </div>
        </span>
      </div>
      <div className="performance-meter__value">
        <strong>{value}</strong>
        <span>/ 100</span>
      </div>
    </div>
  )
}

export function ProductCardView({ product, onViewDetails }: { product: ProductCard; onViewDetails: () => void }) {
  return (
    <article className={`product-card product-card--${product.tone}`}>
      <div className="product-card__media">
        <span className="pill pill--primary">{product.badge}</span>
        <span className="pill pill--accent">{product.ribbon}</span>
        <div className="product-card__shine" aria-hidden="true" />
        <img className="product-card__image" src={product.imageUrl || heroImage} alt="" aria-hidden="true" />
      </div>

      <div className="product-card__body">
        <h3>{product.title}</h3>
        <StarRating value={product.rating} />
        <p className="reviews">({product.reviews})</p>
        <PerformanceMeter value={product.performance} />
        <div className="performance-label-row">
          <span>{product.performanceLabel}</span>
          <div className="performance-track" aria-hidden="true">
            <span style={{ width: `${product.performance}%` }} />
          </div>
        </div>
        <div className="price-row">
          {product.oldPrice ? <span className="old-price">{product.oldPrice}</span> : null}
          <strong className="price">{product.price}</strong>
        </div>
        <button type="button" className="details-button" onClick={onViewDetails}>
          Ver detalles
        </button>
      </div>
    </article>
  )
}

export function PCComponentItem({ component }: { component: PCComponent }) {
  const getIconoForTipo = (tipo: string) => {
    const tipoNormalizado = tipo.toLowerCase().trim()
    const tiposIconos: Record<string, string> = {
      cpu: '⚙️',
      gpu: '🖥️',
      ram: '💾',
      almacenamiento: '💿',
      fuente: '⚡',
      'placa base': '📱',
      caja: '📦',
      'refrigeración': '❄️',
    }
    return tiposIconos[tipoNormalizado] || '🔧'
  }

  const getDescripcionForTipo = (tipo: string) => {
    const tipoNormalizado = tipo.toLowerCase().trim()
    const descripciones: Record<string, string> = {
      cpu: 'El cerebro del ordenador que ejecuta las instrucciones y procesa los datos. Cuanto más potente, mejor rendimiento en aplicaciones y multitarea.',
      gpu: 'Procesa y renderiza imágenes, vídeos y gráficos 3D. Esencial para gaming, diseño gráfico, edición de vídeo y renderizado 3D.',
      ram: 'Memoria de acceso rápido que almacena temporalmente los datos que el procesador necesita. Más RAM permite ejecutar más programas simultáneamente.',
      almacenamiento: 'Disco que guarda permanentemente el sistema operativo, programas y archivos. Los SSD NVMe son mucho más rápidos que los discos duros tradicionales.',
      fuente: 'Proporciona energía estable a todos los componentes del ordenador.',
      'placa base': 'Placa base que conecta todos los componentes del ordenador entre sí.',
      caja: 'Carcasa que aloja y protege todos los componentes del ordenador.',
      'refrigeración': 'Sistema que mantiene la temperatura de los componentes bajo control para evitar sobrecalentamientos y mantener el rendimiento.',
    }
    return descripciones[tipoNormalizado] || 'Componente del ordenador'
  }

  const getNombreTipo = (tipo: string) => {
    const tipoNormalizado = tipo.toLowerCase().trim()
    const nombresAmigables: Record<string, string> = {
      cpu: 'Procesador',
      gpu: 'Tarjeta Gráfica',
      ram: 'Memoria RAM',
      almacenamiento: 'Almacenamiento',
      fuente: 'Fuente de Alimentación',
      'placa base': 'Placa Base',
      caja: 'Carcasa',
      'refrigeración': 'Refrigeración CPU',
    }
    return nombresAmigables[tipoNormalizado] || tipo
  }

  return (
    <div className="component-item">
      <div className="component-icon">{getIconoForTipo(component.tipo)}</div>
      <div className="component-info">
        <h4>{getNombreTipo(component.tipo)}</h4>
        <p className="component-spec">{component.nombre}</p>
        <p className="component-desc">{getDescripcionForTipo(component.tipo)}</p>
      </div>
    </div>
  )
}

export function UserReviewItem({ review }: { review: UserReview }) {
  return (
    <div className="review-item">
      <div className="review-header">
        <h5>{review.usuarioNombre}</h5>
        <span className="review-date">{new Date(review.fecha).toLocaleDateString('es-ES')}</span>
      </div>
      <StarRating value={review.calificacion} />
      <p className="review-text">{review.comentario}</p>
    </div>
  )
}

export function RatingSummary({ product }: { product: CatalogPremontado }) {
  const rating = Math.max(1, Math.min(5, Math.round(product.valoracionMedia || 0)))

  return (
    <div className="rating-summary">
      <div className="rating-summary__header">
        <h2>Valoraciones de clientes</h2>
      </div>
      <div className="rating-summary__main">
        <div className="rating-summary__score">
          <div className="rating-summary__number">{product.valoracionMedia?.toFixed(1) || '—'}</div>
          <StarRating value={rating} />
          <p className="rating-summary__count">Basado en {product.numeroValoraciones} valoraciones</p>
        </div>
      </div>
    </div>
  )
}

export function AppLogo() {
  return (
    <span className="brand" aria-label="OptimaPC">
      <span className="brand-mark" aria-hidden="true">
        <img src="/optimaPC_icon.png" alt="OptimaPC" />
      </span>
      <span className="brand-name">OptimaPC</span>
    </span>
  )
}

const STRENGTH_CONFIG: Record<string, { label: string; color: string; bars: number }> = {
  VERY_WEAK: { label: 'Muy débil', color: '#ef4444', bars: 1 },
  WEAK: { label: 'Débil', color: '#f97316', bars: 2 },
  FAIR: { label: 'Regular', color: '#eab308', bars: 3 },
  STRONG: { label: 'Fuerte', color: '#22c55e', bars: 4 },
  VERY_STRONG: { label: 'Muy fuerte', color: '#16a34a', bars: 5 },
}

export function PasswordStrengthMeter({ strength }: { strength: string }) {
  const config = STRENGTH_CONFIG[strength] ?? STRENGTH_CONFIG.VERY_WEAK

  return (
    <div className="password-strength">
      <div className="strength-bars">
        {Array.from({ length: 5 }).map((_, index) => (
          <div
            key={index}
            className="strength-bar"
            style={{ backgroundColor: index < config.bars ? config.color : undefined }}
          />
        ))}
      </div>
      <span className="strength-label" style={{ color: config.color }}>
        {config.label}
      </span>
    </div>
  )
}
