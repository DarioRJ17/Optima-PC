import { Cpu, Gpu, MemoryStick, HardDrive, PlugZap, CircuitBoard, Box, Fan, Wrench, Heart } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import heroImage from '../assets/hero.png'
import type { CatalogPremontado, PCComponent, ProductCard, UserReview } from '../types'

const TIPOS_ICONOS: Record<string, LucideIcon> = {
  cpu: Cpu,
  gpu: Gpu,
  ram: MemoryStick,
  almacenamiento: HardDrive,
  fuente: PlugZap,
  'placa base': CircuitBoard,
  caja: Box,
  'refrigeración': Fan,
}

const DESCRIPCIONES: Record<string, string> = {
  cpu: 'El cerebro del ordenador que ejecuta las instrucciones y procesa los datos. Cuanto más potente, mejor rendimiento en aplicaciones y multitarea.',
  gpu: 'Procesa y renderiza imágenes, vídeos y gráficos 3D. Esencial para gaming, diseño gráfico, edición de vídeo y renderizado 3D.',
  ram: 'Memoria de acceso rápido que almacena temporalmente los datos que el procesador necesita. Más RAM permite ejecutar más programas simultáneamente.',
  almacenamiento: 'Disco que guarda permanentemente el sistema operativo, programas y archivos. Los SSD NVMe son mucho más rápidos que los discos duros tradicionales.',
  fuente: 'Proporciona energía estable a todos los componentes del ordenador.',
  'placa base': 'Placa base que conecta todos los componentes del ordenador entre sí.',
  caja: 'Carcasa que aloja y protege todos los componentes del ordenador.',
  'refrigeración': 'Sistema que mantiene la temperatura de los componentes bajo control para evitar sobrecalentamientos y mantener el rendimiento.',
}

const NOMBRES_COMPONENTES: Record<string, string> = {
  cpu: 'Procesador',
  gpu: 'Tarjeta Gráfica',
  ram: 'Memoria RAM',
  almacenamiento: 'Almacenamiento',
  fuente: 'Fuente de Alimentación',
  'placa base': 'Placa Base',
  caja: 'Carcasa',
  'refrigeración': 'Refrigeración CPU',
}

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
              Compara el rendimiento estimado de CPU, GPU, RAM y almacenamiento con el precio
              del equipo. Un valor más alto indica mejor relación calidad-precio dentro del
              catálogo.
            </p>
            <p>
              <strong>Limitaciones:</strong> el rendimiento se estima a partir de
              especificaciones técnicas (núcleos, frecuencia, capacidad), sin datos de
              benchmarks reales ni diferencias entre arquitecturas. La puntuación es
              relativa al catálogo: equipos más baratos tienden a puntuar mejor en esta
              métrica aunque su potencia absoluta sea menor.
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

export function ProductCardView({
  product,
  onViewDetails,
  favorita,
  onToggleFavorite,
}: {
  product: ProductCard
  onViewDetails: () => void
  favorita?: boolean
  onToggleFavorite?: () => void
}) {
  return (
    <article className={`product-card product-card--${product.tone}`}>
      <div className="product-card__media">
        <span className="pill pill--primary">{product.badge}</span>
        <span className="pill pill--accent">{product.ribbon}</span>
        <div className="product-card__shine" aria-hidden="true" />
        <img className="product-card__image" src={product.imageUrl || heroImage} alt="" aria-hidden="true" />
        {onToggleFavorite !== undefined && (
          <button
            type="button"
            className={`favorite-btn${favorita ? ' favorite-btn--active' : ''}`}
            onClick={(e) => { e.stopPropagation(); onToggleFavorite() }}
            aria-label={favorita ? 'Quitar de favoritos' : 'Añadir a favoritos'}
          >
            <Heart size={18} fill={favorita ? '#ef4444' : 'none'} stroke={favorita ? '#ef4444' : 'currentColor'} strokeWidth={2} />
          </button>
        )}
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
  const tipoNormalizado = (component.tipo || '').toLowerCase().trim()
  const Icono = TIPOS_ICONOS[tipoNormalizado] || Wrench
  const descripcion = DESCRIPCIONES[tipoNormalizado] || 'Componente del ordenador'
  const nombreTipo = NOMBRES_COMPONENTES[tipoNormalizado] || component.tipo

  return (
    <div className="component-item">
      <div className="component-icon"><Icono size={36} strokeWidth={1.75} /></div>
      <div className="component-info">
        <p className="component-type">{nombreTipo}</p>
        <h4>{component.nombre}</h4>
        {component.especificacion && <p className="component-spec">{component.especificacion}</p>}
        <p className="component-desc">{descripcion}</p>
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
