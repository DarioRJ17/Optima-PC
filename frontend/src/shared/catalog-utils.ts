import type { CardBadge, CatalogPremontado, ProductCard } from '@/shared/types'

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

// Etiqueta legible de cada tipo de uso individual (para listarlos en el detalle).
const USO_LABELS: Record<string, string> = {
  GAMING: 'Gaming',
  OFIMATICA: 'Ofimática',
  PROGRAMACION: 'Programación',
  EDICION: 'Edición',
  STREAMING: 'Streaming',
}

// Prioridad para elegir el uso "principal" que se muestra en la tarjeta.
const USO_PRIORIDAD = ['GAMING', 'EDICION', 'STREAMING', 'PROGRAMACION', 'OFIMATICA']

export function usoLabel(uso: string) {
  return USO_LABELS[normalizarUso(uso)] ?? uso
}

// Usos del premontado ordenados por prioridad (el principal primero).
export function usosOrdenados(item: CatalogPremontado): string[] {
  return item.usosPrevistos
    .map(normalizarUso)
    .sort((a, b) => {
      const ia = USO_PRIORIDAD.indexOf(a)
      const ib = USO_PRIORIDAD.indexOf(b)
      return (ia === -1 ? Number.MAX_SAFE_INTEGER : ia) - (ib === -1 ? Number.MAX_SAFE_INTEGER : ib)
    })
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

// Etiquetas de la tarjeta: un uso principal + "Multiuso" si tiene más, y
// siempre descuento y reacondicionado cuando apliquen. En la sección de
// recomendados se antepone "Para ti".
export function buildCardBadges(item: CatalogPremontado, section: CatalogSectionKey): CardBadge[] {
  const badges: CardBadge[] = []

  if (section === 'recommended') {
    badges.push({ label: 'Para ti', variant: 'foryou' })
  }

  const usos = usosOrdenados(item)
  if (usos.length > 0) {
    badges.push({ label: usoLabel(usos[0]), variant: 'use' })
  }
  if (usos.length > 1) {
    badges.push({ label: 'Multiuso', variant: 'more' })
  }

  if ((item.descuento ?? 0) > 0) {
    badges.push({ label: `-${item.descuento}%`, variant: 'discount' })
  }

  if (item.esReacondicionado) {
    badges.push({ label: 'Reacondicionado', variant: 'refurb' })
  }

  return badges
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
    badges: buildCardBadges(item, section),
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