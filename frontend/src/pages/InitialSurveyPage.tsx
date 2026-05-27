import { useEffect, useState, type ReactNode } from 'react'
import { ArrowRight, Clapperboard, Code2, Euro, Gamepad2, Laptop, Palette, Briefcase, RefreshCcw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { AppLogo } from '../components/common'
import { useAuth } from '../auth/useAuth'
import type { ApiError } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type TipoUsoValue = 'GAMING' | 'OFIMATICA' | 'EDICION' | 'PROGRAMACION' | 'STREAMING'

type TipoUsoOption = {
  value: TipoUsoValue
  title: string
  description: string
  icon: ReactNode
}

type BudgetOption = {
  label: string
  value: number
}

const MAIN_USE_OPTIONS: TipoUsoOption[] = [
  {
    value: 'GAMING',
    title: 'Gaming',
    description: 'Juegos y entretenimiento',
    icon: <Gamepad2 size={19} />,
  },
  {
    value: 'OFIMATICA',
    title: 'Trabajo',
    description: 'Oficina y productividad',
    icon: <Briefcase size={19} />,
  },
  {
    value: 'EDICION',
    title: 'Creación de contenido',
    description: 'Diseño, vídeo y 3D',
    icon: <Palette size={19} />,
  },
  {
    value: 'PROGRAMACION',
    title: 'Programación',
    description: 'Desarrollo, compilar y multitarea',
    icon: <Code2 size={19} />,
  },
  {
    value: 'STREAMING',
    title: 'Streaming',
    description: 'Directos, grabación y retransmisión',
    icon: <Clapperboard size={19} />,
  },
]

const BUDGET_OPTIONS: BudgetOption[] = [
  { label: 'Menos de 500€', value: 400 },
  { label: '500€ - 1.000€', value: 750 },
  { label: '1.000€ - 1.500€', value: 1250 },
  { label: '1.500€ - 2.000€', value: 1750 },
  { label: 'Más de 2.000€', value: 2500 },
]

const RECONDITIONED_OPTIONS = [
  { label: 'Sí', value: true, description: 'Me interesa priorizar reacondicionado' },
  { label: 'No', value: false, description: 'Prefiero componentes nuevos' },
  { label: 'Me da igual', value: null, description: 'No tengo preferencia todavía' },
]

export function InitialSurveyPage({ onBack, onSurveySaved }: { onBack: () => void; onSurveySaved: () => void }) {
  const navigate = useNavigate()
  const { token } = useAuth()
  const [usoPrincipal, setUsoPrincipal] = useState<TipoUsoValue | null>(null)
  const [usosSecundarios, setUsosSecundarios] = useState<TipoUsoValue[]>([])
  const [presupuesto, setPresupuesto] = useState<number | null>(null)
  const [preferenciaReacondicionado, setPreferenciaReacondicionado] = useState<boolean | null | undefined>(undefined)
  const [loading, setLoading] = useState(false)
  const [globalError, setGlobalError] = useState('')

  useEffect(() => {
    if (!token) {
      navigate('/login', { replace: true })
    }
  }, [navigate, token])

  useEffect(() => {
    if (!usoPrincipal) {
      return
    }

    setUsosSecundarios((current) => current.filter((uso) => uso !== usoPrincipal))
  }, [usoPrincipal])

  const secondaryOptions = MAIN_USE_OPTIONS.filter((option) => option.value !== usoPrincipal)

  const parseError = async (response: Response) => {
    try {
      const data = (await response.json()) as ApiError
      setGlobalError(data.message || 'No se pudo procesar la solicitud')
    } catch {
      setGlobalError('Error de conexion con el servidor')
    }
  }

  const enviarEncuesta = async (skip = false) => {
    if (!token) {
      navigate('/login', { replace: true })
      return
    }

    setLoading(true)
    setGlobalError('')

    try {
      const payload = skip
        ? {}
        : {
            usoPrincipal,
            usosSecundariosEncuesta: usosSecundarios.length > 0 ? usosSecundarios : null,
            presupuesto,
            preferenciaReacondicionado,
          }

      const response = await fetch(`${API_BASE_URL}/api/perfil-usuario/encuesta-inicial`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      onSurveySaved()
      navigate('/', { replace: true })
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page onboarding-page">
      <section className="auth-shell onboarding-shell">
        <button type="button" className="back-link" onClick={onBack}>
          &larr; Volver al inicio
        </button>

        <section className="auth-card onboarding-card" aria-live="polite">
          <div className="auth-card__brand">
            <AppLogo />
          </div>

          <div className="onboarding-header">
            <h1>Completa tu perfil inicial</h1>
            <p className="subtitle">Usaremos estas respuestas para afinar tus recomendaciones desde el primer día</p>
          </div>

          <div className="onboarding-section">
            <div className="onboarding-section__header">
              <Gamepad2 size={18} />
              <h2>¿Para qué usarás principalmente tu ordenador?</h2>
            </div>

            <div className="onboarding-grid">
              {MAIN_USE_OPTIONS.map((option) => {
                const selected = usoPrincipal === option.value

                return (
                  <button
                    key={option.value}
                    type="button"
                    className={`survey-option ${selected ? 'survey-option--selected' : ''}`}
                    onClick={() => setUsoPrincipal(selected ? null : option.value)}
                  >
                    <span className="survey-option__icon">{option.icon}</span>
                    <span className="survey-option__content">
                      <strong>{option.title}</strong>
                      <span>{option.description}</span>
                    </span>
                  </button>
                )
              })}
            </div>
          </div>

          <div className="onboarding-section">
            <div className="onboarding-section__header">
              <Euro size={18} />
              <h2>¿Cuál es tu presupuesto aproximado?</h2>
            </div>

            <div className="budget-grid">
              {BUDGET_OPTIONS.map((option) => {
                const selected = presupuesto === option.value

                return (
                  <button
                    key={option.label}
                    type="button"
                    className={`budget-option ${selected ? 'budget-option--selected' : ''}`}
                    onClick={() => setPresupuesto(selected ? null : option.value)}
                  >
                    <Euro size={16} />
                    <span>{option.label}</span>
                  </button>
                )
              })}
            </div>
          </div>

          <div className="onboarding-section">
            <div className="onboarding-section__header">
              <Laptop size={18} />
              <h2>¿Qué nivel de reacondicionado prefieres?</h2>
            </div>

            <div className="toggle-grid">
              {RECONDITIONED_OPTIONS.map((option) => {
                const selected = preferenciaReacondicionado === option.value

                const togglePreferencia = () => {
                  if (option.value === null) {
                    setPreferenciaReacondicionado((current) => (current === null ? undefined : null))
                    return
                  }

                  setPreferenciaReacondicionado((current) => (current === option.value ? undefined : option.value))
                }

                return (
                  <button
                    key={option.label}
                    type="button"
                    className={`toggle-option ${selected ? 'toggle-option--selected' : ''}`}
                    onClick={togglePreferencia}
                  >
                    <strong>{option.label}</strong>
                    <span>{option.description}</span>
                  </button>
                )
              })}
            </div>
          </div>

          <div className="onboarding-section">
            <div className="onboarding-section__header">
              <RefreshCcw size={18} />
              <h2>Usos secundarios</h2>
            </div>

            <div className="chip-list">
              {secondaryOptions.map((option) => {
                const selected = usosSecundarios.includes(option.value)

                return (
                  <button
                    key={option.value}
                    type="button"
                    className={`chip-option ${selected ? 'chip-option--selected' : ''}`}
                    onClick={() =>
                      setUsosSecundarios((current) =>
                        selected ? current.filter((uso) => uso !== option.value) : [...current, option.value]
                      )
                    }
                  >
                    {option.title}
                  </button>
                )
              })}
            </div>
            <p className="survey-helper">Puedes dejar esta parte vacía si quieres que el sistema priorice solo el uso principal.</p>
          </div>

          {globalError ? <p className="global-error">{globalError}</p> : null}

          <div className="onboarding-actions">
            <button type="button" className="onboarding-skip" onClick={() => void enviarEncuesta(true)} disabled={loading}>
              Omitir por ahora
            </button>

            <button type="button" className="onboarding-continue" onClick={() => void enviarEncuesta(false)} disabled={loading}>
              {loading ? 'Guardando...' : 'Guardar y continuar'}
              <ArrowRight size={18} />
            </button>
          </div>
        </section>
      </section>
    </main>
  )
}