import { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import type { CartItem, CompatiblePCComponent, ConsumoData, EquilibrioData } from '../types'
import ComponentDetailModal from './montarPC/ComponentDetailModal.tsx'
import ComponentSidePanel from './montarPC/ComponentSidePanel'
import SummaryPane from './montarPC/SummaryPane'
import './montarPC/MontarPCPage.css'
import { Cpu, CircuitBoard, MemoryStick, Gpu, HardDrive, PlugZap, Box, Fan, Wrench, Info } from 'lucide-react'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

const COMPONENT_INFO: Record<string, string> = {
  'procesador': 'El cerebro del ordenador que ejecuta las instrucciones y procesa los datos. Cuanto más potente, mejor rendimiento en aplicaciones y multitarea.',
  'placa-base': 'Conecta e interconecta todos los componentes entre sí. Determina la compatibilidad con el procesador, la RAM y las ranuras de expansión.',
  'memoria-ram': 'Memoria de acceso rápido que almacena temporalmente los datos que el procesador necesita. Más RAM permite ejecutar más programas simultáneamente.',
  'tarjeta-grafica': 'Procesa y renderiza imágenes, vídeos y gráficos 3D. Esencial para gaming, diseño gráfico, edición de vídeo y renderizado 3D.',
  'almacenamiento': 'Guarda permanentemente el sistema operativo, programas y archivos. Los SSD NVMe son mucho más rápidos que los discos duros tradicionales.',
  'fuente-alimentacion': 'Proporciona energía estable a todos los componentes del ordenador. Es importante elegir una con suficiente potencia y buena eficiencia.',
  'caja': 'Carcasa que aloja y protege todos los componentes del ordenador. Determina el tamaño del sistema y el flujo de aire.',
  'refrigerador-cpu': 'Sistema que mantiene la temperatura del procesador bajo control para evitar sobrecalentamientos y mantener el rendimiento.',
}

const COMPONENT_TYPES = [
  { id: 'procesador', label: 'Procesador (CPU)', Icon: Cpu },
  { id: 'placa-base', label: 'Placa Base', Icon: CircuitBoard },
  { id: 'memoria-ram', label: 'Memoria RAM', Icon: MemoryStick },
  { id: 'tarjeta-grafica', label: 'Tarjeta Gráfica', Icon: Gpu },
  { id: 'almacenamiento', label: 'Almacenamiento', Icon: HardDrive },
  { id: 'fuente-alimentacion', label: 'Fuente de Alimentación', Icon: PlugZap },
  { id: 'caja', label: 'Caja/Torre', Icon: Box },
  { id: 'refrigerador-cpu', label: 'Refrigeración', Icon: Fan },
]

interface SelectedComponent {
  id: number
  tipo: string
  nombre: string
  precio: number
}

interface MontarPCPageProps {
  onBack: () => void
  onAddToCart?: (item: CartItem) => void
}

interface MontarPCLocationState {
  preseleccion?: SelectedComponent[]
}

export function MontarPCPage({ onBack, onAddToCart }: MontarPCPageProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const { token, isAuthenticated } = useAuth()
  const preseleccion = (location.state as MontarPCLocationState | null)?.preseleccion ?? []
  const [selectedComponents, setSelectedComponents] = useState<SelectedComponent[]>(preseleccion)
  const [sidePanelOpen, setSidePanelOpen] = useState<string | null>(null)
  const [availableComponents, setAvailableComponents] = useState<CompatiblePCComponent[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [selectedComponentDetail, setSelectedComponentDetail] = useState<CompatiblePCComponent | null>(null)
  const [equilibrio, setEquilibrio] = useState<EquilibrioData | null>(null)
  const [consumo, setConsumo] = useState<ConsumoData | null>(null)
  const [completing, setCompleting] = useState(false)
  const [completeError, setCompleteError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [saveSuccess, setSaveSuccess] = useState(false)

  const RELEVANT_TYPES = new Set(['procesador', 'tarjeta-grafica', 'memoria-ram', 'almacenamiento'])

  useEffect(() => {
    const relevantes = selectedComponents.filter((c) => RELEVANT_TYPES.has(c.tipo))
    if (relevantes.length === 0) {
      setEquilibrio(null)
      return
    }
    const ids = selectedComponents.map((c) => c.id).join(',')
    fetch(`${API_BASE_URL}/api/configuracion-pc/equilibrio?selectedIds=${ids}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data: EquilibrioData | null) => setEquilibrio(data))
      .catch(() => setEquilibrio(null))
  }, [selectedComponents])

  useEffect(() => {
    if (selectedComponents.length === 0) {
      setConsumo(null)
      return
    }
    const ids = selectedComponents.map((c) => c.id).join(',')
    fetch(`${API_BASE_URL}/api/configuracion-pc/consumo?selectedIds=${ids}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data: ConsumoData | null) => setConsumo(data))
      .catch(() => setConsumo(null))
  }, [selectedComponents])

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
    const entry: SelectedComponent = {
      id: component.id,
      tipo: component.tipo,
      nombre: component.nombre,
      precio: component.precio,
    }

    if (component.tipo === 'memoria-ram') {
      // Para RAM: añadir si no está ya seleccionada (mismo ID), ignorar si ya existe
      const alreadySelected = selectedComponents.some((c) => c.id === component.id)
      if (!alreadySelected) {
        setSelectedComponents([...selectedComponents, entry])
      }
    } else {
      const existingIndex = selectedComponents.findIndex((c) => c.tipo === component.tipo)
      if (existingIndex >= 0) {
        const updated = [...selectedComponents]
        updated[existingIndex] = entry
        setSelectedComponents(updated)
      } else {
        setSelectedComponents([...selectedComponents, entry])
      }
    }
  }

  const handleRemoveComponent = (tipo: string, id?: number) => {
    if (tipo === 'memoria-ram' && id !== undefined) {
      setSelectedComponents(selectedComponents.filter((c) => c.id !== id))
    } else {
      setSelectedComponents(selectedComponents.filter((c) => c.tipo !== tipo))
    }
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

  const handleSave = async (ids: number[], nombre: string) => {
    if (!isAuthenticated || !token) {
      navigate('/login')
      return
    }
    setSaving(true)
    setSaveError('')
    setSaveSuccess(false)
    try {
      const response = await fetch(`${API_BASE_URL}/api/mis-configuraciones`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ componenteIds: ids, nombre }),
      })
      if (!response.ok) {
        setSaveError('No se pudo guardar la configuración. Inténtalo de nuevo.')
        return
      }
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 4000)
    } catch {
      setSaveError('No se pudo conectar con el servidor.')
    } finally {
      setSaving(false)
    }
  }

  const handleComplete = async (ids: number[]) => {
    if (!isAuthenticated || !token) {
      navigate('/login')
      return
    }
    setCompleting(true)
    setCompleteError('')
    try {
      const response = await fetch(`${API_BASE_URL}/api/configuracion-pc`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ componenteIds: ids }),
      })
      if (!response.ok) {
        setCompleteError('No se pudo guardar la configuración. Inténtalo de nuevo.')
        return
      }
      const data = (await response.json()) as { id: number; precio: number }
      onAddToCart?.({
        configuracionId: data.id,
        nombre: 'Configuración personalizada',
        precio: data.precio,
        cantidad: 1,
      })
      navigate('/carrito')
    } catch {
      setCompleteError('No se pudo conectar con el servidor.')
    } finally {
      setCompleting(false)
    }
  }

  return (
    <div className="montar-pc-page">
      <header className="montar-pc-header">
        <button onClick={onBack} className="back-button">
          ← Volver al inicio
        </button>
        <div className="header-title">
          <h1><Wrench size={26} strokeWidth={1.75} aria-hidden="true" />Monta tu propio PC</h1>
          <p>Selecciona cada componente para crear tu ordenador personalizado</p>
        </div>
      </header>

      <div className="montar-pc-container">
        {/* Panel izquierdo: Componentes */}
        <section className="components-section">
          <h2>Componentes</h2>
          <div className="components-list">
            {COMPONENT_TYPES.map((type) => {
              const isRequired = type.id !== 'refrigerador-cpu'
              const selectedRams = selectedComponents.filter((c) => c.tipo === 'memoria-ram')
              const selected = type.id === 'memoria-ram' ? null : getSelectedComponentByType(type.id)

              return (
                <div key={type.id} className="component-item">
                  <button
                    className={`component-header ${isRequired ? 'required' : ''}`}
                    onClick={() => handleTypeClick(type.id)}
                  >
                    <span className="component-icon"><type.Icon size={22} strokeWidth={1.75} /></span>
                    <span className="component-label">
                      {type.label}
                      {isRequired && <span className="required-mark">*</span>}
                    </span>
                    {COMPONENT_INFO[type.id] && (
                      <span className="component-type-info">
                        <Info size={14} strokeWidth={2} aria-hidden="true" />
                        <span className="component-type-tooltip">{COMPONENT_INFO[type.id]}</span>
                      </span>
                    )}
                    <span className="component-status">
                      {type.id === 'memoria-ram' ? (
                        selectedRams.length > 0 ? (
                          <span className="status-selected">✓ {selectedRams.length} kit{selectedRams.length > 1 ? 's' : ''} seleccionado{selectedRams.length > 1 ? 's' : ''}</span>
                        ) : (
                          <span className="status-empty">No seleccionado</span>
                        )
                      ) : selected ? (
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
          equilibrio={equilibrio}
          consumo={consumo}
          onComplete={handleComplete}
          completing={completing}
          completeError={completeError}
          onSave={isAuthenticated ? handleSave : undefined}
          saving={saving}
          saveError={saveError}
          saveSuccess={saveSuccess}
        />
      </div>

      <ComponentDetailModal
        component={selectedComponentDetail}
        isOpen={detailModalOpen}
        onClose={handleCloseDetailModal}
        onSelect={handleSelectComponent}
        isSelected={selectedComponents.some((c) => c.id === selectedComponentDetail?.id)}
      />

      <ComponentSidePanel
        isOpen={sidePanelOpen !== null}
        tipo={sidePanelOpen || ''}
        title={COMPONENT_TYPES.find((t) => t.id === sidePanelOpen)?.label || ''}
        components={availableComponents}
        selectedComponentIds={
          sidePanelOpen === 'memoria-ram'
            ? selectedComponents.filter((c) => c.tipo === 'memoria-ram').map((c) => c.id)
            : selectedComponents.filter((c) => c.tipo === sidePanelOpen).map((c) => c.id)
        }
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
