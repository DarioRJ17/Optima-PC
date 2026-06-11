export type AuthMode = 'login' | 'register'

export type ViewMode = 'home' | 'auth' | 'product-detail'

export type ApiError = {
  message?: string
  fieldErrors?: Record<string, string>
}

export type AuthResponse = {
  mensaje: string
  id: number
  nombre: string
  apellidos: string
  email: string
  fechaRegistro: string
  token: string
}

export type CatalogPremontado = {
  id: number
  titulo: string
  descripcion: string | null
  marca: string
  descuento: number | null
  sistemaOperativo: string | null
  stock: number
  usosPrevistos: string[]
  imagenUrl: string | null
  esReacondicionado: boolean
  precio: number
  precioReducido: number | null
  valoracionMedia: number
  numeroValoraciones: number
  favorita: boolean | null
  rendimientoPorEuro: number
  componentes: PCComponent[]
  numeroCompras: number
}

export type PCComponent = {
  id: number
  tipo: string
  nombre: string
  especificacion: string
  precio: number
  cantidad: number
}

export type CompatibilityWarning = {
  code: 'ram_frequency_mismatch' | 'ram_latency_mismatch' | 'ram_capacity_mismatch'
  message: string
}

export type CompatiblePCComponent = PCComponent & {
  warnings: CompatibilityWarning[]
  propiedades?: Record<string, unknown>
}

export type ReciclajeComponente = {
  id: number
  nombre: string
  categoria: string
  precio: number
  cantidad: number
  esFijo: boolean
}

export type ComponenteDesbalanceado = {
  nombre: string
  sobredimensionado: boolean
  desviacion: number
}

export type EquilibrioData = {
  score: number
  componentes: ComponenteDesbalanceado[]
}

export type ConsumoData = {
  consumoEstimadoW: number
  consumoRecomendadoW: number
  potenciaPSUW: number | null
  disponibleW: number | null
  suficiente: boolean
}

export type ReciclajeConfiguracion = {
  componentes: ReciclajeComponente[]
  scoreRendimiento: number
  scoreEquilibrio: number
  scoreCompuesto: number
  precioTotal: number
  componentesDesbalanceados: ComponenteDesbalanceado[]
}

export type ReciclajeTipoUso = {
  tipoUso: string
  configuraciones: ReciclajeConfiguracion[]
}

export type UserReview = {
  id: number
  usuarioNombre: string
  calificacion: number
  comentario: string
  fecha: string
}

export type ComponenteDetalle = PCComponent & {
  consumoWatts?: number
  detalles: Record<string, unknown>
}

export type ProductCard = {
  id: number
  title: string
  badge: string
  ribbon: string
  rating: number
  reviews: number
  performance: number
  performanceLabel: string
  price: string
  oldPrice?: string
  tone: 'gaming' | 'laptop' | 'office' | 'refurb'
  imageUrl?: string
}

export type PriceRange = {
  minPrice: number
  maxPrice: number
}

export type SelectedFilters = {
  priceRange: PriceRange | null
  tipos: Set<string>
  marcas: Set<string>
  sistemasOperativos: Set<string>
}

export type LoginData = {
  email: string
  password: string
}

export type RegisterData = {
  nombre: string
  apellidos: string
  email: string
  password: string
  confirmPassword: string
}

export type FavoritoDto = {
  id: number
  fechaGuardado: string
  premontado: CatalogPremontado
}

export type CartItem = {
  configuracionId: number
  nombre: string
  precio: number
  imagenUrl?: string | null
  cantidad: number
}

export type ItemPedidoDto = {
  id: number
  configuracionId: number
  nombreProducto: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export type PedidoDto = {
  id: number
  fecha: string
  total: number
  items: ItemPedidoDto[]
}

export type MiConfiguracionComponenteDto = {
  id: number
  tipo: string
  nombre: string
  precio: number
}

export type MiConfiguracionDto = {
  id: number
  nombre: string
  precio: number
  fechaCreacion: string
  componentes: MiConfiguracionComponenteDto[]
}