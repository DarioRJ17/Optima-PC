import type { CatalogPremontado, ProductCard } from './types'

export type CatalogSectionKey = 'featured' | 'offers' | 'refurbished' | 'recommended'

export function formatEuro(value: number) {
  return new Intl.NumberFormat('es-ES', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function normalizarUso(uso: string) {
  return uso.toUpperCase()
}

export function buildBadge(item: CatalogPremontado) {
  if (item.esReacondicionado) {
    return 'Reacondicionado'
  }

  const usos = item.usosPrevistos.map(normalizarUso)
  if (usos.includes('GAMING')) return 'Gaming'
  if (usos.includes('OFIMATICA')) return 'Oficina'
  if (usos.includes('PROGRAMACION')) return 'Workstation'
  if (usos.includes('EDICION') || usos.includes('STREAMING')) return 'Creación de contenido'

  return 'Portátil'
}

export function buildTone(item: CatalogPremontado): ProductCard['tone'] {
  if (item.esReacondicionado) {
    return 'refurb'
  }

  const usos = item.usosPrevistos.map(normalizarUso)
  if (usos.includes('GAMING')) return 'gaming'
  if (usos.includes('OFIMATICA') || usos.includes('PROGRAMACION')) return 'office'
  return 'laptop'
}

export function buildRibbon(item: CatalogPremontado, section: CatalogSectionKey) {
  if (section === 'recommended') {
    return 'Para ti'
  }

  if (section === 'offers' && item.descuento) {
    return `-${item.descuento}%`
  }

  if (section === 'refurbished' && item.esReacondicionado) {
    return 'Reacondicionado'
  }

  return 'Trending'
}

export function buildPerformanceLabel(value: number) {
  if (value >= 85) return 'Excelente'
  if (value >= 65) return 'Muy bueno'
  if (value >= 42) return 'Bueno'
  return 'Regular'
}

export function buildPerformance(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)))
}

export function toProductCard(item: CatalogPremontado, section: CatalogSectionKey): ProductCard {
  const rating = Math.max(0, Math.min(5, item.valoracionMedia || 0))
  const performance = buildPerformance(item.rendimientoPorEuro || 0)
  const price = item.precioReducido ?? item.precio

  return {
    id: item.id,
    title: item.titulo,
    badge: buildBadge(item),
    ribbon: buildRibbon(item, section),
    rating,
    reviews: item.numeroValoraciones,
    performance,
    performanceLabel: buildPerformanceLabel(performance),
    price: `${formatEuro(price)}€`,
    oldPrice: item.precioReducido ? `${formatEuro(item.precio)}€` : undefined,
    tone: buildTone(item),
    imageUrl:
      item.imagenUrl && !item.imagenUrl.includes('images.example.com') ? item.imagenUrl : undefined,
  }
}