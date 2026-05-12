import { useEffect, useState } from 'react'
import type { CompatiblePCComponent, ComponenteDetalle } from '../../types'
import './ComponentDetailModal.css'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

interface Props {
  component: CompatiblePCComponent | null
  isOpen: boolean
  onClose: () => void
  onSelect?: (component: CompatiblePCComponent) => void
  isSelected?: boolean
}

export default function ComponentDetailModal({
  component,
  isOpen,
  onClose,
}: Props) {
  const [detalles, setDetalles] = useState<ComponenteDetalle | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!isOpen || !component) {
      return
    }

    fetch(`${API_BASE_URL}/api/configuracion-pc/components/${component.id}/detail`)
      .then((res) => {
        if (!res.ok) {
          throw new Error('No se pudieron cargar los detalles')
        }
        return res.json()
      })
      .then((data: ComponenteDetalle) => {
        setDetalles(data)
      })
      .catch(() => {
        setDetalles(null)
      })
      .finally(() => {
        setLoading(false)
      })
  }, [isOpen, component])

  if (!isOpen || !component) return null

  const formatDetalleValue = (key: string, value: unknown): string => {
    if (value === null || value === undefined) return 'N/A'
    if (typeof value === 'boolean') return value ? 'Sí' : 'No'
    if (typeof value === 'number') {
      if (key.toLowerCase().includes('precio')) return `${value.toFixed(2)}€`
      if (key.toLowerCase().includes('frecuencia') || key.toLowerCase().includes('velocidad')) return `${value} MHz`
      if (key.toLowerCase().includes('potencia') || key.toLowerCase().includes('tdp')) return `${value}W`
      if (key.toLowerCase().includes('capacidad') || key.toLowerCase().includes('memoria') || key.toLowerCase().includes('gb')) {
        return `${value}`
      }
      return `${value}`
    }
    return String(value)
  }

  const formatLabel = (key: string): string => {
    return key
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (str) => str.toUpperCase())
      .trim()
  }

  const getDetailOrder = (tipo: string): string[] => {
    switch (tipo) {
      case 'procesador':
        return ['socket', 'nucleos', 'frecuenciaBase', 'frecuenciaBoost', 'microarquitectura', 'tdp', 'graficaIntegrada']
      case 'placa-base':
        return ['socket', 'tipoDDR', 'factorForma', 'memoriaMaxima', 'ranurasMemoria', 'color']
      case 'memoria-ram':
        return ['tipoDDR', 'velocidad', 'gbPorModulo', 'numModulos', 'totalGB', 'latenciaCAS', 'color']
      case 'tarjeta-grafica':
        return ['chipset', 'memoria', 'frecuenciaBase', 'frecuenciaBoost', 'longitud', 'color']
      case 'almacenamiento':
        return ['tipo', 'capacidad', 'interfaz', 'factorForma']
      case 'fuente-alimentacion':
        return ['potencia', 'eficiencia', 'tipo', 'modular', 'color']
      case 'caja':
        return ['tipo', 'color', 'panelLateral']
      case 'refrigerador-cpu':
        return ['rpm', 'nivelRuido', 'tamano', 'color']
      default:
        return []
    }
  }

  const detailOrder = getDetailOrder(component.tipo)

  const detallesList = detalles?.detalles
    ? detailOrder
        .map((key) => [key, detalles.detalles[key]] as const)
        .filter(([, value]) => value !== undefined && value !== null)
    : []

  return (
    <div className="detail-modal-overlay" onClick={onClose}>
      <div className="detail-modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="detail-modal-close" onClick={onClose}>
          ✕
        </button>

        <div className="detail-modal-header">
          <div className="detail-modal-title-row">
            <h2>{component.nombre}</h2>
            <span className="detail-price">{component.precio.toFixed(2)}€</span>
          </div>
        </div>

        <div className="detail-modal-body">
          {loading ? (
            <div className="detail-loading">Cargando detalles...</div>
          ) : (
            <>
              {component.warnings && component.warnings.length > 0 && (
                <div className="detail-section warnings-section">
                  <h3>Advertencias de compatibilidad</h3>
                  <div className="warnings-list">
                    {component.warnings.map((w) => (
                      <div key={w.code} className="warning-item">
                        <span className="warning-code">{w.code}</span>
                        <span className="warning-message">{w.message}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <div className="detail-section">
                <h3>Información</h3>
                <div className="info-grid">
                  {detallesList.map(([key, value]) => (
                    <div key={key} className="info-item">
                      <span className="info-label">{formatLabel(key)}</span>
                      <span className="info-value">{formatDetalleValue(key, value)}</span>
                    </div>
                  ))}
                  {detallesList.length === 0 && (
                    <div className="info-item">
                      <span className="info-label">Detalles</span>
                      <span className="info-value">No disponibles</span>
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
