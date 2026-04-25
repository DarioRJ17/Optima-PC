import { useState, useEffect } from 'react'
import './App.css'

type AuthMode = 'login' | 'register'

type ApiError = {
  message?: string
  fieldErrors?: Record<string, string>
}

type AuthResponse = {
  mensaje: string
  id: number
  nombre: string
  apellidos: string
  email: string
  fechaRegistro: string
}

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080'

const STRENGTH_CONFIG: Record<string, { label: string; color: string; bars: number }> = {
  VERY_WEAK:   { label: 'Muy débil',  color: '#ef4444', bars: 1 },
  WEAK:        { label: 'Débil',      color: '#f97316', bars: 2 },
  FAIR:        { label: 'Regular',    color: '#eab308', bars: 3 },
  STRONG:      { label: 'Fuerte',     color: '#22c55e', bars: 4 },
  VERY_STRONG: { label: 'Muy fuerte', color: '#16a34a', bars: 5 },
}

function PasswordStrengthMeter({ strength }: { strength: string }) {
  const config = STRENGTH_CONFIG[strength] ?? STRENGTH_CONFIG.VERY_WEAK

  return (
    <div className="password-strength">
      <div className="strength-bars">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="strength-bar"
            style={{ backgroundColor: i < config.bars ? config.color : undefined }}
          />
        ))}
      </div>
      <span className="strength-label" style={{ color: config.color }}>
        {config.label}
      </span>
    </div>
  )
}

function App() {
  const [mode, setMode] = useState<AuthMode>('login')

  const [loginData, setLoginData] = useState({ email: '', password: '' })
  const [registerData, setRegisterData] = useState({
    nombre: '',
    apellidos: '',
    email: '',
    password: '',
    confirmPassword: '',
  })
  const [passwordStrength, setPasswordStrength] = useState<string | null>(null)

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [globalError, setGlobalError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
  if (!registerData.password) {
    setPasswordStrength(null)
    return
  }

  const timer = setTimeout(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/password-strength`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: registerData.password }),
      })
      if (response.ok) {
        const data = await response.json()
        setPasswordStrength(data.strength)
      }
    } catch {
      // silencioso, no es crítico
    }
  }, 300)

  return () => clearTimeout(timer)
}, [registerData.password])

  const switchMode = (newMode: AuthMode) => {
    setMode(newMode)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')
    setPasswordStrength(null)
  }

  const parseError = async (response: Response) => {
    try {
      const data = (await response.json()) as ApiError
      const nextFieldErrors = data.fieldErrors ?? {}
      const hasFieldErrors = Object.keys(nextFieldErrors).length > 0

      setFieldErrors(nextFieldErrors)
      setGlobalError(hasFieldErrors ? '' : data.message || 'No se pudo procesar la solicitud')
    } catch {
      setFieldErrors({})
      setGlobalError('Error de conexion con el servidor')
    }
  }

  const onLoginSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      const data = (await response.json()) as AuthResponse
      setSuccessMessage(`Bienvenido, ${data.nombre} ${data.apellidos}`)
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  const onRegisterSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setFieldErrors({})
    setGlobalError('')
    setSuccessMessage('')

    if (registerData.password !== registerData.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Las contraseñas no coinciden' })
      setLoading(false)
      return
    }

    try {
      const payload = {
        nombre: registerData.nombre,
        apellidos: registerData.apellidos,
        email: registerData.email,
        password: registerData.password,
      }

      const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })

      if (!response.ok) {
        await parseError(response)
        return
      }

      const data = (await response.json()) as AuthResponse
      setSuccessMessage(`Cuenta creada para ${data.nombre} ${data.apellidos}`)
      setLoginData({ email: data.email, password: '' })
      setRegisterData({
        nombre: '',
        apellidos: '',
        email: '',
        password: '',
        confirmPassword: '',
      })
      setMode('login')
    } catch {
      setGlobalError('No se pudo conectar con el backend')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-shell">
        <a className="back-link" href="#" aria-label="Volver al inicio">
          &larr; Volver al inicio
        </a>

        <section className="auth-card" aria-live="polite">
          <div className="logo-badge">O</div>

          {mode === 'login' ? (
            <>
              <h1>Iniciar sesion</h1>
              <p className="subtitle">Accede a tu cuenta de Optima PC</p>

              <form className="auth-form" onSubmit={onLoginSubmit} noValidate>
                <label htmlFor="login-email">Correo electronico</label>
                <input
                  id="login-email"
                  type="email"
                  placeholder="ejemplo@correo.com"
                  value={loginData.email}
                  onChange={(event) =>
                    setLoginData((previous) => ({
                      ...previous,
                      email: event.target.value,
                    }))
                  }
                />
                {fieldErrors.email ? (
                  <p className="field-error">{fieldErrors.email}</p>
                ) : null}

                <label htmlFor="login-password">Contraseña</label>
                <input
                  id="login-password"
                  type="password"
                  placeholder="********"
                  value={loginData.password}
                  onChange={(event) =>
                    setLoginData((previous) => ({
                      ...previous,
                      password: event.target.value,
                    }))
                  }
                />
                {fieldErrors.password ? (
                  <p className="field-error">{fieldErrors.password}</p>
                ) : null}

                <a className="inline-link" href="#">
                  Olvidaste tu contraseña?
                </a>

                <button type="submit" disabled={loading}>
                  {loading ? 'Validando...' : 'Iniciar sesion'}
                </button>
              </form>

              <p className="mode-link">
                No tienes cuenta?{' '}
                <button type="button" onClick={() => switchMode('register')}>
                  Registrate aqui
                </button>
              </p>
            </>
          ) : (
            <>
              <h1>Crear cuenta</h1>
              <p className="subtitle">Unete a Optima PC hoy mismo</p>

              <form className="auth-form" onSubmit={onRegisterSubmit} noValidate>
                <label htmlFor="register-nombre">Nombre</label>
                <input
                  id="register-nombre"
                  type="text"
                  placeholder="Tu nombre"
                  value={registerData.nombre}
                  onChange={(event) =>
                    setRegisterData((previous) => ({
                      ...previous,
                      nombre: event.target.value,
                    }))
                  }
                />
                {fieldErrors.nombre ? (
                  <p className="field-error">{fieldErrors.nombre}</p>
                ) : null}

                <label htmlFor="register-apellidos">Apellidos</label>
                <input
                  id="register-apellidos"
                  type="text"
                  placeholder="Tus apellidos"
                  value={registerData.apellidos}
                  onChange={(event) =>
                    setRegisterData((previous) => ({
                      ...previous,
                      apellidos: event.target.value,
                    }))
                  }
                />
                {fieldErrors.apellidos ? (
                  <p className="field-error">{fieldErrors.apellidos}</p>
                ) : null}

                <label htmlFor="register-email">Correo electronico</label>
                <input
                  id="register-email"
                  type="email"
                  placeholder="ejemplo@correo.com"
                  value={registerData.email}
                  onChange={(event) =>
                    setRegisterData((previous) => ({
                      ...previous,
                      email: event.target.value,
                    }))
                  }
                />
                {fieldErrors.email ? (
                  <p className="field-error">{fieldErrors.email}</p>
                ) : null}

                <label htmlFor="register-password">Contraseña</label>
                <input
                  id="register-password"
                  type="password"
                  placeholder="Minimo 8 caracteres"
                  value={registerData.password}
                  onChange={(event) =>
                    setRegisterData((previous) => ({
                      ...previous,
                      password: event.target.value,
                    }))
                  }
                />
                {passwordStrength && <PasswordStrengthMeter strength={passwordStrength} />}
                {fieldErrors.password ? (
                  <p className="field-error">{fieldErrors.password}</p>
                ) : null}

                <label htmlFor="register-confirm-password">Confirmar contraseña</label>
                <input
                  id="register-confirm-password"
                  type="password"
                  placeholder="Repite tu contraseña"
                  value={registerData.confirmPassword}
                  onChange={(event) =>
                    setRegisterData((previous) => ({
                      ...previous,
                      confirmPassword: event.target.value,
                    }))
                  }
                />
                {fieldErrors.confirmPassword ? (
                  <p className="field-error">{fieldErrors.confirmPassword}</p>
                ) : null}

                <button type="submit" disabled={loading}>
                  {loading ? 'Creando cuenta...' : 'Crear cuenta'}
                </button>
                {fieldErrors.confirmPassword ? (
                  <p className="field-error">{fieldErrors.confirmPassword}</p>
                ) : null}
              </form>

              <p className="mode-link">
                Ya tienes cuenta?{' '}
                <button type="button" onClick={() => switchMode('login')}>
                  Inicia sesion
                </button>
              </p>
            </>
          )}

          {globalError ? <p className="global-error">{globalError}</p> : null}
          {successMessage ? <p className="success-message">{successMessage}</p> : null}
        </section>

        <div className="bg-orb orb-a" aria-hidden="true" />
        <div className="bg-orb orb-b" aria-hidden="true" />
      </div>
    </main>
  )
}

export default App
