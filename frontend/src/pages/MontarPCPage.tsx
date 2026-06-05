import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import type { CompatiblePCComponent } from '../types'
import ComponentDetailModal from './montarPC/ComponentDetailModal.tsx'
import ComponentSidePanel from './montarPC/ComponentSidePanel'
import SummaryPane from './montarPC/SummaryPane'
import './montarPC/MontarPCPage.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

const COMPONENT_TYPES = [
  { id: 'procesador', label: 'Procesador (CPU)', icon: '⚙️' },
  { id: 'placa-base', label: 'Placa Base', icon: '⬜' },
  { id: 'memoria-ram', label: 'Memoria RAM', icon: '🧠' },
  { id: 'tarjeta-grafica', label: 'Tarjeta Gráfica', icon: '🎮' },
  { id: 'almacenamiento', label: 'Almacenamiento', icon: '💾' },
  { id: 'fuente-alimentacion', label: 'Fuente de Alimentación', icon: '⚡' },
  { id: 'caja', label: 'Caja/Torre', icon: '📦' },
  { id: 'refrigerador-cpu', label: 'Refrigeración', icon: '❄️' },
]

interface SelectedComponent {
  id: number
  tipo: string
  nombre: string
  precio: number
}

interface MontarPCPageProps {
  onBack: () => void
}

interface MontarPCLocationState {
  preseleccion?: SelectedComponent[]
}

export function MontarPCPage({ onBack }: MontarPCPageProps) {
  const location = useLocation()
  const preseleccion = (location.state as MontarPCLocationState | null)?.preseleccion ?? []
  const [selectedComponents, setSelectedComponents] = useState<SelectedComponent[]>(preseleccion)
  const [sidePanelOpen, setSidePanelOpen] = useState<string | null>(null)
  const [availableComponents, setAvailableComponents] = useState<CompatiblePCComponent[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedComponentDetail, setSelectedComponentDetail] = useState<CompatiblePCComponent | null>(null)

  const totalPrice = selectedComponents.reduce((sum, c) => sum + c.precio, 0)
  const requiredCount = COMPONENT_TYPES.filter((componentType) => componentType.id !== 'refrigerador-cpu').length

  const handleTypeClick = async (typeId: string) => {
    setSidePanelOpen(typeId)
    setLoading(true)
    setError('')

    try {
      const selectedIds = selectedComponents.map((c) => c.id).join(',')
      const url = new URL(`${API_BASE_URL}/api/configuracion-pc/components/compatible-with-warnings`)
      if (selectedIds) {
        url.searchParams.append('selectedIds', selectedIds)
      }

      const response = await fetch(url.toString())
      if (!response.ok) {
        throw new Error('No se pudieron cargar los componentes')
      }

      const data = (await response.json()) as CompatiblePCComponent[]
      // Filtrar solo los componentes del tipo seleccionado
      const filtered = data.filter((c) => c.tipo === typeId)
      setAvailableComponents(filtered)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido')
    } finally {
      setLoading(false)
    }
  }

  const handleCloseSidePanel = () => {
    setSidePanelOpen(null)
  }

  const handleSelectComponent = (component: CompatiblePCComponent) => {
    // Buscar si ya existe un componente de este tipo
    const existingIndex = selectedComponents.findIndex((c) => c.tipo === component.tipo)

    if (existingIndex >= 0) {
      // Reemplazar el existente
      const updated = [...selectedComponents]
      updated[existingIndex] = {
        id: component.id,
        tipo: component.tipo,
        nombre: component.nombre,
        precio: component.precio,
      }
      setSelectedComponents(updated)
    } else {
      // Añadir nuevo
      setSelectedComponents([
        ...selectedComponents,
        {
          id: component.id,
          tipo: component.tipo,
          nombre: component.nombre,
          precio: component.precio,
        },
      ])
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
    setTimeout(() => {
      setSelectedComponentDetail(null)
    }, 300)
  }

  return (
    <div className="montar-pc-page">
      <header className="montar-pc-header">
        <button onClick={onBack} className="back-button">
          ← Volver al inicio
        </button>
        <div className="header-title">
          <h1>Monta tu propio PC</h1>
          <p>Selecciona cada componente para crear tu ordenador personalizado</p>
        </div>
      </header>

      <div className="montar-pc-container">
        {/* Panel izquierdo: Componentes */}
        <section className="components-section">
          <h2>Componentes</h2>
          <div className="components-list">
            {COMPONENT_TYPES.map((type) => {
              const selected = getSelectedComponentByType(type.id)
              const isRequired = type.id !== 'refrigerador-cpu'

              return (
                <div key={type.id} className="component-item">
                  <button
                    className={`component-header ${isRequired ? 'required' : ''}`}
                    onClick={() => handleTypeClick(type.id)}
                  >
                    <span className="component-icon">{type.icon}</span>
                    <span className="component-label">
                      {type.label}
                      {isRequired && <span className="required-mark">*</span>}
                    </span>
                    <span className="component-status">
                      {selected ? (
                        <span className="status-selected">✓ {selected.nombre}</span>
                      ) : (
                        <span className="status-empty">No seleccionado</span>
                      )}
                    </span>
                    <span className="expand-icon">→</span>
                  </button>
                </div>
              )
            })}
          </div>

          <div className="note">
            <p>
              <strong>Nota:</strong> Los componentes marcados con <span className="required-mark">*</span> son
              obligatorios. Solo se muestran componentes compatibles con tu configuración actual.
            </p>
          </div>
        </section>

        {/* Panel derecho: Resumen */}
        <SummaryPane
          componentTypes={COMPONENT_TYPES}
          selectedComponents={selectedComponents}
          onRemove={handleRemoveComponent}
          totalPrice={totalPrice}
          requiredCount={requiredCount}
        />
      </div>

      <ComponentDetailModal
        component={selectedComponentDetail}
        isOpen={detailModalOpen}
        onClose={handleCloseDetailModal}
        onSelect={handleSelectComponent}
        isSelected={selectedComponentDetail?.id === selectedComponents.find((c) => c.tipo === selectedComponentDetail?.tipo)?.id}
      />

      <ComponentSidePanel
        isOpen={sidePanelOpen !== null}
        title={COMPONENT_TYPES.find((t) => t.id === sidePanelOpen)?.label || ''}
        components={availableComponents}
        selectedComponentId={getSelectedComponentByType(sidePanelOpen || '')?.id}
        loading={loading}
        error={error}
        onClose={handleCloseSidePanel}
        onSelect={handleSelectComponent}
        onViewDetails={handleViewComponentDetails}
      />

      {/* Styles are imported from montarPC/MontarPCPage.css */}
    </div>
  )
}
