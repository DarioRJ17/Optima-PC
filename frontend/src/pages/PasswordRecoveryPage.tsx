import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppLogo } from '../components/common'
import type { ApiError } from '../types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

type PasswordRecoveryPageProps = {
  onBack: () => void
}

export function PasswordRecoveryPage({ onBack }: PasswordRecoveryPageProps) {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [globalError, setGlobalError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const parseError = async (response: Response) => {
    try {
      const data = (await response.json()) as ApiError
      setGlobalError(data.message || 'No se pudo procesar la solicitud')
    } catch {
      setGlobalError('Error de conexion con el servidor')
    }
  }

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setGlobalError('')
    setSuccessMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/password-reset/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      setSuccessMessage('Si el correo existe, recibirás un enlace para restablecer tu contraseña.')
      setEmail('')
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <button type="button" className="back-link" onClick={onBack}>
          &larr; Volver al inicio de sesión
        </button>

        <section className="auth-card" aria-live="polite">
          <div className="auth-card__brand">
            <AppLogo />
          </div>

          <h1>Recuperar contraseña</h1>
          <p className="subtitle">Te enviaremos un enlace para crear una nueva contraseña</p>

          <form className="auth-form" onSubmit={onSubmit} noValidate>
            <label htmlFor="recovery-email">Correo electrónico</label>
            <input
              id="recovery-email"
              type="email"
              placeholder="ejemplo@correo.com"
              value={email}
              onChange={(event) => setEmail(event.currentTarget.value)}
            />

            <button type="submit" disabled={loading}>
              {loading ? 'Enviando...' : 'Enviar enlace'}
            </button>
          </form>

          {globalError ? <p className="global-error">{globalError}</p> : null}
          {successMessage ? <p className="success-message">{successMessage}</p> : null}

          <p className="mode-link">
            ¿Ya recuerdas tu contraseña?{' '}
            <button type="button" onClick={() => navigate('/login')}>
              Volver a iniciar sesión
            </button>
          </p>
        </section>
      </section>
    </main>
  )
}