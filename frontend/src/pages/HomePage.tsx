import type { Dispatch, SetStateAction } from 'react'
import { useNavigate } from 'react-router-dom'
import heroImage from '../assets/hero.png'
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

type HomePageProps = {
  catalogItems: CatalogPremontado[]
  catalogLoading: boolean
  catalogError: string
  selectedFilters: SelectedFilters
  setSelectedFilters: Dispatch<SetStateAction<SelectedFilters>>
  openAuth: (nextMode: AuthMode) => void
}

export function HomePage({
  catalogItems,
  catalogLoading,
  catalogError,
  selectedFilters,
  setSelectedFilters,
  openAuth,
}: HomePageProps) {
  const navigate = useNavigate()
  const bestSellers = [...catalogItems]
    .sort(
      (left, right) =>
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0) ||
        (right.numeroValoraciones || 0) - (left.numeroValoraciones || 0)
    )
    .slice(0, 3)

  const offers = [...catalogItems]
    .filter((item) => (item.descuento ?? 0) > 0)
    .sort(
      (left, right) =>
        (right.descuento ?? 0) - (left.descuento ?? 0) ||
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0)
    )
    .slice(0, 3)

  const refurbished = [...catalogItems]
    .filter((item) => item.esReacondicionado)
    .sort(
      (left, right) =>
        (right.valoracionMedia || 0) - (left.valoracionMedia || 0) ||
        (right.numeroValoraciones || 0) - (left.numeroValoraciones || 0)
    )
    .slice(0, 3)

  const fallbackProducts = catalogItems.slice(0, 3)
  const sections: Array<{ title: string; icon: string; key: CatalogSectionKey; products: CatalogPremontado[] }> = [
    {
      title: 'Ordenadores más vendidos',
      icon: '↗',
      key: 'featured',
      products: bestSellers.length > 0 ? bestSellers : fallbackProducts,
    },
    {
      title: 'Mejores ofertas',
      icon: '🏷',
      key: 'offers',
      products: offers.length > 0 ? offers : fallbackProducts,
    },
    {
      title: 'Ordenadores reacondicionados',
      icon: '⟲',
      key: 'refurbished',
      products: refurbished.length > 0 ? refurbished : fallbackProducts,
    },
  ]

  return (
    <main className="home-page">
      <aside className="filters-panel">
        <div className="filters-panel__header">
          <h2>Filtros</h2>
          {(selectedFilters.priceRange || selectedFilters.tipos.size > 0) && (
            <span className="filters-badge">
              {(selectedFilters.priceRange ? 1 : 0) + selectedFilters.tipos.size}
            </span>
          )}
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
                            const newTipos = new Set(selectedFilters.tipos)
                            if (event.target.checked) {
                              newTipos.add(option.value)
                            } else {
                              newTipos.delete(option.value)
                            }
                            setSelectedFilters({
                              ...selectedFilters,
                              tipos: newTipos,
                            })
                          }}
                        />
                        <span>{option.label}</span>
                      </label>
                    ))}
              </div>
            </details>
          )
        })}

        <button
          type="button"
          className="filters-panel__apply"
          onClick={() => {
            setSelectedFilters({
              priceRange: null,
              tipos: new Set(),
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
            <h1>Encuentra el ordenador que encaja con cada uso</h1>
            <p>
              Compara rendimiento, precio y estado de forma rápida. La home está pensada para
              que puedas explorar categorías y volver al acceso desde la barra superior.
            </p>
            <div className="hero-actions">
              <button type="button" className="hero-primary" onClick={() => openAuth('register')}>
                Crear cuenta
              </button>
              <button type="button" className="hero-secondary" onClick={() => openAuth('login')}>
                Ya tengo cuenta
              </button>
            </div>
            {catalogLoading ? <p className="catalog-message">Cargando catálogo real...</p> : null}
            {catalogError ? <p className="catalog-message catalog-message--error">{catalogError}</p> : null}
          </div>
          <div className="hero-visual" aria-hidden="true">
            <img src={heroImage} alt="" />
            <span className="hero-visual__tag hero-visual__tag--top">Equipos destacados</span>
            <span className="hero-visual__tag hero-visual__tag--bottom">Listos para comprar</span>
          </div>
        </section>

        <div className="catalog-sections">
          {sections.map((section) => (
            <section className="catalog-section" key={section.title}>
              <div className="section-title">
                <span className="section-title__icon" aria-hidden="true">
                  {section.icon}
                </span>
                <h2>{section.title}</h2>
              </div>

              <div className="product-grid">
                {section.products.map((item) => {
                  const productCard = toProductCard(item, section.key)
                  return (
                    <ProductCardView
                      key={item.id}
                      product={productCard}
                      onViewDetails={() => navigate(`/productos/${item.id}`)}
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