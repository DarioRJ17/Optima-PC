import { useState, useEffect } from 'react'
import type { ConsumoData, EquilibrioData } from '../../types'
import { Zap } from 'lucide-react'

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
  equilibrio: EquilibrioData | null
  consumo: ConsumoData | null
  onComplete?: (ids: number[]) => void
  completing?: boolean
  completeError?: string
  onSave?: (ids: number[], nombre: string) => void
  saving?: boolean
  saveError?: string
  saveSuccess?: boolean
}

function scoreColor(score: number): string {
  if (score >= 70) return '#4caf50'
  if (score >= 40) return '#ff9800'
  return '#f44336'
}

export default function SummaryPane({
  componentTypes,
  selectedComponents,
  onRemove,
  totalPrice,
  requiredCount,
  equilibrio,
  consumo,
  onComplete,
  completing = false,
  completeError,
  onSave,
  saving = false,
  saveError,
  saveSuccess = false,
}: Props) {
  const [saveName, setSaveName] = useState('')

  useEffect(() => {
    if (saveSuccess) setSaveName('')
  }, [saveSuccess])

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
          Componentes obligatorios seleccionados:{' '}
          <strong>
            {selectedRequiredCount} / {requiredCount}
          </strong>
        </p>
      </div>

      {/* Indicador de equilibrio */}
      {equilibrio && (
        <div className="summary-equilibrio">
          <div className="equilibrio-header">
            <span className="equilibrio-label">Equilibrio de la build</span>
            <span className="equilibrio-score" style={{ color: scoreColor(equilibrio.score) }}>
              {Math.round(equilibrio.score)}<span className="equilibrio-max">/100</span>
            </span>
          </div>
          <div className="equilibrio-track">
            <div
              className="equilibrio-fill"
              style={{
                width: `${Math.min(100, Math.max(0, equilibrio.score))}%`,
                background: scoreColor(equilibrio.score),
              }}
            />
          </div>
          {equilibrio.componentes.length > 0 && (
            <div className="equilibrio-avisos">
              {equilibrio.componentes.map((d) => (
                <span key={d.nombre} className="equilibrio-aviso">
                  {d.sobredimensionado ? '▲' : '▼'} {d.nombre}{' '}
                  {d.sobredimensionado ? 'sobredimensionada' : 'es cuello de botella'}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Indicador de consumo */}
      {consumo && consumo.consumoEstimadoW > 0 && (
        <div className={`summary-consumo${!consumo.suficiente ? ' summary-consumo--warn' : ''}`}>
          <div className="consumo-header">
            <span className="consumo-label">
              <Zap size={13} strokeWidth={2.5} aria-hidden="true" />
              Consumo estimado
            </span>
            <span className="consumo-valor">{consumo.consumoEstimadoW}W</span>
          </div>
          {consumo.potenciaPSUW != null ? (
            <>
              <div className="consumo-row">
                <span>Fuente seleccionada</span>
                <span>{consumo.potenciaPSUW}W</span>
              </div>
              <div className="consumo-row">
                <span>Disponible (con margen 25%)</span>
                <span className={consumo.suficiente ? 'consumo-ok' : 'consumo-nok'}>
                  {consumo.disponibleW != null
                    ? consumo.disponibleW >= 0
                      ? `+${consumo.disponibleW}W`
                      : `−${Math.abs(consumo.disponibleW)}W`
                    : '—'}
                </span>
              </div>
              {!consumo.suficiente && (
                <p className="consumo-aviso">La fuente puede quedarse corta. Se recomiendan al menos {consumo.consumoRecomendadoW}W.</p>
              )}
            </>
          ) : (
            <div className="consumo-row">
              <span>Fuente recomendada</span>
              <span className="consumo-recom">≥ {consumo.consumoRecomendadoW}W</span>
            </div>
          )}
        </div>
      )}

      <div className="summary-total">
        <span>Precio total:</span>
        <span className="total-price">{totalPrice.toFixed(2)}€</span>
      </div>

      {completeError && <p className="complete-error">{completeError}</p>}

      <button
        className="complete-button"
        disabled={!canCompleteConfiguration || completing}
        onClick={() => onComplete?.(selectedComponents.map((c) => c.id))}
      >
        {completing ? 'Añadiendo...' : 'Añadir al carrito'}
      </button>

      {onSave && (
        <div className="save-config-section">
          <input
            type="text"
            className="save-config-input"
            placeholder="Nombre de la configuración"
            value={saveName}
            maxLength={100}
            onChange={(e) => setSaveName(e.currentTarget.value)}
          />
          <button
            className="save-config-btn"
            disabled={!canCompleteConfiguration || saving || !saveName.trim()}
            onClick={() => onSave(selectedComponents.map((c) => c.id), saveName.trim())}
          >
            {saving ? 'Guardando...' : 'Guardar configuración'}
          </button>
          {saveSuccess && <p className="save-config-success">¡Configuración guardada correctamente!</p>}
          {saveError && <p className="complete-error">{saveError}</p>}
        </div>
      )}
    </aside>
  )
}
