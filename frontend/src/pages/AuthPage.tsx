import type { Dispatch, FormEvent, SetStateAction } from 'react'
import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { AppLogo, PasswordStrengthMeter } from '../components/common'
import type { AuthMode, LoginData, RegisterData } from '../types'

type AuthPageProps = {
  mode: AuthMode
  switchMode: (newMode: AuthMode) => void
  onForgotPassword: () => void
  loginData: LoginData
  setLoginData: Dispatch<SetStateAction<LoginData>>
  registerData: RegisterData
  setRegisterData: Dispatch<SetStateAction<RegisterData>>
  passwordStrength: string | null
  fieldErrors: Record<string, string>
  globalError: string
  successMessage: string
  loading: boolean
  onLoginSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  onRegisterSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>
  goHome: () => void
}

export function AuthPage({
  mode,
  switchMode,
  onForgotPassword,
  loginData,
  setLoginData,
  registerData,
  setRegisterData,
  passwordStrength,
  fieldErrors,
  globalError,
  successMessage,
  loading,
  onLoginSubmit,
  onRegisterSubmit,
  goHome,
}: AuthPageProps) {
  const [showLoginPassword, setShowLoginPassword] = useState(false)
  const [showRegisterPassword, setShowRegisterPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  return (
    <main className="auth-page">
      <section className="auth-shell">
        <button type="button" className="back-link" onClick={goHome}>
          &larr; Volver al inicio
        </button>

        <section className="auth-card" aria-live="polite">
          <div className="auth-card__brand">
            <AppLogo />
          </div>

          {mode === 'login' ? (
            <>
              <h1>Iniciar sesión</h1>
              <p className="subtitle">Accede a tu cuenta de OptimaPC</p>

              <form className="auth-form" onSubmit={onLoginSubmit} noValidate>
                <label htmlFor="login-email">Correo electrónico</label>
                <input
                  id="login-email"
                  type="email"
                  placeholder="ejemplo@correo.com"
                  value={loginData.email}
                  onChange={(event) => {
                    const value = event.currentTarget.value
                    setLoginData((previous) => ({
                      ...previous,
                      email: value,
                    }))
                  }}
                />
                {fieldErrors.email ? <p className="field-error">{fieldErrors.email}</p> : null}

                <label htmlFor="login-password">Contraseña</label>
                <div className="password-wrapper">
                  <input
                    id="login-password"
                    type={showLoginPassword ? 'text' : 'password'}
                    placeholder="********"
                    value={loginData.password}
                    onChange={(event) => {
                      const value = event.currentTarget.value
                      setLoginData((previous) => ({
                        ...previous,
                        password: value,
                      }))
                    }}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowLoginPassword((p) => !p)}
                    aria-label={showLoginPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showLoginPassword ? <EyeOff /> : <Eye />}
                  </button>
                </div>
                {fieldErrors.password ? <p className="field-error">{fieldErrors.password}</p> : null}

                <button type="button" className="inline-link" onClick={onForgotPassword}>
                  ¿Olvidaste tu contraseña?
                </button>

                <button type="submit" disabled={loading}>
                  {loading ? 'Validando...' : 'Iniciar sesión'}
                </button>
              </form>

              <p className="mode-link">
                No tienes cuenta?{' '}
                <button type="button" onClick={() => switchMode('register')}>
                  Regístrate aquí
                </button>
              </p>
            </>
          ) : (
            <>
              <h1>Crear cuenta</h1>
              <p className="subtitle">Únete a OptimaPC hoy mismo</p>

              <form className="auth-form" onSubmit={onRegisterSubmit} noValidate>
                <label htmlFor="register-nombre">Nombre</label>
                <input
                  id="register-nombre"
                  type="text"
                  placeholder="Tu nombre"
                  value={registerData.nombre}
                  onChange={(event) => {
                    const value = event.currentTarget.value
                    setRegisterData((previous) => ({
                      ...previous,
                      nombre: value,
                    }))
                  }}
                />
                {fieldErrors.nombre ? <p className="field-error">{fieldErrors.nombre}</p> : null}

                <label htmlFor="register-apellidos">Apellidos</label>
                <input
                  id="register-apellidos"
                  type="text"
                  placeholder="Tus apellidos"
                  value={registerData.apellidos}
                  onChange={(event) => {
                    const value = event.currentTarget.value
                    setRegisterData((previous) => ({
                      ...previous,
                      apellidos: value,
                    }))
                  }}
                />
                {fieldErrors.apellidos ? <p className="field-error">{fieldErrors.apellidos}</p> : null}

                <label htmlFor="register-email">Correo electrónico</label>
                <input
                  id="register-email"
                  type="email"
                  placeholder="ejemplo@correo.com"
                  value={registerData.email}
                  onChange={(event) => {
                    const value = event.currentTarget.value
                    setRegisterData((previous) => ({
                      ...previous,
                      email: value,
                    }))
                  }}
                />
                {fieldErrors.email ? <p className="field-error">{fieldErrors.email}</p> : null}

                <label htmlFor="register-password">Contraseña</label>
                <div className="password-wrapper">
                  <input
                    id="register-password"
                    type={showRegisterPassword ? 'text' : 'password'}
                    placeholder="Mínimo 8 caracteres"
                    value={registerData.password}
                    onChange={(event) => {
                      const value = event.currentTarget.value
                      setRegisterData((previous) => ({
                        ...previous,
                        password: value,
                      }))
                    }}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowRegisterPassword((p) => !p)}
                    aria-label={showRegisterPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showRegisterPassword ? <EyeOff /> : <Eye />}
                  </button>
                </div>
                {passwordStrength ? <PasswordStrengthMeter strength={passwordStrength} /> : null}
                {fieldErrors.password ? <p className="field-error">{fieldErrors.password}</p> : null}

                <label htmlFor="register-confirm-password">Confirmar contraseña</label>
                <div className="password-wrapper">
                  <input
                    id="register-confirm-password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder="Repite tu contraseña"
                    value={registerData.confirmPassword}
                    onChange={(event) => {
                      const value = event.currentTarget.value
                      setRegisterData((previous) => ({
                        ...previous,
                        confirmPassword: value,
                      }))
                    }}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowConfirmPassword((p) => !p)}
                    aria-label={showConfirmPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  >
                    {showConfirmPassword ? <EyeOff /> : <Eye />}
                  </button>
                </div>
                {fieldErrors.confirmPassword ? <p className="field-error">{fieldErrors.confirmPassword}</p> : null}

                <button type="submit" disabled={loading}>
                  {loading ? 'Creando cuenta...' : 'Crear cuenta'}
                </button>
              </form>

              <p className="mode-link">
                Ya tienes cuenta?{' '}
                <button type="button" onClick={() => switchMode('login')}>
                  Inicia sesión
                </button>
              </p>
            </>
          )}

          {globalError ? <p className="global-error">{globalError}</p> : null}
          {successMessage ? <p className="success-message">{successMessage}</p> : null}
        </section>
      </section>
    </main>
  )
}
