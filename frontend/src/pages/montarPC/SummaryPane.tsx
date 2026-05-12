interface SelectedComponent {
  id: number
  tipo: string
  nombre: string
  precio: number
}

interface ComponentType {
  id: string
  label: string
}

interface Props {
  componentTypes: ComponentType[]
  selectedComponents: SelectedComponent[]
  onRemove: (tipo: string) => void
  totalPrice: number
  requiredCount: number
}

export default function SummaryPane({ componentTypes, selectedComponents, onRemove, totalPrice, requiredCount }: Props) {
  const selectedTypeIds = new Set(selectedComponents.map((component) => component.tipo))
  const requiredTypeIds = componentTypes
    .filter((componentType) => componentType.id !== 'refrigerador-cpu')
    .map((componentType) => componentType.id)

  const selectedRequiredCount = requiredTypeIds.filter((typeId) => selectedTypeIds.has(typeId)).length
  const canCompleteConfiguration = selectedRequiredCount === requiredTypeIds.length

  return (
    <aside className="summary-section">
      <h2>Resumen de la configuración</h2>

      <div className="summary-content">
        {componentTypes.map((type) => {
          const selected = selectedComponents.find((c) => c.tipo === type.id)
          return (
            <div key={type.id} className="summary-item">
              <span className="summary-label">{type.label}</span>
              {selected ? (
                <div className="summary-value">
                  <span className="component-name">{selected.nombre}</span>
                  <button
                    className="remove-button"
                    onClick={() => onRemove(type.id)}
                    title="Eliminar"
                  >
                    ✕
                  </button>
                </div>
              ) : (
                <span className="summary-empty">No seleccionado</span>
              )}
            </div>
          )
        })}
      </div>

      <div className="summary-stats">
        <p>
          Componentes obligatiorios seleccionados: <strong>{selectedRequiredCount} / {requiredCount}</strong>
        </p>
      </div>

      <div className="summary-total">
        <span>Precio total:</span>
        <span className="total-price">{totalPrice.toFixed(2)}€</span>
      </div>

      <button className="complete-button" disabled={!canCompleteConfiguration}>
        Completar configuración
      </button>
    </aside>
  )
}
