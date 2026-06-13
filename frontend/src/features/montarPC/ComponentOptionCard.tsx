import type { CompatiblePCComponent } from '@/shared/types'

interface Props {
  component: CompatiblePCComponent
  isSelected: boolean
  onSelect: (c: CompatiblePCComponent) => void
  onViewDetails: (c: CompatiblePCComponent) => void
}

export default function ComponentOptionCard({
  component,
  isSelected,
  onSelect,
  onViewDetails,
}: Props) {
  const handleCardClick = () => {
    onViewDetails(component)
  }

  const handleSelectClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    onSelect(component)
  }

  return (
    <div className={`option-card ${isSelected ? 'selected' : ''}`} onClick={handleCardClick}>
      <div className="option-card-top">
        <div className="option-content">
          <h4 className="option-name">{component.nombre}</h4>
          {component.especificacion && <p className="option-spec">{component.especificacion}</p>}
        </div>

        <div className="option-price-and-badges">
          {component.warnings && component.warnings.length > 0 && (
            <span className="option-badge warning-badge" title={`${component.warnings.length} advertencia(s)`}>
              ⚠ {component.warnings.length}
            </span>
          )}
          {isSelected && <span className="option-badge selected-badge">✓</span>}
          <span className="option-price">{component.precio.toFixed(2)}€</span>
        </div>
      </div>

      <div className="option-footer">
        <button className="select-button" onClick={handleCardClick} title="Ver detalles">
          Ver detalles
        </button>
        <button
          className={`select-button ${isSelected ? 'selected' : ''}`}
          onClick={handleSelectClick}
          title={isSelected ? 'Componente seleccionado' : 'Seleccionar este componente'}
        >
          {isSelected ? '✓ Seleccionado' : 'Seleccionar'}
        </button>
      </div>
    </div>
  )
}
