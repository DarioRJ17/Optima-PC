import { useState, useEffect, useMemo } from 'react'
import type { CompatiblePCComponent } from '../../types'
import ComponentOptionCard from './ComponentOptionCard'
import ComponentFilters, { applyFilters } from './ComponentFilters'
import type { ActiveFilters } from './ComponentFilters'
import './ComponentSidePanel.css'

interface Props {
  isOpen: boolean
  tipo: string
  title: string
  components: CompatiblePCComponent[]
  selectedComponentIds: number[]
  loading: boolean
  error: string
  onClose: () => void
  onSelect: (component: CompatiblePCComponent) => void
  onViewDetails: (component: CompatiblePCComponent) => void
}

export default function ComponentSidePanel({
  isOpen,
  tipo,
  title,
  components,
  selectedComponentIds,
  loading,
  error,
  onClose,
  onSelect,
  onViewDetails,
}: Props) {
  const [activeFilters, setActiveFilters] = useState<ActiveFilters>({})
  const [priceMin, setPriceMin] = useState<number | null>(null)
  const [priceMax, setPriceMax] = useState<number | null>(null)
  const [nameFilter, setNameFilter] = useState('')
  const [filtersVisible, setFiltersVisible] = useState(true)

  // Resetear todos los filtros al cambiar de tipo de componente
  useEffect(() => {
    setActiveFilters({})
    setPriceMin(null)
    setPriceMax(null)
    setNameFilter('')
  }, [tipo])

  const handleFilterChange = (key: string, value: string, checked: boolean) => {
    setActiveFilters((prev) => {
      const current = prev[key] ? new Set(prev[key]) : new Set<string>()
      checked ? current.add(value) : current.delete(value)
      return { ...prev, [key]: current }
    })
  }

  const handlePriceChange = (min: number | null, max: number | null) => {
    setPriceMin(min)
    setPriceMax(max)
  }

  const handleReset = () => {
    setActiveFilters({})
    setPriceMin(null)
    setPriceMax(null)
  }

  const filteredComponents = useMemo(() => {
    const byAttributes = applyFilters(components, activeFilters, priceMin, priceMax, tipo)
    if (!nameFilter.trim()) return byAttributes
    const term = nameFilter.trim().toLowerCase()
    return byAttributes.filter((c) => c.nombre.toLowerCase().includes(term))
  }, [components, activeFilters, priceMin, priceMax, tipo, nameFilter])

  const hasActiveFilters =
    Object.values(activeFilters).some((s) => s.size > 0) ||
    priceMin !== null ||
    priceMax !== null

  return (
    <>
      {isOpen && <div className="side-panel-overlay" onClick={onClose} />}

      <aside className={`side-panel ${isOpen ? 'open' : ''}`}>
        <div className="side-panel-header">
          <h2>{title}</h2>
          <button
            className={`filters-toggle-btn ${filtersVisible ? 'active' : ''}`}
            onClick={() => setFiltersVisible((v) => !v)}
            title={filtersVisible ? 'Ocultar filtros' : 'Mostrar filtros'}
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="4" y1="6" x2="20" y2="6" />
              <line x1="8" y1="12" x2="16" y2="12" />
              <line x1="11" y1="18" x2="13" y2="18" />
            </svg>
            Filtros
            {hasActiveFilters && <span className="filters-active-dot" />}
          </button>
          <button className="side-panel-close" onClick={onClose}>
            ✕
          </button>
        </div>

        {/* Búsqueda por nombre — ocupa todo el ancho del panel */}
        <div className="side-panel-search">
          <svg
            className="search-icon"
            width="15"
            height="15"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder={`Buscar ${title.toLowerCase()}...`}
            value={nameFilter}
            onChange={(e) => setNameFilter(e.target.value)}
          />
          {nameFilter && (
            <button className="search-clear" onClick={() => setNameFilter('')}>
              ✕
            </button>
          )}
        </div>

        <div className="side-panel-wrapper">
          {/* Columna izquierda: filtros */}
          {filtersVisible && (
            <div className="side-panel-filters">
              <ComponentFilters
                components={components}
                tipo={tipo}
                activeFilters={activeFilters}
                priceMin={priceMin}
                priceMax={priceMax}
                onFilterChange={handleFilterChange}
                onPriceChange={handlePriceChange}
                onReset={handleReset}
              />
            </div>
          )}

          {/* Columna derecha: lista de componentes */}
          <div className="side-panel-content">
            {loading && <div className="side-panel-loading">Cargando componentes...</div>}

            {error && <div className="side-panel-error">{error}</div>}

            {!loading && !error && filteredComponents.length === 0 && (
              <div className="side-panel-empty">
                {components.length > 0
                  ? 'Ningún componente coincide con los filtros aplicados'
                  : 'No hay componentes disponibles'}
              </div>
            )}

            {!loading && !error && filteredComponents.length > 0 && (
              <div className="side-panel-components">
                {filteredComponents.map((component) => (
                  <ComponentOptionCard
                    key={component.id}
                    component={component}
                    isSelected={selectedComponentIds.includes(component.id)}
                    onSelect={onSelect}
                    onViewDetails={onViewDetails}
                  />
                ))}
              </div>
            )}
          </div>
        </div>
      </aside>
    </>
  )
}
