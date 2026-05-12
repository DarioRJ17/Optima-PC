import type { CompatiblePCComponent } from '../../types'
import ComponentOptionCard from './ComponentOptionCard'
import './ComponentSidePanel.css'

interface Props {
  isOpen: boolean
  title: string
  components: CompatiblePCComponent[]
  selectedComponentId?: number
  loading: boolean
  error: string
  onClose: () => void
  onSelect: (component: CompatiblePCComponent) => void
  onViewDetails: (component: CompatiblePCComponent) => void
}

export default function ComponentSidePanel({
  isOpen,
  title,
  components,
  selectedComponentId,
  loading,
  error,
  onClose,
  onSelect,
  onViewDetails,
}: Props) {
  return (
    <>
      {isOpen && <div className="side-panel-overlay" onClick={onClose} />}

      <aside className={`side-panel ${isOpen ? 'open' : ''}`}>
        <div className="side-panel-header">
          <h2>{title}</h2>
          <button className="side-panel-close" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="side-panel-wrapper">
          {/* Componentes en panel derecho */}
          <div className="side-panel-content">
            {loading && <div className="side-panel-loading">Cargando componentes...</div>}

            {error && <div className="side-panel-error">{error}</div>}

            {!loading && !error && components.length === 0 && (
              <div className="side-panel-empty">No hay componentes disponibles</div>
            )}

            {!loading && !error && components.length > 0 && (
              <div className="side-panel-components">
                {components.map((component) => (
                  <ComponentOptionCard
                    key={component.id}
                    component={component}
                    isSelected={selectedComponentId === component.id}
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
