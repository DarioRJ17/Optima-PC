import type { Dispatch, SetStateAction } from 'react'
import { useNavigate } from 'react-router-dom'
import { Star, TrendingUp, Tag, RefreshCw } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { ProductCardView } from '../components/common'
import { toProductCard, type CatalogSectionKey } from '../catalog-utils'
import type { AuthMode, CatalogPremontado, SelectedFilters } from '../types'

const filterGroups = [
  {
    title: 'Precio',
    open: true,
    key: 'price',
    type: 'radio' as const,
    options: [
      { label: 'Menos de 500€', minPrice: 0, maxPrice: 500 },
      { label: '500€ - 1000€', minPrice: 500, maxPrice: 1000 },
      { label: '1000€ - 1500€', minPrice: 1000, maxPrice: 1500 },
      { label: 'Más de 1500€', minPrice: 1500, maxPrice: 999999 },
    ],
  },
  {
    title: 'Tipo de ordenador',
    open: true,
    key: 'tipos',
    type: 'checkbox' as const,
    options: [
      { label: 'Gaming', value: 'GAMING' },
      { label: 'Oficina', value: 'OFIMATICA' },
      { label: 'Programación', value: 'PROGRAMACION' },
      { label: 'Creación de contenido', value: 'EDICION' },
      { label: 'Streaming', value: 'STREAMING' },
      { label: 'Reacondicionado', value: '__reacondicionado__' },
    ],
  },
]

const REACONDICIONADO_FILTER = '__reacondicionado__'
const SIN_SISTEMA_OPERATIVO_FILTER = '__sin_so__'

function normalizarFiltroTexto(value: string) {
  return value.trim().toLowerCase()
}

function formatSistemaOperativoOption(value: string) {
  return value === SIN_SISTEMA_OPERATIVO_FILTER ? 'Sin sistema operativo' : value
}

function buildUniqueFilterOptions(values: Array<string | null | undefined>) {
  return Array.from(
    new Set(
      values
        .map((value) => value?.trim())
        .filter((value): value is string => Boolean(value))
    )
  )
    .sort((left, right) => left.localeCompare(right, 'es'))
    .map((value) => ({ label: value, value }))
}

function buildSistemaOperativoOptions(items: CatalogPremontado[]) {
  const values = new Set<string>()
  let hasNullSistemaOperativo = false

  items.forEach((item) => {
    if (item.sistemaOperativo && item.sistemaOperativo.trim()) {
      values.add(item.sistemaOperativo.trim())
    } else {
      hasNullSistemaOperativo = true
    }
  })

  const options = Array.from(values)
    .sort((left, right) => left.localeCompare(right, 'es'))
    .map((value) => ({ label: value, value }))

  if (hasNullSistemaOperativo) {
    options.push({ label: 'Sin sistema operativo', value: SIN_SISTEMA_OPERATIVO_FILTER })
  }

  return options
}

function matchesSelectedFilters(item: CatalogPremontado, filters: SelectedFilters) {
  const precioEfectivo = item.precioReducido ?? item.precio

  if (
    filters.priceRange &&
    (precioEfectivo < filters.priceRange.minPrice || precioEfectivo > filters.priceRange.maxPrice)
  ) {
    return false
  }

  const tiposSeleccionados = Array.from(filters.tipos)
  if (tiposSeleccionados.length > 0) {
    const reacondicionadoSelected = filters.tipos.has(REACONDICIONADO_FILTER)
    const tiposNormales = tiposSeleccionados.filter((tipo) => tipo !== REACONDICIONADO_FILTER)
    const usosItem = new Set(item.usosPrevistos.map(normalizarFiltroTexto))
    const coincideTipo =
      tiposNormales.length === 0 ||
      tiposNormales.some((tipo) => usosItem.has(normalizarFiltroTexto(tipo)))

    if (reacondicionadoSelected && !item.esReacondicionado) {
      return false
    }

    if (tiposNormales.length > 0 && !coincideTipo) {
      return false
    }
  }

  if (filters.marcas.size > 0 && !filters.marcas.has(item.marca)) {
    return false
  }

  if (filters.sistemasOperativos.size > 0) {
    const sistemaOperativo = item.sistemaOperativo?.trim() || SIN_SISTEMA_OPERATIVO_FILTER
    if (!filters.sistemasOperativos.has(sistemaOperativo)) {
      return false
    }
  }

  return true
}

type HomePageProps = {
  catalogItems: CatalogPremontado[]
  catalogLoading: boolean
  catalogError: string
  recommendationItems: CatalogPremontado[]
  recommendationsLoading: boolean
  recommendationsError: string
  showRecommendations: boolean
  isAuthenticated: boolean
  selectedFilters: SelectedFilters
  setSelectedFilters: Dispatch<SetStateAction<SelectedFilters>>
  openAuth: (nextMode: AuthMode) => void
  favoritosIds?: Set<number>
  toggleFavorito?: (id: number) => void
}

export function HomePage({
  catalogItems,
  catalogLoading,
  catalogError,
  recommendationItems,
  recommendationsLoading,
  recommendationsError,
  showRecommendations,
  isAuthenticated,
  selectedFilters,
  setSelectedFilters,
  openAuth,
  favoritosIds,
  toggleFavorito,
}: HomePageProps) {
  const navigate = useNavigate()
  const allCatalogAndRecommendationItems = [...catalogItems, ...recommendationItems]
  const brandOptions = buildUniqueFilterOptions(allCatalogAndRecommendationItems.map((item) => item.marca))
  const sistemaOperativoOptions = buildSistemaOperativoOptions(allCatalogAndRecommendationItems)
  const filteredCatalogItems = catalogItems.filter((item) => matchesSelectedFilters(item, selectedFilters))
  const filteredRecommendationItems = recommendationItems.filter((item) =>
    matchesSelectedFilters(item, selectedFilters)
  )

  const bestSellers = [...filteredCatalogItems]
    .sort(
      (left, right) =>
        (right.numeroCompras || 0) - (left.numeroCompras || 0) ||
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0)
    )
    .slice(0, 3)

  const offers = [...filteredCatalogItems]
    .filter((item) => (item.descuento ?? 0) > 0)
    .sort(
      (left, right) =>
        (right.descuento ?? 0) - (left.descuento ?? 0) ||
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0)
    )
    .slice(0, 3)

  const refurbished = [...filteredCatalogItems]
    .filter((item) => item.esReacondicionado)
    .sort(
      (left, right) =>
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0) ||
        (right.numeroValoraciones || 0) - (left.numeroValoraciones || 0)
    )
    .slice(0, 3)

  const fallbackProducts = filteredCatalogItems.slice(0, 3)
  const activeFilterCount =
    (selectedFilters.priceRange ? 1 : 0) +
    selectedFilters.tipos.size +
    selectedFilters.marcas.size +
    selectedFilters.sistemasOperativos.size

  const toggleSelectedValue = (
    filterKey: 'tipos' | 'marcas' | 'sistemasOperativos',
    value: string,
    checked: boolean
  ) => {
    setSelectedFilters((current) => {
      const nextValues = new Set(current[filterKey])

      if (checked) {
        nextValues.add(value)
      } else {
        nextValues.delete(value)
      }

      return {
        ...current,
        [filterKey]: nextValues,
      }
    })
  }

  const sections: Array<{ title: string; Icon: LucideIcon; key: CatalogSectionKey; products: CatalogPremontado[] }> = [
    {
      title: 'Ordenadores más vendidos',
      Icon: TrendingUp,
      key: 'featured',
      products: bestSellers.length > 0 ? bestSellers : fallbackProducts,
    },
    {
      title: 'Mejores ofertas',
      Icon: Tag,
      key: 'offers',
      products: offers.length > 0 ? offers : fallbackProducts,
    },
    {
      title: 'Ordenadores reacondicionados',
      Icon: RefreshCw,
      key: 'refurbished',
      products: refurbished.length > 0 ? refurbished : fallbackProducts,
    },
  ]

  if (showRecommendations) {
    sections.unshift({
      title: 'Recomendados para ti',
      Icon: Star,
      key: 'recommended',
      products: filteredRecommendationItems.slice(0, 3),
    })
  }

  return (
    <main className="home-page">
      <aside className="filters-panel">
        <div className="filters-panel__header">
          <h2>Filtros</h2>
          {activeFilterCount > 0 && <span className="filters-badge">{activeFilterCount}</span>}
        </div>

        {filterGroups.map((group) => {
          const isPriceGroup = group.key === 'price'
          const selectedPrice = isPriceGroup ? selectedFilters.priceRange : undefined

          return (
            <details className="filter-group" key={group.title} open={group.open}>
              <summary>{group.title}</summary>
              <div className="filter-group__items">
                {isPriceGroup
                  ? (group.options as Array<{ label: string; minPrice: number; maxPrice: number }>).map((option) => (
                      <label key={option.label} className="filter-option">
                        <input
                          type="radio"
                          name={group.key}
                          checked={
                            selectedPrice?.minPrice === option.minPrice &&
                            selectedPrice?.maxPrice === option.maxPrice
                          }
                          onChange={() => {
                            const newSelectedFilters = { ...selectedFilters }
                            if (
                              newSelectedFilters.priceRange?.minPrice === option.minPrice &&
                              newSelectedFilters.priceRange?.maxPrice === option.maxPrice
                            ) {
                              newSelectedFilters.priceRange = null
                            } else {
                              newSelectedFilters.priceRange = {
                                minPrice: option.minPrice,
                                maxPrice: option.maxPrice,
                              }
                            }
                            setSelectedFilters(newSelectedFilters)
                          }}
                        />
                        <span>{option.label}</span>
                      </label>
                    ))
                  : (group.options as Array<{ label: string; value: string }>).map((option) => (
                      <label key={option.value} className="filter-option">
                        <input
                          type="checkbox"
                          checked={selectedFilters.tipos.has(option.value)}
                          onChange={(event) => {
                            toggleSelectedValue('tipos', option.value, event.target.checked)
                          }}
                        />
                        <span>{option.label}</span>
                      </label>
                    ))}
              </div>
            </details>
          )
        })}

        <details className="filter-group" open>
          <summary>Marca</summary>
          <div className="filter-group__items">
            {brandOptions.length > 0 ? (
              brandOptions.map((option) => (
                <label key={option.value} className="filter-option">
                  <input
                    type="checkbox"
                    checked={selectedFilters.marcas.has(option.value)}
                    onChange={(event) => toggleSelectedValue('marcas', option.value, event.target.checked)}
                  />
                  <span>{option.label}</span>
                </label>
              ))
            ) : (
              <p className="catalog-message">No hay marcas disponibles.</p>
            )}
          </div>
        </details>

        <details className="filter-group" open={sistemaOperativoOptions.length > 0}>
          <summary>Sistema operativo</summary>
          <div className="filter-group__items">
            {sistemaOperativoOptions.length > 0 ? (
              sistemaOperativoOptions.map((option) => (
                <label key={option.value} className="filter-option">
                  <input
                    type="checkbox"
                    checked={selectedFilters.sistemasOperativos.has(option.value)}
                    onChange={(event) =>
                      toggleSelectedValue('sistemasOperativos', option.value, event.target.checked)
                    }
                  />
                  <span>{formatSistemaOperativoOption(option.value)}</span>
                </label>
              ))
            ) : (
              <p className="catalog-message">No hay sistemas operativos disponibles.</p>
            )}
          </div>
        </details>

        <button
          type="button"
          className="filters-panel__apply"
          onClick={() => {
            setSelectedFilters({
              priceRange: null,
              tipos: new Set(),
              marcas: new Set(),
              sistemasOperativos: new Set(),
            })
          }}
        >
          Limpiar filtros
        </button>
      </aside>

      <section className="catalog-shell">
        <section className="hero-panel">
          <div className="hero-copy">
            <p className="eyebrow">Tu tienda para gaming, oficina y reacondicionados</p>
            <h1>Encuentra el ordenador acorde a tus necesidades</h1>
            <p>
              Elige entre equipos premontados listos para usar, móntalo tú mismo pieza a pieza
              o ahorra con un reacondicionado con garantía. Compara rendimiento y precio y
              llévate el ordenador perfecto para ti.
            </p>
            {!isAuthenticated ? (
              <div className="hero-actions">
                <button type="button" className="hero-primary" onClick={() => openAuth('register')}>
                  Crear cuenta
                </button>
                <button type="button" className="hero-secondary" onClick={() => openAuth('login')}>
                  Ya tengo cuenta
                </button>
              </div>
            ) : null}
            {catalogLoading ? <p className="catalog-message">Cargando catálogo real...</p> : null}
            {catalogError ? <p className="catalog-message catalog-message--error">{catalogError}</p> : null}
          </div>
          <div className="hero-visual">
            <p className="hero-visual__title">Comprar con OptimaPC es fácil</p>
            <ul className="hero-visual__list">
              <li>Configura tu PC pieza a pieza con compatibilidad comprobada al instante.</li>
              <li>Premontados listos para gaming, trabajo y creación de contenido.</li>
              <li>Reacondicionados con garantía y al mejor precio.</li>
              <li>Recomendaciones personalizadas según tu uso y tu presupuesto.</li>
            </ul>
          </div>
        </section>

        <div className="catalog-sections">
          {sections.map((section) => (
            <section className="catalog-section" key={section.title}>
              <div className="section-title">
                <span className="section-title__icon" aria-hidden="true">
                  <section.Icon size={22} strokeWidth={1.75} />
                </span>
                <h2>{section.title}</h2>
              </div>

              {section.key === 'recommended' && recommendationsLoading ? (
                <p className="catalog-message">Cargando recomendaciones personalizadas...</p>
              ) : null}

              {section.key === 'recommended' && recommendationsError ? (
                <p className="catalog-message catalog-message--error">{recommendationsError}</p>
              ) : null}

              {section.key === 'recommended' &&
              !recommendationsLoading &&
              !recommendationsError &&
              section.products.length === 0 ? (
                <p className="catalog-message">
                  Marca productos como favoritos, añade valoraciones o realiza compras para que podamos recomendarte equipos personalizados.
                </p>
              ) : null}

              <div className="product-grid">
                {section.products.map((item) => {
                  const productCard = toProductCard(item, section.key)
                  return (
                    <ProductCardView
                      key={item.id}
                      product={productCard}
                      onViewDetails={() => navigate(`/productos/${item.id}`)}
                      favorita={favoritosIds ? favoritosIds.has(item.id) : undefined}
                      onToggleFavorite={toggleFavorito ? () => toggleFavorito(item.id) : undefined}
                    />
                  )
                })}
              </div>
            </section>
          ))}
        </div>
      </section>
    </main>
  )
}