import { useEffect, useState, type FormEvent } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { AppLogo, PasswordStrengthMeter } from '@/shared/components/common'
import type { ApiError } from '@/shared/types'

import { API_BASE_URL } from '@/api/client'

type PasswordResetPageProps = {
  onBack: () => void
}

export function PasswordResetPage({ onBack }: PasswordResetPageProps) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [confirmPasswordError, setConfirmPasswordError] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [passwordStrength, setPasswordStrength] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [globalError, setGlobalError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const validatePassword = (value: string) => {
    if (value.length < 8) {
      return 'La contraseña debe tener al menos 8 caracteres'
    }

    if (!/[A-Z]/.test(value)) {
      return 'La contraseña debe incluir al menos una mayúscula'
    }

    if (!/[a-z]/.test(value)) {
      return 'La contraseña debe incluir al menos una minúscula'
    }

    if (!/\d/.test(value)) {
      return 'La contraseña debe incluir al menos un número'
    }

    if (!/[^A-Za-z0-9]/.test(value)) {
      return 'La contraseña debe incluir al menos un carácter especial'
    }

    return ''
  }

  useEffect(() => {
    if (!password) {
      setPasswordStrength(null)
      return
    }

    const timer = setTimeout(async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/auth/password-strength`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password }),
        })

        if (response.ok) {
          const data = (await response.json()) as { strength: string }
          setPasswordStrength(data.strength)
        }
      } catch {
        // silencioso: la validación local sigue funcionando
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [password])

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
    setPasswordError('')
    setConfirmPasswordError('')

    if (!token) {
      setGlobalError('El enlace de recuperación no es válido')
      setLoading(false)
      return
    }

    const passwordError = validatePassword(password)
    if (passwordError) {
      setPasswordError(passwordError)
      setLoading(false)
      return
    }

    if (password !== confirmPassword) {
      setConfirmPasswordError('Las contraseñas no coinciden')
      setLoading(false)
      return
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/password-reset/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, password }),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      setSuccessMessage('La contraseña se ha actualizado correctamente. Ya puedes iniciar sesión.')
      setPassword('')
      setConfirmPassword('')
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

          <h1>Nueva contraseña</h1>
          <p className="subtitle">Introduce la nueva contraseña para finalizar la recuperación</p>

          <form className="auth-form" onSubmit={onSubmit} noValidate>
            <label htmlFor="reset-password">Nueva contraseña</label>
            <div className="password-wrapper">
              <input
                id="reset-password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Mínimo 8 caracteres"
                value={password}
                onChange={(event) => setPassword(event.currentTarget.value)}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {showPassword ? <EyeOff /> : <Eye />}
              </button>
            </div>

            {passwordError ? <p className="field-error">{passwordError}</p> : null}
            {passwordStrength ? <PasswordStrengthMeter strength={passwordStrength} /> : null}

            <label htmlFor="reset-confirm-password">Confirmar contraseña</label>
            <div className="password-wrapper">
              <input
                id="reset-confirm-password"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Repite tu contraseña"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.currentTarget.value)}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowConfirmPassword((current) => !current)}
                aria-label={showConfirmPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {showConfirmPassword ? <EyeOff /> : <Eye />}
              </button>
            </div>
            {confirmPasswordError ? <p className="field-error">{confirmPasswordError}</p> : null}

            <button type="submit" disabled={loading}>
              {loading ? 'Guardando...' : 'Actualizar contraseña'}
            </button>
          </form>

          {globalError ? <p className="global-error">{globalError}</p> : null}
          {successMessage ? <p className="success-message">{successMessage}</p> : null}

          {successMessage ? (
            <p className="mode-link">
              <button type="button" onClick={() => navigate('/login')}>
                Ir al inicio de sesión
              </button>
            </p>
          ) : null}
        </section>
      </section>
    </main>
  )
}