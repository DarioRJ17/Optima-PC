import { useState, useMemo } from 'react'
import type { CompatiblePCComponent } from '@/shared/types'
import './ComponentFilters.css'

type FilterBucket = { label: string; min: number; max: number }

type FilterSpec = {
  key: string
  label: string
  type: 'checkbox' | 'bucket' | 'boolean'
  buckets?: FilterBucket[]
  normalize?: (val: unknown) => string
  trueLabel?: string
  falseLabel?: string
}

const FILTER_SPECS: Record<string, FilterSpec[]> = {
  procesador: [
    { key: 'socket', label: 'Socket', type: 'checkbox' },
    {
      key: 'nucleos',
      label: 'Núcleos',
      type: 'bucket',
      buckets: [
        { label: '≤6 núcleos', min: 0, max: 6 },
        { label: '8 núcleos', min: 8, max: 8 },
        { label: '10–12 núcleos', min: 10, max: 12 },
        { label: '14–16 núcleos', min: 14, max: 16 },
        { label: '20+ núcleos', min: 20, max: Infinity },
      ],
    },
    {
      key: 'graficaIntegrada',
      label: 'Gráfica integrada',
      type: 'boolean',
      trueLabel: 'Con gráfica',
      falseLabel: 'Sin gráfica',
    },
    {
      key: 'tdp',
      label: 'TDP',
      type: 'bucket',
      buckets: [
        { label: '≤65 W', min: 0, max: 65 },
        { label: '66–125 W', min: 66, max: 125 },
        { label: '>125 W', min: 126, max: Infinity },
      ],
    },
  ],
  'tarjeta-grafica': [
    { key: 'fabricante', label: 'Fabricante', type: 'checkbox' },
    {
      key: 'memoria',
      label: 'VRAM',
      type: 'bucket',
      buckets: [
        { label: '≤4 GB', min: 0, max: 4 },
        { label: '5–8 GB', min: 5, max: 8 },
        { label: '10–12 GB', min: 10, max: 12 },
        { label: '16 GB', min: 16, max: 16 },
        { label: '20–24 GB', min: 20, max: 24 },
        { label: '32+ GB', min: 32, max: Infinity },
      ],
    },
    {
      key: 'longitud',
      label: 'Longitud',
      type: 'bucket',
      buckets: [
        { label: '≤240 mm', min: 0, max: 240 },
        { label: '241–300 mm', min: 241, max: 300 },
        { label: '>300 mm', min: 301, max: Infinity },
      ],
    },
  ],
  'memoria-ram': [
    { key: 'tipoDDR', label: 'Tipo', type: 'checkbox' },
    {
      key: 'totalGB',
      label: 'Capacidad total',
      type: 'bucket',
      buckets: [
        { label: '8 GB', min: 8, max: 8 },
        { label: '16 GB', min: 16, max: 16 },
        { label: '32 GB', min: 32, max: 32 },
        { label: '64 GB', min: 64, max: 64 },
        { label: '128+ GB', min: 128, max: Infinity },
      ],
    },
    {
      key: 'velocidad',
      label: 'Velocidad',
      type: 'bucket',
      buckets: [
        { label: '<3600 MHz', min: 0, max: 3599 },
        { label: '3600–4800 MHz', min: 3600, max: 4800 },
        { label: '4801–6000 MHz', min: 4801, max: 6000 },
        { label: '>6000 MHz', min: 6001, max: Infinity },
      ],
    },
  ],
  almacenamiento: [
    { key: 'tipo', label: 'Tipo', type: 'checkbox' },
    {
      key: 'factorForma',
      label: 'Factor de forma',
      type: 'checkbox',
      normalize: (val) => {
        const s = String(val ?? '')
        return s.startsWith('M.2') ? 'M.2' : s
      },
    },
    {
      key: 'capacidad',
      label: 'Capacidad',
      type: 'bucket',
      buckets: [
        { label: '≤256 GB', min: 0, max: 256 },
        { label: '257–500 GB', min: 257, max: 500 },
        { label: '501 GB–1 TB', min: 501, max: 1000 },
        { label: '1–2 TB', min: 1001, max: 2000 },
        { label: '2–4 TB', min: 2001, max: 4000 },
        { label: '4–8 TB', min: 4001, max: 8000 },
        { label: '>8 TB', min: 8001, max: Infinity },
      ],
    },
  ],
  'placa-base': [
    { key: 'socket', label: 'Socket', type: 'checkbox' },
    { key: 'factorForma', label: 'Factor de forma', type: 'checkbox' },
    { key: 'tipoDDR', label: 'Tipo DDR', type: 'checkbox' },
  ],
  'fuente-alimentacion': [
    {
      key: 'potencia',
      label: 'Potencia',
      type: 'bucket',
      buckets: [
        { label: '≤550 W', min: 0, max: 550 },
        { label: '551–750 W', min: 551, max: 750 },
        { label: '751–1000 W', min: 751, max: 1000 },
        { label: '>1000 W', min: 1001, max: Infinity },
      ],
    },
    { key: 'eficiencia', label: 'Eficiencia', type: 'checkbox' },
    {
      key: 'modular',
      label: 'Modularidad',
      type: 'checkbox',
      normalize: (val) => {
        const s = String(val ?? '').toLowerCase()
        if (s === 'false') return 'No modular'
        if (s === 'semi') return 'Semi-modular'
        if (s === 'full') return 'Totalmente modular'
        if (s === 'full / side') return 'Totalmente modular (lateral)'
        return ''
      },
    },
  ],
  'refrigerador-cpu': [
    {
      key: 'nivelRuidoMax',
      label: 'Nivel de ruido',
      type: 'bucket',
      buckets: [
        { label: 'Silencioso (≤25 dBA)', min: 0, max: 25 },
        { label: 'Moderado (25–35 dBA)', min: 25.01, max: 35 },
        { label: 'Ruidoso (>35 dBA)', min: 35.01, max: Infinity },
      ],
    },
    { key: 'tamano', label: 'Tamaño (mm)', type: 'checkbox' },
  ],
  caja: [
    { key: 'tipo', label: 'Factor de forma', type: 'checkbox' },
    { key: 'panelLateral', label: 'Panel lateral', type: 'checkbox' },
  ],
}

const SHOW_MORE_THRESHOLD = 4

export type ActiveFilters = Record<string, Set<string>>

export function applyFilters(
  components: CompatiblePCComponent[],
  activeFilters: ActiveFilters,
  priceMin: number | null,
  priceMax: number | null,
  tipo: string,
): CompatiblePCComponent[] {
  const specs = FILTER_SPECS[tipo] ?? []

  return components.filter((c) => {
    if (priceMin !== null && c.precio < priceMin) return false
    if (priceMax !== null && c.precio > priceMax) return false

    for (const spec of specs) {
      const selected = activeFilters[spec.key]
      if (!selected || selected.size === 0) continue

      const rawVal = c.propiedades?.[spec.key]

      if (spec.type === 'checkbox') {
        const normalized = spec.normalize ? spec.normalize(rawVal) : String(rawVal ?? '')
        if (!selected.has(normalized)) return false
      } else if (spec.type === 'bucket') {
        if (rawVal === null || rawVal === undefined) return false
        const numVal = Number(rawVal)
        const matches = spec.buckets!.some(
          (b) => selected.has(b.label) && numVal >= b.min && numVal <= b.max,
        )
        if (!matches) return false
      } else if (spec.type === 'boolean') {
        const isTrue = Boolean(rawVal)
        const boolLabel = isTrue ? spec.trueLabel! : spec.falseLabel!
        if (!selected.has(boolLabel)) return false
      }
    }
    return true
  })
}

// ── Slider de precio de doble manilla ──────────────────────────────────────

interface DualRangeSliderProps {
  dataMin: number
  dataMax: number
  valueMin: number
  valueMax: number
  onChange: (min: number, max: number) => void
}

function DualRangeSlider({ dataMin, dataMax, valueMin, valueMax, onChange }: DualRangeSliderProps) {
  const range = dataMax - dataMin || 1
  const leftPct = ((valueMin - dataMin) / range) * 100
  const rightPct = ((valueMax - dataMin) / range) * 100

  return (
    <div className="dual-range-wrapper">
      <div className="dual-range-track">
        <div
          className="dual-range-fill"
          style={{ left: `${leftPct}%`, width: `${rightPct - leftPct}%` }}
        />
      </div>
      {/* Manilla mínima */}
      <input
        type="range"
        className="dual-range-input"
        min={dataMin}
        max={dataMax}
        step={1}
        value={valueMin}
        onChange={(e) => {
          const val = Math.min(Number(e.target.value), valueMax - 1)
          onChange(val, valueMax)
        }}
      />
      {/* Manilla máxima */}
      <input
        type="range"
        className="dual-range-input"
        min={dataMin}
        max={dataMax}
        step={1}
        value={valueMax}
        onChange={(e) => {
          const val = Math.max(Number(e.target.value), valueMin + 1)
          onChange(valueMin, val)
        }}
      />
    </div>
  )
}

// ── Componente principal ────────────────────────────────────────────────────

interface Props {
  components: CompatiblePCComponent[]
  tipo: string
  activeFilters: ActiveFilters
  priceMin: number | null
  priceMax: number | null
  onFilterChange: (key: string, value: string, checked: boolean) => void
  onPriceChange: (min: number | null, max: number | null) => void
  onReset: () => void
}

export default function ComponentFilters({
  components,
  tipo,
  activeFilters,
  priceMin,
  priceMax,
  onFilterChange,
  onPriceChange,
  onReset,
}: Props) {
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const specs = FILTER_SPECS[tipo] ?? []

  const toggleExpand = (key: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev)
      next.has(key) ? next.delete(key) : next.add(key)
      return next
    })
  }

  // Rango de precios derivado de los componentes disponibles
  const { dataMinPrice, dataMaxPrice } = useMemo(() => {
    if (components.length === 0) return { dataMinPrice: 0, dataMaxPrice: 9999 }
    const prices = components.map((c) => c.precio)
    const min = Math.floor(Math.min(...prices))
    const max = Math.ceil(Math.max(...prices))
    return { dataMinPrice: min, dataMaxPrice: Math.max(min + 1, max) }
  }, [components])

  const sliderMin = priceMin ?? dataMinPrice
  const sliderMax = priceMax ?? dataMaxPrice

  const handleSliderChange = (min: number, max: number) => {
    onPriceChange(
      min === dataMinPrice ? null : min,
      max === dataMaxPrice ? null : max,
    )
  }

  // Opciones disponibles para filtros de checkbox, derivadas de los datos
  const checkboxOptions = useMemo(() => {
    const result: Record<string, string[]> = {}
    for (const spec of specs) {
      if (spec.type !== 'checkbox') continue
      const values = new Set<string>()
      for (const c of components) {
        const rawVal = c.propiedades?.[spec.key]
        if (rawVal === null || rawVal === undefined) continue
        const normalized = spec.normalize ? spec.normalize(rawVal) : String(rawVal)
        if (normalized) values.add(normalized)
      }
      result[spec.key] = Array.from(values).sort()
    }
    return result
  }, [components, specs])

  const hasActiveFilters =
    Object.values(activeFilters).some((s) => s.size > 0) ||
    priceMin !== null ||
    priceMax !== null

  return (
    <div className="component-filters">
      <div className="filters-header">
        <span className="filters-title">Filtros</span>
        {hasActiveFilters && (
          <button className="filters-reset" onClick={onReset}>
            Limpiar
          </button>
        )}
      </div>

      {/* Precio */}
      <div className="filter-group">
        <div className="filter-group-label">Precio</div>
        <div className="price-range-labels">
          <span>{sliderMin.toLocaleString('es-ES')} €</span>
          <span>{sliderMax.toLocaleString('es-ES')} €</span>
        </div>
        <DualRangeSlider
          dataMin={dataMinPrice}
          dataMax={dataMaxPrice}
          valueMin={sliderMin}
          valueMax={sliderMax}
          onChange={handleSliderChange}
        />
        <div className="price-range-bounds">
          <span>{dataMinPrice} €</span>
          <span>{dataMaxPrice} €</span>
        </div>
      </div>

      {/* Filtros por atributo */}
      {specs.map((spec) => {
        if (spec.type === 'checkbox') {
          const options = checkboxOptions[spec.key] ?? []
          if (options.length === 0) return null
          const isExpanded = expandedGroups.has(spec.key)
          const visibleOptions = isExpanded ? options : options.slice(0, SHOW_MORE_THRESHOLD)
          const hiddenCount = options.length - SHOW_MORE_THRESHOLD

          return (
            <div key={spec.key} className="filter-group">
              <div className="filter-group-label">{spec.label}</div>
              {visibleOptions.map((val) => (
                <label key={val} className="filter-option">
                  <input
                    type="checkbox"
                    checked={activeFilters[spec.key]?.has(val) ?? false}
                    onChange={(e) => onFilterChange(spec.key, val, e.target.checked)}
                  />
                  <span>{val}</span>
                </label>
              ))}
              {options.length > SHOW_MORE_THRESHOLD && (
                <button className="show-more-btn" onClick={() => toggleExpand(spec.key)}>
                  {isExpanded ? 'Mostrar menos' : `Mostrar ${hiddenCount} más`}
                </button>
              )}
            </div>
          )
        }

        if (spec.type === 'bucket') {
          return (
            <div key={spec.key} className="filter-group">
              <div className="filter-group-label">{spec.label}</div>
              {spec.buckets!.map((bucket) => (
                <label key={bucket.label} className="filter-option">
                  <input
                    type="checkbox"
                    checked={activeFilters[spec.key]?.has(bucket.label) ?? false}
                    onChange={(e) => onFilterChange(spec.key, bucket.label, e.target.checked)}
                  />
                  <span>{bucket.label}</span>
                </label>
              ))}
            </div>
          )
        }

        if (spec.type === 'boolean') {
          return (
            <div key={spec.key} className="filter-group">
              <div className="filter-group-label">{spec.label}</div>
              {[spec.trueLabel!, spec.falseLabel!].map((label) => (
                <label key={label} className="filter-option">
                  <input
                    type="checkbox"
                    checked={activeFilters[spec.key]?.has(label) ?? false}
                    onChange={(e) => onFilterChange(spec.key, label, e.target.checked)}
                  />
                  <span>{label}</span>
                </label>
              ))}
            </div>
          )
        }

        return null
      })}
    </div>
  )
}
