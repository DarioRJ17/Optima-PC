import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type {
  CompatiblePCComponent,
  ReciclajeTipoUso,
  ReciclajeConfiguracion,
  ReciclajeComponente,
} from '../types'
import ComponentDetailModal from './montarPC/ComponentDetailModal.tsx'
import ComponentSidePanel from './montarPC/ComponentSidePanel'
import './ReciclajePage.css'
import { Cpu, Gpu, MemoryStick, HardDrive, Recycle } from 'lucide-react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

// Tipos de componente que admite el reciclaje (los que entran en la métrica de rendimiento).
const COMPONENT_TYPES = [
  { id: 'procesador', label: 'Procesador (CPU)', Icon: Cpu },
  { id: 'tarjeta-grafica', label: 'Tarjeta Gráfica', Icon: Gpu },
  { id: 'memoria-ram', label: 'Memoria RAM', Icon: MemoryStick },
  { id: 'almacenamiento', label: 'Almacenamiento', Icon: HardDrive },
]

// Mapeo categoría del backend de reciclaje (CPU/GPU/RAM/STORAGE) → tipo del configurador.
const CATEGORIA_A_TIPO: Record<string, string> = {
  CPU: 'procesador',
  GPU: 'tarjeta-grafica',
  RAM: 'memoria-ram',
  STORAGE: 'almacenamiento',
}

// Nombres legibles para cada tipo de uso.
const TIPO_USO_LABEL: Record<string, string> = {
  GAMING: 'Gaming',
  OFIMATICA: 'Ofimática',
  EDICION: 'Edición',
  PROGRAMACION: 'Programación',
  STREAMING: 'Streaming',
}

interface SelectedComponent {
  id: number
  tipo: string
  nombre: string
  precio: number
}

interface ReciclajePageProps {
  onBack: () => void
}

export function ReciclajePage({ onBack }: ReciclajePageProps) {
  const navigate = useNavigate()

  const [selectedComponents, setSelectedComponents] = useState<SelectedComponent[]>([])
  const [sidePanelOpen, setSidePanelOpen] = useState<string | null>(null)
  const [availableComponents, setAvailableComponents] = useState<CompatiblePCComponent[]>([])
  const [loadingComponents, setLoadingComponents] = useState(false)
  const [componentsError, setComponentsError] = useState('')

  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedComponentDetail, setSelectedComponentDetail] = useState<CompatiblePCComponent | null>(null)

  const [resultados, setResultados] = useState<ReciclajeTipoUso[]>([])
  const [calculando, setCalculando] = useState(false)
  const [resultadosError, setResultadosError] = useState('')
  const [tipoUsoActivo, setTipoUsoActivo] = useState<string | null>(null)

  const handleTypeClick = async (typeId: string) => {
    setSidePanelOpen(typeId)
    setLoadingComponents(true)
    setComponentsError('')

    try {
      const url = new URL(`${API_BASE_URL}/api/configuracion-pc/components/compatible-with-warnings`)
      const response = await fetch(url.toString())
      if (!response.ok) {
        throw new Error('No se pudieron cargar los componentes')
      }

      const data = (await response.json()) as CompatiblePCComponent[]
      setAvailableComponents(data.filter((c) => c.tipo === typeId))
    } catch (err) {
      setComponentsError(err instanceof Error ? err.message : 'Error desconocido')
    } finally {
      setLoadingComponents(false)
    }
  }

  const handleSelectComponent = (component: CompatiblePCComponent) => {
    const existingIndex = selectedComponents.findIndex((c) => c.tipo === component.tipo)
    const nuevo: SelectedComponent = {
      id: component.id,
      tipo: component.tipo,
      nombre: component.nombre,
      precio: component.precio,
    }

    if (existingIndex >= 0) {
      const updated = [...selectedComponents]
      updated[existingIndex] = nuevo
      setSelectedComponents(updated)
    } else {
      setSelectedComponents([...selectedComponents, nuevo])
    }
  }

  const handleRemoveComponent = (tipo: string) => {
    setSelectedComponents(selectedComponents.filter((c) => c.tipo !== tipo))
  }

  const getSelectedComponentByType = (tipo: string) => {
    return selectedComponents.find((c) => c.tipo === tipo)
  }

  const handleViewComponentDetails = (component: CompatiblePCComponent) => {
    setSelectedComponentDetail(component)
    setDetailModalOpen(true)
  }

  const handleCloseDetailModal = () => {
    setDetailModalOpen(false)
    setTimeout(() => setSelectedComponentDetail(null), 300)
  }

  const handleCalcular = async () => {
    if (selectedComponents.length === 0) return

    setCalculando(true)
    setResultadosError('')
    setResultados([])

    try {
      const response = await fetch(`${API_BASE_URL}/api/reciclaje/sugerir`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ componenteIds: selectedComponents.map((c) => c.id) }),
      })

      if (!response.ok) {
        throw new Error('No se pudieron calcular las configuraciones')
      }

      const data = (await response.json()) as ReciclajeTipoUso[]
      setResultados(data)
      setTipoUsoActivo(data.length > 0 ? data[0].tipoUso : null)
    } catch (err) {
      setResultadosError(err instanceof Error ? err.message : 'Error desconocido')
    } finally {
      setCalculando(false)
    }
  }

  // Navega al configurador con los 4 componentes de la configuración ya preseleccionados.
  const handleCompletarEnConfigurador = (config: ReciclajeConfiguracion) => {
    const preseleccion: SelectedComponent[] = config.componentes.map((c: ReciclajeComponente) => ({
      id: c.id,
      tipo: CATEGORIA_A_TIPO[c.categoria] ?? c.categoria.toLowerCase(),
      nombre: c.nombre,
      precio: c.precio,
    }))
    navigate('/montar-pc', { state: { preseleccion } })
  }

  const resultadoActivo = resultados.find((r) => r.tipoUso === tipoUsoActivo)

  return (
    <div className="reciclaje-page">
      <header className="reciclaje-header">
        <button onClick={onBack} className="back-button">
          ← Volver al inicio
        </button>
        <div className="header-title">
          <h1><Recycle size={26} strokeWidth={1.75} aria-hidden="true" />Reciclaje de componentes</h1>
          <p>
            Indica los componentes que ya tienes y el sistema calculará las mejores configuraciones
            posibles a su alrededor, según el tipo de uso.
          </p>
        </div>
      </header>

      <div className="reciclaje-container">
        {/* Selección de componentes del usuario */}
        <section className="reciclaje-seleccion">
          <h2>Tus componentes</h2>
          <p className="reciclaje-seleccion__hint">Selecciona al menos uno.</p>

          <div className="reciclaje-tipos">
            {COMPONENT_TYPES.map((type) => {
              const selected = getSelectedComponentByType(type.id)
              return (
                <div key={type.id} className="reciclaje-tipo">
                  <button className="reciclaje-tipo__btn" onClick={() => handleTypeClick(type.id)}>
                    <span className="reciclaje-tipo__icon"><type.Icon size={22} strokeWidth={1.75} /></span>
                    <span className="reciclaje-tipo__label">{type.label}</span>
                    <span className="reciclaje-tipo__status">
                      {selected ? (
                        <span className="status-selected">✓ {selected.nombre}</span>
                      ) : (
                        <span className="status-empty">Añadir</span>
                      )}
                    </span>
                  </button>
                  {selected && (
                    <button
                      className="reciclaje-tipo__remove"
                      onClick={() => handleRemoveComponent(type.id)}
                      aria-label={`Quitar ${type.label}`}
                    >
                      ✕
                    </button>
                  )}
                </div>
              )
            })}
          </div>

          <button
            className="reciclaje-calcular"
            onClick={handleCalcular}
            disabled={selectedComponents.length === 0 || calculando}
          >
            {calculando ? 'Calculando…' : 'Calcular configuraciones óptimas'}
          </button>

          {resultadosError && <div className="reciclaje-error">{resultadosError}</div>}
        </section>

        {/* Resultados */}
        <section className="reciclaje-resultados">
          {resultados.length === 0 && !calculando && (
            <div className="reciclaje-vacio">
              <p>Aún no hay resultados. Selecciona tus componentes y pulsa «Calcular».</p>
            </div>
          )}

          {resultados.length > 0 && (
            <>
              <div className="reciclaje-tabs" role="tablist">
                {resultados.map((r) => (
                  <button
                    key={r.tipoUso}
                    role="tab"
                    aria-selected={r.tipoUso === tipoUsoActivo}
                    className={`reciclaje-tab ${r.tipoUso === tipoUsoActivo ? 'active' : ''}`}
                    onClick={() => setTipoUsoActivo(r.tipoUso)}
                  >
                    {TIPO_USO_LABEL[r.tipoUso] ?? r.tipoUso}
                  </button>
                ))}
              </div>

              <div className="reciclaje-configs">
                {resultadoActivo?.configuraciones.map((config, idx) => (
                  <ConfiguracionCard
                    key={idx}
                    config={config}
                    ranking={idx + 1}
                    onCompletar={() => handleCompletarEnConfigurador(config)}
                  />
                ))}
              </div>
            </>
          )}
        </section>
      </div>

      <ComponentDetailModal
        component={selectedComponentDetail}
        isOpen={detailModalOpen}
        onClose={handleCloseDetailModal}
        onSelect={handleSelectComponent}
        isSelected={
          selectedComponentDetail?.id ===
          selectedComponents.find((c) => c.tipo === selectedComponentDetail?.tipo)?.id
        }
      />

      <ComponentSidePanel
        isOpen={sidePanelOpen !== null}
        title={COMPONENT_TYPES.find((t) => t.id === sidePanelOpen)?.label || ''}
        components={availableComponents}
        selectedComponentId={getSelectedComponentByType(sidePanelOpen || '')?.id}
        loading={loadingComponents}
        error={componentsError}
        onClose={() => setSidePanelOpen(null)}
        onSelect={handleSelectComponent}
        onViewDetails={handleViewComponentDetails}
      />
    </div>
  )
}

// --- Tarjeta de una configuración sugerida ---

interface ConfiguracionCardProps {
  config: ReciclajeConfiguracion
  ranking: number
  onCompletar: () => void
}

function ConfiguracionCard({ config, ranking, onCompletar }: ConfiguracionCardProps) {
  return (
    <article className="config-card">
      <div className="config-card__header">
        <span className="config-card__rank">#{ranking}</span>
        <span className="config-card__price">{config.precioTotal.toLocaleString('es-ES')} €</span>
      </div>

      <ul className="config-card__componentes">
        {config.componentes.map((c) => (
          <li key={c.id} className={`config-comp ${c.esFijo ? 'config-comp--fijo' : ''}`}>
            <span className="config-comp__nombre">{c.nombre}</span>
            {c.esFijo ? (
              <span className="config-comp__badge">Tuyo</span>
            ) : (
              <span className="config-comp__badge config-comp__badge--sugerido">Sugerido</span>
            )}
          </li>
        ))}
      </ul>

      <div className="config-card__scores">
        <ScoreBar label="Rendimiento/€" value={config.scoreRendimiento} />
        <ScoreBar label="Equilibrio" value={config.scoreEquilibrio} />
        <ScoreBar label="Global" value={config.scoreCompuesto} highlight />
      </div>

      {config.componentesDesbalanceados.length > 0 && (
        <div className="config-card__avisos">
          {config.componentesDesbalanceados.map((d) => (
            <span key={d.nombre} className="config-aviso">
              {d.sobredimensionado ? '▲' : '▼'} {d.nombre}{' '}
              {d.sobredimensionado ? 'sobredimensionada' : 'es cuello de botella'}
            </span>
          ))}
        </div>
      )}

      <button className="config-card__completar" onClick={onCompletar}>
        Completar en el configurador →
      </button>
    </article>
  )
}

function ScoreBar({ label, value, highlight }: { label: string; value: number; highlight?: boolean }) {
  return (
    <div className={`score-bar ${highlight ? 'score-bar--highlight' : ''}`}>
      <div className="score-bar__top">
        <span className="score-bar__label">{label}</span>
        <span className="score-bar__value">{Math.round(value)}</span>
      </div>
      <div className="score-bar__track">
        <div className="score-bar__fill" style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
      </div>
    </div>
  )
}
