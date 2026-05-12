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